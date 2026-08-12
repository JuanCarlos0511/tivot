package com.codecroco.tivot.domain.model

/**
 * Configuration for the local GGUF model.
 * Values are tuned for 3 GB RAM devices with eMMC 5.1 storage.
 *
 * Memory budget:
 * - Model file (Qwen2.5-0.5B Q4_K_M): ~491 MB on disk, ~350 MB mmap'd
 * - KV Cache (n_ctx=512): ~32 MB
 * - Working memory: ~50 MB
 * - Total native: ~430 MB (well within the ~1.5 GB native budget on a 3 GB device)
 */
data class ModelConfig(
    /** Context window size. Default 512, max 1024. */
    val nCtx: Int = 512,
    /** Number of CPU threads for inference. Default 2, never use all 4 vCPUs. */
    val nThreads: Int = 2,
    /** Batch size for prompt evaluation. */
    val nBatch: Int = 32,
    /** Maximum tokens to generate per completion. */
    val maxTokens: Int = 256,
    /** Model filename inside filesDir/models/ directory. */
    val modelFileName: String = "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
    /** Subdirectory inside context.filesDir for model storage. */
    val modelsDir: String = "models"
)
