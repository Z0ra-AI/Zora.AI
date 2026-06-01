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
