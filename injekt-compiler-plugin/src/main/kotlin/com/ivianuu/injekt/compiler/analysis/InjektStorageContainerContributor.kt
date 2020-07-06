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

package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

class InjektStorageContainerContributor(
    private val typeAnnotationChecker: TypeAnnotationChecker
) : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(AnnotatedBindingChecker())
        container.useInstance(BindingEffectChecker())
        container.useInstance(CompositionComponentChecker())
        container.useInstance(DslCallChecker())
        container.useInstance(FactoryChecker())
        container.useInstance(MapChecker())
        container.useInstance(ModuleChecker())
        container.useInstance(QualifierChecker())
        container.useInstance(ReaderChecker(typeAnnotationChecker))
        container.useInstance(SetChecker())
        container.useInstance(typeAnnotationChecker)
    }
}
