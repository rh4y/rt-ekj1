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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import org.junit.Test

class GraphTest {

    @Test
    fun testMissingBindingFails() = codegen(
        """
        @Unscoped class Dep(bar: Bar)
        @Factory fun createDep(): TestComponent1<Dep> = create()
        """
    ) {
        assertInternalError("no binding")
    }

    // todo name
    @Test
    fun testCannotResolveDirectBindingWithAssistedParameters() = codegen(
        """
        @Unscoped class Dep(bar: @Assisted Bar)
        @Factory fun createDep(): TestComponent1<Dep> = create()
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDuplicatedBindingFails() = codegen(
        """
        @Factory
        fun createFoo(): TestComponent1<Foo> {
            unscoped { Foo() }
            unscoped { Foo() }
            return create()
        }
        """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testCircularDependency() = codegen(
        """
        @Unscoped class A(b: B)
        @Unscoped class B(a: A)
        @Factory fun createA(): TestComponent1<A> = create()
    """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testCircularDependencyWithProvider() = codegen(
        """
        @Scoped(TestComponent1::class) class A(b: B)
        @Unscoped class B(a: @Provider () -> A)
        @Factory fun invoke(): TestComponent1<A> {
            return create()
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCircularDependencyWithProvider2() = codegen(
        """
        @Scoped(TestComponent1::class) class A(b: B)
        @Unscoped class B(a: @Provider () -> A)
        @Factory fun invoke(): TestComponent1<B> {
            return create()
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCircularDependencyWithIrrelevantProvider() = codegen(
        """
        @Scoped(TestComponent::class) class A(b: B)
        @Unscoped class B(a: A)
        @Unscoped class C(b: @Provider () -> B)
        @Factory fun invoke(): TestComponent1<B> {
            return create()
        }
    """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testComponentMismatch() = codegen(
        """
        @Scoped(Any::class) class Dep

        @Factory
        fun createDep(): TestComponent1<Dep> {
            return create()
        }
        """
    ) {
        assertInternalError("component mismatch")
    }

    @Test
    fun testDistinctTypeDistinction() = codegen(
        """
        @DistinctType typealias Foo1 = Foo
        @DistinctType typealias Foo2 = Foo
        
        @Unscoped @Reader fun foo1(): Foo1 = Foo()
        @Unscoped @Reader fun foo2(): Foo2 = Foo()
        
        fun invoke(): Pair<Foo, Foo> {
            buildComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            return component.runReader { get<Foo1>() to get<Foo2>() }
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testIgnoresNullability() = codegen(
        """
        @Factory
        fun createFoo(): TestComponent1<Foo> {
            unscoped<Foo> { Foo() }
            unscoped<Foo?> { null }
            return create()
        }
    """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testReturnsInstanceForNullableBinding() = codegen(
        """
        @Factory
        fun invoke(): TestComponent1<Foo?> {
            unscoped<Foo?>()
            return create()
        }
        """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableBinding() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Foo?> {
            return create()
        }
        
        fun invoke() = factory().a
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testTypeWithStarProjectedArg() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<List<*>> {
            unscoped<List<*>> { listOf<Any?>() }
            return create()
        }
    """
    )

    @Test
    fun testGenericAnnotatedClass() = codegen(
        """
        @Unscoped
        class Wrapper<T>(val value: T)
        
        interface WrappedComponent {
            val fooWrapper: Wrapper<Foo>
            val barWrapper: Wrapper<Bar>
        }
        
        @Factory
        fun createWrapperComponent(): WrappedComponent {
            unscoped<Foo>()
            unscoped<Bar>()
            return create()
        }
    """
    )

}
