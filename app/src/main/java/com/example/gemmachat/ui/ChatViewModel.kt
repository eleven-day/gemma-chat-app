package com.example.gemmachat.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmachat.data.ChatMessage
import com.example.gemmachat.data.ChatRepository
import com.example.gemmachat.data.ConversationManager
import com.example.gemmachat.model.LlmInferenceWrapper
import com.example.gemmachat.model.ModelConfig
import com.example.gemmachat.model.ModelManager
import com.example.gemmachat.model.ModelSetupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "ChatViewModel"

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val modelManager = ModelManager(application)
    private var llmWrapper: LlmInferenceWrapper? = null

    // 替换ChatRepository为ConversationManager
    private val conversationManager = ConversationManager()

    // 跟踪当前正在生成的机器人消息
    private var currentBotMessageId: String? = null
    private val currentBotResponse = StringBuilder()
    
    // 模型相关状态
    private val _downloadingModel = MutableStateFlow<ModelConfig?>(null)
    val downloadingModel: StateFlow<ModelConfig?> = _downloadingModel.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    init {
        loadModel()

        // 观察对话管理器中的消息并更新UI状态
        viewModelScope.launch {
            conversationManager.currentMessages.collect { messages ->
                _uiState.update { currentState ->
                    if (currentState is ChatUiState.Ready) {
                        ChatUiState.Ready(messages)
                    } else currentState
                }
            }
        }
    }

    private fun loadModel() {
        viewModelScope.launch {
            val existingModel = modelManager.getModelFile()

            if (existingModel != null) {
                // 如果模型已存在，直接初始化
                modelManager.initializeExistingModel(existingModel).collect { state ->
                    handleModelSetupState(state)
                }
            } else {
                // 否则，下载并初始化模型
                modelManager.downloadAndInitializeModel().collect { state ->
                    handleModelSetupState(state)
                }
            }
        }
    }

    private fun handleModelSetupState(state: ModelSetupState) {
        when (state) {
            is ModelSetupState.Downloading -> {
                _uiState.update {
                    ChatUiState.LoadingModel("正在下载模型... ${state.progress}%")
                }
            }
            is ModelSetupState.Initializing -> {
                _uiState.update {
                    ChatUiState.LoadingModel("正在初始化模型...")
                }
            }            is ModelSetupState.Ready -> {
                llmWrapper = state.llmWrapper
                // 创建初始欢迎消息
                val welcomeMessage = ChatMessage(
                    text = "您好！我是基于Gemma模型的聊天助手。请问有什么可以帮到您的吗？",
                    isFromUser = false
                )
                conversationManager.addMessageToCurrentConversation(welcomeMessage)
                _uiState.update {
                    ChatUiState.Ready(messages = listOf(welcomeMessage))
                }
            }
            is ModelSetupState.Error -> {
                _uiState.update {
                    ChatUiState.Error(state.message)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (_uiState.value !is ChatUiState.Ready || text.isBlank()) {
            return
        }        // 创建并添加用户消息
        val userMessage = ChatMessage(
            text = text,
            isFromUser = true
        )
        conversationManager.addMessageToCurrentConversation(userMessage)

        // 创建"正在输入"消息
        val typingMessageId = UUID.randomUUID().toString()
        val typingMessage = ChatMessage(
            id = typingMessageId,
            text = "...",
            isFromUser = false,
            isLoading = true
        )
        conversationManager.addMessageToCurrentConversation(typingMessage)

        // 构建提示
        val recentMessages = conversationManager.getRecentMessagesForPrompt()
        val prompt = buildPrompt(recentMessages)

        // 生成响应
        viewModelScope.launch {
            try {
                currentBotMessageId = typingMessageId
                currentBotResponse.clear()

                llmWrapper?.generateResponseAsync(
                    prompt = prompt,
                    onPartialResult = { partialResult, done ->
                        handlePartialResult(partialResult, done)
                    }
                ) ?: run {
                    // 如果LLM未初始化，显示错误
                    updateBotMessage(typingMessageId, "抱歉，模型尚未准备好。请稍后再试。", true)
                    currentBotMessageId = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating response", e)
                updateBotMessage(typingMessageId, "抱歉，生成回复时出错：${e.message}", true)
                currentBotMessageId = null
            }
        }
    }

    private fun buildPrompt(messages: List<ChatMessage>): String {
        // 构建格式化的提示，包含聊天历史
        val promptBuilder = StringBuilder()

        promptBuilder.append("你是一个友好、乐于助人的AI助手。根据以下的对话历史回答用户问题：\n\n")

        for (message in messages) {
            val role = if (message.isFromUser) "用户" else "助手"
            promptBuilder.append("$role: ${message.text}\n")
        }

        // 添加最后的提示指令
        promptBuilder.append("助手: ")

        return promptBuilder.toString()
    }

    private fun handlePartialResult(partialResult: String, done: Boolean) {
        val botMessageId = currentBotMessageId ?: return

        // 添加新的部分结果到当前响应
        currentBotResponse.append(partialResult)

        // 更新UI中的机器人消息
        updateBotMessage(botMessageId, currentBotResponse.toString(), !done)

        // 如果响应生成完成，重置状态
        if (done) {
            currentBotMessageId = null
            currentBotResponse.clear()
        }
    }    private fun updateBotMessage(messageId: String, text: String, isLoading: Boolean) {
        conversationManager.updateMessageInCurrentConversation(messageId, text, isLoading)
    }

    fun retryModelSetup() {
        loadModel()
    }
    
    // 对话管理相关方法
    fun getConversations() = conversationManager.conversations
    fun getCurrentConversationId() = conversationManager.currentConversationId
    
    fun createNewConversation() {
        conversationManager.createNewConversation()
    }
    
    fun switchToConversation(conversationId: String) {
        conversationManager.switchToConversation(conversationId)
    }
    
    fun deleteConversation(conversationId: String) {
        conversationManager.deleteConversation(conversationId)
    }
    
    fun renameConversation(conversationId: String, newTitle: String) {
        conversationManager.renameConversation(conversationId, newTitle)
    }
    
    fun clearCurrentConversation() {
        conversationManager.clearCurrentConversation()
    }
    
    fun clearAllConversations() {
        conversationManager.clearAllConversations()
    }
    
    // 模型管理相关方法
    fun getAvailableModels(): List<ModelConfig> = ModelConfig.AVAILABLE_MODELS
    
    fun getDownloadedModels(): List<ModelConfig> = modelManager.getDownloadedModels()
    
    fun getCurrentModel(): ModelConfig? = modelManager.getCurrentModel()
    
    fun downloadModel(modelConfig: ModelConfig) {
        viewModelScope.launch {
            _downloadingModel.value = modelConfig
            _downloadProgress.value = 0
            
            modelManager.downloadAndInitializeModel(modelConfig).collect { state ->
                when (state) {
                    is ModelSetupState.Downloading -> {
                        _downloadProgress.value = state.progress
                    }
                    is ModelSetupState.Initializing -> {
                        _downloadProgress.value = null
                    }
                    is ModelSetupState.Ready -> {
                        _downloadingModel.value = null
                        _downloadProgress.value = null
                        // 模型下载完成后，可以选择是否自动切换
                    }
                    is ModelSetupState.Error -> {
                        _downloadingModel.value = null
                        _downloadProgress.value = null
                        // 处理下载错误
                        Log.e(TAG, "Model download failed: ${state.message}")
                    }
                }
            }
        }
    }
    
    fun switchToModel(modelConfig: ModelConfig) {
        val modelFile = modelManager.getModelFile(modelConfig)
        if (modelFile != null) {
            viewModelScope.launch {
                // 先关闭当前模型
                llmWrapper?.close()
                llmWrapper = null
                
                _uiState.update { ChatUiState.LoadingModel("正在切换模型...") }
                
                // 初始化新模型
                modelManager.initializeExistingModel(modelFile, modelConfig).collect { state ->
                    handleModelSetupState(state)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        modelManager.cleanup()
    }
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    data class LoadingModel(val message: String) : ChatUiState()
    data class Ready(val messages: List<ChatMessage>) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}