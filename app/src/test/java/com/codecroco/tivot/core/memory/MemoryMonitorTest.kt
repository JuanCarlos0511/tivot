package com.codecroco.tivot.core.memory

import android.content.ComponentCallbacks2
import com.codecroco.tivot.core.jni.NativeInferenceEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MemoryMonitor.
 * Validates that correct MemoryPressureLevel events are emitted
 * and appropriate JNI cleanup actions are triggered for each memory trim level.
 */
class MemoryMonitorTest {

    private lateinit var nativeEngine: NativeInferenceEngine
    private lateinit var memoryMonitor: MemoryMonitor

    @Before
    fun setUp() {
        nativeEngine = mockk(relaxed = true)
        coEvery { nativeEngine.clearCache() } returns Unit
        coEvery { nativeEngine.releaseModel() } returns Unit
        memoryMonitor = MemoryMonitor(nativeEngine)
    }

    @Test
    fun `onTrimMemory CRITICAL emits CRITICAL level and releases model`() = runBlocking {
        memoryMonitor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)

        val emittedLevel = withTimeout(1000) {
            memoryMonitor.pressureLevel.first()
        }
        assertEquals(MemoryPressureLevel.CRITICAL, emittedLevel)
        coVerify { nativeEngine.releaseModel() }
    }

    @Test
    fun `onTrimMemory LOW emits LOW level and clears cache`() = runBlocking {
        memoryMonitor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)

        val emittedLevel = withTimeout(1000) {
            memoryMonitor.pressureLevel.first()
        }
        assertEquals(MemoryPressureLevel.LOW, emittedLevel)
        coVerify { nativeEngine.clearCache() }
    }

    @Test
    fun `onTrimMemory MODERATE emits MODERATE level`() = runBlocking {
        memoryMonitor.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)

        val emittedLevel = withTimeout(1000) {
            memoryMonitor.pressureLevel.first()
        }
        assertEquals(MemoryPressureLevel.MODERATE, emittedLevel)
    }

    @Test
    fun `onLowMemory releases model and emits CRITICAL`() = runBlocking {
        memoryMonitor.onLowMemory()

        val emittedLevel = withTimeout(1000) {
            memoryMonitor.pressureLevel.first()
        }
        assertEquals(MemoryPressureLevel.CRITICAL, emittedLevel)
        coVerify { nativeEngine.releaseModel() }
    }

    @Test
    fun `high trim level above CRITICAL still triggers CRITICAL action`() = runBlocking {
        // Level 80 (TRIM_MEMORY_COMPLETE) should still trigger CRITICAL path
        memoryMonitor.onTrimMemory(80)

        val emittedLevel = withTimeout(1000) {
            memoryMonitor.pressureLevel.first()
        }
        assertEquals(MemoryPressureLevel.CRITICAL, emittedLevel)
        coVerify { nativeEngine.releaseModel() }
    }
}
