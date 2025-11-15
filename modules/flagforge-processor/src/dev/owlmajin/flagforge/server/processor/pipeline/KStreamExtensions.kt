package dev.owlmajin.flagforge.server.processor.pipeline

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.kstream.KStream

val log = KotlinLogging.logger {}

inline fun <K, V> KStream<K, V>.debugLog(
    label: String,
    crossinline message: (key: K, value: V) -> String,
): KStream<K, V> =
    peek { key, value -> log.debug { "[$label] ${message(key, value)}" } }

inline fun <K, V> KStream<K, V>.infoLog(
    label: String,
    crossinline message: (key: K, value: V) -> String,
): KStream<K, V> =
    peek { key, value -> log.info { "[$label] ${message(key, value)}" } }