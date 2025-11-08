package dev.owlmajin.flagforge.server.common.kafka.producer

import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.kafka.common.KafkaException
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resumeWithException

suspend fun <K : Any, V : Any> TopicProducer<K, V>.sendAwait(
    key: K,
    value: V,
): SendResult<K, V> = send(key, value).await()

suspend fun <K : Any, V : Any> TopicProducer<K, V>.sendAwait(
    key: K,
    value: V,
    extraHeaders: Map<String, Any?>,
): SendResult<K, V> = send(key, value, extraHeaders).await()

suspend fun <K : Any, V : Any> TopicProducer<K, V>.sendAllAwait(records: List<Pair<K, V>>) {
    if (records.isEmpty()) return

    for ((key, value) in records) {
        try {
            send(key, value).await()
        } catch (ex: Exception) {
            throw KafkaException("Failed to send record to topic ${topic.effectiveName}", ex)
        }
    }
}

private suspend fun <K : Any, V : Any> CompletableFuture<SendResult<K, V>>.await(): SendResult<K, V> =
    suspendCancellableCoroutine { cont ->
        whenComplete { result, error ->
            if (error != null) cont.resumeWithException(error)
            else cont.resumeWith(
                Result.success(
                requireNotNull(result) { "SendResult must not be null" }
            ))
        }

        cont.invokeOnCancellation {
            if (!isDone && !isCancelled) {
                cancel(true)
            }
        }
    }
