/*
 * Copyright 2019 Manuel Wrage
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

import java.util.*

/**
 * A module is a collection of [Binding]s to drive [Component]s
 */
class Module @PublishedApi internal constructor() {

    internal val bindings = hashMapOf<Key, Binding<*>>()
    internal var mapBindings: MapBindings? = null
        private set
    internal var setBindings: SetBindings? = null
        private set

    fun <T> bind(
        binding: Binding<T>,
        key: Key,
        override: Boolean = false,
        unscoped: Boolean = true
    ): BindingContext<T> {
        if (key in bindings && !override) {
            error("Already declared binding for $binding.key")
        }

        binding.override = override
        binding.unscoped = unscoped

        bindings[key] = binding

        return BindingContext(binding, key, override, this)
    }

    fun include(module: Module) {
        module.bindings.forEach {
            bind(it.value, it.key, it.value.override, it.value.unscoped)
        }
        module.mapBindings?.let { nonNullMapBindings().putAll(it) }
        module.setBindings?.let { nonNullSetBindings().addAll(it) }
    }

    inline fun <K, V> map(
        mapKeyType: Type<K>,
        mapValueType: Type<V>,
        mapName: Any? = null,
        block: BindingMap<K, V>.() -> Unit = {}
    ) {
        val mapKey = keyOf(typeOf<Any?>(Map::class, mapKeyType, mapValueType), mapName)
        nonNullMapBindings().get<K, V>(mapKey).apply(block)
    }

    inline fun <E> set(
        setElementType: Type<E>,
        setName: Any? = null,
        block: BindingSet<E>.() -> Unit = {}
    ) {
        val setKey = keyOf(typeOf<Any?>(Set::class, setElementType), setName)
        nonNullSetBindings().get<E>(setKey).apply(block)
    }

    @PublishedApi
    internal fun nonNullMapBindings(): MapBindings =
        mapBindings ?: MapBindings().also { mapBindings = it }

    @PublishedApi
    internal fun nonNullSetBindings(): SetBindings =
        setBindings ?: SetBindings().also { setBindings = it }

}

inline fun module(block: Module.() -> Unit): Module = Module().apply(block)

inline fun <reified T> Module.bind(
    binding: Binding<T>,
    name: Any? = null,
    override: Boolean = false,
    unscoped: Boolean = true
): BindingContext<T> = this.bind(binding, typeOf<T>(), name, override, unscoped)

fun <T> Module.bind(
    binding: Binding<T>,
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    unscoped: Boolean = true
): BindingContext<T> = bind(binding, keyOf(type, name), override, unscoped)

inline fun <reified T> Module.factory(
    name: Any? = null,
    override: Boolean = false,
    optimizing: Boolean = true,
    noinline definition: Definition<T>
): BindingContext<T> = factory(typeOf(), name, override, optimizing, definition)

fun <T> Module.factory(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    optimizing: Boolean = true,
    definition: Definition<T>
): BindingContext<T> = bind(definitionBinding(optimizing, definition), type, name, override, true)

inline fun <reified T> Module.single(
    name: Any? = null,
    override: Boolean = false,
    noinline definition: Definition<T>
): BindingContext<T> = single(typeOf(), name, override, definition)

fun <T> Module.single(
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false,
    definition: Definition<T>
): BindingContext<T> =
    bind(definitionBinding(false, definition).asScoped(), type, name, override, false)

inline fun <reified K, reified V> Module.map(
    mapName: Any? = null,
    block: BindingMap<K, V>.() -> Unit = {}
) {
    map(typeOf<K>(), typeOf<V>(), mapName, block)
}

inline fun <reified E> Module.set(
    setName: Any? = null,
    block: BindingSet<E>.() -> Unit = {}
) {
    set(typeOf<E>(), setName, block)
}

inline fun <reified T> Module.withBinding(
    name: Any? = null,
    block: BindingContext<T>.() -> Unit
) {
    withBinding(typeOf(), name, block)
}

inline fun <T> Module.withBinding(
    type: Type<T>,
    name: Any? = null,
    block: BindingContext<T>.() -> Unit
) {
    bindProxy(type, name).block()
}

@PublishedApi
internal fun <T> Module.bindProxy(
    type: Type<T>,
    name: Any?
): BindingContext<T> {
    // we create a additional binding because we have no reference to the original one
    // we use a unique id here to make sure that the binding does not collide with any user config
    // this binding acts as proxy and just calls trough the original implementation
    return bind(UnlinkedProxyBinding(keyOf(type, name)), type, UUID.randomUUID().toString())
}

private class UnlinkedProxyBinding<T>(private val originalKey: Key) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> =
        linker.get(originalKey)
}

inline fun <reified T> Module.instance(
    instance: T,
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = instance(instance, typeOf(), name, override)

fun <T> Module.instance(
    instance: T,
    type: Type<T>,
    name: Any? = null,
    override: Boolean = false
): BindingContext<T> = bind(LinkedInstanceBinding(instance), type, name, override, false)