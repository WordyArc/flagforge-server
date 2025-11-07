package dev.owlmajin.flagforge.server.common.kafka.producer

import kotlinx.coroutines.future.await
import org.apache.kafka.common.header.Header
import org.springframework.kafka.support.SendResult

suspend fun <K: Any, V: Any> KafkaProducer<K, V>.sendAndAwait(key: K, value: V): SendResult<K, V> {
    return send(key, value).await()
}

suspend fun <K: Any, V: Any> KafkaProducer<K, V>.sendAndAwait(key: K, value: V, headers: List<Header>): SendResult<K, V> {
    return send(key, value, headers).await()
}
