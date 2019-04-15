/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.android

import androidx.fragment.app.Fragment
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.addConstant

/**
 * Fragment scope
 */
object PerFragment : StringScope("PerFragment")

/**
 * Child fragment scope
 */
object PerChildFragment : StringScope("PerChildFragment")

/**
 * Fragment qualifier
 */
object ForFragment : StringQualifier("ForFragment")

/**
 * Child fragment qualifier
 */
object ForChildFragment : StringQualifier("ForChildFragment")

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : Fragment> T.fragmentComponent(
    createEagerInstances: Boolean = true,
    definition: Component.() -> Unit = {}
): Component = component(createEagerInstances) {
    scopes(PerFragment)
    (getParentFragmentComponentOrNull()
        ?: getActivityComponentOrNull()
        ?: getApplicationComponentOrNull())?.let { dependencies(it) }
    addConstant(this@fragmentComponent)
    definition.invoke(this)
}

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : Fragment> T.childFragmentComponent(
    createEagerInstances: Boolean = true,
    definition: Component.() -> Unit = {}
): Component = component(createEagerInstances) {
    scopes(PerChildFragment)
    (getParentFragmentComponentOrNull()
        ?: getActivityComponentOrNull()
        ?: getApplicationComponentOrNull())?.let { dependencies(it) }
    addConstant(this@childFragmentComponent)
    definition.invoke(this)
}

/**
 * Returns the [Component] of the parent fragment or null
 */
fun Fragment.getParentFragmentComponentOrNull(): Component? =
    (parentFragment as? InjektTrait)?.component

/**
 * Returns the [Component] of the parent fragment or throws
 */
fun Fragment.getParentFragmentComponent(): Component =
    getParentFragmentComponentOrNull() ?: error("No parent fragment component found for $this")

/**
 * Returns the [Component] of the activity or null
 */
fun Fragment.getActivityComponentOrNull(): Component? =
    (activity as? InjektTrait)?.component

/**
 * Returns the [Component] of the activity or throws
 */
fun Fragment.getActivityComponent(): Component =
    getActivityComponentOrNull() ?: error("No activity component found for $this")

/**
 * Returns the [Component] of the application or null
 */
fun Fragment.getApplicationComponentOrNull(): Component? =
    (activity?.application as? InjektTrait)?.component

/**
 * Returns the [Component] of the application or throws
 */
fun Fragment.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")