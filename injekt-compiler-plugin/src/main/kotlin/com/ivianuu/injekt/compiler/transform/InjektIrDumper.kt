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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektWritableSlices
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.FakeOverridesStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import java.io.File

class InjektIrDumper(private val fileManager: FileManager) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.files.forEach { irFile ->
            fileManager.markFileSeen(irFile.fileEntry.name)
            if (pluginContext.bindingContext[InjektWritableSlices.GIVEN_CALLS_IN_FILE,
                        irFile.fileEntry.name] != null) {
                fileManager.markGivenCallInFile(irFile.fileEntry.name)
            }
            val file = File(irFile.fileEntry.name)
            val content = try {
                irFile.dumpKotlinLike(
                    KotlinLikeDumpOptions(
                        useNamedArguments = true,
                        printFakeOverridesStrategy = FakeOverridesStrategy.NONE
                    )
                )
            } catch (e: Throwable) {
                e.stackTraceToString()
            }
            fileManager.generateFile(
                irFile.fqName,
                file.name.removeSuffix(".kt"),
                file.absolutePath,
                content
            )
        }
        fileManager.postGenerate()
    }
}