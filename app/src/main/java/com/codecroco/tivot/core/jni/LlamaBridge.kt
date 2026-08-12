package com.codecroco.tivot.core.jni

import android.util.Log

/**
 * JNI Bridge to the native tivot_native library.
 * Provides direct access to llama.cpp functions via C++.
 * 
 * Thread Safety: All native methods are protected by a C++ mutex.
 * Memory: Model allocation happens in native C++ memory (mmap), NOT in JVM heap.
 */
object LlamaBridge {

    private const val TAG = "LlamaBridge"

    init {
        try {
            System.loadLibrary("tivot_native")
            Log.i(TAG, "Native library tivot_native loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}")
            throw e
        }
    }

    /**
     * Initialize the GGUF model from the given file path.
     * MUST be called from a background thread (Dispatchers.IO).
     *
     * @param modelPath Absolute path to the .gguf file
     * @param nCtx Context window size (default: 512, max: 1024)
     * @param nThreads Number of CPU threads (default: 2, max: 4)
     * @return true if model loaded successfully, false otherwise
     */
    external fun initModel(modelPath: String, nCtx: Int, nThreads: Int): Boolean

    /**
     * Run text completion on the given prompt.
     * MUST be called from a background thread.
     *
     * @param prompt The full prompt string (system + user)
     * @param nBatch Batch size for evaluation (default: 32)
     * @return Generated text response
     */
    external fun completion(prompt: String, nBatch: Int): String

    /**
     * Clear the KV cache to free memory.
     * Called by MemoryMonitor on TRIM_MEMORY_RUNNING_LOW.
     */
    external fun clearKVCache()

    /**
     * Free all native model resources.
     * Called by MemoryMonitor on TRIM_MEMORY_RUNNING_CRITICAL
     * or when user "uninstalls" Tivot.
     */
    external fun freeModel()
}
