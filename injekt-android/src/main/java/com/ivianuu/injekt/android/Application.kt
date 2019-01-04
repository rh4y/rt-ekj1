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

import android.app.Application
import android.content.Context
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.instanceModule

const val APPLICATION_SCOPE = "application_scope"

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Application> T.applicationComponent(
    instance: T,
    name: String? = javaClass.simpleName + "Component",
    scope: String? = APPLICATION_SCOPE,
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition? = null
) = component(name, scope, createEagerInstances) {
    modules(instanceModule(instance), applicationModule(instance))
    definition?.invoke(this)
}

const val APPLICATION = "application"
const val APPLICATION_CONTEXT = "application_context"

/**
 * Returns a [Module] with convenient definitions
 */
fun <T : Application> applicationModule(
    instance: T,
    name: String? = "ApplicationModule"
) = module(name) {
    // service
    factory(APPLICATION) { instance as Application }
    bind<Context, Application>(APPLICATION_CONTEXT)
}

fun DefinitionContext.application() = get<Application>(APPLICATION)

fun DefinitionContext.applicationContext() = get<Context>(APPLICATION_CONTEXT)