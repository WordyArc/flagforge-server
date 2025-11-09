package dev.owlmajin.flagforge.server.common

import kotlin.uuid.Uuid

interface IdGenerator {
    fun next() : String
}

class DefaultIdGenerator : IdGenerator {
    override fun next() = Uuid.random().toString()
}
