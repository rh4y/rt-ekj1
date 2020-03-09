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

/**
 * Create a [Component] configured by [block]
 *
 * @see Component
 */
inline fun Component(block: ComponentBuilder.() -> Unit = {}): Component =
    ComponentBuilder().apply(block).build()

/**
 * Builder for a [Component]
 *
 * @see Component
 */
class ComponentBuilder {

    private val _scopes = mutableListOf<Scope>()
    val scopes: List<Scope> get() = _scopes

    private val _dependencies = mutableListOf<Component>()
    val dependencies: List<Component> get() = _dependencies

    private val _bindings = mutableMapOf<Key<*>, Binding<*>>()
    val bindings: Map<Key<*>, Binding<*>> get() = _bindings

    /**
     * Adds the [scopes] this allows generated [Binding]s
     * to be associated with components.
     *
     * @see ScopeMarker
     */
    fun scopes(vararg scopes: Scope) {
        scopes.forEach { scope ->
            check(scope !in this._scopes) { "Duplicated scope $scope" }
            this._scopes += scope
        }
    }

    /**
     * Adds the [dependencies] to the component if this component cannot resolve a instance
     * it will ask it's dependencies
     */
    fun dependencies(vararg dependencies: Component) {
        dependencies.forEach { dependency ->
            check(dependency !in this._dependencies) { "Duplicated dependency $dependency" }
            this._dependencies += dependency
        }
    }

    inline fun <reified T> bind(
        qualifier: Qualifier = Qualifier.None,
        behavior: Behavior = Behavior.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        noinline provider: BindingProvider<T>
    ): BindingContext<T> = bind(
        key = keyOf(qualifier = qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )

    fun <T> bind(
        key: Key<T>,
        behavior: Behavior = Behavior.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        provider: BindingProvider<T>
    ): BindingContext<T> {
        return bind(
            Binding(
                key = key,
                behavior = behavior,
                duplicateStrategy = duplicateStrategy,
                provider = provider
            )
        )
    }

    /**
     * Adds the [binding]
     * This function is rarely used directly instead use [factory] or [single]
     *
     * @see factory
     * @see single
     * @see BindingContext
     */
    fun <T> bind(binding: Binding<T>): BindingContext<T> {
        if (binding.duplicateStrategy.check(
                existsPredicate = { binding.key in _bindings },
                errorMessage = { "Already declared binding for ${binding.key}" }
            )
        ) {
            _bindings[binding.key] = binding
        }

        return BindingContext(binding = binding, componentBuilder = this)
    }

    /**
     * Create a new [Component] instance.
     */
    fun build(): Component {
        checkScopes()

        val dependencyBindings = _dependencies
            .map { it.getAllBindings() }
            .fold(mutableMapOf<Key<*>, Binding<*>>()) { acc, current ->
                current.forEach { (key, binding) ->
                    if (binding.duplicateStrategy.check(
                            existsPredicate = { key in acc },
                            errorMessage = { "Already declared binding for $key" }
                        )
                    ) {
                        acc[key] = binding
                    }
                }

                return@fold acc
            }

        val finalBindings = mutableMapOf<Key<*>, Binding<*>>()

        fun addBinding(binding: Binding<*>) {
            if (binding.duplicateStrategy.check(
                    existsPredicate = { binding.key in dependencyBindings },
                    errorMessage = { "Already declared key ${binding.key}" })
            ) {
                finalBindings[binding.key] = binding
            }
        }

        _bindings.values.forEach { binding -> addBinding(binding) }

        includeComponentBindings(finalBindings)

        return Component(
            scopes = _scopes,
            dependencies = _dependencies,
            bindings = finalBindings
        )
    }

    private fun checkScopes() {
        val dependencyScopes = mutableSetOf<Scope>()

        fun addScope(scope: Scope) {
            check(scope !in dependencyScopes) {
                "Duplicated scope $scope"
            }

            dependencyScopes += scope
        }

        _dependencies
            .flatMap { it.scopes }
            .forEach { addScope(it) }

        _scopes.forEach { addScope(it) }
    }

    private fun includeComponentBindings(bindings: MutableMap<Key<*>, Binding<*>>) {
        val componentBinding = Binding(
            key = keyOf(),
            behavior = BoundBehavior(),
            duplicateStrategy = DuplicateStrategy.Permit,
            provider = { this }
        )

        bindings[componentBinding.key] = componentBinding
    }

    private fun Component.getAllBindings(): Map<Key<*>, Binding<*>> =
        mutableMapOf<Key<*>, Binding<*>>().also { collectBindings(it) }

    private fun Component.collectBindings(bindings: MutableMap<Key<*>, Binding<*>>) {
        dependencies.forEach { it.collectBindings(bindings) }
        bindings += this.bindings
    }
}
