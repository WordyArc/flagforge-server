package dev.owlmajin.flagforge.server.evaluation.api.service

import dev.owlmajin.flagforge.server.evaluation.api.repository.FlagRepository
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.RuleAction
import dev.owlmajin.flagforge.server.model.api.v1.EvaluationDto
import dev.owlmajin.flagforge.server.model.api.v1.EvaluationResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.security.MessageDigest

interface EvaluationService {
    suspend fun evaluate(request: EvaluationDto): EvaluationResult
}

@Service
class EvaluationServiceImpl(private val flagRepository: FlagRepository) : EvaluationService {

    private val log = KotlinLogging.logger { javaClass }

    override suspend fun evaluate(request: EvaluationDto): EvaluationResult {
        val flag: FlagState? = flagRepository.findByKey(
            projectId = request.projectId,
            environmentKey = request.environment,
            flagKey = request.flagKey,
        )
        if (flag == null) {
            log.debug { "Flag not found in read model. projectId=${request.projectId}, env=${request.environment}, key=${request.flagKey}" }
            return notFound(request)
        }

        val eval = evaluateFlag(flag, request.uid, request.attributes)

        return EvaluationResult(
            projectId = request.projectId,
            environment = request.environment,
            flagKey = request.flagKey,
            enabled = eval.enabled,
            variant = eval.variant,
            reason = eval.reason,
        )
    }

    private fun notFound(request: EvaluationDto) = EvaluationResult(
        projectId = request.projectId,
        environment = request.environment,
        flagKey = request.flagKey,
        enabled = false,
        variant = null,
        reason = "FLAG_NOT_FOUND",
    )

    // TODO: чистая функция оценки флага. Перенести в отдельный сервис
    private data class LocalEval(
        val enabled: Boolean,
        val variant: String?,
        val reason: String,
    )

    // - игнорируем RuleTarget
    // - bucket = hash(uid + flagId + salt) % 100
    private fun evaluateFlag(
        flag: FlagState,
        uid: String,
        attrs: Map<String, String>,
    ): LocalEval {
        // TODO: когда появятся сегменты и DSL, сюда добавим фильтрацию по target.
        val rules = flag.rules.sortedBy { it.priority }

        if (rules.isEmpty()) {
            return LocalEval(
                enabled = flag.enabled,
                variant = flag.defaultVariant,
                reason = "FLAG_NO_RULES",
            )
        }

        val bucket = stableBucket("$uid|${flag.id}|${flag.salt}")

        for (rule in rules) {
            when (val action = rule.action) {
                is RuleAction.BooleanAction -> {
                    val enabled = action.enabled
                    val variant = flag.defaultVariant ?: if (enabled) "true" else "false"
                    return LocalEval(
                        enabled = enabled,
                        variant = variant,
                        reason = "RULE_BOOLEAN:${rule.id}",
                    )
                }

                is RuleAction.PercentageAction -> {
                    val enabled = bucket < action.truePercent
                    return LocalEval(
                        enabled = enabled,
                        variant = flag.defaultVariant,
                        reason = "RULE_PERCENTAGE:${rule.id}:bucket=$bucket",
                    )
                }

                is RuleAction.MultiVariantAction -> {
                    val variant = chooseVariant(bucket, action.variants)
                    return LocalEval(
                        enabled = flag.enabled,
                        variant = variant,
                        reason = "RULE_MULTIVARIANT:${rule.id}:bucket=$bucket",
                    )
                }
            }
        }

        // Фолбэк, если ни одно правило не сработало
        return LocalEval(
            enabled = flag.enabled,
            variant = flag.defaultVariant,
            reason = "NO_MATCHING_RULE",
        )
    }

    private fun stableBucket(key: String): Int {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(key.toByteArray(Charsets.UTF_8))

        var value = 0
        for (i in 0 until 4) {
            value = (value shl 8) or (bytes[i].toInt() and 0xFF)
        }
        val positive = value.toLong() and 0xFFFF_FFFFL
        return (positive % 100L).toInt()
    }

    /**
     * Простой выбор варианта по кумулятивным весам.
     * bucket 0..99 → target 0..(total-1).
     */
    private fun chooseVariant(bucket: Int, variants: Map<String, Int>): String? {
        if (variants.isEmpty()) return null

        val sanitized = variants.mapValues { (_, weight) -> weight.coerceAtLeast(0) }
        val total = sanitized.values.sum()
        if (total <= 0) {
            // fallback: просто первый ключ
            return sanitized.keys.first()
        }

        val target = bucket * total / 100

        var acc = 0
        for ((variant, weight) in sanitized) {
            acc += weight
            if (target < acc) return variant
        }

        return sanitized.keys.last()
    }
}
