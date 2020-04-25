package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class KeyOfTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

    private val key = getClass(InjektFqNames.Key)

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        val callee = expression.symbol.ensureBound().owner

        if ((callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.keyOf" &&
                    callee.fqNameForIrSerialization.asString() != "com.ivianuu.injekt.KeyKt.keyOf") ||
            callee.valueParameters.size != 1 ||
            !callee.isInline
        ) return expression

        val type = expression.getTypeArgument(0)!!
        if (!type.isFullyResolved()) return expression

        return DeclarationIrBuilder(pluginContext, expression.symbol)
            .irKeyOf(type, expression.getValueArgument(0))
    }

    private fun IrBuilderWithScope.irKeyOf(
        type: IrType,
        qualifier: IrExpression? = null
    ): IrExpression {
        val kotlinType = type.toKotlinType()

        return if (kotlinType.arguments.isNotEmpty()) {
            val parameterizedKey = key.sealedSubclasses
                .single { it.name.asString() == "ParameterizedKey" }

            irCall(
                symbolTable.referenceConstructor(parameterizedKey.constructors.first {
                    it.valueParameters.size == 4
                }),
                KotlinTypeFactory.simpleType(
                    baseType = parameterizedKey.defaultType,
                    arguments = listOf(type.toKotlinType().asTypeProjection())
                ).toIrType()
            ).apply {
                putTypeArgument(0, type)

                putValueArgument(
                    0,
                    IrClassReferenceImpl(
                        startOffset,
                        endOffset,
                        pluginContext.irBuiltIns.kClassClass.typeWith(type),
                        type.classifierOrFail,
                        type
                    )
                )

                putValueArgument(1, qualifier ?: irNull())

                putValueArgument(2, irBoolean(type.isMarkedNullable()))

                pluginContext.irBuiltIns.arrayClass
                    .typeWith(getTypeArgument(0)!!)
                val argumentsType = pluginContext.irBuiltIns.arrayClass
                    .typeWith(symbolTable.referenceClass(key).starProjectedType)

                putValueArgument(
                    3,
                    irCall(
                        pluginContext.symbols.arrayOf,
                        argumentsType
                    ).apply {
                        putValueArgument(
                            0,
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                argumentsType,
                                argumentsType,
                                type.toKotlinType().arguments
                                    .map { irKeyOf(it.type.toIrType()) }
                            )
                        )
                    }
                )
            }
        } else {
            val simpleKey = key.sealedSubclasses
                .single { it.name.asString() == "SimpleKey" }

            irCall(
                symbolTable.referenceConstructor(simpleKey.constructors.first {
                    it.valueParameters.size == 3
                }),
                KotlinTypeFactory.simpleType(
                    baseType = simpleKey.defaultType,
                    arguments = listOf(type.toKotlinType().asTypeProjection())
                ).toIrType()
            ).apply {
                putTypeArgument(0, type)

                putValueArgument(
                    0,
                    IrClassReferenceImpl(
                        startOffset,
                        endOffset,
                        pluginContext.irBuiltIns.kClassClass.typeWith(type),
                        type.classifierOrFail,
                        type
                    )
                )

                putValueArgument(1, qualifier ?: irNull())

                putValueArgument(2, irBoolean(type.isMarkedNullable()))
            }
        }
    }
}
