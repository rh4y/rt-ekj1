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

import java.util.concurrent.ConcurrentHashMap

/**
 * Returns the same instance for the given parameters
 *
 * ´´´
 * val component = Component {
 *     multi { (url: String) -> Api(url = url) }
 * }
 *
 * val googleApi1 = component.get<Api>(parameters = parametersOf("www.google.com"))
 * val googleApi2 = component.get<Api>(parameters = parametersOf("www.google.com"))
 * val yahooApi = component.get<Api>(parameters = parametersOf("www.yahoo.com"))
 * assertSame(googleApi1, googleApi2) // true
 * assertSame(googleApi1, yahooApi) // false
 * ´´´
 *
 */
object MultiBehavior : Behavior.Element {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> =
        MultiProvider(provider)
}

inline fun <reified T> ComponentBuilder.multi(
    qualifier: Qualifier = Qualifier.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
): BindingContext<T> = multi(
    key = keyOf(qualifier = qualifier),
    duplicateStrategy = duplicateStrategy,
    provider = provider
)

/**
 * Dsl builder for the [MultiBehavior]
 */
fun <T> ComponentBuilder.multi(
    key: Key<T>,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
): BindingContext<T> = bind(
    Binding(
        key = key,
        behavior = MultiBehavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
)

/**
 * Annotation for the [MultiBehavior]
 */
@BehaviorMarker(MultiBehavior::class)
@Target(AnnotationTarget.CLASS)
annotation class Multi

private class MultiProvider<T>(
    private val provider: BindingProvider<T>
) : (Component, Parameters) -> T, ComponentInitObserver {

    private val values = ConcurrentHashMap<Int, T>()

    override fun onInit(component: Component) {
        (provider as? ComponentInitObserver)?.onInit(component)
    }

    override fun invoke(component: Component, parameters: Parameters): T =
        values.getOrPut(parameters.hashCode()) { provider(component, parameters) }
}
