package com.codecroco.tivot.data.source

import android.content.Context
import android.util.Log
import com.codecroco.tivot.core.jni.NativeInferenceEngine
import com.codecroco.tivot.core.util.DispatcherProvider
import com.codecroco.tivot.domain.model.ModelConfig
import com.codecroco.tivot.domain.model.ModelState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of the Tivot local AI model.
 * Handles model discovery, validation, initialization, and cleanup.
 *
 * The "Install Tivot" metaphor maps to:
 * - Checking if the .gguf file exists in filesDir/models/
 * - Validating file integrity (basic size check)
 * - Loading the model into native memory via NativeInferenceEngine
 *
 * Thread Safety: All operations run on Dispatchers.IO via DispatcherProvider.
 */
@Singleton
class TivotModelLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeEngine: NativeInferenceEngine,
    private val dispatcherProvider: DispatcherProvider
) {

    companion object {
        private const val TAG = "TivotModelLoader"
        private const val MIN_MODEL_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB minimum
    }

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Idle)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val config = ModelConfig()

    /**
     * Returns the directory where model files are stored.
     */
    private fun getModelsDir(): File {
        return File(context.filesDir, config.modelsDir).also {
            if (!it.exists()) it.mkdirs()
        }
    }

    /**
     * Returns the full path to the model file.
     */
    fun getModelFile(): File {
        return File(getModelsDir(), config.modelFileName)
    }

    /**
     * Check if the model file exists and is valid.
     */
    suspend fun isModelAvailable(): Boolean = withContext(dispatcherProvider.io) {
        val modelFile = getModelFile()
        modelFile.exists() && modelFile.length() > MIN_MODEL_SIZE_BYTES
    }

    /**
     * Install (initialize) the Tivot model.
     * Validates the .gguf file exists, then loads it into native memory.
     *
     * @return true if model was successfully loaded, false otherwise
     */
    suspend fun installModel(): Boolean = withContext(dispatcherProvider.io) {
        try {
            _modelState.value = ModelState.Validating
            Log.i(TAG, "Starting Tivot model installation...")

            val modelFile = getModelFile()

            // Check if model file exists
            if (!modelFile.exists()) {
                val errorMsg = "Model file not found: ${modelFile.absolutePath}"
                Log.e(TAG, errorMsg)
                _modelState.value = ModelState.Error(
                    message = errorMsg,
                    isRecoverable = true
                )
                return@withContext false
            }

            // Basic size validation
            if (modelFile.length() < MIN_MODEL_SIZE_BYTES) {
                val errorMsg = "Model file too small (${modelFile.length()} bytes). Possibly corrupt."
                Log.e(TAG, errorMsg)
                _modelState.value = ModelState.Error(
                    message = errorMsg,
                    isRecoverable = true
                )
                return@withContext false
            }

            Log.i(TAG, "Model file validated: ${modelFile.absolutePath} (${modelFile.length() / 1024 / 1024} MB)")

            // Initialize model via JNI
            val success = nativeEngine.loadModel(
                modelPath = modelFile.absolutePath,
                nCtx = config.nCtx,
                nThreads = config.nThreads
            )

            if (success) {
                _modelState.value = ModelState.Ready
                Log.i(TAG, "Tivot model installed successfully. Ready for inference.")
            } else {
                _modelState.value = ModelState.Error(
                    message = "Failed to initialize model in native memory",
                    isRecoverable = true
                )
                Log.e(TAG, "Native model initialization failed")
            }

            success
        } catch (e: Exception) {
            val errorMsg = "Exception during model installation: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _modelState.value = ModelState.Error(
                message = errorMsg,
                isRecoverable = true
            )
            false
        }
    }

    /**
     * Uninstall (release) the Tivot model from native memory.
     * Does NOT delete the .gguf file from disk.
     */
    suspend fun uninstallModel() = withContext(dispatcherProvider.io) {
        try {
            Log.i(TAG, "Uninstalling Tivot model (releasing native memory)...")
            nativeEngine.releaseModel()
            _modelState.value = ModelState.Idle
            Log.i(TAG, "Tivot model uninstalled.")
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling model: ${e.message}", e)
            _modelState.value = ModelState.Error(
                message = "Failed to release model: ${e.message}",
                isRecoverable = false
            )
        }
    }

    /**
     * Called by MemoryMonitor when the model has been evicted due to memory pressure.
     * Updates state to Evicted so the UI can inform the user.
     */
    fun onModelEvicted() {
        _modelState.value = ModelState.Evicted
        Log.w(TAG, "Model evicted due to memory pressure")
    }

    /**
     * Delete the model file from disk entirely.
     */
    suspend fun deleteModelFile(): Boolean = withContext(dispatcherProvider.io) {
        try {
            nativeEngine.releaseModel()
            val deleted = getModelFile().delete()
            _modelState.value = ModelState.Idle
            Log.i(TAG, "Model file deleted: $deleted")
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting model file: ${e.message}", e)
            false
        }
    }
}
