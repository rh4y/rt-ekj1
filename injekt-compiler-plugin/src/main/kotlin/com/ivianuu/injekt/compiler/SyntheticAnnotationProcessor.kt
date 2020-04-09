package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.declarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class SyntheticAnnotationProcessor : ElementProcessor {

    override fun processFile(
        file: KtFile,
        bindingTrace: BindingTrace,
        generateFile: (FileSpec) -> Unit
    ) {
        val syntheticAnnotationDeclarations = mutableListOf<DeclarationDescriptor>()
        file.accept(
            declarationRecursiveVisitor { declaration ->
                when (declaration) {
                    is KtFunction -> {
                        val descriptor =
                            bindingTrace[BindingContext.FUNCTION, declaration] as? FunctionDescriptor
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.hasAnnotatedAnnotations(InjektClassNames.SyntheticAnnotationMarker)) {
                            syntheticAnnotationDeclarations += descriptor
                        }
                    }
                    is KtProperty -> {
                        val descriptor =
                            bindingTrace[BindingContext.VARIABLE, declaration] as? PropertyDescriptor
                                ?: return@declarationRecursiveVisitor

                        if (descriptor.hasAnnotatedAnnotations(InjektClassNames.SyntheticAnnotationMarker)) {
                            syntheticAnnotationDeclarations += descriptor
                        }
                    }
                }
            }
        )

        syntheticAnnotationDeclarations.forEach { declaration ->
            FileSpec.builder(
                    file.packageFqName.child(Name.identifier("synthetic")).asString(),
                    declaration.name.asString().capitalize()
                )
                .addType(
                    TypeSpec.annotationBuilder(declaration.name.asString().capitalize())
                        .apply {
                            if ((declaration as MemberDescriptor).visibility == Visibilities.INTERNAL) {
                                addModifiers(KModifier.INTERNAL)
                            }
                        }
                        .addKdoc("Annotation for [${declaration.fqNameSafe.asString()}]")
                        .addAnnotation(
                            declaration.module.findClassAcrossModuleDependencies(
                                ClassId.topLevel(InjektClassNames.SyntheticAnnotation)
                            )!!.asClassName()!!
                        )
                        .apply {
                            if (declaration is FunctionDescriptor) {
                                addTypeVariables(
                                    declaration.typeParameters.map { typeParameter ->
                                        TypeVariableName(
                                            typeParameter.name.asString(),
                                            typeParameter.upperBounds
                                                .map { it.asTypeName()!! }
                                        )
                                    }
                                )

                                addProperties(
                                    declaration.valueParameters.map { valueParameter ->
                                        PropertySpec.builder(
                                                valueParameter.name.asString(),
                                                valueParameter.type.asTypeName()!!
                                            )
                                            .initializer(valueParameter.name.asString())
                                            .build()
                                    }
                                )
                                primaryConstructor(
                                    FunSpec.constructorBuilder()
                                        .addParameters(
                                            declaration.valueParameters.map { valueParameter ->
                                                ParameterSpec.builder(
                                                        valueParameter.name.asString(),
                                                        valueParameter.type.asTypeName()!!
                                                    )
                                                    .apply {
                                                        if (valueParameter.declaresDefaultValue()) {
                                                            defaultValue((valueParameter.findPsi() as KtParameter).defaultValue!!.text)
                                                        }
                                                    }
                                                    .build()
                                            }
                                        )
                                        .build()
                                )
                            }
                        }
                        .build()
                )
                .build()
                .let(generateFile)
        }
    }

}