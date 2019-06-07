/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class BindingFactoryDescriptor(
    val target: ClassName,
    val factoryName: ClassName,
    val isInternal: Boolean,
    val scope: ClassName?,
    val constructorParams: List<ParamDescriptor>
) {
    val hasDependencies
        get() = constructorParams.any { it is ParamDescriptor.Dependency }
    val hasDynamicParams
        get() = constructorParams.any { it is ParamDescriptor.Dynamic }
}

sealed class ParamDescriptor {
    abstract val paramName: String

    data class Dynamic(
        override val paramName: String,
        val index: Int
    ) : ParamDescriptor()

    data class Dependency(
        override val paramName: String,
        val paramType: TypeName,
        val qualifierName: ClassName?
    ) : ParamDescriptor()
}