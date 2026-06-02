package com.yozora.aichat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class ChatDao {
    @Query("SELECT * FROM sessions ORDER BY sortOrder ASC")
    abstract suspend fun sessions(): List<ChatSessionEntity>

    @Query("SELECT * FROM group_members WHERE sessionId = :sessionId ORDER BY position ASC")
    abstract suspend fun membersForSession(sessionId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM messages WHERE chatId = :sessionId ORDER BY position ASC")
    abstract suspend fun messagesForSession(sessionId: String): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM sessions")
    abstract suspend fun sessionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSessions(sessions: List<ChatSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMembers(members: List<GroupMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTtsAudioCache(cache: TtsAudioCacheEntity)

    @Query(
        """
        SELECT * FROM tts_audio_cache
        WHERE messageId = :messageId
            AND sourceHash = :sourceHash
            AND provider = :provider
            AND voiceId = :voiceId
            AND modelId = :modelId
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    abstract suspend fun latestTtsAudioCache(
        messageId: String,
        sourceHash: String,
        provider: String,
        voiceId: String,
        modelId: String
    ): TtsAudioCacheEntity?

    @Query(
        """
        SELECT * FROM tts_audio_cache
        WHERE messageId = :messageId
            AND sourceHash = :sourceHash
            AND preparedTextHash = :preparedTextHash
            AND provider = :provider
            AND voiceId = :voiceId
            AND modelId = :modelId
        LIMIT 1
        """
    )
    abstract suspend fun exactTtsAudioCache(
        messageId: String,
        sourceHash: String,
        preparedTextHash: String,
        provider: String,
        voiceId: String,
        modelId: String
    ): TtsAudioCacheEntity?

    @Query("DELETE FROM tts_audio_cache WHERE id = :id")
    abstract suspend fun deleteTtsAudioCache(id: String)

    @Query("DELETE FROM messages")
    abstract suspend fun deleteMessages()

    @Query("DELETE FROM group_members")
    abstract suspend fun deleteMembers()

    @Query("DELETE FROM sessions")
    abstract suspend fun deleteSessions()

    @Transaction
    open suspend fun replaceAll(
        sessions: List<ChatSessionEntity>,
        members: List<GroupMemberEntity>,
        messages: List<MessageEntity>
    ) {
        deleteMessages()
        deleteMembers()
        deleteSessions()
        insertSessions(sessions)
        if (members.isNotEmpty()) insertMembers(members)
        if (messages.isNotEmpty()) insertMessages(messages)
    }
}
