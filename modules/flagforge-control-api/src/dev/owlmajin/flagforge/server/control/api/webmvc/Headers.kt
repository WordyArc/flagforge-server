package dev.owlmajin.flagforge.server.control.api.webmvc

const val ACTOR_HEADER = "X-Actor-Id"

fun resolveActorId(rawHeader: String?): String =
    rawHeader?.takeIf { it.isNotBlank() } ?: "system"