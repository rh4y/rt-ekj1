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

package com.ivianuu.injekt

data class BindingContribution<T> internal constructor(
    val binding: Binding<T>,
    val key: Key,
    val override: Boolean
)

data class SetContribution<E> internal constructor(
    val binding: Binding<out E>,
    val override: Boolean
)

/**
 * A module is a collection of [Binding]s to drive [Component]s
 */
interface Module {
    val bindings: Map<Key, BindingContribution<*>> get() = emptyMap()
    val mapBindings: MapBindings?
    val setBindings: Map<Key, Set<SetContribution<*>>> get() = emptyMap()
}

internal class DefaultModule(
    override val bindings: Map<Key, BindingContribution<*>>,
    override val mapBindings: MapBindings?,
    override val setBindings: Map<Key, Set<SetContribution<*>>> = emptyMap()
) : Module

