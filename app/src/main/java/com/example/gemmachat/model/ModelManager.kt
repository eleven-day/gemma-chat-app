package com.example.gemmachat.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

private const val TAG = "ModelManager"

class ModelManager(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val downloader = ModelDownloader(
        context,
        okHttpClient
    )

    private val llmWrapper = LlmInferenceWrapper(context)
    
    // 当前使用的模型配置
    private var currentModel: ModelConfig? = null

    /**
     * 检查指定模型是否已下载
     */
    fun getModelFile(modelConfig: ModelConfig): File? {
        val file = File(context.filesDir, modelConfig.fileName)
        return if (file.exists() && file.length() > 0) file else null
    }
    
    /**
     * 获取当前默认模型文件
     */
    fun getModelFile(): File? {
        return getModelFile(ModelConfig.getDefaultModel())
    }
    
    /**
     * 获取所有已下载的模型
     */
    fun getDownloadedModels(): List<ModelConfig> {
        return ModelConfig.AVAILABLE_MODELS.filter { modelConfig ->
            getModelFile(modelConfig) != null
        }
    }
    
    /**
     * 获取当前使用的模型
     */
    fun getCurrentModel(): ModelConfig? = currentModel    /**
     * 下载并初始化指定模型
     */
    fun downloadAndInitializeModel(modelConfig: ModelConfig): Flow<ModelSetupState> = flow {
        emit(ModelSetupState.Downloading(0))

        try {
            downloader.downloadModel(modelConfig.downloadUrl, modelConfig.fileName).collect { result ->
                when (result) {
                    is DownloadResult.Progress -> {
                        emit(ModelSetupState.Downloading(result.percentage))
                    }
                    is DownloadResult.Success -> {
                        Log.d(TAG, "Model download success: ${result.file.absolutePath}")
                        emit(ModelSetupState.Initializing)

                        // 使用挂起函数包装初始化过程
                        val initSuccess = initializeModelSuspend(result.file, modelConfig)
                        if (initSuccess) {
                            emit(ModelSetupState.Ready(llmWrapper))
                        }
                    }
                    is DownloadResult.Error -> {
                        Log.e(TAG, "Model download failed: ${result.message}")
                        emit(ModelSetupState.Error("Download failed: ${result.message}"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in download flow", e)
            emit(ModelSetupState.Error("Process failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 下载并初始化默认模型
     */
    fun downloadAndInitializeModel(): Flow<ModelSetupState> {
        return downloadAndInitializeModel(ModelConfig.getDefaultModel())
    }

    /**
     * 初始化已存在的模型
     */
    fun initializeExistingModel(modelFile: File, modelConfig: ModelConfig): Flow<ModelSetupState> = flow {
        emit(ModelSetupState.Initializing)

        try {
            // 使用挂起函数包装初始化过程
            val initSuccess = initializeModelSuspend(modelFile, modelConfig)
            if (initSuccess) {
                emit(ModelSetupState.Ready(llmWrapper))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
            emit(ModelSetupState.Error("Initialization failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun initializeExistingModel(modelFile: File): Flow<ModelSetupState> = flow {
        emit(ModelSetupState.Initializing)

        try {
            // 使用挂起函数包装初始化过程
            val initSuccess = initializeModelSuspend(modelFile)
            if (initSuccess) {
                emit(ModelSetupState.Ready(llmWrapper))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
            emit(ModelSetupState.Error("Initialization failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)    // 创建一个挂起函数版本的初始化方法
    private suspend fun initializeModelSuspend(modelFile: File, modelConfig: ModelConfig? = null): Boolean {
        return suspendCancellableCoroutine { continuation ->
            var hasResponded = false

            try {
                llmWrapper.initialize(
                    modelFile = modelFile,
                    onSuccess = {
                        if (!hasResponded) {
                            hasResponded = true
                            // 设置当前模型
                            currentModel = modelConfig ?: ModelConfig.getDefaultModel()
                            continuation.resume(true)
                        }
                    },
                    onError = { errorMessage ->
                        if (!hasResponded) {
                            hasResponded = true
                            Log.e(TAG, "Model initialization failed: $errorMessage")
                            continuation.resume(false)
                        }
                    }
                )
            } catch (e: Exception) {
                if (!hasResponded) {
                    hasResponded = true
                    Log.e(TAG, "Exception during model initialization", e)
                    continuation.resume(false)
                }
            }
        }
    }

    fun cleanup() {
        llmWrapper.close()
    }
}

sealed class ModelSetupState {
    data class Downloading(val progress: Int) : ModelSetupState()
    object Initializing : ModelSetupState()
    data class Ready(val llmWrapper: LlmInferenceWrapper) : ModelSetupState()
    data class Error(val message: String) : ModelSetupState()
}