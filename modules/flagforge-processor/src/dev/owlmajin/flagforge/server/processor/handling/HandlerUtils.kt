package dev.owlmajin.flagforge.server.processor.handling

import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

internal fun <T : Any> Any?.requireTypeOrNull(expected: KClass<out T>?): T? {
    if (expected == null) return null
    if (this == null) return null

    return expected.safeCast(this)
        ?: throw IllegalArgumentException("State type mismatch: expected ${expected.qualifiedName}, actual ${this::class.qualifiedName}")
}
