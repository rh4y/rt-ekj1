package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.FactoryTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Graph(
    val factoryTransformer: FactoryTransformer,
    val factoryExpressions: FactoryExpressions,
    val factoryMembers: FactoryMembers,
    factoryImplementationNode: FactoryImplementationNode,
    context: IrPluginContext,
    factoryImplementationModule: ModuleNode?,
    declarationStore: InjektDeclarationStore,
    symbols: InjektSymbols
) {

    private val scopes = mutableSetOf<FqName>()

    private val explicitBindingResolvers = mutableListOf<BindingResolver>()
    private val implicitBindingResolvers = mutableListOf<BindingResolver>()
    private val setBindingResolver = SetBindingResolver(context, symbols)
    private val resolvedBindings = mutableMapOf<Key, BindingNode>()

    init {
        if (factoryImplementationModule != null) addModule(factoryImplementationModule)
        implicitBindingResolvers += LazyOrProviderBindingResolver(symbols)
        implicitBindingResolvers += setBindingResolver
        implicitBindingResolvers += FactoryImplementationBindingResolver(
            factoryImplementationNode
        )
        implicitBindingResolvers += AnnotatedClassBindingResolver(
            context,
            declarationStore
        )
    }

    fun getBinding(key: Key): BindingNode {
        return resolvedBindings.getOrPut(key) {
            val explicitBindings = explicitBindingResolvers.flatMap { it(key) }
            if (explicitBindings.size > 1) {
                error("Multiple bindings found for $key")
            }

            var binding = explicitBindings.singleOrNull()

            if (binding == null) {
                val implicitBindings = implicitBindingResolvers.flatMap { it(key) }
                binding = implicitBindings.singleOrNull() ?: error("No binding found for $key")
                if (binding.targetScope != null && binding.targetScope !in scopes) {
                    error(
                        "Scope mismatch binding ${binding.key} " +
                                "with scope ${binding.targetScope} is not compatible with this component $scopes"
                    )
                }
            }

            binding
        }
    }

    private fun addModule(moduleNode: ModuleNode) {
        val module = moduleNode.module

        val descriptor = module.declarations.single {
            it is IrClass && it.nameForIrSerialization.asString() == "Descriptor"
        } as IrClass

        val functions = descriptor.functions

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstScope) }
            .forEach { addScope(it.returnType.classOrNull!!.descriptor.fqNameSafe) }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstDependency) }
            .forEach { function ->
                val field = module.fields
                    .single { field ->
                        field.name.asString() == function.getAnnotation(InjektFqNames.AstFieldPath)!!
                            .getValueArgument(0)!!
                            .let { it as IrConst<String> }
                            .value
                    }
                addExplicitBindingResolver(
                    DependencyBindingResolver(
                        injektTransformer = factoryTransformer,
                        dependencyNode = DependencyNode(
                            dependency = function.returnType.classOrNull!!.owner,
                            key = Key(function.returnType),
                            initializerAccessor = moduleNode.initializerAccessor.child(field)
                        ),
                        expressions = factoryExpressions,
                        members = factoryMembers
                    )
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstSet) }
            .forEach { function ->
                addSet(Key(function.returnType))
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstSetElement) }
            .forEach { function ->
                addSetElement(
                    Key(function.valueParameters[0].type),
                    Key(function.valueParameters[1].type)
                )
            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstModule) }
            .map { it to it.returnType.classOrNull?.owner as IrClass }
            .forEach { (function, includedModule) ->
                val field = module.fields
                    .single { field ->
                        field.name.asString() == function.getAnnotation(InjektFqNames.AstFieldPath)!!
                            .getValueArgument(0)!!
                            .let { it as IrConst<String> }
                            .value
                    }
                addModule(
                    ModuleNode(
                        includedModule,
                        Key(includedModule.defaultType),
                        moduleNode.initializerAccessor.child(field)
                    )
                )
            }

        addExplicitBindingResolver(
            ModuleBindingResolver(
                moduleNode,
                descriptor
            )
        )
    }

    private fun addScope(scope: FqName) {
        scopes += scope
    }

    private fun addExplicitBindingResolver(bindingResolver: BindingResolver) {
        explicitBindingResolvers += bindingResolver
    }

    private fun addSet(key: Key) {
        setBindingResolver.addSet(key)
    }

    private fun addSetElement(setKey: Key, elementKey: Key) {
        setBindingResolver.addSetElement(setKey, DependencyRequest(elementKey))
    }
}
