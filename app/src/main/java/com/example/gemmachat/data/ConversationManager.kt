package com.example.gemmachat.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * 对话管理器，负责管理多个对话会话
 */
class ConversationManager {
    
    // 当前活跃的对话会话列表
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: Flow<List<Conversation>> = _conversations.asStateFlow()
    
    // 当前选中的对话ID
    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: Flow<String?> = _currentConversationId.asStateFlow()
    
    // 当前对话的消息
    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: Flow<List<ChatMessage>> = _currentMessages.asStateFlow()
    
    init {
        // 创建默认对话
        createNewConversation("默认对话")
    }
    
    /**
     * 创建新对话
     */
    fun createNewConversation(title: String = "新对话"): String {
        val conversationId = UUID.randomUUID().toString()
        val newConversation = Conversation(
            id = conversationId,
            title = title,
            createdAt = System.currentTimeMillis(),
            lastMessageAt = System.currentTimeMillis()
        )
        
        _conversations.update { conversations ->
            conversations + newConversation
        }
        
        // 自动切换到新对话
        switchToConversation(conversationId)
        
        return conversationId
    }
    
    /**
     * 切换到指定对话
     */
    fun switchToConversation(conversationId: String) {
        val conversation = _conversations.value.find { it.id == conversationId }
        if (conversation != null) {
            _currentConversationId.value = conversationId
            // 加载该对话的消息
            loadConversationMessages(conversationId)
        }
    }
    
    /**
     * 删除对话
     */
    fun deleteConversation(conversationId: String) {
        _conversations.update { conversations ->
            conversations.filterNot { it.id == conversationId }
        }
        
        // 如果删除的是当前对话，切换到其他对话或创建新对话
        if (_currentConversationId.value == conversationId) {
            val remainingConversations = _conversations.value
            if (remainingConversations.isNotEmpty()) {
                switchToConversation(remainingConversations.first().id)
            } else {
                createNewConversation("默认对话")
            }
        }
    }
    
    /**
     * 重命名对话
     */
    fun renameConversation(conversationId: String, newTitle: String) {
        _conversations.update { conversations ->
            conversations.map { conversation ->
                if (conversation.id == conversationId) {
                    conversation.copy(title = newTitle)
                } else {
                    conversation
                }
            }
        }
    }
    
    /**
     * 添加消息到当前对话
     */
    fun addMessageToCurrentConversation(message: ChatMessage) {
        val currentId = _currentConversationId.value ?: return
        
        // 添加消息
        _currentMessages.update { messages ->
            messages + message
        }
        
        // 更新对话的最后消息时间和标题（如果需要）
        _conversations.update { conversations ->
            conversations.map { conversation ->
                if (conversation.id == currentId) {
                    val newTitle = if (conversation.title == "新对话" && message.isFromUser) {
                        // 用第一条用户消息的前20个字符作为标题
                        message.text.take(20) + if (message.text.length > 20) "..." else ""
                    } else {
                        conversation.title
                    }
                    conversation.copy(
                        title = newTitle,
                        lastMessageAt = System.currentTimeMillis()
                    )
                } else {
                    conversation
                }
            }
        }
    }
    
    /**
     * 更新当前对话中的消息
     */
    fun updateMessageInCurrentConversation(messageId: String, text: String, isLoading: Boolean) {
        _currentMessages.update { messages ->
            messages.map { message ->
                if (message.id == messageId) {
                    message.copy(text = text, isLoading = isLoading)
                } else {
                    message
                }
            }
        }
    }
    
    /**
     * 清空当前对话的消息
     */
    fun clearCurrentConversation() {
        _currentMessages.value = emptyList()
    }
    
    /**
     * 清空所有对话
     */
    fun clearAllConversations() {
        _conversations.value = emptyList()
        _currentMessages.value = emptyList()
        _currentConversationId.value = null
        
        // 创建新的默认对话
        createNewConversation("默认对话")
    }
    
    /**
     * 获取当前对话的最近消息用于构建提示
     */
    fun getRecentMessagesForPrompt(): List<ChatMessage> {
        return _currentMessages.value.takeLast(10)
    }
    
    /**
     * 加载指定对话的消息（这里简化处理，实际项目中可能需要从数据库加载）
     */
    private fun loadConversationMessages(conversationId: String) {
        // TODO: 实际项目中应该从持久化存储加载消息
        // 这里为了简化，暂时清空消息列表
        _currentMessages.value = emptyList()
        
        // 添加欢迎消息
        val welcomeMessage = ChatMessage(
            text = "您好！我是基于Gemma模型的聊天助手。请问有什么可以帮到您的吗？",
            isFromUser = false
        )
        _currentMessages.value = listOf(welcomeMessage)
    }
}

/**
 * 对话数据类
 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastMessageAt: Long
)
