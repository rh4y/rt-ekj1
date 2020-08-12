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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.transform.InjektContext

class InjektSymbols(private val injektContext: InjektContext) {
    val effect = injektContext.referenceClass(InjektFqNames.Effect)!!
    val given = injektContext.referenceClass(InjektFqNames.Given)!!
    val mapEntries = injektContext.referenceClass(InjektFqNames.MapEntries)!!
    val module = injektContext.referenceClass(InjektFqNames.Module)!!
    val reader = injektContext.referenceClass(InjektFqNames.Reader)!!
    val setElements = injektContext.referenceClass(InjektFqNames.SetElements)!!
    val storage = injektContext.referenceClass(InjektFqNames.Storage)!!

    val context = injektContext.referenceClass(InjektFqNames.Context)!!
    val genericContext = injektContext.referenceClass(InjektFqNames.GenericContext)!!
    val givenContext = injektContext.referenceClass(InjektFqNames.GivenContext)!!
    val index = injektContext.referenceClass(InjektFqNames.Index)!!
    val qualifier = injektContext.referenceClass(InjektFqNames.Qualifier)!!
    val readerImpl = injektContext.referenceClass(InjektFqNames.ReaderImpl)!!
    val readerInvocation = injektContext.referenceClass(InjektFqNames.ReaderInvocation)!!
    val runReaderContext = injektContext.referenceClass(InjektFqNames.RunReaderContext)!!
    val signature = injektContext.referenceClass(InjektFqNames.Signature)!!
}
