package dev.owlmajin.flagforge.server.processor.handler

import dev.owlmajin.flagforge.server.model.Message
import kotlin.reflect.KClass


interface AbstractMessageHandler<M : Message<*>, C, R : MessageHandlingResult> {
    val messageType: KClass<out M>

    fun handleMessage(message: M, context: C): R

    fun isMessageValid(message: M, context: C): Boolean = true
}
