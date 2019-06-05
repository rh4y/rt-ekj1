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

import kotlin.reflect.KClass

data class BindingContext<T>(
    val binding: Binding<T>,
    val key: Key,
    val override: Boolean,
    val moduleBuilder: ModuleBuilder
)

inline fun <reified T> BindingContext<*>.bindAlias(
    name: Qualifier? = null,
    override: Boolean = false
) {
    bindAlias(typeOf<T>(), name, override)
}

fun BindingContext<*>.bindAlias(
    type: Type<*>,
    name: Qualifier? = null,
    override: Boolean = false
) {
    moduleBuilder.add(binding as Binding<Any?>, type as Type<Any?>, name, override)
}

inline fun <reified T> BindingContext<*>.bindType() {
    bindAlias(typeOf<T>())
}

infix fun <T> BindingContext<T>.bindType(type: Type<*>): BindingContext<T> {
    bindAlias(type)
    return this
}

fun <T> BindingContext<T>.bindTypes(vararg types: Type<*>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

infix fun <T> BindingContext<T>.bindTypes(types: Iterable<Type<*>>): BindingContext<T> {
    types.forEach { bindType(it) }
    return this
}

inline fun <reified T> BindingContext<*>.bindClass() {
    bindClass(T::class)
}

infix fun <T> BindingContext<T>.bindClass(clazz: KClass<*>): BindingContext<T> {
    bindAlias(typeOf<Any?>(clazz))
    return this
}

fun <T> BindingContext<T>.bindClasses(vararg classes: KClass<*>): BindingContext<T> {
    classes.forEach { bindClass(it) }
    return this
}

infix fun <T> BindingContext<T>.bindClasses(classes: Iterable<KClass<*>>): BindingContext<T> {
    classes.forEach { bindClass(it) }
    return this
}

infix fun <T> BindingContext<T>.bindName(name: Qualifier): BindingContext<T> {
    bindAlias(key.type, name)
    return this
}

fun <T> BindingContext<T>.bindNames(vararg names: Qualifier): BindingContext<T> {
    names.forEach { bindName(it) }
    return this
}

infix fun <T> BindingContext<T>.bindNames(names: Iterable<Qualifier>): BindingContext<T> {
    names.forEach { bindName(it) }
    return this
}