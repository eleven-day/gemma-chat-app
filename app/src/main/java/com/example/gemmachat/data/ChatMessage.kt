package com.example.gemmachat.data

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val isLoading: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)