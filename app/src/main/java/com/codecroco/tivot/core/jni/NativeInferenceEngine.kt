package com.codecroco.tivot.core.jni

import android.util.Log
import com.codecroco.tivot.core.util.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeInferenceEngine @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {
    enum class ModelStatus {
        UNLOADED, LOADING, READY, ERROR
    }

    private val _status = MutableStateFlow(ModelStatus.UNLOADED)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    suspend fun loadModel(modelPath: String, nCtx: Int = 512, nThreads: Int = 2): Boolean = withContext(dispatcherProvider.io) {
        _status.value = ModelStatus.LOADING
        return@withContext try {
            val success = LlamaBridge.initModel(modelPath, nCtx, nThreads)
            if (success) {
                _status.value = ModelStatus.READY
            } else {
                _status.value = ModelStatus.ERROR
            }
            success
        } catch (e: Exception) {
            Log.e("NativeInferenceEngine", "Error loading model: ${e.message}")
            _status.value = ModelStatus.ERROR
            false
        }
    }

    suspend fun generateCompletion(prompt: String, nBatch: Int = 32): Result<String> = withContext(dispatcherProvider.io) {
        if (_status.value != ModelStatus.READY) {
            return@withContext Result.failure(IllegalStateException("Model is not ready. Current status: ${_status.value}"))
        }
        return@withContext try {
            val result = LlamaBridge.completion(prompt, nBatch)
            Result.success(result)
        } catch (e: Exception) {
            Log.e("NativeInferenceEngine", "Error generating completion: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun clearCache() = withContext(dispatcherProvider.io) {
        try {
            LlamaBridge.clearKVCache()
        } catch (e: Exception) {
            Log.e("NativeInferenceEngine", "Error clearing cache: ${e.message}")
        }
    }

    suspend fun releaseModel() = withContext(dispatcherProvider.io) {
        try {
            LlamaBridge.freeModel()
            _status.value = ModelStatus.UNLOADED
        } catch (e: Exception) {
            Log.e("NativeInferenceEngine", "Error releasing model: ${e.message}")
        }
    }

    fun isModelLoaded(): Boolean {
        return _status.value == ModelStatus.READY
    }
}
