package com.novalpie.nativeapp.feature.messages

import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.*

internal interface MessagesRepository {
    suspend fun page(query: MessageQuery, page: Int, pageSize: Int): MessagePage
    suspend fun stats(): MessageStats
    suspend fun detail(id: Long): SiteMessage
    suspend fun markRead(ids: List<Long>): MessageActionResult
    suspend fun markAllRead(): MessageActionResult
    suspend fun star(id: Long, starred: Boolean): MessageActionResult
    suspend fun delete(ids: List<Long>): MessageActionResult
    suspend fun conversation(targetId: Long, page: Int, pageSize: Int): List<DirectMessage>
    suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String): MessageActionResult
    suspend fun settings(): MessageSettings
    suspend fun saveSettings(settings: MessageSettings): MessageActionResult
}

internal class WebsiteMessagesRepository(private val api: NovalPieApi) : MessagesRepository {
    override suspend fun page(query: MessageQuery, page: Int, pageSize: Int) = api.messagePage(query, page, pageSize)
    override suspend fun stats() = api.messageStats()
    override suspend fun detail(id: Long) = api.messageDetail(id)
    override suspend fun markRead(ids: List<Long>) = if (ids.size == 1) api.markMessageRead(ids.single()) else api.markMessagesRead(ids)
    override suspend fun markAllRead() = api.markAllMessagesRead()
    override suspend fun star(id: Long, starred: Boolean) = api.starMessage(id, starred)
    override suspend fun delete(ids: List<Long>) = if (ids.size == 1) api.deleteMessage(ids.single()) else api.deleteMessages(ids)
    override suspend fun conversation(targetId: Long, page: Int, pageSize: Int) = api.messageConversation(targetId, page, pageSize)
    override suspend fun send(senderId: Long, senderName: String, targetId: Long, content: String) =
        api.sendDirectMessage(senderId, targetId, senderName, content)
    override suspend fun settings() = api.messageSettings()
    override suspend fun saveSettings(settings: MessageSettings) = api.updateMessageSettings(settings)
}
