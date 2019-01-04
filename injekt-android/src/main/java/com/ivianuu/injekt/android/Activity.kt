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

import android.app.Activity
import android.content.Context
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.instanceModule

const val ACTIVITY_SCOPE = "activity_scope"

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Activity> activityComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    scope: String? = ACTIVITY_SCOPE,
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition? = null
) = component(name, scope, createEagerInstances) {
    instance.parentComponent()?.let { components(it) }
    modules(instanceModule(instance), activityModule(instance))
    definition?.invoke(this)
}

/**
 * Returns the parent [Component] if available or null
 */
fun Activity.parentComponent() = (application as? InjektTrait)?.component

const val ACTIVITY = "activity"
const val ACTIVITY_CONTEXT = "activity_context"

/**
 * Returns a [Module] with convenient definitions
 */
fun <T : Activity> activityModule(
    instance: T,
    name: String? = "ActivityModule"
) = module(name) {
    // activity
    factory(ACTIVITY) { instance as Activity }
    bind<Context, Activity>(ACTIVITY_CONTEXT)
}

fun DefinitionContext.activity() = get<Activity>(ACTIVITY)

fun DefinitionContext.activityContext() = get<Context>(ACTIVITY_CONTEXT)