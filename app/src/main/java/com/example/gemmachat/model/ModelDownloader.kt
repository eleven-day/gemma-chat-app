package com.example.gemmachat.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

private const val TAG = "ModelDownloader"

sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
    data class Progress(val percentage: Int) : DownloadResult()
}

class ModelDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    fun downloadModel(modelUrl: String, fileName: String): Flow<DownloadResult> = flow {
        val directory = context.filesDir
        val file = File(directory, fileName)

        // 如果模型已经下载过，直接返回成功
        if (file.exists() && file.length() > 0) {
            Log.d(TAG, "Model file already exists: ${file.absolutePath}")
            emit(DownloadResult.Success(file))
            return@flow
        }

        Log.d(TAG, "Starting download from $modelUrl to ${file.absolutePath}")

        try {
            val request = Request.Builder()
                .url(modelUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadResult.Error("Download failed: ${response.code} ${response.message}"))
                return@flow
            }

            response.body?.let { body ->
                val totalBytes = body.contentLength()
                var bytesCopied = 0L

                file.outputStream().use { fileOut ->
                    body.source().use { source ->
                        val buffer = okio.Buffer()
                        var lastEmittedProgress = 0

                        while (true) {
                            val read = source.read(buffer, 8192L)
                            if (read == -1L) break

                            fileOut.write(buffer.readByteArray())
                            bytesCopied += read

                            if (totalBytes > 0) {
                                val progress = ((bytesCopied * 100) / totalBytes).toInt()
                                // 只在进度变化时发射进度
                                if (progress > lastEmittedProgress) {
                                    lastEmittedProgress = progress
                                    emit(DownloadResult.Progress(progress))
                                }
                            }
                        }
                    }
                }

                Log.d(TAG, "Model downloaded successfully.")
                emit(DownloadResult.Success(file))
            } ?: emit(DownloadResult.Error("Response body is null"))

        } catch (e: IOException) {
            Log.e(TAG, "Download error", e)
            emit(DownloadResult.Error(e.message ?: "Unknown download error"))

            // 清理可能不完整的文件
            if (file.exists()) {
                file.delete()
            }
        }
    }.flowOn(Dispatchers.IO)
}