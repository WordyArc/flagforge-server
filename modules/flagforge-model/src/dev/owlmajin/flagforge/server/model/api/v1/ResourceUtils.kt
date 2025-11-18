package dev.owlmajin.flagforge.server.model.api.v1

import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.model.project.ProjectState

private const val PROJECTS = "projects"
private const val ENVS = "envs"
private const val FLAGS = "flags"

fun projectResourceName(projectId: String): String = "$PROJECTS/$projectId"

fun environmentResourceName(
    projectId: String,
    environmentKey: String,
): String = "$PROJECTS/$projectId/$ENVS/$environmentKey"

fun flagResourceName(
    projectId: String,
    environmentKey: String,
    flagKey: String,
): String = environmentResourceName(projectId, environmentKey) + "/$FLAGS/$flagKey"


fun ProjectState.resourceName(): String = projectResourceName(id)

fun EnvironmentState.resourceName(): String = environmentResourceName(projectId = projectId, environmentKey = key)

fun FlagState.resourceName(): String = flagResourceName(
    projectId = projectId,
    environmentKey = environmentKey,
    flagKey = key,
)