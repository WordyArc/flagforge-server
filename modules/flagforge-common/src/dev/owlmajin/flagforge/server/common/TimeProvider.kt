package dev.owlmajin.flagforge.server.common

import java.time.Instant

interface TimeProvider {
    fun now(): Instant
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()
}
