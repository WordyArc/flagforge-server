package dev.owlmajin.flagforge.server.processor.pipeline

import org.apache.kafka.streams.kstream.KStream

class StreamChain<K, V> internal constructor(
    private val name: String,
    private val stream: KStream<K, V>,
) {


    fun current(): KStream<K, V> =
        requireNotNull(stream) { "StreamChain[$name] has no current stream. Did you forget from(...)?" }


    fun <V2> step(
        description: String,
        transform: (KStream<K, V>) -> KStream<K, V2>,
    ): StreamChain<K, V2> {
        val next = transform(stream)
        return StreamChain(name = "$name/$description", stream = next)
    }

    fun sideEffect(
        description: String,
        effect: (KStream<K, V>) -> Unit,
    ): StreamChain<K, V> {
        effect(stream)
        return this
    }

    fun build(): KStream<K, V> = stream
}


fun <K, Vin, Vout> KStream<K, Vin>.chain(
    name: String,
    block: StreamChain<K, Vin>.() -> KStream<K, Vout>,
): KStream<K, Vout> {
    val chain = StreamChain(name = name, stream = this)
    return chain.block()
}