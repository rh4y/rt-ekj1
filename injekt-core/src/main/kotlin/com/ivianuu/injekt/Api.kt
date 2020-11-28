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

package com.ivianuu.injekt

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Component

@Target(AnnotationTarget.CLASS)
annotation class ChildComponent

fun <T> component(vararg inputs: Any?): T = error("Intrinsic")

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Module

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)
annotation class Binding

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)
annotation class Bound(
    val component: KClass<*> = Nothing::class
)

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)
annotation class Scoped(val component: KClass<*> = Nothing::class)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Qualifier

@Target(AnnotationTarget.FUNCTION)
annotation class FunBinding

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class FunApi

@Target(AnnotationTarget.CLASS)
annotation class ImplBinding

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class MapEntries

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class SetElements

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Decorator

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Effect

@Qualifier
@Target(AnnotationTarget.TYPE)
annotation class ForEffect

@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER)
annotation class Arg(val name: String)

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)
annotation class Eager

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY
)
annotation class Default
