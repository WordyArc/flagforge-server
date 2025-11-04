package dev.owlmajin.flagforge.server.model

import kotlin.uuid.Uuid

@JvmInline
value class ProjectId(val value: Uuid)

@JvmInline
value class EnvironmentId(val value: Uuid)

@JvmInline
value class SegmentId(val value: Uuid)

@JvmInline
value class FlagId(val value: Uuid)

@JvmInline
value class SdkKeyId(val value: Uuid)

@JvmInline
value class ProjectKey(val value: String)

@JvmInline
value class EnvironmentKey(val value: String)

@JvmInline
value class FlagKey(val value: String)

@JvmInline
value class ActorId(val value: String)

@JvmInline
value class CommandId(val value: Uuid)
