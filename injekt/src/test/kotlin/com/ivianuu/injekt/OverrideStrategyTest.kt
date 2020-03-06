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

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class OverrideStrategyTest {

    @Test
    fun testStrategies() {
        assertTrue(OverrideStrategy.Permit.check(
            existsPredicate = { false },
            errorMessage = { "" }
        ))

        assertTrue(OverrideStrategy.Permit.check(
            existsPredicate = { true },
            errorMessage = { "" }
        ))

        assertTrue(OverrideStrategy.Drop.check(
            existsPredicate = { false },
            errorMessage = { "" }
        ))

        assertFalse(OverrideStrategy.Drop.check(
            existsPredicate = { true },
            errorMessage = { "" }
        ))

        assertTrue(
            OverrideStrategy.Fail.check(
                existsPredicate = { false },
                errorMessage = { "" }
            )
        )

        assertTrue(
            try {
                OverrideStrategy.Fail.check(
                    existsPredicate = { true },
                    errorMessage = { "" }
                )
                false
            } catch (e: Exception) {
                true
            }
        )
    }

}