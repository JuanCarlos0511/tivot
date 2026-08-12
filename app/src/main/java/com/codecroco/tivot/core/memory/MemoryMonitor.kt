package com.codecroco.tivot.core.memory

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.util.Log
import com.codecroco.tivot.core.jni.NativeInferenceEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors system memory pressure via ComponentCallbacks2 and takes
 * defensive action to prevent the LowMemoryKiller (LMK) from killing Tivot.
 *
 * Actions:
 * - TRIM_MEMORY_RUNNING_MODERATE (5): Log warning only.
 * - TRIM_MEMORY_RUNNING_LOW (10): Flush KV cache via JNI.
 * - TRIM_MEMORY_RUNNING_CRITICAL (15): Release entire model from native memory.
 *
 * Emits MemoryPressureLevel events via SharedFlow for ViewModel observation.
 */
@Singleton
class MemoryMonitor @Inject constructor(
    private val nativeEngine: NativeInferenceEngine
) : ComponentCallbacks2 {

    companion object {
        private const val TAG = "MemoryMonitor"
    }

    private val _pressureLevel = MutableSharedFlow<MemoryPressureLevel>(
        replay = 1,
        extraBufferCapacity = 4
    )
    val pressureLevel: SharedFlow<MemoryPressureLevel> = _pressureLevel.asSharedFlow()

    override fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.e(TAG, "CRITICAL memory pressure (level=$level). Freeing model entirely.")
                // Synchronous release — LMK may kill us at any moment
                runBlocking {
                    nativeEngine.releaseModel()
                }
                _pressureLevel.tryEmit(MemoryPressureLevel.CRITICAL)
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.w(TAG, "LOW memory pressure (level=$level). Clearing KV cache.")
                runBlocking {
                    nativeEngine.clearCache()
                }
                _pressureLevel.tryEmit(MemoryPressureLevel.LOW)
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.i(TAG, "MODERATE memory pressure (level=$level). Monitoring.")
                _pressureLevel.tryEmit(MemoryPressureLevel.MODERATE)
            }
            else -> {
                Log.d(TAG, "Memory trim event (level=$level). No action needed.")
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        // No-op: Configuration changes handled by Activity
    }

    override fun onLowMemory() {
        Log.e(TAG, "onLowMemory() called. Freeing model.")
        runBlocking {
            nativeEngine.releaseModel()
        }
        _pressureLevel.tryEmit(MemoryPressureLevel.CRITICAL)
    }
}
