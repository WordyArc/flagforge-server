package dev.owlmajin.flagforge.server.processor.rocksdb

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Produced

data class KafkaTopic<K, V>(
    val name: String,
    val keySerde: Serde<K>,
    val valueSerde: Serde<V>,
)

fun <K, V> topic(
    name: String,
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
): KafkaTopic<K, V> = KafkaTopic(name, keySerde, valueSerde)

fun <K, V> StreamsBuilder.stream(topic: KafkaTopic<K, V>): KStream<K, V> =
    stream(topic.name, Consumed.with(topic.keySerde, topic.valueSerde))

fun <K, V> StreamsBuilder.table(topic: KafkaTopic<K, V>): KTable<K, V> =
    table(topic.name, Consumed.with(topic.keySerde, topic.valueSerde))

infix fun <K, V> KStream<K, V>.into(topic: KafkaTopic<K, V>) {
    to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}

infix fun <K, V> KStream<K, V?>.intoNullable(topic: KafkaTopic<K, V>) {
    @Suppress("UNCHECKED_CAST")
    val serde = topic.valueSerde as Serde<V?>
    to(topic.name, Produced.with(topic.keySerde, serde))
}

infix fun <K, V> KTable<K, V>.into(topic: KafkaTopic<K, V>) {
    toStream().to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}


class Topology(
    private val builder: StreamsBuilder,
    val topics: Topics,
) {
    fun <K, V> stream(topic: KafkaTopic<K, V>) = builder.stream(topic)
    fun <K, V> table(topic: KafkaTopic<K, V>) = builder.table(topic)
}

inline fun <T> StreamsBuilder.topology(
    topics: Topics,
    block: Topology.() -> T,
): T = Topology(this, topics).block()