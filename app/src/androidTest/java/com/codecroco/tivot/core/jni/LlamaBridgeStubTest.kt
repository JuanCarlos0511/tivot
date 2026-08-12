package com.codecroco.tivot.core.jni

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the LlamaBridge JNI stub.
 * Validates that the native library loads, stub methods return expected values,
 * and all JNI calls execute off the main thread.
 */
@RunWith(AndroidJUnit4::class)
class LlamaBridgeStubTest {

    @After
    fun tearDown() {
        // Ensure cleanup after each test
        LlamaBridge.freeModel()
    }

    @Test
    fun nativeLibrary_loadsWithoutCrash() {
        // LlamaBridge's static init block loads the library.
        // If this doesn't throw, the .so loaded successfully.
        assertNotNull(LlamaBridge)
    }

    @Test
    fun initModel_withInvalidPath_returnsExpectedResult() = runBlocking {
        withContext(Dispatchers.IO) {
            // In stub mode, initModel always returns true.
            // With real llama.cpp, an invalid path would return false.
            val result = LlamaBridge.initModel("/invalid/path/model.gguf", 512, 2)
            // Stub mode returns true; this test documents the expected behavior.
            assertTrue("Stub mode should return true for initModel", result)
        }
    }

    @Test
    fun completion_inStubMode_returnsNonEmptyString() = runBlocking {
        withContext(Dispatchers.IO) {
            LlamaBridge.initModel("/stub/model.gguf", 512, 2)
            val response = LlamaBridge.completion("Hello Tivot", 32)
            assertNotNull("Completion response should not be null", response)
            assertTrue("Completion response should not be empty", response.isNotEmpty())
            assertTrue(
                "Stub response should contain the prompt text",
                response.contains("Hello Tivot")
            )
        }
    }

    @Test
    fun clearKVCache_doesNotCrash() = runBlocking {
        withContext(Dispatchers.IO) {
            // Should not throw, even without a loaded model
            LlamaBridge.clearKVCache()
        }
    }

    @Test
    fun freeModel_doesNotCrash() = runBlocking {
        withContext(Dispatchers.IO) {
            LlamaBridge.initModel("/stub/model.gguf", 512, 2)
            // Free should complete without exceptions
            LlamaBridge.freeModel()
        }
    }

    @Test
    fun freeModel_calledMultipleTimes_doesNotCrash() = runBlocking {
        withContext(Dispatchers.IO) {
            LlamaBridge.initModel("/stub/model.gguf", 512, 2)
            LlamaBridge.freeModel()
            LlamaBridge.freeModel() // Double free should be safe
        }
    }

    @Test
    fun allJniCalls_executeOffMainThread() = runBlocking {
        withContext(Dispatchers.IO) {
            assertFalse(
                "JNI calls must NOT run on the main thread",
                Looper.getMainLooper().isCurrentThread
            )
            LlamaBridge.initModel("/stub/model.gguf", 512, 2)
            LlamaBridge.completion("Test", 32)
            LlamaBridge.clearKVCache()
            LlamaBridge.freeModel()
        }
    }
}
