package dev.owlmajin.flagforge.server.common

import dev.owlmajin.flagforge.server.model.CommandId
import dev.owlmajin.flagforge.server.model.FlagId
import kotlin.uuid.Uuid

interface IdGenerator {
    fun nextCommandId() : CommandId
    fun nextFlagId() : FlagId
}

class DefaultIdGenerator : IdGenerator {
    override fun nextCommandId() = CommandId(generateUuid())
    override fun nextFlagId() = FlagId(generateUuid())
}

private fun generateUuid() = Uuid.random()