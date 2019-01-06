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

package com.ivianuu.injekt.sample

import android.app.Application
import android.content.Context
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.androidLogger
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.annotations.Module
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.injekt.multibinding.bindIntoMap
import com.ivianuu.injekt.multibinding.injectMap
import kotlin.reflect.KClass

@Module private object AutoModule

/**
 * @author Manuel Wrage (IVIanuu)
 */
class App : Application(), InjektTrait {

    override val component by lazy {
        applicationComponent(this) {
            modules(appModule, autoModule)
        }
    }

    private val appDependency by inject<AppDependency>()

    private val dependencies by injectMap<KClass<out Dependency>, Dependency>(DEPS)

    override fun onCreate() {
        configureInjekt {
            androidLogger()
        }

        d { "Injected app dependency $appDependency" }
        d { "All dependencies $dependencies" }

        super.onCreate()
    }
}

const val DEPS = "deps"

@Single(scopeId = ACTIVITY_SCOPE)
class ADep

class AppDependency(val app: App, val context: Context) : Dependency

val appModule = module {
    single { AppDependency(get(), get()) } bindIntoMap (DEPS to AppDependency::class)
}