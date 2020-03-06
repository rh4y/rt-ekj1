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

package com.ivianuu.injekt.android

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

inline fun <reified T : Application> ApplicationComponent(
    instance: T,
    block: ComponentBuilder.() -> Unit = {}
): Component = ApplicationComponent(instance = instance, type = typeOf(), block = block)

inline fun <T : Application> ApplicationComponent(
    instance: T,
    type: Type<T>,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(ApplicationScope)

        instance(instance, type = type)
            .bindAlias<Application>()
        contextBindings(ForApplication) { instance }
        maybeLifecycleBindings(
            ProcessLifecycleOwner.get(),
            ForApplication
        )
        componentAlias(ApplicationScope)

        block()
    }

@Scope
annotation class ApplicationScope {
    companion object
}

@Name
annotation class ForApplication {
    companion object
}
