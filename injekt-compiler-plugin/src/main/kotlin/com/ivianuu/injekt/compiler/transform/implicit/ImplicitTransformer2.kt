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

package com.ivianuu.injekt.compiler.transform.implicit

import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ImplicitTransformer2(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val scopeStack = mutableListOf<Scope>()

    private inline fun <R> inScope(
        scope: Scope,
        block: () -> R
    ): R {
        scopeStack.push(scope)
        val result = block()
        scopeStack.pop()
        return result
    }

    override fun lower() {
        module.transformChildrenVoid(ImplicitTransformer())
    }

    private inner class ImplicitTransformer : IrElementTransformerVoid() {

        override fun visitClass(declaration: IrClass): IrStatement {
            return inScope(
                Scope.Class(
                    declaration
                )
            ) {
                super.visitClass(declaration)
            }
        }

        override fun visitFunction(declaration: IrFunction): IrStatement {
            return super.visitFunction(declaration)
        }

    }

    private sealed class Scope {

        class Class(val declaration: IrClass) : Scope() {

        }

        class Function(val declaration: IrFunction) : Scope() {

        }

    }

}
