package dev.owlmajin.flagforge.server.model

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

enum class FlagType {
    BOOLEAN,
    PERCENTAGE,
    MULTIVARIANT,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlagAggregate(
    val id: String,
    val projectId: String,
    val environmentKey: String,
    val key: String,
    val type: FlagType,
    val enabled: Boolean,
    val rules: List<FlagRule>,
    val defaultVariant: String?,
    val version: Long,
    val salt: String,
    val updatedAt: Instant,
)

data class FlagRule(
    val id: String,
    val priority: Int,
    val target: RuleTarget,
    val action: RuleAction,
)

sealed interface RuleTarget {
    data class SegmentRef(val segmentId: String) : RuleTarget
    data class InlineCondition(val expression: String) : RuleTarget
}

sealed interface RuleAction {
    data class BooleanAction(val enabled: Boolean) : RuleAction
    data class PercentageAction(val truePercent: Int) : RuleAction
    data class MultiVariantAction(val variants: Map<String, Int>) : RuleAction
}
