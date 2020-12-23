package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class DivergenceTest {

    @Test
    fun testWithGiven() = codegen(
        """
            fun invoke() = withGiven("", 0) {
                given<String>()
                given<Int>()
            }
        """
    )

    @Test
    fun testUnresolvableDivergence() = codegen(
        """
                interface Wrapper<T> {
                    val value: T
                }

                @Given fun <T> unwrapped(@Given wrapped: Wrapper<T>): T = wrapped.value

                fun lol() {
                    given<Foo>()
                }
        """
    ) {
        assertCompileError("divergent")
    }

    @Test
    fun testResolvableDivergence() = codegen(
        """
                interface Wrapper<T> {
                    val value: T
                }

                @Given fun <T> unwrapped(@Given wrapped: Wrapper<T>): T = wrapped.value

                @Given fun fooWrapper(): Wrapper<Wrapper<Foo>> = error("")

                fun lol() {
                    given<Foo>()
                }
        """
    )

}