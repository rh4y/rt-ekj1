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

package com.ivianuu.injekt

/**
 * Interface to distinct [Binding]s of the same type
 */
interface Name

/**
 * A [Name] which uses a [name]
 */
open class StringName(val name: String) : Name {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StringName) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

}

/**
 * Returns a new [StringName] for [name]
 */
fun named(name: String): StringName = StringName(name)