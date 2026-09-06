package com.novalpie.nativeapp.feature.messages

import com.novalpie.nativeapp.model.*

data class MessageCenterState(
    val query: MessageQuery = MessageQuery(),
    val appliedQuery: MessageQuery? = null,
    val messages: LoadResult<List<SiteMessage>> = LoadResult.Idle,
    val pagination: MessagePagination = MessagePagination(),
    val stats: LoadResult<MessageStats> = LoadResult.Idle,
    val selectedIds: Set<Long> = emptySet(),
    val loadingMore: Boolean = false,
    val refreshing: Boolean = false,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null
)

data class MessageDetailState(
    val messageId: Long = 0,
    val detail: LoadResult<SiteMessage> = LoadResult.Idle,
    val actionLoading: Boolean = false,
    val actionMessage: String? = null
)

data class MessageConversationState(
    val targetUserId: Long = 0,
    val targetName: String? = null,
    val messages: LoadResult<List<DirectMessage>> = LoadResult.Idle,
    val draft: String = "",
    val sending: Boolean = false,
    val actionMessage: String? = null,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = false,
)

data class MessageSettingsState(
    val settings: LoadResult<MessageSettings> = LoadResult.Idle,
    val draft: MessageSettings = MessageSettings(),
    val saving: Boolean = false,
    val actionMessage: String? = null
)
