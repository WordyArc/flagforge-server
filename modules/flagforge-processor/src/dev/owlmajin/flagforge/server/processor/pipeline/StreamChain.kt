package dev.owlmajin.flagforge.server.processor.pipeline

import dev.owlmajin.flagforge.server.processor.rocksdb.KafkaTopic
import dev.owlmajin.flagforge.server.processor.rocksdb.Topology
import org.apache.kafka.streams.kstream.KStream

class StreamChain<K, V> internal constructor(
    private val name: String,
    private val topology: Topology,
    private var stream: KStream<K, V>?,
) {

    fun current(): KStream<K, V> =
        requireNotNull(stream) { "StreamChain[$name] has no current stream. Did you forget from(...)?" }

    fun from(stream: KStream<K, V>): StreamChain<K, V> {
        this.stream = stream
        return this
    }

    fun from(topic: KafkaTopic<K, V>): StreamChain<K, V> =
        from(topology.stream(topic))


    fun <V2> step(
        description: String,
        transform: (KStream<K, V>) -> KStream<K, V2>,
    ): StreamChain<K, V2> {
        val next = transform(current())
        return StreamChain(name = "$name/$description", topology = topology, stream = next)
    }

    fun sideEffect(
        description: String,
        effect: (KStream<K, V>) -> Unit,
    ): StreamChain<K, V> {
        effect(current())
        return this
    }

    fun build(): KStream<K, V> = current()
}

//fun <K, V> Topology.chain(
//    name: String,
//    block: StreamChain<K, V>.() -> KStream<K, V>,
//): KStream<K, V> {
//    val chain = StreamChain<K, V>(name = name, topology = this, stream = null)
//    return chain.block()
//}

fun <K, Vin, Vout> Topology.chain(
    name: String,
    block: StreamChain<K, Vin>.() -> KStream<K, Vout>,
): KStream<K, Vout> {
    val chain = StreamChain<K, Vin>(name = name, topology = this, stream = null)
    return chain.block()
}
