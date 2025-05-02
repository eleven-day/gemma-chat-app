package com.example.gemmachat.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "ModelManager"
private const val MODEL_FILENAME = "gemma3-1b-it-int4.task"
private const val GITHUB_MODEL_URL = "https://github.com/eleven-day/gemma-chat-app/releases/download/v1.0/gemma3-1b-it-int4.task"

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

    fun getModelFile(): File? {
        val file = File(context.filesDir, MODEL_FILENAME)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun downloadAndInitializeModel(): Flow<ModelSetupState> = flow {
        emit(ModelSetupState.Downloading(0))

        downloader.downloadModel(GITHUB_MODEL_URL, MODEL_FILENAME).collect { result ->
            when (result) {
                is DownloadResult.Progress -> {
                    emit(ModelSetupState.Downloading(result.percentage))
                }
                is DownloadResult.Success -> {
                    Log.d(TAG, "Model download success: ${result.file.absolutePath}")
                    emit(ModelSetupState.Initializing)

                    llmWrapper.initialize(
                        modelFile = result.file,
                        onSuccess = {
                            emit(ModelSetupState.Ready(llmWrapper))
                        },
                        onError = { errorMessage ->
                            emit(ModelSetupState.Error(errorMessage))
                        }
                    )
                }
                is DownloadResult.Error -> {
                    Log.e(TAG, "Model download failed: ${result.message}")
                    emit(ModelSetupState.Error("Download failed: ${result.message}"))
                }
            }
        }
    }

    fun initializeExistingModel(modelFile: File): Flow<ModelSetupState> = flow {
        emit(ModelSetupState.Initializing)

        llmWrapper.initialize(
            modelFile = modelFile,
            onSuccess = {
                emit(ModelSetupState.Ready(llmWrapper))
            },
            onError = { errorMessage ->
                emit(ModelSetupState.Error(errorMessage))
            }
        )
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