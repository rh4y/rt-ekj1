package com.ivianuu.injekt.internal

import kotlin.reflect.KClass

object InjektAst {

    annotation class Path {
        annotation class Field(val name: String)
        annotation class Class(val clazz: KClass<*>)
    }

    annotation class ChildFactory

    annotation class Dependency

    annotation class Binding

    annotation class Alias

    annotation class Module

    @Target(AnnotationTarget.TYPE)
    annotation class Qualifiers(val entries: Array<KClass<*>>)

    annotation class Scope

    annotation class Scoped

    annotation class Map {
        annotation class Entry

        annotation class ClassKey(val value: KClass<*>)
        annotation class IntKey(val value: Int)
        annotation class LongKey(val value: Long)
        annotation class StringKey(val value: String)
    }

    annotation class Set {
        annotation class Element
    }
}
