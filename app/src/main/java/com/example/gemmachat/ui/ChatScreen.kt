package com.example.gemmachat.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gemmachat.data.ChatMessage
import com.example.gemmachat.ui.components.ChatBubble
import com.example.gemmachat.ui.components.ConversationSidebar
import com.example.gemmachat.ui.components.MessageInput
import com.example.gemmachat.ui.components.ModelSelectionDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.getConversations().collectAsState(initial = emptyList())
    val currentConversationId by viewModel.getCurrentConversationId().collectAsState(initial = null)
    val downloadingModel by viewModel.downloadingModel.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var showModelDialog by remember { mutableStateOf(false) }
    var showTopBarMenu by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationSidebar(
                conversations = conversations,
                currentConversationId = currentConversationId,
                onConversationSelected = { conversationId ->
                    viewModel.switchToConversation(conversationId)
                    scope.launch { drawerState.close() }
                },
                onNewConversation = {
                    viewModel.createNewConversation()
                    scope.launch { drawerState.close() }
                },
                onDeleteConversation = { conversationId ->
                    viewModel.deleteConversation(conversationId)
                },
                onRenameConversation = { conversationId, newTitle ->
                    viewModel.renameConversation(conversationId, newTitle)
                },
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text("Gemma 聊天助手")
                            viewModel.getCurrentModel()?.let { model ->
                                Text(
                                    text = "当前模型: ${model.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "打开对话列表")
                        }
                    },
                    actions = {
                        // 更多选项按钮
                        IconButton(onClick = { showTopBarMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                        }
                        
                        DropdownMenu(
                            expanded = showTopBarMenu,
                            onDismissRequest = { showTopBarMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("选择模型") },
                                onClick = {
                                    showTopBarMenu = false
                                    showModelDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("清空当前对话") },
                                onClick = {
                                    showTopBarMenu = false
                                    viewModel.clearCurrentConversation()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("新建对话") },
                                onClick = {
                                    showTopBarMenu = false
                                    viewModel.createNewConversation()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            )
                        }
                    }
                )
            }        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is ChatUiState.Idle -> {
                        // 闲置状态，显示加载指示器
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is ChatUiState.LoadingModel -> {
                        // 模型加载状态
                        LoadingScreen(message = state.message)
                    }

                    is ChatUiState.Ready -> {
                        // 聊天就绪状态
                        ChatContent(
                            messages = state.messages,
                            onSendMessage = { message -> viewModel.sendMessage(message) }
                        )
                    }

                    is ChatUiState.Error -> {
                        // 错误状态
                        ErrorScreen(
                            errorMessage = state.message,
                            onRetry = { viewModel.retryModelSetup() }
                        )
                    }
                }
            }
        }
    }
    
    // 模型选择对话框
    if (showModelDialog) {
        ModelSelectionDialog(
            availableModels = viewModel.getAvailableModels(),
            downloadedModels = viewModel.getDownloadedModels(),
            currentModel = viewModel.getCurrentModel(),
            downloadingModel = downloadingModel,
            downloadProgress = downloadProgress,
            onModelSelected = { model ->
                viewModel.switchToModel(model)
                showModelDialog = false
            },
            onModelDownload = { model ->
                viewModel.downloadModel(model)
            },
            onDismiss = { showModelDialog = false }
        )
    }
}

@Composable
fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 提取进度值（如果消息包含百分比）
        val progressRegex = ".*?(\\d+)%.*?".toRegex()
        val progressMatch = progressRegex.find(message)
        val progress = progressMatch?.groupValues?.get(1)?.toFloatOrNull()?.div(100f)

        if (progress != null) {
            // 显示确定进度的进度条
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LinearProgressIndicator(
                // 修复：progress现在需要一个返回float的lambda函数
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        } else {
            // 显示不确定进度的圆形进度指示器
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun ErrorScreen(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "出错了",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
fun ChatContent(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit
) {
    val lazyListState = rememberLazyListState()
    var userInput by remember { mutableStateOf("") }

    // 当消息列表更新时，滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 消息列表
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
        }

        // 输入区域
        MessageInput(
            value = userInput,
            onValueChange = { userInput = it },
            onSendClick = {
                if (userInput.isNotBlank()) {
                    onSendMessage(userInput)
                    userInput = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}