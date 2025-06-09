/*
 * 适配 tasks-genai ≤ 0.10.19 的完整封装
 * - setMaxTopK() 取代了新版本的 setTopK()
 * - 无 setResultListener()/setErrorListener()/setTemperature()
 *   → 流式输出通过 ProgressListener 实现
 */
package com.example.gemmachat.model

import android.content.Context
import android.util.Log
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "LlmInferenceWrapper"

class LlmInferenceWrapper(private val context: Context) {

    private var llmInference: LlmInference? = null

    /**
     * 初始化 MediaPipe LLM
     *
     * @param modelFile  已下载到本地的 .task/.lite 文件
     * @param maxTokens  输入+输出最大 token 数
     * @param topK       采样 top-k（旧版 API 名为 maxTopK）
     * @param useGpu     是否优先使用 GPU
     */
    fun initialize(
        modelFile: File,
        maxTokens: Int = 1024,
        topK: Int = 40,
        useGpu: Boolean = false,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val backend = if (useGpu)
                LlmInference.Backend.GPU
            else
                LlmInference.Backend.CPU

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(maxTokens)
                .setMaxTopK(topK)          // 旧版 API
                .setPreferredBackend(backend)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LlmInference", e)
            onError(e.message ?: "Failed to init model")
        }
    }

    /**
     * 异步流式推理
     *
     * @param prompt           用户输入
     * @param onPartialResult  (partialText, done) 回调
     */
    suspend fun generateResponseAsync(
        prompt: String,
        onPartialResult: (String, Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine<Unit> { cont ->
            val llm = llmInference
                ?: return@suspendCancellableCoroutine cont.resumeWithException(
                    IllegalStateException("LLM not initialized")
                )

            try {
                val future = llm.generateResponseAsync(
                    prompt,
                    ProgressListener<String> { chunk, done ->
                        onPartialResult(chunk ?: "", done)
                    }
                )

                // 监听最终完成或失败
                Futures.addCallback(
                    future,
                    object : FutureCallback<String> {
                        override fun onSuccess(result: String?) {
                            if (cont.isActive) cont.resume(Unit)
                        }

                        override fun onFailure(t: Throwable) {
                            if (cont.isActive) cont.resumeWithException(t)
                        }
                    },
                    MoreExecutors.directExecutor()
                )

                // 协程取消时同步取消推理
                cont.invokeOnCancellation { future.cancel(true) }
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
    }

    /** 释放底层资源 */
    fun close() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing LlmInference", e)
        }
        llmInference = null
        Log.d(TAG, "LlmInference closed")
    }
}