package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.contextNameOf
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.joinedNameOf
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

@Given
class EffectGenerator : Generator {

    private val declarationStore = given<DeclarationStore>()
    private val indexer = given<Indexer>()
    private val readerContextGenerator = given<ReaderContextGenerator>()

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitClass(klass: KtClass) {
                        super.visitClass(klass)
                        val descriptor = klass.descriptor<ClassDescriptor>()
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.Effect)) {
                            generateEffectsForDeclaration(descriptor)
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        super.visitNamedFunction(function)
                        val descriptor = function.descriptor<FunctionDescriptor>()
                        if (descriptor.hasAnnotatedAnnotations(InjektFqNames.Effect)) {
                            generateEffectsForDeclaration(descriptor)
                        }
                    }
                }
            )
        }
    }

    private fun generateEffectsForDeclaration(declaration: DeclarationDescriptor) {
        val effects = declaration
            .getAnnotatedAnnotations(InjektFqNames.Effect)
            .map { it.type.constructor.declarationDescriptor as ClassDescriptor }

        val packageName = declaration.findPackage().fqName
        val effectsName = joinedNameOf(
            packageName,
            FqName("${declaration.fqNameSafe.asString()}${declaration.uniqueKey()}Effects")
        )

        val rawGivenType = declaration.getGivenType()
        val aliasedType = SimpleTypeRef(
            classifier = ClassifierRef(
                fqName = packageName.child("${effectsName}Alias".asNameId())
            )
        )

        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package $packageName")
            emitLine("import ${InjektFqNames.Given}")
            emitLine("import ${declaration.fqNameSafe}")
            emitLine()
            emitLine("typealias ${aliasedType.classifier.fqName.shortName()} = ${rawGivenType.render()}")
            emitLine()
            emit("object $effectsName ")
            braced {
                emit("@Given fun aliasedGiven(): ${aliasedType.render()} ")
                braced {
                    if (declaration is FunctionDescriptor) {
                        emit("return { ")
                        val valueParameters = rawGivenType.typeArguments
                            .dropLast(1)
                        valueParameters.forEachIndexed { index, _ ->
                            emit("p$index")
                            if (index != valueParameters.lastIndex) emit(", ") else emit(" -> ")
                        }
                        indented {
                            emit("${declaration.name}(")
                            valueParameters.indices.forEach { index ->
                                emit("p$index")
                                if (index != valueParameters.lastIndex) emit(", ")
                            }
                            emit(")")
                        }
                        emitLine(" }")
                    } else {
                        declaration as ClassDescriptor
                        val constructorCallable =
                            declaration.getReaderConstructor(given())!!.toCallableRef()
                        if (constructorCallable.valueParameters.isNotEmpty()) {
                            emit("return { ")
                            val valueParameters = rawGivenType.typeArguments
                                .dropLast(1)
                            valueParameters.forEachIndexed { index, _ ->
                                emit("p$index")
                                if (index != valueParameters.lastIndex) emit(", ") else emit(" -> ")
                            }
                            indented {
                                emit("${declaration.name}(")
                                valueParameters.indices.forEach { index ->
                                    emit("p$index")
                                    if (index != valueParameters.lastIndex) emit(", ")
                                }
                                emit(")")
                            }
                            emitLine(" }")
                        } else {
                            emit("return ${rawGivenType.classifier.fqName}()")
                        }
                    }
                }
                indexer.index(
                    givensPathOf(aliasedType),
                    packageName.child(effectsName)
                        .child("aliasedGiven".asNameId()),
                    "function"
                )
                val aliasedGivenFqName =
                    packageName.child(effectsName).child("aliasedGiven".asNameId())
                val aliasedGivenUniqueKey = uniqueFunctionKeyOf(
                    fqName = aliasedGivenFqName,
                    visibility = Visibilities.PUBLIC,
                    startOffset = null,
                    parameterTypes = listOf(
                        packageName.child(
                            effectsName
                        )
                    )
                )
                readerContextGenerator.addPromisedReaderContextDescriptor(
                    PromisedReaderContextDescriptor(
                        type = SimpleTypeRef(
                            classifier = ClassifierRef(
                                packageName.child(
                                    contextNameOf(
                                        packageFqName = packageName,
                                        fqName = aliasedGivenFqName,
                                        uniqueKey = aliasedGivenUniqueKey
                                    )
                                )
                            ),
                            isContext = true
                        ),
                        callee = declaration,
                        calleeTypeArguments = emptyList(),
                        origin = aliasedGivenFqName
                    )
                )

                declarationStore.addInternalGiven(
                    CallableRef(
                        packageFqName = packageName,
                        fqName = aliasedGivenFqName,
                        name = aliasedGivenFqName.shortName(),
                        uniqueKey = aliasedGivenUniqueKey,
                        type = aliasedType,
                        staticReceiver = null,
                        typeParameters = emptyList(),
                        valueParameters = emptyList(),
                        targetContext = null,
                        givenKind = CallableRef.GivenKind.GIVEN,
                        isExternal = false,
                        isPropertyAccessor = false
                    )
                )
                effects
                    .map { it.companionObjectDescriptor!! }
                    .flatMap { effectGivenSet ->
                        effectGivenSet.unsubstitutedMemberScope
                            .getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                            .filterIsInstance<FunctionDescriptor>()
                            .filter {
                                it.hasAnnotation(InjektFqNames.Given) ||
                                        it.hasAnnotation(InjektFqNames.GivenMapEntries) ||
                                        it.hasAnnotation(InjektFqNames.GivenSetElements)
                            }
                    }
                    .map { effectFunction ->
                        val name = joinedNameOf(
                            effectFunction.findPackage().fqName,
                            effectFunction.fqNameSafe
                        )
                        val effectCallableRef = effectFunction.toCallableRef()

                        val returnType = effectCallableRef.type
                            .substitute(
                                effectCallableRef.typeParameters
                                    .zip(
                                        if (effectCallableRef.typeParameters.size == 1)
                                            listOf(aliasedType)
                                        else listOf(aliasedType, rawGivenType)
                                    )
                                    .toMap()
                            )

                        when (effectCallableRef.givenKind) {
                            CallableRef.GivenKind.GIVEN -> {
                                emit("@Given")
                                if (effectCallableRef.targetContext != null) {
                                    emitLine("(${effectCallableRef.targetContext.classifier.fqName}::class)")
                                } else {
                                    emitLine()
                                }
                            }
                            CallableRef.GivenKind.GIVEN_MAP_ENTRIES ->
                                emitLine("@com.ivianuu.injekt.GivenMapEntries")
                            CallableRef.GivenKind.GIVEN_SET_ELEMENTS ->
                                emitLine("@com.ivianuu.injekt.GivenSetElements")
                            CallableRef.GivenKind.GIVEN_SET -> {
                                // todo
                            }
                        }.let {}

                        emit("fun $name(): ${returnType.render()} ")
                        braced {
                            emit("return ${effectFunction.fqNameSafe}<${aliasedType.render()}")
                            if (effectCallableRef.typeParameters.size == 2) emit(", ${rawGivenType.render()}")
                            emit(">(")
                            effectFunction.valueParameters.indices.forEach { index ->
                                emit("p$index")
                                if (index != effectFunction.valueParameters.lastIndex) emit(", ")
                            }
                            emitLine(")")
                        }
                        indexer.index(
                            when (effectCallableRef.givenKind) {
                                CallableRef.GivenKind.GIVEN ->
                                    givensPathOf(returnType)
                                CallableRef.GivenKind.GIVEN_MAP_ENTRIES ->
                                    givenMapEntriesPathOf(returnType)
                                CallableRef.GivenKind.GIVEN_SET_ELEMENTS ->
                                    givenSetElementsPathOf(returnType)
                                CallableRef.GivenKind.GIVEN_SET -> error("Unexpected given kind")
                            },
                            packageName.child(effectsName)
                                .child(name),
                            "function"
                        )
                        val effectFunctionFqName = packageName.child(effectsName).child(name)
                        val effectFunctionUniqueKey = uniqueFunctionKeyOf(
                            fqName = effectFunctionFqName,
                            visibility = Visibilities.PUBLIC,
                            startOffset = null,
                            parameterTypes = listOf(
                                packageName.child(
                                    effectsName
                                )
                            )
                        )
                        readerContextGenerator.addPromisedReaderContextDescriptor(
                            PromisedReaderContextDescriptor(
                                type = SimpleTypeRef(
                                    classifier = ClassifierRef(
                                        packageName.child(
                                            contextNameOf(
                                                packageFqName = packageName,
                                                fqName = effectFunctionFqName,
                                                uniqueKey = effectFunctionUniqueKey
                                            )
                                        )
                                    ),
                                    isContext = true
                                ),
                                callee = effectFunction,
                                calleeTypeArguments = if (effectFunction.typeParameters.size == 1)
                                    listOf(aliasedType) else listOf(aliasedType, rawGivenType),
                                origin = effectFunctionFqName
                            )
                        )
                        declarationStore.addInternalGiven(
                            CallableRef(
                                packageFqName = packageName,
                                fqName = effectFunctionFqName,
                                name = effectFunctionFqName.shortName(),
                                uniqueKey = effectFunctionUniqueKey,
                                type = returnType,
                                staticReceiver = null,
                                typeParameters = emptyList(),
                                valueParameters = emptyList(),
                                targetContext = effectCallableRef.targetContext,
                                givenKind = effectCallableRef.givenKind,
                                isExternal = false,
                                isPropertyAccessor = false
                            )
                        )
                    }
            }
        }

        generateFile(
            packageFqName = declaration.findPackage().fqName,
            fileName = "$effectsName.kt",
            code = code
        )
    }

    private fun DeclarationDescriptor.getGivenType(): TypeRef {
        return when (this) {
            is ClassDescriptor -> {
                getReaderConstructor(given())!!.toCallableRef().type
            }
            is FunctionDescriptor -> {
                if (hasAnnotation(InjektFqNames.Given)) {
                    returnType!!.toTypeRef()
                } else {
                    val parametersSize = valueParameters.size
                    (if (isSuspend) moduleDescriptor.builtIns.getSuspendFunction(parametersSize)
                    else moduleDescriptor.builtIns.getFunction(parametersSize))
                        .defaultType
                        .replace(
                            newArguments = (valueParameters
                                .take(parametersSize)
                                .map { it.type } + returnType!!)
                                .map { it.asTypeProjection() }
                        )
                        .toTypeRef()
                        .copy(
                            isComposable = hasAnnotation(InjektFqNames.Composable),
                            isReader = hasAnnotation(InjektFqNames.Reader)
                        )
                }
            }
            is PropertyDescriptor -> getter!!.toCallableRef().type
            else -> error("Unexpected given declaration $this")
        }
    }
}
