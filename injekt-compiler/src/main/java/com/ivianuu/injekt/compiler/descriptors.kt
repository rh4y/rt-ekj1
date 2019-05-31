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

import com.ivianuu.injekt.FactoryKind
import com.ivianuu.injekt.SingleKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

data class MultiCreatorDescriptor(
    val multiCreatorName: ClassName,
    val creatorNames: Set<ClassName>
)

data class CreatorDescriptor(
    val target: ClassName,
    val creatorName: ClassName,
    val kind: Kind,
    val scope: ClassName?,
    val constructorParams: List<ParamDescriptor>
) {
    data class Kind(val impl: ClassName) {
        companion object {
            val Factory = Kind(FactoryKind::class.asClassName())
            val Single = Kind(SingleKind::class.asClassName())
        }
    }
}

data class ParamDescriptor(
    val kind: Kind,
    val paramName: String,
    val name: ClassName?,
    val paramIndex: Int
) {
    enum class Kind { VALUE, LAZY, PROVIDER }
}