package com.yozora.aichat.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class ElevenLabsTtsRepository {
    suspend fun generateSpeechMp3(
        apiKey: String,
        voiceId: String,
        modelId: String,
        text: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val encodedVoiceId = URLEncoder.encode(voiceId, "UTF-8")
        val url = URL(
            "https://api.elevenlabs.io/v1/text-to-speech/$encodedVoiceId" +
                "?output_format=mp3_44100_128"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Accept", "audio/mpeg")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("xi-api-key", apiKey)
        }

        val body = JSONObject()
            .put("text", text)
            .put("model_id", modelId)
            .put(
                "voice_settings",
                JSONObject()
                    .put("stability", 0.5)
                    .put("similarity_boost", 0.75)
                    .put("style", 0.25)
                    .put("use_speaker_boost", true)
            )

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(body.toString())
        }

        val status = connection.responseCode
        if (status in 200..299) {
            connection.inputStream.use { it.readBytes() }
        } else {
            val errorBody = runCatching {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
            }.getOrNull().orEmpty()
            throw ElevenLabsTtsException(
                statusCode = status,
                detail = errorBody.ifBlank { connection.responseMessage.orEmpty() }
            )
        }
    }
}

class ElevenLabsTtsException(
    val statusCode: Int,
    detail: String
) : Exception("ElevenLabs TTS failed ($statusCode): ${detail.take(240)}")
