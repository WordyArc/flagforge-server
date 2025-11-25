package dev.owlmajin.flagforge.server.processor.handling.flag

import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.flag.FlagToggledEvent
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.processor.handling.EventContext
import dev.owlmajin.flagforge.server.processor.handling.EventHandler
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.reflect.KClass
import org.springframework.stereotype.Component

@Component
class FlagToggledEventHandler : AbstractFlagEventHandler<FlagToggledEvent>(FlagToggledEvent::class) {

    override fun handle(
        message: EventMessage<FlagToggledEvent>,
        context: EventContext<FlagState>,
    ): EventResult {
        val current = context.currentState
        val payload = message.payload

        if (current == null) {
            klog.warn { "FlagToggled ignored because state does not exist. flagId=${payload.flagId}" }
            return EventResult.Ignored
        }

        if (payload.version <= current.version) {
            klog.debug { "FlagToggled ignored because older or equal version. flagId=${payload.flagId}, eventVersion=${payload.version}, currentVersion=${current.version}" }
            return EventResult.Ignored
        }

        val state = current.copy(
            enabled = payload.enabled,
            version = payload.version,
            updatedAt = message.header.timestamp,
        )

        return EventResult.Applied(current, state, message)
    }

}
