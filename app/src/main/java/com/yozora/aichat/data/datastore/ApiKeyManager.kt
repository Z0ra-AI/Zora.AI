package com.yozora.aichat.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ApiKeyManager(
    private val dataStore: DataStore<Preferences>
) {
    private val keysKey = stringSetPreferencesKey("gemini_api_keys")
    private val activeIndexKey = intPreferencesKey("gemini_active_key_index")
    private val tavilyApiKey = stringPreferencesKey("tavily_api_key")
    private val r34ApiKey = stringPreferencesKey("r34_api_key")
    private val r34UserId = stringPreferencesKey("r34_user_id")
    private val elevenLabsApiKey = stringPreferencesKey("elevenlabs_api_key")
    private val elevenLabsVoiceId = stringPreferencesKey("elevenlabs_voice_id")
    private val elevenLabsModelId = stringPreferencesKey("elevenlabs_model_id")
    private val summarizerApiKey = stringPreferencesKey("summarizer_api_key")

    val keys: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[keysKey]?.toList().orEmpty()
    }

    suspend fun addKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val updated = preferences[keysKey].orEmpty().toMutableSet()
            updated += trimmed
            preferences[keysKey] = updated
            preferences[activeIndexKey] = preferences[activeIndexKey] ?: 0
        }
    }

    suspend fun replaceWithSingleKey(key: String) {
        val trimmed = key.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(keysKey)
                preferences.remove(activeIndexKey)
            } else {
                preferences[keysKey] = setOf(trimmed)
                preferences[activeIndexKey] = 0
            }
        }
    }

    suspend fun clearKeys() {
        dataStore.edit { preferences ->
            preferences.remove(keysKey)
            preferences.remove(activeIndexKey)
        }
    }

    suspend fun removeKey(key: String) {
        dataStore.edit { preferences ->
            val updated = preferences[keysKey].orEmpty().toMutableSet()
            updated -= key
            preferences[keysKey] = updated
            val lastIndex = (updated.size - 1).coerceAtLeast(0)
            preferences[activeIndexKey] = (preferences[activeIndexKey] ?: 0).coerceAtMost(lastIndex)
        }
    }

    suspend fun currentKey(): String? {
        val preferences = dataStore.data.first()
        val allKeys = preferences[keysKey]?.toList().orEmpty()
        if (allKeys.isEmpty()) return null
        val index = (preferences[activeIndexKey] ?: 0).floorMod(allKeys.size)
        return allKeys[index]
    }

    suspend fun keyForProvider(providerId: String): String? {
        val preferences = dataStore.data.first()
        return preferences[providerKey(providerId)] ?: currentKey()
    }

    suspend fun replaceProviderKey(providerId: String, key: String) {
        val trimmed = key.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(providerKey(providerId))
            } else {
                preferences[providerKey(providerId)] = trimmed
            }
        }
    }

    suspend fun clearProviderKey(providerId: String) {
        dataStore.edit { preferences ->
            preferences.remove(providerKey(providerId))
        }
    }

    val tavilyKey: Flow<String?> = dataStore.data.map { preferences ->
        preferences[tavilyApiKey]
    }

    suspend fun keyForTavily(): String? {
        return dataStore.data.first()[tavilyApiKey]
    }

    suspend fun replaceTavilyKey(key: String) {
        val trimmed = key.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(tavilyApiKey)
            } else {
                preferences[tavilyApiKey] = trimmed
            }
        }
    }

    suspend fun clearTavilyKey() {
        dataStore.edit { preferences ->
            preferences.remove(tavilyApiKey)
        }
    }

    val rule34ApiKey: Flow<String?> = dataStore.data.map { preferences ->
        preferences[r34ApiKey]
    }

    val rule34UserId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[r34UserId]
    }

    val elevenLabsKey: Flow<String?> = dataStore.data.map { preferences ->
        preferences[elevenLabsApiKey]
    }

    val elevenLabsVoice: Flow<String?> = dataStore.data.map { preferences ->
        preferences[elevenLabsVoiceId]
    }

    val elevenLabsModel: Flow<String?> = dataStore.data.map { preferences ->
        preferences[elevenLabsModelId]
    }

    val summarizerKey: Flow<String?> = dataStore.data.map { preferences ->
        preferences[summarizerApiKey]
    }

    suspend fun keyForRule34Api(): String? {
        return dataStore.data.first()[r34ApiKey]
    }

    suspend fun userIdForRule34(): String? {
        return dataStore.data.first()[r34UserId]
    }

    suspend fun replaceRule34ApiKey(key: String) {
        val trimmed = key.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(r34ApiKey)
            } else {
                preferences[r34ApiKey] = trimmed
            }
        }
    }

    suspend fun replaceRule34UserId(userId: String) {
        val trimmed = userId.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(r34UserId)
            } else {
                preferences[r34UserId] = trimmed
            }
        }
    }

    suspend fun clearRule34ApiKey() {
        dataStore.edit { preferences ->
            preferences.remove(r34ApiKey)
        }
    }

    suspend fun clearRule34UserId() {
        dataStore.edit { preferences ->
            preferences.remove(r34UserId)
        }
    }

    suspend fun keyForElevenLabs(): String? {
        return dataStore.data.first()[elevenLabsApiKey]
    }

    suspend fun voiceIdForElevenLabs(): String? {
        return dataStore.data.first()[elevenLabsVoiceId]
    }

    suspend fun modelIdForElevenLabs(): String {
        return dataStore.data.first()[elevenLabsModelId]
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_ELEVENLABS_MODEL_ID
    }

    suspend fun replaceElevenLabsKey(key: String) {
        val trimmed = key.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(elevenLabsApiKey)
            } else {
                preferences[elevenLabsApiKey] = trimmed
            }
        }
    }

    suspend fun replaceElevenLabsVoiceId(voiceId: String) {
        val trimmed = voiceId.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(elevenLabsVoiceId)
            } else {
                preferences[elevenLabsVoiceId] = trimmed
            }
        }
    }

    suspend fun replaceElevenLabsModelId(modelId: String) {
        val trimmed = modelId.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(elevenLabsModelId)
            } else {
                preferences[elevenLabsModelId] = trimmed
            }
        }
    }

    suspend fun clearElevenLabsKey() {
        dataStore.edit { preferences ->
            preferences.remove(elevenLabsApiKey)
        }
    }

    suspend fun clearElevenLabsVoiceId() {
        dataStore.edit { preferences ->
            preferences.remove(elevenLabsVoiceId)
        }
    }

    suspend fun clearElevenLabsModelId() {
        dataStore.edit { preferences ->
            preferences.remove(elevenLabsModelId)
        }
    }

    suspend fun keyForSummarizer(): String? {
        return dataStore.data.first()[summarizerApiKey]
    }

    suspend fun replaceSummarizerKey(key: String) {
        val trimmed = key.trim()
        dataStore.edit { preferences ->
            if (trimmed.isEmpty()) {
                preferences.remove(summarizerApiKey)
            } else {
                preferences[summarizerApiKey] = trimmed
            }
        }
    }

    suspend fun clearSummarizerKey() {
        dataStore.edit { preferences ->
            preferences.remove(summarizerApiKey)
        }
    }

    fun providerKeys(providerIds: List<String>): Flow<Map<String, String>> {
        return dataStore.data.map { preferences ->
            providerIds.mapNotNull { id ->
                preferences[providerKey(id)]?.let { id to it }
            }.toMap()
        }
    }

    suspend fun rotateKey(): String? {
        var nextKey: String? = null
        dataStore.edit { preferences ->
            val allKeys = preferences[keysKey]?.toList().orEmpty()
            if (allKeys.isEmpty()) return@edit
            val nextIndex = ((preferences[activeIndexKey] ?: 0) + 1).floorMod(allKeys.size)
            preferences[activeIndexKey] = nextIndex
            nextKey = allKeys[nextIndex]
        }
        return nextKey
    }

    fun mask(key: String): String {
        if (key.length <= 10) return "*".repeat(key.length)
        return key.take(4) + "..." + key.takeLast(6)
    }

    private fun providerKey(providerId: String) = stringPreferencesKey("api_key_$providerId")

    private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

    companion object {
        const val DEFAULT_ELEVENLABS_MODEL_ID = "eleven_multilingual_v2"
    }
}
