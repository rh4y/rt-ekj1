/*
 * Copyright 2019 Manuel Wrage
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

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ProxyTest {

    @Test
    fun testBridgingDoesNotModifyOriginalBindingState() {
        val module = module {
            single(override = true) { "value" }
            withBinding<String> { bindType<CharSequence>() }
        }

        val component = component { modules(module) }

        val original = component.getBinding<String>(keyOf<String>())
        val proxy = component.getBinding<CharSequence>(keyOf<CharSequence>())

        assertTrue(original === proxy)
        assertFalse(original.unscoped)
        assertTrue(original.override)
    }

}