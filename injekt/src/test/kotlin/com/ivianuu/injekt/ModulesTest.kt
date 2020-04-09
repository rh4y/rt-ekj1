package com.ivianuu.injekt

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ModulesTest {

    @Test
    fun testOrdering() {
        val existingModules = Modules.modulesByScope.toMap()
        Modules.modulesByScope.clear()

        val initA = Module(AnyScope, invocationPhase = Module.InvocationPhase.Init) { }
        val initB = Module(AnyScope, invocationPhase = Module.InvocationPhase.Init) { }
        val nonInitA = Module(AnyScope, invocationPhase = Module.InvocationPhase.Config) { }
        val nonInitB = Module(AnyScope, invocationPhase = Module.InvocationPhase.Config) { }

        Injekt {
            modules(
                initA, nonInitA, nonInitB, initB
            )
        }

        assertEquals(listOf(initA, initB, nonInitA, nonInitB), Modules.get(AnyScope))
        Modules.modulesByScope.clear()
        Modules.modulesByScope += existingModules
    }

    @Test
    fun testGlobalModulesWillBeAppliedToEachOpenComponentBuilderOnRegister() {
        var called = false
        Component {
            Injekt {
                module(AnyScope) {
                    called = true
                }
            }
        }

        assertTrue(called)
    }

    @Test
    fun testAnyScopeWillBeAppliedToEveryComponent() {
        var called = false

        Injekt {
            module(AnyScope) {
                called = true
            }
        }

        Component()

        assertTrue(called)
    }

    @Test
    fun testOnlyAppliedToSpecifiedScopes() {
        var called = false

        Injekt {
            module(ApplicationScope) {
                called = true
            }
        }

        Component()
        assertFalse(called)
        Component { scopes(ApplicationScope) }
        assertTrue(called)

        assertTrue(called)
    }

}
