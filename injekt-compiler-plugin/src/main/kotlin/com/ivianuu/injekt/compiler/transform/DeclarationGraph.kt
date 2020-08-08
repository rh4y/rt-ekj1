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

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import com.ivianuu.injekt.compiler.transform.runreader.RunReaderContextImplTransformer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation

class DeclarationGraph(
    private val indexer: Indexer,
    val module: IrModuleFragment,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer
) {

    private val _runReaderContexts = mutableListOf<IrClass>()
    val runReaderContexts: List<IrClass> get() = _runReaderContexts

    private val _bindings = mutableListOf<IrFunction>()
    val bindings: List<IrFunction> get() = _bindings

    private val _mapEntries = mutableListOf<IrFunction>()
    val mapEntries: List<IrFunction> get() = _mapEntries

    private val _setElements = mutableListOf<IrFunction>()
    val setElements: List<IrFunction> get() = _setElements

    private val _genericContexts = mutableListOf<IrClass>()
    val genericContexts: List<IrClass> get() = _genericContexts

    lateinit var runReaderContextImplTransformer: RunReaderContextImplTransformer

    sealed class ParentRunReaderContext {
        data class Known(val clazz: IrClass) : ParentRunReaderContext()
        object Unknown : ParentRunReaderContext()
    }

    fun getParentRunReaderContexts(context: IrClass): Set<ParentRunReaderContext> {
        val parents = mutableSetOf<ParentRunReaderContext>()

        val processedClasses = mutableSetOf<IrClass>()

        val invokers = getInvokers(context)

        fun collectParents(invoker: IrClass) {
            if (invoker in processedClasses) return
            processedClasses += invoker

            if (isRunReaderContext(invoker)) {
                parents += ParentRunReaderContext.Known(invoker)
                return
            }

            if (isContextOfGivenDeclaration(invoker)) {
                val generatedContextsWithInvokerSuperType = runReaderContextImplTransformer
                    .generatedContexts
                    .values
                    .flatten()
                    .filter { invoker.defaultType in it.superTypes }
                    .map { it.superTypes.first().classOrNull!!.owner }

                if (generatedContextsWithInvokerSuperType.isNotEmpty()) {
                    parents += generatedContextsWithInvokerSuperType
                        .map { ParentRunReaderContext.Known(it) }
                } else {
                    parents += ParentRunReaderContext.Unknown
                }
                return
            }

            getInvokers(invoker)
                .forEach { collectParents(it) }
        }

        invokers.forEach { collectParents(it) }

        return parents
    }

    private fun isRunReaderContext(context: IrClass): Boolean {
        return runReaderContexts
            .map { it.superTypes.first() }
            .any { it == context.defaultType }
    }

    private fun isContextOfGivenDeclaration(context: IrClass): Boolean {
        return (_bindings + _mapEntries + _setElements)
            .any { it.getContext()?.defaultType == context.defaultType }
    }

    private fun getInvokers(context: IrClass): Set<IrClass> {
        val allContexts = listOf(context) + getAllSuperContexts(context)

        val invokerIfRunChildReader = runReaderContexts
            .singleOrNull { it.superTypes[0] == context.defaultType }
            ?.superTypes
            ?.getOrNull(1)
            ?.classOrNull
            ?.owner

        return setOfNotNull(
            *indexer.classIndices
                .filter { it.hasAnnotation(InjektFqNames.ReaderInvocation) }
                .filter { clazz ->
                    clazz.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        0
                    ) in allContexts
                }
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        1
                    )!!
                }
                .filter { it != context }
                .toTypedArray(),
            invokerIfRunChildReader
        )
    }

    private fun getAllSuperContexts(
        context: IrClass
    ): Set<IrClass> {
        return indexer.classIndices
            .filter { it.hasAnnotation(InjektFqNames.ReaderImpl) }
            .filter { clazz ->
                clazz.getClassFromAnnotation(
                    InjektFqNames.ReaderImpl,
                    1
                ) == context
            }
            .map {
                it.getClassFromAnnotation(
                    InjektFqNames.ReaderImpl,
                    0
                )!!
            }
            .toSet()
    }

    fun getAllContextImplementations(
        context: IrClass
    ): Set<IrClass> {
        val contexts = mutableSetOf<IrClass>()

        contexts += context

        val processedClasses = mutableSetOf<IrClass>()

        fun collectImplementations(context: IrClass) {
            if (context in processedClasses) return
            processedClasses += context

            indexer.classIndices
                .filter { it.hasAnnotation(InjektFqNames.ReaderImpl) }
                .filter { clazz ->
                    clazz.getClassFromAnnotation(
                        InjektFqNames.ReaderImpl,
                        0
                    ) == context
                }
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderImpl,
                        1
                    )!!
                }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }

            indexer.classIndices
                .filter { it.hasAnnotation(InjektFqNames.ReaderInvocation) }
                .filter { clazz ->
                    clazz.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        1
                    ) == context && clazz.getConstantFromAnnotationOrNull<Boolean>(
                        InjektFqNames.ReaderInvocation, 2
                    )!!
                }
                .map {
                    it.getClassFromAnnotation(
                        InjektFqNames.ReaderInvocation,
                        0
                    )!!
                }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }

            context.superTypes
                .map { it.classOrNull!!.owner }
                .forEach {
                    contexts += it
                    collectImplementations(it)
                }
        }

        collectImplementations(context)

        return contexts
    }

    fun initialize() {
        collectRunReaderContexts()
        collectBindings()
        collectMapEntries()
        collectSetElements()
        collectGenericContexts()
    }

    private fun collectRunReaderContexts() {
        indexer.classIndices
            .filter { it.hasAnnotation(InjektFqNames.RunReaderContext) }
            .distinct()
            .forEach { _runReaderContexts += it }
    }

    private fun collectBindings() {
        (indexer.functionIndices + indexer.classIndices
            .flatMapFix { it.constructors.toList() } +
                indexer.propertyIndices.mapNotNull { it.getter })
            .filter {
                it.hasAnnotation(InjektFqNames.Given) ||
                        (it is IrConstructor && it.constructedClass.hasAnnotation(InjektFqNames.Given)) ||
                        (it is IrSimpleFunction && it.correspondingPropertySymbol?.owner?.hasAnnotation(
                            InjektFqNames.Given
                        ) == true)
            }
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _bindings += it }
    }

    private fun collectMapEntries() {
        indexer.functionIndices
            .filter { it.hasAnnotation(InjektFqNames.MapEntries) }
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _mapEntries += it }
    }

    private fun collectSetElements() {
        indexer.functionIndices
            .filter { it.hasAnnotation(InjektFqNames.SetElements) }
            .map { implicitContextParamTransformer.getTransformedFunction(it) }
            .filter { it.getContext() != null }
            .distinct()
            .forEach { _setElements += it }
    }

    private fun collectGenericContexts() {
        indexer.classIndices
            .filter { it.hasAnnotation(InjektFqNames.GenericContext) }
            .distinct()
            .forEach { _genericContexts += it }
    }

}
