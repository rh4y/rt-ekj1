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

    @Test
    fun testGet() {
        val instance = TestDep1()

        val component = Component {
            factory { instance }
        }

        assertEquals(instance, component.get<TestDep1>())
    }

    @Test
    fun testGetQualified() {
        val instance = TestDep1()

        val component = Component {
            factory(qualifier = TestQualifier1) { instance }
        }

        assertEquals(instance, component.get<TestDep1>(qualifier = TestQualifier1))
    }

    @Test
    fun testGetMultiQualified() {
        val instance = TestDep1()

        val component = Component {
            factory(qualifier = TestQualifier1 + TestQualifier2) { instance }
        }

        assertEquals(instance, component.get<TestDep1>(qualifier = TestQualifier1 + TestQualifier2))
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

    @Test(expected = IllegalStateException::class)
    fun testGetNonNullableNotReturnsNullable() {
        val component = Component {
            factory<String?> { null }
        }

        component.get<String>()
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
            factory(duplicateStrategy = DuplicateStrategy.Override) { "my_overridden_value" }
        }

        assertEquals("my_overridden_value", component.get<String>())
    }

    @Test
    fun testOverrideDrop() {
        val component = Component {
            factory { "my_value" }
            factory(duplicateStrategy = DuplicateStrategy.Drop) { "my_overridden_value" }
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
            factory(duplicateStrategy = DuplicateStrategy.Override) { "my_overridden_value" }
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
            factory(duplicateStrategy = DuplicateStrategy.Drop) { "my_overridden_value" }
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
            factory(duplicateStrategy = DuplicateStrategy.Fail) { "my_overridden_value" }
        }
    }

    @Test
    fun testDependencyOverride() {
        val dependencyComponentA = Component {
            factory { "value_a" }
        }
        val dependencyComponentB = Component {
            factory(duplicateStrategy = DuplicateStrategy.Override) { "value_b" }
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
            factory(duplicateStrategy = DuplicateStrategy.Drop) { "value_b" }
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
            factory(duplicateStrategy = DuplicateStrategy.Fail) { "value_b" }
        }

        val childComponent = Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testReverseDependencyOverrideFail() {
        val dependencyComponentA = Component {
            factory(duplicateStrategy = DuplicateStrategy.Override) { "value_a" }
        }
        val dependencyComponentB = Component {
            factory(duplicateStrategy = DuplicateStrategy.Fail) { "value_b" }
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

        /*assertEquals(componentA, componentA.get<Component>())
        assertEquals(componentA, componentA.get<Component>(qualifier = TestScopeOne))

        assertEquals(componentB, componentB.get<Component>())
        assertEquals(componentB, componentB.get<Component>(qualifier = TestScopeTwo))
        assertEquals(componentA, componentB.get<Component>(qualifier = TestScopeOne))*/ // todo
    }

    @Test
    fun testInstantiatesUnscopedBindingsInTheRequestingComponent() {
        val componentA = Component {
            bind { Context(get()) }
            alias<Context, Environment>()
        }
        val componentB = Component { dependencies(componentA) }
        val componentC = Component { dependencies(componentB) }

        val contextA = componentA.get<Context>()
        val contextB = componentB.get<Context>()
        val contextC = componentC.get<Context>()

        println("a $componentA b $componentB c $componentC")

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
}

class Context(val component: Component) : Environment

interface Environment

@TestScopeOne
@Single
class SingleJustInTimeDep
