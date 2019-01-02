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

import android.content.Context
import androidx.fragment.app.Fragment
import com.ivianuu.injekt.ComponentHolder
import com.ivianuu.injekt.android.fragment.fragmentComponent
import com.ivianuu.injekt.codegen.Single
import com.ivianuu.injekt.inject

/**
 * @author Manuel Wrage (IVIanuu)
 */
class MyFragment : Fragment(), ComponentHolder {

    override val component by lazy { fragmentComponent(this) }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()
    private val myFragmentDependency by inject<MyFragmentDependency>()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        appDependency
        mainActivityDependency
        myFragmentDependency
    }
}

@Single
class MyFragmentDependency(
    val app: App,
    val mainActivity: MainActivity,
    val myFragment: MyFragment
)