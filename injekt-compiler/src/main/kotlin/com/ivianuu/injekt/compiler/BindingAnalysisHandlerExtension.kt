/*
 * Copyright 2019 Manuel Wrage
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

import java.io.File
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.classRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

class BindingAnalysisHandlerExtension(
    private val outputDir: File
) : AnalysisHandlerExtension {

    private var generatedFiles = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (generatedFiles) return null
        generatedFiles = true

        msg { "on analysis complete" }

        val resolveSession = componentProvider.get<ResolveSession>()

        files.forEach { file ->
            val diagnosticCount = bindingTrace.bindingContext.diagnostics.all().size

            file.accept(
                classRecursiveVisitor { ktClass ->
                    val classDescriptor =
                        resolveSession.resolveToDescriptor(ktClass) as ClassDescriptor
                    msg { "process class $ktClass desc is $classDescriptor" }

                    val descriptor = createBindingDescriptor(
                        classDescriptor,
                        bindingTrace
                    ) ?: return@classRecursiveVisitor

                    if (bindingTrace.bindingContext.diagnostics.all().size != diagnosticCount) {
                        return@classRecursiveVisitor
                    }

                    val generator = BindingGenerator(descriptor)
                    generator.generate().writeTo(outputDir)
                }
            )
        }

        return if (bindingTrace.bindingContext.diagnostics.isEmpty()) {
            msg { "analysis try with addional roots" }
            AnalysisResult.RetryWithAdditionalRoots(
                bindingContext = bindingTrace.bindingContext,
                moduleDescriptor = module,
                additionalJavaRoots = emptyList(),
                additionalKotlinRoots = listOf(outputDir)
            )
        } else {
            msg { "analysis error" }
            AnalysisResult.compilationError(bindingTrace.bindingContext)
        }
    }
}
