package dev.owlmajin.flagforge.server.evaluation.api.repository

import dev.owlmajin.flagforge.server.evaluation.api.FLAG_INDEX_STORE_NAME
import dev.owlmajin.flagforge.server.evaluation.api.FLAG_STATE_STORE_NAME
import dev.owlmajin.flagforge.server.model.FlagState
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.springframework.kafka.streams.KafkaStreamsInteractiveQueryService
import org.springframework.stereotype.Repository

@Repository
class FlagRepository(private val interactiveQueryService: KafkaStreamsInteractiveQueryService) {

    private val indexStore: ReadOnlyKeyValueStore<String, String>
        get() = interactiveQueryService.retrieveQueryableStore(
            FLAG_INDEX_STORE_NAME,
            QueryableStoreTypes.keyValueStore(),
        )

    private val flagStore: ReadOnlyKeyValueStore<String, FlagState>
        get() = interactiveQueryService.retrieveQueryableStore(
            FLAG_INDEX_STORE_NAME,
            QueryableStoreTypes.keyValueStore(),
        )

    fun findByKey(
        projectId: String,
        environmentKey: String,
        flagKey: String,
    ): FlagState? {
        val indexKey = "$projectId|$environmentKey|$flagKey"
        val flagId = indexStore.get(indexKey) ?: return null
        return flagStore.get(flagId)
    }

    fun getFlagByIndexKey(indexKey: String): String? =
        indexStore.get(indexKey)

    fun getFlagStateById(flagId: String): FlagState? =
        flagStore.get(flagId)
}