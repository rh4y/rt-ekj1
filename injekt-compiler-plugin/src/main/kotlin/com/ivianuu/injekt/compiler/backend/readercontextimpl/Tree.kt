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

package com.ivianuu.injekt.compiler.backend.readercontextimpl

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.backend.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.backend.isTypeParameter
import com.ivianuu.injekt.compiler.backend.typeArguments
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

sealed class Given(
    val key: Key,
    val owner: IrClass,
    val origin: FqName?,
    val external: Boolean,
    val targetContext: IrClass?,
    val givenSetAccessExpression: ContextExpression?
) {
    abstract val contexts: List<IrType>
}

class GivenSelfContext(
    key: Key,
    val context: IrClass
) : Given(key, context, null, false, null, null) {
    override val contexts: List<IrType>
        get() = emptyList()
}

class GivenChildContext(
    key: Key,
    owner: IrClass,
    origin: FqName?,
    private val generator: ReaderContextFactoryImplGenerator
) : Given(key, owner, origin, false, null, null) {
    override val contexts: List<IrType>
        get() = emptyList()
    val factory by unsafeLazy {
        generator.generateFactory()
            .also { owner.addChild(it) }
    }
}

class GivenCalleeContext(
    key: Key,
    owner: IrClass,
    origin: FqName?,
    val lazyContextImpl: () -> IrClass?,
    val lazyContexts: () -> List<IrType>
) : Given(key, owner, origin, false, null, null) {
    val contextImpl by lazy(lazyContextImpl)
    override val contexts by unsafeLazy {
        contextImpl
        lazyContexts()
    }
}

class GivenFunction(
    key: Key,
    owner: IrClass,
    override val contexts: List<IrType>,
    origin: FqName?,
    external: Boolean,
    targetContext: IrClass?,
    givenSetAccessExpression: ContextExpression?,
    val explicitParameters: List<IrValueParameter>,
    val function: IrFunction
) : Given(key, owner, origin, external, targetContext, givenSetAccessExpression)

class GivenInstance(
    val inputField: IrField,
    owner: IrClass
) : Given(
    inputField.type.asKey(),
    owner,
    inputField.descriptor.fqNameSafe,
    false,
    null,
    null
) {
    override val contexts: List<IrType>
        get() = emptyList()
}

class GivenMap(
    key: Key,
    owner: IrClass,
    override val contexts: List<IrType>,
    givenSetAccessExpression: ContextExpression?,
    val functions: List<IrFunction>
) : Given(
    key,
    owner,
    null,
    false,
    null,
    givenSetAccessExpression
)

class GivenSet(
    key: Key,
    owner: IrClass,
    override val contexts: List<IrType>,
    givenSetAccessExpression: ContextExpression?,
    val functions: List<IrFunction>
) : Given(
    key,
    owner,
    null,
    false,
    null,
    givenSetAccessExpression
)

class GivenNull(
    key: Key,
    owner: IrClass
) : Given(
    key,
    owner,
    null,
    true,
    null,
    null
) {
    override val contexts: List<IrType>
        get() = emptyList()
}

fun IrType.asKey(): Key =
    Key(this)

class Key(val type: IrType) {

    init {
        check(type !is IrErrorType) {
            "Cannot be error type ${type.render()}"
        }
        check(!type.isTypeParameter()) {
            "Must be concrete type ${type.render()}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Key
        return compareTypeWithDistinct(type, other.type)
    }

    override fun hashCode(): Int = type.hashWithDistinct()

    override fun toString(): String {
        return when (val distinctedType = type.typeOrTypeAlias) {
            is IrTypeAliasSymbol -> {
                buildString {
                    append(distinctedType.descriptor.fqNameSafe.asString())
                    if (type.typeArguments.isNotEmpty()) {
                        type.typeArguments.joinToString(
                            prefix = "<",
                            postfix = ">",
                            separator = ", "
                        ) {
                            it.render()
                        }
                    }
                    if (type.isMarkedNullable()) append("?")
                }
            }
            else -> type.render()
        }.toString()
    }

    private val IrType.typeOrTypeAlias: Any
        get() = (this as? IrSimpleType)?.abbreviation
            ?.typeAlias
            ?: this

    private fun compareTypeWithDistinct(
        a: IrType?,
        b: IrType?
    ): Boolean = a?.hashWithDistinct() == b?.hashWithDistinct()

    private fun IrType.hashWithDistinct(): Int {
        var result = 0
        val distinctedType = typeOrTypeAlias
        if (distinctedType is IrSimpleType) {
            result += 31 * distinctedType.classifier.hashCode()
            result += 31 * distinctedType.arguments.map { it.typeOrNull?.hashWithDistinct() ?: 0 }
                .hashCode()
        } else {
            result += 31 * distinctedType.hashCode()
        }

        val qualifier = getConstantFromAnnotationOrNull<String>(InjektFqNames.Qualifier, 0)
        if (qualifier != null) result += 31 * qualifier.hashCode()

        return result
    }

}