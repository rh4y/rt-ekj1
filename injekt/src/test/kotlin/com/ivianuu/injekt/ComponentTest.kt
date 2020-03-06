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

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.Test

class ComponentTest {

    private object Named

    @Test
    fun testGet() {
        val typed = TestDep1()
        val named = TestDep1()

        val component = Component {
            factory { typed }
            factory(name = Named) { named }
        }

        val typedGet = component.get<TestDep1>()
        assertEquals(typed, typedGet)

        val namedGet = component.get<TestDep1>(Named)
        assertEquals(named, namedGet)
    }

    @Test
    fun testGetNested() {
        val componentA = Component {
            factory { TestDep1() }
        }
        val componentB = Component {
            dependencies(componentA)
            factory { TestDep2(get()) }
        }

        val componentC = Component {
            dependencies(componentB)
            factory { TestDep3(get(), get()) }
        }

        componentC.get<TestDep3>()
    }

    @Test(expected = IllegalStateException::class)
    fun testGetUnknownInstanceThrows() {
        val component = Component()
        component.get<Int>()
    }

    @Test
    fun testGetNullableInstanceReturnsNonNullable() {
        val component = Component {
            factory { "string" }
        }
        assertEquals("string", component.get<String?>())
    }

    @Test
    fun testGetUnknownNullableInstanceReturnsNull() {
        val component = Component()
        assertNull(component.get<String?>())
    }

    @Test
    fun testGetLazy() {
        var called = false

        val component = Component {
            factory {
                called = true
                TestDep1()
            }
        }

        assertFalse(called)
        val depLazy = component.get<Lazy<TestDep1>>()
        assertFalse(called)
        depLazy()
        assertTrue(called)
    }

    @Test
    fun testGetProvider() {
        var called = 0
        val component = Component {
            factory {
                ++called
                TestDep1()
            }
        }

        assertEquals(0, called)
        val depProvider = component.get<Provider<TestDep1>>()
        assertEquals(0, called)
        depProvider()
        assertEquals(1, called)
        depProvider()
        assertEquals(2, called)
    }

    @Test
    fun testOverride() {
        val component = Component {
            factory { "my_value" }
            factory(overrideStrategy = OverrideStrategy.Permit) { "my_overridden_value" }
        }

        assertEquals("my_overridden_value", component.get<String>())
    }

    @Test
    fun testOverrideDrop() {
        val component = Component {
            factory { "my_value" }
            factory(overrideStrategy = OverrideStrategy.Drop) { "my_overridden_value" }
        }

        assertEquals("my_value", component.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testOverrideFail() {
        Component {
            factory { "my_value" }
            factory { "my_overridden_value" }
        }
    }

    @Test
    fun testNestedOverride() {
        val parentComponent = Component {
            factory { "my_value" }
        }

        val childComponent = Component {
            dependencies(parentComponent)
            factory(overrideStrategy = OverrideStrategy.Permit) { "my_overridden_value" }
        }

        assertEquals("my_value", parentComponent.get<String>())
        assertEquals("my_overridden_value", childComponent.get<String>())
    }

    @Test
    fun testNestedOverrideDrop() {
        val parentComponent = Component {
            factory { "my_value" }
        }

        val childComponent = Component {
            dependencies(parentComponent)
            factory(overrideStrategy = OverrideStrategy.Drop) { "my_overridden_value" }
        }

        assertEquals("my_value", parentComponent.get<String>())
        assertEquals("my_value", childComponent.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testNestedOverrideFail() {
        val parentComponent = Component {
            factory { "my_value" }
        }

        val childComponent = Component {
            dependencies(parentComponent)
            factory(overrideStrategy = OverrideStrategy.Fail) { "my_overridden_value" }
        }
    }

    @Test
    fun testDependencyOverride() {
        val dependencyComponentA = Component {
            factory { "value_a" }
        }
        val dependencyComponentB = Component {
            factory(overrideStrategy = OverrideStrategy.Permit) { "value_b" }
        }

        val childComponent = Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }

        assertEquals("value_b", childComponent.get<String>())
    }

    @Test
    fun testDependencyOverrideDrop() {
        val dependencyComponentA = Component {
            factory { "value_a" }
        }
        val dependencyComponentB = Component {
            factory(overrideStrategy = OverrideStrategy.Drop) { "value_b" }
        }

        val childComponent = Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }

        assertEquals("value_a", childComponent.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testDependencyOverrideFail() {
        val dependencyComponentA = Component {
            factory { "value_a" }
        }
        val dependencyComponentB = Component {
            factory(overrideStrategy = OverrideStrategy.Fail) { "value_b" }
        }

        val childComponent = Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testReverseDependencyOverrideFail() {
        val dependencyComponentA = Component {
            factory(overrideStrategy = OverrideStrategy.Permit) { "value_a" }
        }
        val dependencyComponentB = Component {
            factory(overrideStrategy = OverrideStrategy.Fail) { "value_b" }
        }

        val childComponent = Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }
    }

    @Test
    fun testTypeDistinction() {
        val component = Component {
            factory { listOf(1, 2, 3) }
            factory { listOf("one", "two", "three") }
        }

        val ints = component.get<List<Int>>()
        val strings = component.get<List<String>>()

        assertEquals(listOf(1, 2, 3), ints)
        assertEquals(listOf("one", "two", "three"), strings)
        assertNotSame(ints, strings)
    }

    @Test
    fun testImplicitComponentBindings() {
        InjektPlugins.logger = PrintLogger()
        val componentA = Component { scopes(TestScopeOne) }
        val componentB = Component {
            scopes(TestScopeTwo)
            dependencies(componentA)
        }

        assertEquals(componentA, componentA.get<Component>())
        assertEquals(componentA, componentA.get<Component>(TestScopeOne))

        assertEquals(componentB, componentB.get<Component>())
        assertEquals(componentB, componentB.get<Component>(TestScopeTwo))
        assertEquals(componentA, componentB.get<Component>(TestScopeOne))
    }

    @Test
    fun testReusesSingleBindings() {
        val componentA = Component {
            single { TestDep1() }
        }

        val componentB = Component { dependencies(componentA) }
        val componentC = Component { dependencies(componentB) }

        val depA = componentA.get<TestDep1>()
        val depA2 = componentA.get<TestDep1>()
        val depB = componentB.get<TestDep1>()
        val depC = componentC.get<TestDep1>()

        assertEquals(depA, depA2)
        assertEquals(depA, depB)
        assertEquals(depA, depC)
    }

    @Test
    fun testReusesSingleJustInTimeBindings() {
        val componentA = Component { scopes(TestScopeOne) }

        val componentB = Component {
            scopes(TestScopeTwo)
            dependencies(componentA)
        }
        val componentC = Component {
            scopes(TestScopeThree)
            dependencies(componentB)
        }

        val depA = componentA.get<SingleJustInTimeDep>()
        val depA2 = componentA.get<SingleJustInTimeDep>()
        val depB = componentB.get<SingleJustInTimeDep>()
        val depC = componentC.get<SingleJustInTimeDep>()

        assertEquals(depA, depA2)
        assertEquals(depA, depB)
        assertEquals(depA, depC)
    }

    @Test
    fun testInstantiatesUnscopedBindingsInTheRequestingComponent() {
        val componentA = Component {
            single(scoping = Scoping.Unscoped) { Context(get()) }
                .bindAlias<Environment>()
        }
        val componentB = Component { dependencies(componentA) }
        val componentC = Component { dependencies(componentB) }

        val contextA = componentA.get<Context>()
        val contextB = componentB.get<Context>()
        val contextC = componentC.get<Context>()

        assertEquals(componentA, contextA.component)
        assertEquals(componentB, contextB.component)
        assertEquals(componentC, contextC.component)

        val environmentA = componentA.get<Environment>()
        val environmentB = componentB.get<Environment>()
        val environmentC = componentC.get<Environment>()

        environmentA as Context
        environmentB as Context
        environmentC as Context

        assertEquals(componentA, environmentA.component)
        assertEquals(componentB, environmentB.component)
        assertEquals(componentC, environmentC.component)
    }

    @Test
    fun testInstantiatesEagerBindingOnStart() {
        var called = false
        Component {
            single(eager = false) { called = true }
        }
        assertFalse(called)
        Component {
            single(eager = true) { called = true }
        }
        assertTrue(called)
    }
}

class Context(val component: Component) : Environment

interface Environment

@TestScopeOne
@Single
class SingleJustInTimeDep
