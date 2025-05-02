package com.example.gemmachat.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 存储和管理聊天消息的仓库
 */
class ChatRepository {
    // 使用StateFlow存储和观察聊天消息列表
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    /**
     * 添加新的聊天消息
     */
    fun addMessage(message: ChatMessage) {
        _messages.update { currentMessages ->
            currentMessages + message
        }
    }

    /**
     * 更新现有消息
     */
    fun updateMessage(messageId: String, text: String, isLoading: Boolean) {
        _messages.update { currentMessages ->
            currentMessages.map { message ->
                if (message.id == messageId) {
                    message.copy(text = text, isLoading = isLoading)
                } else {
                    message
                }
            }
        }
    }

    /**
     * 删除指定ID的消息
     */
    fun removeMessage(messageId: String) {
        _messages.update { currentMessages ->
            currentMessages.filterNot { it.id == messageId }
        }
    }

    /**
     * 清空所有消息
     */
    fun clearAllMessages() {
        _messages.value = emptyList()
    }

    /**
     * 获取最近的消息用于构建提示
     */
    fun getRecentMessagesForPrompt(): List<ChatMessage> {
        // 为避免超出上下文窗口，仅返回最近的几条消息
        return _messages.value.takeLast(10)
    }
}