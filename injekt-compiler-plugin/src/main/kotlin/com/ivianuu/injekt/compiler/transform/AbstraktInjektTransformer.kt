package com.ivianuu.injekt.compiler.transform

/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.classOrFail
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.withQualifiers
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi2ir.findSingleFunction

abstract class AbstractInjektTransformer(
    val context: IrPluginContext
) : IrElementTransformerVoid(), ModuleLoweringPass {

    val symbols = InjektSymbols(context)

    val irProviders = context.irProviders
    protected val symbolTable = context.symbolTable
    val irBuiltIns = context.irBuiltIns
    protected val builtIns = context.builtIns
    protected val typeTranslator = context.typeTranslator

    lateinit var moduleFragment: IrModuleFragment

    override fun lower(module: IrModuleFragment) {
        moduleFragment = module
        visitModuleFragment(module, null)
        module.patchDeclarationParents()
    }

    fun IrBuilderWithScope.irInjektIntrinsicUnit(): IrExpression {
        return irCall(
            symbolTable.referenceFunction(
                symbols.internalPackage.memberScope
                    .findSingleFunction(Name.identifier("injektIntrinsic"))
            ),
            this@AbstractInjektTransformer.context.irBuiltIns.unitType
        )
    }

    fun IrBuilderWithScope.noArgSingleConstructorCall(clazz: IrClassSymbol): IrConstructorCall =
        irCall(clazz.constructors.single())

    fun IrBuilderWithScope.fieldPathAnnotation(field: IrField): IrConstructorCall =
        irCall(symbols.astFieldPath.constructors.single()).apply {
            putValueArgument(0, irString(field.name.asString()))
        }

    fun IrBuilderWithScope.classPathAnnotation(clazz: IrClass): IrConstructorCall =
        irCall(symbols.astClassPath.constructors.single()).apply {
            putValueArgument(
                0,
                IrClassReferenceImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.kClassClass.typeWith(clazz.defaultType),
                    clazz.symbol,
                    clazz.defaultType
                )
            )
        }

    fun IrBlockBodyBuilder.initializeClassWithAnySuperClass(symbol: IrClassSymbol) {
        +IrDelegatingConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            irBuiltIns.unitType,
            symbolTable.referenceConstructor(
                builtIns.any
                    .unsubstitutedPrimaryConstructor!!
            )
        )
        +IrInstanceInitializerCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            irBuiltIns.unitType
        )
    }

    fun IrBuilderWithScope.irMapKeyConstructorForKey(
        expression: IrExpression
    ): IrConstructorCall {
        return when (expression) {
            is IrClassReference -> {
                irCall(symbols.astMapClassKey.constructors.single())
                    .apply {
                        putValueArgument(0, expression.deepCopyWithVariables())
                    }
            }
            is IrConst<*> -> {
                when (expression.kind) {
                    is IrConstKind.Int -> irCall(symbols.astMapIntKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    is IrConstKind.Long -> irCall(symbols.astMapLongKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    is IrConstKind.String -> irCall(symbols.astMapStringKey.constructors.single())
                        .apply {
                            putValueArgument(0, expression.deepCopyWithVariables())
                        }
                    else -> error("Unexpected expression ${expression.dump()}")
                }
            }
            else -> error("Unexpected expression ${expression.dump()}")
        }
    }

    data class ProviderParameter(
        val name: String,
        val type: IrType,
        val assisted: Boolean
    )

    fun IrBuilderWithScope.provider(
        name: Name,
        parameters: List<ProviderParameter>,
        returnType: IrType,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ): IrClass {
        val (assistedParameters, nonAssistedParameters) = parameters
            .partition { it.assisted }
        return buildClass {
            kind = if (nonAssistedParameters.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
            this.name = name
        }.apply clazz@{
            val superType = symbols.getFunction(assistedParameters.size)
                .typeWith(
                    assistedParameters
                        .map { it.type } + returnType
                )
            superTypes += superType

            (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val fieldsByNonAssistedParameter = nonAssistedParameters
                .toList()
                .associateWith { (name, type) ->
                    addField(
                        name,
                        symbols.getFunction(0)
                            .typeWith(type)
                            .withQualifiers(symbols, listOf(InjektFqNames.Provider))
                    )
                }

            addConstructor {
                this.returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                fieldsByNonAssistedParameter.forEach { (_, field) ->
                    addValueParameter(
                        field.name.asString(),
                        field.type
                    )
                }

                body = irBlockBody {
                    initializeClassWithAnySuperClass(this@clazz.symbol)
                    valueParameters
                        .forEach { valueParameter ->
                            +irSetField(
                                irGet(thisReceiver!!),
                                fieldsByNonAssistedParameter.values.toList()[valueParameter.index],
                                irGet(valueParameter)
                            )
                        }
                }
            }

            val companion = if (nonAssistedParameters.isNotEmpty()) {
                providerCompanion(parameters, returnType, createBody).also { addChild(it) }
            } else null

            val createFunction = if (nonAssistedParameters.isEmpty()) {
                providerCreateFunction(parameters, returnType, this, createBody)
            } else {
                null
            }

            addFunction {
                this.name = Name.identifier("invoke")
                this.returnType = returnType
                visibility = Visibilities.PUBLIC
            }.apply func@{
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

                overriddenSymbols += superType.classOrFail
                    .ensureBound(this@AbstractInjektTransformer.context.irProviders)
                    .owner
                    .functions
                    .single { it.name.asString() == "invoke" }
                    .symbol

                val valueParametersByAssistedParameter = assistedParameters.associateWith {
                    addValueParameter(
                        it.name,
                        it.type
                    )
                }

                body = irExprBody(
                    irCall(companion?.functions?.single() ?: createFunction!!).apply {
                        dispatchReceiver =
                            if (companion != null) irGetObject(companion.symbol) else irGet(
                                dispatchReceiverParameter!!
                            )

                        parameters.forEachIndexed { index, parameter ->
                            putValueArgument(
                                index,
                                if (parameter in assistedParameters) {
                                    irGet(valueParametersByAssistedParameter.getValue(parameter))
                                } else {
                                    irCall(
                                        symbols.getFunction(0)
                                            .functions
                                            .single { it.owner.name.asString() == "invoke" })
                                        .apply {
                                            dispatchReceiver = irGetField(
                                                irGet(dispatchReceiverParameter!!),
                                                fieldsByNonAssistedParameter.getValue(parameter)
                                            )
                                        }
                                }
                            )
                        }
                    }
                )
            }
        }
    }

    private fun IrBuilderWithScope.providerCompanion(
        parameters: List<ProviderParameter>,
        returnType: IrType,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ) = buildClass {
        kind = ClassKind.OBJECT
        name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        isCompanion = true
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

        addConstructor {
            this.returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)
            }
        }

        providerCreateFunction(parameters, returnType, this, createBody)
    }

    private fun IrBuilderWithScope.providerCreateFunction(
        parameters: List<ProviderParameter>,
        returnType: IrType,
        owner: IrClass,
        createBody: IrBuilderWithScope.(IrFunction) -> IrBody
    ): IrFunction {
        return owner.addFunction {
            name = Name.identifier("create")
            this.returnType = returnType
            visibility = Visibilities.PUBLIC
            isInline = true
        }.apply {
            dispatchReceiverParameter = owner.thisReceiver?.copyTo(this, type = owner.defaultType)

            parameters
                .forEach { (name, type) ->
                    addValueParameter(
                        name,
                        type
                    )
                }

            body = createBody(this@providerCreateFunction, this)
        }
    }
}
