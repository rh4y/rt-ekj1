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

import com.ivianuu.injekt.internal.DeclarationName
import com.ivianuu.injekt.internal.SyntheticAnnotationMarker
import com.ivianuu.injekt.internal.declarationName

/**
 * Scopes are used to name [Component]s
 * This allows annotation api [Binding]s to be associated with a specific [Component]
 *
 * A scope can be declared like this
 *
 * ´´´
 * @ScopeMarker
 * val ActivityScope = Scope()
 * ´´´
 *
 * The following code ensures that the view model will be only instantiated in the activity scoped [Component]
 *
 * ´´´
 * @ActivityScope
 * @Factory
 * class MyViewModel
 * ´´´
 *
 * @see ComponentBuilder.scopes
 */
interface Scope : Qualifier.Element

/**
 * Returns a scope which uses [name] for comparisons
 */
fun Scope(@DeclarationName name: Any = declarationName()): Scope = DefaultScope(name)

/**
 * Marker for [Scope]s
 *
 * @see Scope
 */
@SyntheticAnnotationMarker
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ScopeMarker

/**
 * Applies the [Module] to every [ComponentBuilder]
 */
val AnyScope = Scope()

private data class DefaultScope(val name: Any) : Scope