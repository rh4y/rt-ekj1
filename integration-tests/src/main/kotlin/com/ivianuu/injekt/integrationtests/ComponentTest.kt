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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ComponentTest {

    @Test
    fun testSimple() = codegen(
        """
            @Component
            abstract class TestComponent { 
                abstract val bar: Bar
                @Binding protected fun foo() = Foo()
                @Binding protected fun bar(foo: Foo) = Bar(foo)
            }
            
            fun invoke(): Bar {
                return component<TestComponent>().bar
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testChild() = codegen(
        """
            @Component
            abstract class ParentComponent {
                abstract val childComponentFactory: (Foo) -> MyChildComponent
            }
            
            @ChildComponent
            abstract class MyChildComponent(@Binding protected val _foo: Foo) {
                abstract val foo: Foo
            }

            fun invoke(foo: Foo): Foo {
                return component<ParentComponent>().childComponentFactory(foo).foo
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testChildOverridesParentBinding() = codegen(
        """
            class Context
            
            val parentContext = Context()
            @Binding(ParentComponent::class) fun parentContext() = parentContext
            
            val childContext = Context()
            @Binding(MyChildComponent::class) fun childContext() = childContext
            
            @Component
            abstract class ParentComponent {
                abstract val childComponentFactory: () -> MyChildComponent
                abstract val context: Context
            }
            
            @ChildComponent
            abstract class MyChildComponent {
                abstract val context: Context
            }

            fun invoke(): List<Any> {
                val parentComponent = component<ParentComponent>()
                val childComponent = parentComponent.childComponentFactory()
                
                return listOf(
                    parentComponent.context,
                    parentComponent.context,
                    childComponent.context,
                    childComponent.context
                )
            }
    """
    ) {
        val (a1, a2, b1, b2) = invokeSingleFile<List<Any>>()
        assertSame(a1, a2)
        assertSame(b1, b2)
        assertNotSame(a1, b1)
    }

    @Test
    fun testChildWithAdditionalArguments() = codegen(
        """
            @Component
            abstract class ParentComponent {
                abstract val childComponentFactory: (Foo) -> MyChildComponent
            }
            
            @ChildComponent
            abstract class MyChildComponent {
                abstract val foo: Foo
            }

            fun invoke(foo: Foo): Foo {
                return component<ParentComponent>().childComponentFactory(foo).foo
            }
    """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testUnscopedBindingReturnsDifferentInstance() = codegen(
        """
            @Component
            abstract class MyComponent { 
                abstract val foo: Foo
                @Binding 
                protected fun foo() = Foo()
            }
        
            val component: MyComponent = component<MyComponent>()
        
            fun invoke() = component.foo
    """
    ) {
        assertNotSame(invokeSingleFile(), invokeSingleFile())
    }

    @Test
    fun testScopedBindingReturnsSameInstance() = codegen(
        """
            @Component
            abstract class MyComponent { 
                abstract val foo: Foo
                @Binding(MyComponent::class) 
                protected fun foo() = Foo()
            }
        
            val component: MyComponent = component<MyComponent>()
        
            fun invoke() = component.foo
    """
    ) {
        assertSame(invokeSingleFile(), invokeSingleFile())
    }

    @Test
    fun testGenericBindingsWithDifferentArgumentsHasDifferentIdentity() = codegen(
        """
            @Binding(MyComponent::class)
            class Option<T>(val value: T)
            
            @Component
            abstract class MyComponent {
                abstract val stringOption: Option<String>
                abstract val intOption: Option<Int>
                
                @Binding protected fun string() = ""
                @Binding protected fun int() = 0
            }
            
            val component = component<MyComponent>()
            
            fun invoke(): List<Any> {
                return listOf(
                    component.stringOption,
                    component.stringOption,
                    component.intOption,
                    component.intOption
                )
            }
            
        """
    ) {
        val (a1, a2, b1, b2) = invokeSingleFile<List<Any>>()
        assertSame(a1, a2)
        assertSame(b1, b2)
        assertNotSame(a1, b1)
    }

    @Test
    fun testStarProjectedBindingsHasSharedIdentity() = codegen(
        """
            class Option<T>(val value: T)
            
            @Binding(MyComponent::class)
            fun stringOption(value: String) = Option(value)
            
            @Component
            abstract class MyComponent {
                abstract val stringOption: Option<String> 
                abstract val starOption: Option<*>
                @Binding protected fun string() = ""
            }
            
            val component = component<MyComponent>()
            
            fun invoke(): Pair<Any, Any> {
                return component.stringOption to component.starOption
            }
        """
    ) {
        val (a1, a2) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a1, a2)
    }

    @Test
    fun testParentScopedBinding() = codegen(
        """
            @Component
            abstract class MyParentComponent {
                abstract val childFactory: () -> MyChildComponent
            
                @Binding
                protected fun foo() = Foo()
                
                @Binding(MyParentComponent::class)
                protected fun bar(foo: Foo) = Bar(foo)
            }
            
            val parentComponent: MyParentComponent = component<MyParentComponent>()
            
            @ChildComponent
            abstract class MyChildComponent {
                abstract val bar: Bar
            }

            val childComponent = parentComponent.childFactory()
         
            fun invoke(): Bar {
                return childComponent.bar
            }
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testClassBinding() = codegen(
        """
            @Binding
            class AnnotatedBar(foo: Foo)
            
            @Component
            abstract class FooComponent {
                abstract val annotatedBar: AnnotatedBar
                
                @Binding
                protected fun foo() = Foo()
            }

            fun invoke() {
                component<FooComponent>().annotatedBar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testObjectBinding() = codegen(
        """
            @Binding
            object AnnotatedBar
            
            @Component
            abstract class MyComponent {
                abstract val annotationBar: AnnotatedBar
            }
            
            fun invoke() {
                component<MyComponent>().annotationBar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testPropertyBinding() = codegen(
        """
            @Component
            abstract class FooComponent {
                abstract val foo: Foo
                @Binding protected val _foo = Foo()
            }
            
            fun invoke(): Foo {
                return component<FooComponent>().foo
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testTopLevelFunctionBinding() = codegen(
        """
            @Binding
            fun foo() = Foo()
            
            @Component
            abstract class FooComponent {
                abstract val foo: Foo
            }

            fun invoke() {
                component<FooComponent>().foo
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testFunctionBindingInObject() = codegen(
        """
            object BarDeps {
                @Binding
                fun Foo.bar() = Bar(this)
                
                @Binding
                fun foo() = Foo()
            }
            
            @Component
            abstract class BarComponent {
                abstract val bar: Bar
            }

            fun invoke() {
                component<BarComponent>().bar
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testTopLevelPropertyBinding() = codegen(
        """
            @Binding
            val foo get() = Foo()
            
            @Component
            abstract class FooComponent {
                abstract val foo: Foo
            }

            fun invoke() {
                component<FooComponent>().foo
            }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testProvider() = codegen(
        """
            @Component
            abstract class ProviderComponent {
                abstract val fooFactory: () -> Foo
                @Binding
                protected fun foo() = Foo()
            }

            fun invoke() {
                component<ProviderComponent>().fooFactory()
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testSuspendProvider() = codegen(
        """
            @Component
            abstract class ProviderComponent {
                abstract val fooFactory: suspend () -> Foo
                @Binding
                protected suspend fun foo() = Foo()
            }

            fun invoke() {
                runBlocking {
                    component<ProviderComponent>().fooFactory()
                }
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testComposableProvider() = codegen(
        """
            @Component
            abstract class ProviderComponent {
                abstract val fooFactory: @Composable () -> Foo
                @Composable
                @Binding
                protected fun foo() = Foo()
            }

            fun invoke() {
                component<ProviderComponent>().fooFactory
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testAssistedSuspendBindingFunction() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: suspend (Foo) -> Bar
                
                @Binding
                protected suspend fun bar(foo: Foo) = Bar(foo)
            }

            fun invoke(foo: Foo): Bar { 
                return runBlocking { component<BarComponent>().barFactory(foo) }
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedComposableBindingFunction() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: @Composable (Foo) -> Bar
                
                @Binding
                @Composable
                protected fun bar(foo: Foo) = Bar(foo)
            }

            fun invoke(foo: Foo) { 
                component<BarComponent>().barFactory
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedBindingFunction() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: (Foo) -> Bar
                
                @Binding
                protected fun bar(foo: Foo) = Bar(foo)
            }

            fun invoke(foo: Foo): Bar { 
                return component<BarComponent>().barFactory(foo)
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testComplexAssistedBindingFunction() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: (Foo, Int) -> Bar
                
                @Binding
                protected fun bar(foo: Foo, string: String, int: Int) = Bar(foo)
                
                @Binding
                protected val string = ""
            }
            fun invoke(foo: Foo): Bar { 
                return component<BarComponent>().barFactory(foo, 0)
            }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testScopedAssistedBinding() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val barFactory: (Foo) -> Bar
                
                @Binding(BarComponent::class)
                protected fun bar(foo: Foo) = Bar(foo)
            }
            
            private val component = component<BarComponent>()

            fun invoke(): Pair<Bar, Bar> { 
                return component.barFactory(Foo()) to component.barFactory(Foo())
            }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Bar, Bar>>()
        assertSame(a, b)
    }

    // todo @Test
    fun testScopedAssistedBindingInChild() = codegen(
        """
            @Component
            abstract class ParentComponent {
                abstract val childFactory: () -> MyChildComponent
                @Binding(BarComponent::class)
                protected fun bar(foo: Foo) = Bar(foo)
            }
            
            @ChildComponent
            abstract class MyChildComponent {
                abstract val barFactory: (Foo) -> Bar
            }
            
            private val parentComponent = component<BarComponent>()
            private val childComponent = parentComponent.childFactory()

            fun invoke(): Pair<Bar, Bar> { 
                return childComponent.barFactory(Foo()) to childComponent.barFactory(Foo())
            }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Bar, Bar>>()
        assertSame(a, b)
    }

    @Test
    fun testAssistedBindingClass() = codegen(
        """
            @Binding
            class AnnotatedBar(foo: Foo)
            
            @Component
            abstract class MyComponent {
                abstract val annotatedBar: (Foo) -> AnnotatedBar
            }

            fun invoke(foo: Foo): AnnotatedBar = component<MyComponent>().annotatedBar(foo)
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testGenericBindingClass() = codegen(
        """
            @Binding class Dep<T>(val value: T)
            
            @Component
            abstract class FooComponent {
                abstract val fooDep: Dep<Foo>
                @Binding
                protected fun foo() = Foo()
            }
            
            fun invoke() {
                component<FooComponent>().fooDep
            }
    """
    )

    @Test
    fun testGenericBindingFunction() = codegen(
        """    
            class Dep<T>(val value: T)
            
            @Component
            abstract class MyComponent { 
                abstract val fooDep: Dep<Foo>
                @Binding protected fun <T> dep(value: T) = Dep(value)
                @Binding protected fun foo() = Foo() 
            }

            fun invoke() {
                component<MyComponent>().fooDep
            }
    """
    )

    @Test
    fun testComplexGenericBindingFunction() = codegen(
        """    
            class Dep<A, B, C>(val value: A)
            
            @Component
            abstract class MyComponent { 
                abstract val dep: Dep<Foo, Foo, Foo>
                @Binding protected fun <A, B : A, C : B> dep(a: A) = Dep<A, A, A>(a)
                @Binding protected fun foo() = Foo()
            }
    """
    )

    @Test
    fun testComponentFunction() = codegen(
        """
            @Component
            abstract class FunctionComponent {
                abstract fun foo(): Foo
                
                @Binding
                protected fun _foo() = Foo()
            }
        """
    )

    @Test
    fun testComponentSuspendFunction() = codegen(
        """
            @Component
            abstract class SuspendFunctionComponent {
                abstract suspend fun bar(): Bar
                @Binding
                protected suspend fun _suspendFoo() = Foo()
                @Binding
                protected suspend fun _suspendBar(foo: Foo) = Bar(foo)
            }
        """
    )

    @Test
    fun testScopedSuspendFunction() = codegen(
        """
            @Component
            abstract class SuspendFunctionComponent {
                abstract suspend fun foo(): Foo
                @Binding(SuspendFunctionComponent::class)
                protected suspend fun _suspendFoo() = Foo()
            }
            
            private val component = component<SuspendFunctionComponent>()
            fun invoke(): Pair<Foo, Foo> {
                return runBlocking { component.foo() to component.foo() }
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testComponentComposableFunction() = codegen(
        """
            @Component
            abstract class SuspendFunctionComponent {
                @Composable
                abstract fun bar(): Bar
                @Composable
                @Binding
                protected fun _composableFoo() = Foo()
                @Composable
                @Binding
                protected fun _composableBar(foo: Foo) = Bar(foo)
            }
        """
    )

    // todo @Test
    // todo find a way to invoke composables
    fun testScopedComposableFunction() = codegen(
        """
            @Component
            abstract class ComposableFunctionComponent {
                @Composable
                abstract fun foo(): Foo
                @Binding(ComposableFunctionComponent::class)
                @Composable
                protected fun _composableFoo() = Foo()
            }
            
            private val component = component<ComposableFunctionComponent>()
            fun invoke(): Pair<Foo, Foo> {
                return component.foo() to component.foo()
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testComponentWithConstructorParameters() = codegen(
        """
            @Component
            abstract class MyComponent(@Binding protected val _foo: Foo) {
                abstract val foo: Foo
            }
            fun invoke(): Pair<Foo, Foo> {
                val foo = Foo()
                return foo to component<MyComponent>(foo).foo
            }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testNestedComponent() = codegen(
        """
            @Component
            abstract class BarComponent {
                abstract val bar: Bar
            
                @Binding
                protected fun foo() = Foo()
                
                @Module
                protected val nested = NestedModule()
                
                @Module
                class NestedModule {
                    @Binding
                    fun bar(foo: Foo) = Bar(foo)
                }
            }
            
            fun invoke(): Bar {
                return component<BarComponent>().bar
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testGenericNestedComponent() = codegen(
        """
            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            
                @Module
                protected val fooModule = InstanceModule<Foo>(Foo())
                
                @Module
                class InstanceModule<T>(@Binding val instance: T)
            }

            fun invoke(): Foo {
                return component<MyComponent>().foo
            }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testMissingBindingFails() = codegen(
        """
            class Dep
            
            @Component
            abstract class DepComponent {
                abstract val dep: Dep
            }
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDeeplyMissingBindingFails() = codegen(
        """
            @Component
            abstract class BazComponent {
                abstract val baz: Baz
            
                @Binding
                protected fun bar(foo: Foo) = Bar(foo)
        
                @Binding
                protected fun baz(bar: Bar, foo: Foo) = Baz(bar, foo)
            }
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDistinctTypeParameter() = codegen(
        """
            @Component
            abstract class MyComponent {
                abstract val setOfStrings: Set<String>
                abstract val setOfInts: Set<Int>
            
                @SetElements protected fun _setA() = setOf("a")
                @SetElements protected fun _setB() = setOf(0)
            }

            fun invoke(): Pair<Set<String>, Set<Int>> {
                val component = component<MyComponent>()
                return component.setOfStrings to component.setOfInts
            }
            """
    ) {
        val (setA, setB) = invokeSingleFile<Pair<Set<String>, Set<Int>>>()
        assertNotSame(setA, setB)
    }

    @Test
    fun testDistinctTypeAlias() = codegen(
        """
            typealias Foo1 = Foo
            typealias Foo2 = Foo
            
            @Component
            abstract class FooComponent {
                abstract val foo1: Foo1
                abstract val foo2: Foo2
                @Binding protected fun _foo1(): Foo1 = Foo()
                @Binding protected fun _foo2(): Foo2 = Foo()
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = component<FooComponent>()
                return component.foo1 to component.foo2
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctTypeAliasMulti() = multiCodegen(
        listOf(
            source(
                """
                    typealias Foo1 = Foo
                    @Module
                    object Foo1Module {
                        @Binding fun foo1(): Foo1 = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    typealias Foo2 = Foo
                    @Module
                    object Foo2Module {
                        @Binding fun foo2(): Foo2 = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @Component
                    abstract class MyComponent {
                        abstract val foo1: Foo1
                        abstract val foo2: Foo2
                        
                        @Module protected val foo1Module = Foo1Module
                        @Module protected val foo2Module = Foo2Module
                    }
                    fun invoke(): Pair<Foo1, Foo2> {
                        val component = component<MyComponent>()
                        return component.foo1 to component.foo2
                    }
            """,
                name = "File.kt"
            )
        )
    ) {
        val (foo1, foo2) = it.last().invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifier() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier
            
            @Component
            abstract class FooComponent {
                abstract val foo1: Foo
                abstract val foo2: @MyQualifier Foo
                @Binding protected fun _foo1(): Foo = Foo()
                @Binding protected fun _foo2(): @MyQualifier Foo = Foo()
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = component<FooComponent>()
                return component.foo1 to component.foo2
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifierMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Target(AnnotationTarget.TYPE)
                    @Qualifier
                    annotation class MyQualifier
                    @Module
                    object Foo1Module {
                        @Binding fun foo1(): @MyQualifier Foo = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @Module
                    object Foo2Module {
                        @Binding fun foo2(): Foo = Foo()
                    }
            """
            )
        ),
        listOf(
            source(
                """
                    @Component
                    abstract class MyComponent {
                        abstract val foo1: @MyQualifier Foo
                        abstract val foo2: Foo
                        
                        @Module protected val foo1Module = Foo1Module
                        @Module protected val foo2Module = Foo2Module
                    }
                    fun invoke(): Pair<Foo, Foo> {
                        val component = component<MyComponent>()
                        return component.foo1 to component.foo2
                    }
            """,
                name = "File.kt"
            )
        )
    ) {
        val (foo1, foo2) = it.last().invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifierAnnotationWithArguments() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier(val value: String)
            
            @Component
            abstract class FooComponent {
                abstract val foo1: @MyQualifier("1") Foo
                abstract val foo2: @MyQualifier("2") Foo
                @Binding protected fun _foo1(): @MyQualifier("1") Foo = Foo()
                @Binding protected fun _foo2(): @MyQualifier("2") Foo = Foo()
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = component<FooComponent>()
                return component.foo1 to component.foo2
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctQualifierAnnotationWithTypeArguments() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier<T>
            
            @Component
            abstract class FooComponent {
                abstract val foo1: @MyQualifier<String> Foo
                abstract val foo2: @MyQualifier<Int> Foo
                @Binding protected fun _foo1(): @MyQualifier<String> Foo = Foo()
                @Binding protected fun _foo2(): @MyQualifier<Int> Foo = Foo()
            }
       
            fun invoke(): Pair<Foo, Foo> {
                val component = component<FooComponent>()
                return component.foo1 to component.foo2
            }
            """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testTypeParameterWithQualifierUpperBound() = codegen(
        """
            @Target(AnnotationTarget.TYPE)
            @Qualifier
            annotation class MyQualifier
            
            @Binding
            class Dep<T>(val value: @MyQualifier T)
            
            @Binding
            fun qualified(): @MyQualifier String = ""
            
            @Component
            abstract class FooComponent {
                abstract val dep: Dep<String>
            }
            """
    )

    @Test
    fun testIgnoresNullability() = codegen(
        """
            @Component
            abstract class FooComponent { 
                abstract val foo: Foo
                @Binding protected fun foo(): Foo = Foo()
                @Binding protected fun nullableFoo(): Foo? = null
            }
            """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testReturnsInstanceForNullableBinding() = codegen(
        """
            @Component
            abstract class FooComponent {
                abstract val foo: Foo?
                @Binding protected fun foo(): Foo = Foo()
            }
            
            fun invoke(): Foo? {
                return component<FooComponent>().foo
            }
        """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableRequest() = codegen(
        """
            @Component
            abstract class FooComponent {
                abstract val foo: Foo?
            }
            fun invoke(): Foo? { 
                return component<FooComponent>().foo
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testReturnsDefaultOnMissingOpenRequest() = codegen(
        """
            val DEFAULT_FOO = Foo()
            @Component
            abstract class FooComponent {
                open val foo: Foo = DEFAULT_FOO
            }
            fun invoke(): Pair<Foo, Foo> { 
                return DEFAULT_FOO to component<FooComponent>().foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testReturnsDefaultOnMissingOpenNullableRequest() = codegen(
        """
            val DEFAULT_FOO = Foo()
            @Component
            abstract class FooComponent {
                open val foo: Foo? = DEFAULT_FOO
            }
            fun invoke(): Pair<Foo?, Foo?> { 
                return DEFAULT_FOO to component<FooComponent>().foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo?, Foo?>>()
        assertSame(a, b)
    }

    @Test
    fun testUsesNullOnMissingNullableDependency() = codegen(
        """
            @Binding
            class Dep(val foo: Foo?)
            
            @Component
            abstract class FooComponent {
                abstract val dep: Dep
            }
            
            fun invoke(): Foo? { 
                return component<FooComponent>().dep.foo
            }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testUsesDefaultOnMissingDependency() = codegen(
        """
            @Binding            
            class Dep(val foo: Foo = DEFAULT_FOO)
            val DEFAULT_FOO = Foo()
            
            @Component
            abstract class FooComponent {
                abstract val dep: Dep
            }
            fun invoke(): Pair<Foo, Foo> { 
                return DEFAULT_FOO to component<FooComponent>().dep.foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

    @Test
    fun testUsesDefaultOnMissingNullableDependency() = codegen(
        """
            @Binding
            class Dep(val foo: Foo? = DEFAULT_FOO)
            val DEFAULT_FOO = Foo()
            
            @Component
            abstract class FooComponent {
                abstract val dep: Dep
            }
            
            fun invoke(): Pair<Foo?, Foo?> { 
                return DEFAULT_FOO to component<FooComponent>().dep.foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo?, Foo?>>()
        assertSame(a, b)
    }

    @Test
    fun testTypeWithStarProjectedArg() = codegen(
        """ 
            @Component
            abstract class MyComponent(@Binding protected val _list: List<*>) {
                abstract val list: List<*>
            }
        """
    )

    @Test
    fun testCanRequestStarProjectedType() = codegen(
        """ 
            class Store<S, A>
            
            @Binding
            fun stringStore() = Store<String, String>()
            
            @Component
            abstract class MyComponent {
                abstract val storeS: Store<String, *>
                abstract val storeA: Store<*, String>
            }
        """
    )

    @Test
    fun testStarProjectedTypeAmbiguity() = codegen(
        """ 
            class Store<S, A>
            
            @Binding
            fun stringStringStore() = Store<String, String>()
            
            @Binding
            fun stringIntStore() = Store<String, Int>()
            
            @Component
            abstract class MyComponent {
                abstract val store: Store<String, *>
            }
        """
    ) {
        assertInternalError("multiple")
    }

    // todo @Test
    fun testGenericBindingWithStarProjection() = codegen(
        """
            class Store<S, A>
            
            @Binding
            fun store() = Store<String, Int>()
            
            @Binding
            fun <S> Store<S, *>.storeState(): S = error("")
            
            @Component
            abstract class MyComponent {
                abstract val state: String
            }
        """
    )

    // todo @Test
    fun testGenericBindingWithIrrelevantTypeParameters() = codegen(
        """
            class Store<S, A>
            
            @Binding
            fun store() = Store<String, Int>()
            
            @Binding
            fun <S, A> Store<S, A>.storeState(): S = error("")
            
            @Component
            abstract class MyComponent {
                abstract val state: String
            }
        """
    )

    @Test
    fun testPrefersExplicitOverImplicitBinding() = codegen(
        """
            @Binding
            class Dep
            
            @Component
            abstract class MyComponent(@Binding protected val _dep: Dep) { 
                abstract val dep: Dep
            }
            
            fun invoke(): Pair<Any, Any> {
                val dep = Dep()
                return dep to component<MyComponent>(dep).dep
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testMultipleResolvableExplicitBindingFails() = codegen(
        """
            @Component
            abstract class MyComponent(
                @Binding protected val foo1: Foo,
                @Binding protected val foo2: Foo
            ) {
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("multiple explicit bindings")
    }

    @Test
    fun testPrefersInternalImplicitOverExternalImplicitBinding() = multiCodegen(
        listOf(
            source(
                """
                    var externalFooField: Foo? = null
                    @Binding
                    val externalFoo: Foo get() = externalFooField!!
                """
            )
        ),
        listOf(
            source(
                """
                    var internalFooField: Foo? = null
                    @Binding
                    val internalFoo: Foo get() = internalFooField!!

                    @Component
                    abstract class MyComponent {
                        abstract val foo: Foo
                    }
                    
                    fun invoke(
                        internalFoo: Foo,
                        externalFoo: Foo
                    ): Foo {
                        externalFooField = externalFoo
                        internalFooField = internalFoo
                        return component<MyComponent>().foo
                    }
                """,
                name = "File.kt"
            )
        )
    ) {
        val externalFoo = Foo()
        val internalFoo = Foo()
        assertSame(internalFoo, it.last().invokeSingleFile(internalFoo, externalFoo))
    }

    @Test
    fun testMultipleResolvableInternalImplicitBindingFails() = codegen(
        """
        @Binding fun foo1() = Foo()
        @Binding fun foo2() = Foo()
        
        @Component
        abstract class MyComponent {
            abstract val foo: Foo
        }
        
        fun invoke(): Foo { 
            return component<MyComponent>().foo
        }
        """
    ) {
        assertInternalError("multiple internal implicit bindings")
    }

    @Test
    fun testMultipleResolvableExternalImplicitBindingsFails() = multiCodegen(
        listOf(
            source(
                """
                    @Binding fun foo1() = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    @Binding fun foo2() = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    @Component
                    abstract class MyComponent {
                        abstract val foo: Foo
                    }
                    fun invoke(): Foo { 
                        return component<MyComponent>().foo
                    }
                """
            )
        )
    ) {
        it.last().assertInternalError("multiple external implicit bindings")
    }

    @Test
    fun testPrefsUserBindingOverFrameworkBinding() = codegen(
        """
            @Component
            abstract class MyComponent(
                @Binding protected val _lazyFoo: () -> Foo
            ) {
                abstract val lazyFoo: () -> Foo
            }
            
            fun invoke(): Pair<() -> Foo, () -> Foo> {
                val lazyFoo = { Foo() }
                return lazyFoo to component<MyComponent>(lazyFoo).lazyFoo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testBindingPerComponent() = codegen(
        """
            @Component
            abstract class MyParentComponent {
                abstract val childFactory: () -> MyChildComponent
                abstract val foo: Foo
                @Binding(MyParentComponent::class) protected fun parentFoo() = Foo()
            }

            @ChildComponent
            abstract class MyChildComponent {
                abstract val foo: Foo
                @Binding(MyChildComponent::class) protected fun childFoo() = Foo()
            }

            fun invoke(): Pair<Foo, Foo> {
                val parent = component<MyParentComponent>()
                val child = parent.childFactory()
                return parent.foo to child.foo
            }
        """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testInjectingComponent() = codegen(
        """ 
            @Component
            abstract class SelfComponent {
                abstract val self: SelfComponent
            }

            fun invoke(): Pair<SelfComponent, SelfComponent> {
                val component = component<SelfComponent>()
                return component to component.self
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testPrefersResolvableBinding() = codegen(
        """
            val defaultFoo = Foo()
            
            @Binding
            fun bar() = Bar(defaultFoo)
            
            @Binding
            fun bar(foo: Foo) = Bar(foo)
            
            @Component
            abstract class MyComponent { 
                abstract val bar: Bar
            }
            
            fun invoke(): Pair<Any, Any> {
                return defaultFoo to component<MyComponent>().bar.foo
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testPrefersExactType() = codegen(
        """
            class Dep<T>(val value: T)
            
            @Component
            abstract class FooComponent {
                abstract val fooDep: Dep<Foo>
                
                @Binding
                protected fun <T> genericDep(t: T): Dep<T> = error("")
                
                @Binding
                protected fun fooDep(foo: Foo): Dep<Foo> = Dep(foo)
                
                @Binding
                protected fun foo() = Foo()
            }
            
            fun invoke() {
                component<FooComponent>().fooDep
            }
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGenericTypeAlias() = codegen(
        """
            interface Comparator<T> {
                fun compare(a: T, b: T): Int
            }
            typealias AliasComparator<T> = Comparator<T>
            
            @Component
            abstract class MyComponent {
                abstract val compareInt: compare<Int>
                @Binding
                protected fun intComparator(): AliasComparator<Int> = error("")
            }

            @FunBinding
            fun <T> compare(@FunApi a: T, @FunApi b: T, comparator: AliasComparator<T>): Int = comparator
                .compare(a, b)

        """
    )

    @Test
    fun testBindingsCanBeInternalizedViaInternalTypeAliases() = multiCodegen(
        listOf(
            source(
                """
                    internal typealias InternalFoo = Foo

                    @Module
                    object FooBarModule {
                        @Binding
                        fun foo(): InternalFoo = Foo()
                        
                        @Binding
                        fun bar(foo: () -> InternalFoo) = Bar(foo())
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component 
                    abstract class MyComponent {
                        abstract val bar: Bar
                        
                        @Module
                        protected val fooBarModule = FooBarModule
                    }
                """
            )
        )
    )

    @Test
    fun testBindingsCanBeInternalizedViaInternalQualifiers() = multiCodegen(
        listOf(
            source(
                """
                    @Qualifier
                    @Target(AnnotationTarget.TYPE)
                    internal annotation class Internal

                    @Module
                    object FooBarModule {
                        @Binding
                        fun foo(): @Internal Foo = Foo()
                        
                        @Binding
                        fun bar(foo: () -> @Internal Foo) = Bar(foo())
                    }
                """
            )
        ),
        listOf(
            source(
                """
                    @Component 
                    abstract class MyComponent {
                        abstract val bar: Bar
                        
                        @Module
                        protected val fooBarModule = FooBarModule
                    }
                """
            )
        )
    )

    @Test
    fun testBindingTypeParameterInference() = codegen(
        """
            @Binding
            fun map() = mapOf("a" to 0)
            
            @Binding
            fun <M : Map<K, V>, K : CharSequence, V> firstKey(map: M): K = map.keys.first()

            @Component
            abstract class MyComponent {
                abstract val key: String
            }
        """
    )

    @Test
    fun testSuspendDependencyCannotBeRequestedFromComposable() = codegen(
        """
            @Binding
            @Composable
            fun foo() = Foo()

            @Binding
            suspend fun bar(foo: Foo) = Bar(foo)

            @Component
            abstract class MyComponent {
                abstract val bar: Bar
            }
        """
    ) {
        assertInternalError("Call context mismatch")
    }

    @Test
    fun testComposableDependencyCannotBeRequestedFromSuspend() = codegen(
        """
            @Binding
            suspend fun foo() = Foo()

            @Binding
            @Composable
            fun bar(foo: Foo) = Bar(foo)

            @Component
            abstract class MyComponent {
                abstract val bar: Bar
            }
        """
    ) {
        assertInternalError("Call context mismatch")
    }

    @Test
    fun testBindingCannotMixedCallContextDependencies() = codegen(
        """
            @Binding
            suspend fun foo() = Foo()

            @Binding
            @Composable
            fun bar() = Bar(Foo())
            
            @Binding
            fun baz(bar: Bar, foo: Foo) = Baz(bar, foo)

            @Component
            abstract class MyComponent {
                abstract val baz: Baz
            }
        """
    ) {
        assertInternalError("Dependencies call context mismatch")
    }

    @Test
    fun testBindingAdaptsCallContextOfDependency() = codegen(
        """
            @Binding
            @Composable
            fun foo() = Foo()

            @Binding
            fun bar(foo: Foo) = Bar(foo)

            @Component
            abstract class MyComponent {
                @Composable
                abstract val bar: Bar
            }
        """
    )

    @Test
    fun testComponentCannotRequestDependencyWithDifferentCallContext() = codegen(
        """
            @Binding
            @Composable
            fun foo() = Foo()

            @Component
            abstract class MyComponent {
                abstract val foo: Foo
            }
        """
    ) {
        assertInternalError("Call context mismatch")
    }

    @Test
    fun testComponentDoesNotImplementFinalFunction() = codegen(
        """
            @Component
            abstract class MyComponent {
                fun string() = ""
            }
        """
    )
}
