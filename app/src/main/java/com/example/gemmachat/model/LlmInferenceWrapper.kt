
package com.example.gemmachat.model

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "LlmInferenceWrapper"

class LlmInferenceWrapper(private val context: Context) {

    private var llmInference: LlmInference? = null

    fun initialize(
        modelFile: File,
        maxTokens: Int = 1024,
        temperature: Float = 0.7f,
        topK: Int = 40,
        useGpu: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val backend = if (useGpu) LlmInference.Backend.GPU else LlmInference.Backend.CPU

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(maxTokens)
                .setTemperature(temperature)
                .setTopK(topK)
                .setPreferredBackend(backend)
                .setErrorListener { error ->
                    Log.e(TAG, "Inference error: ${error.message}")
                    onError("Inference error: ${error.message}")
                }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.d(TAG, "LlmInference initialized successfully.")
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LlmInference", e)
            onError("Failed to initialize model: ${e.message}")
        }
    }

    suspend fun generateResponseAsync(
        prompt: String,
        onPartialResult: (String, Boolean) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            try {
                llmInference?.let { llm ->
                    var success = false

                    // 创建结果监听器
                    val resultListener = LlmInference.ResultListener { partialResult, done ->
                        onPartialResult(partialResult ?: "", done)
                        if (done) {
                            success = true
                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        }
                    }

                    // 设置结果监听器并启动异步生成
                    llm.resultListener = resultListener
                    llm.generateResponseAsync(prompt)

                    // 在协程取消时移除监听器
                    continuation.invokeOnCancellation {
                        llm.resultListener = null
                        if (!success) {
                            Log.d(TAG, "Response generation was cancelled")
                        }
                    }
                } ?: run {
                    Log.e(TAG, "LlmInference is not initialized")
                    continuation.resumeWithException(IllegalStateException("LLM model not initialized"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate response", e)
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
        Log.d(TAG, "LlmInference closed")
    }
}