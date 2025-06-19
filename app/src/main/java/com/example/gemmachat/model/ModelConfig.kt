package com.example.gemmachat.model

/**
 * 模型配置信息
 */
data class ModelConfig(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadUrl: String,
    val description: String,
    val size: String,
    val isDefault: Boolean = false
) {
    companion object {
        
        /**
         * 可用的模型列表
         */
        val AVAILABLE_MODELS = listOf(
            ModelConfig(
                id = "gemma3-1b-it-int4",
                name = "Gemma 3 1B INT4",
                fileName = "gemma3-1b-it-int4.task",
                downloadUrl = "https://github.com/eleven-day/gemma-chat-app/releases/download/v1.0/gemma3-1b-it-int4.task",
                description = "轻量级模型，适合大多数对话场景，速度快，内存占用少",
                size = "529 MB",
                isDefault = true
            ),
            ModelConfig(
                id = "gemma-3n-E2B-it-int4",
                name = "Gemma 3N E2B INT4",
                fileName = "gemma-3n-E2B-it-int4.task",
                downloadUrl = "https://huggingface.co/xiaohan1/gemma3n/resolve/main/gemma-3n-E2B-it-int4.task",
                description = "优化版本，在保持速度的同时提供更好的对话质量",
                size = "3.0 GB"
            ),
            ModelConfig(
                id = "gemma-3n-E4B-it-int4",
                name = "Gemma 3N E4B INT4",
                fileName = "gemma-3n-E4B-it-int4.task",
                downloadUrl = "https://huggingface.co/xiaohan1/gemma3n/resolve/main/gemma-3n-E4B-it-int4.task",
                description = "高质量模型，提供最佳的对话体验，但需要更多资源",
                size = "4.4 GB"
            )
        )
        
        /**
         * 获取默认模型
         */
        fun getDefaultModel(): ModelConfig {
            return AVAILABLE_MODELS.first { it.isDefault }
        }
        
        /**
         * 根据ID获取模型配置
         */
        fun getModelById(id: String): ModelConfig? {
            return AVAILABLE_MODELS.find { it.id == id }
        }
    }
}
