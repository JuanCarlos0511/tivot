package com.codecroco.tivot.data.source

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.codecroco.tivot.core.jni.NativeInferenceEngine
import com.codecroco.tivot.core.util.DispatcherProvider
import com.codecroco.tivot.domain.model.ModelState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests for TivotModelLoader.
 * Validates the model installation state machine and file management.
 */
@RunWith(AndroidJUnit4::class)
class TivotModelLoaderTest {

    private lateinit var context: Context
    private lateinit var loader: TivotModelLoader
    private lateinit var engine: NativeInferenceEngine

    /** Simple DispatcherProvider that uses IO for all dispatchers in tests */
    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.IO
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        engine = NativeInferenceEngine(testDispatcherProvider)
        loader = TivotModelLoader(context, engine, testDispatcherProvider)

        // Clean up models directory before each test
        val modelsDir = File(context.filesDir, "models")
        if (modelsDir.exists()) {
            modelsDir.deleteRecursively()
        }
    }

    @After
    fun tearDown() = runBlocking {
        loader.uninstallModel()
        val modelsDir = File(context.filesDir, "models")
        if (modelsDir.exists()) {
            modelsDir.deleteRecursively()
        }
    }

    @Test
    fun initialState_isIdle() {
        assertEquals("Initial state should be Idle", ModelState.Idle, loader.modelState.value)
    }

    @Test
    fun installModel_withoutGgufFile_returnsError() = runBlocking {
        val result = loader.installModel()
        assertFalse("Install should fail without .gguf file", result)
        val currentState = loader.modelState.value
        assertTrue(
            "State should be Error after failed install, got: $currentState",
            currentState is ModelState.Error
        )
    }

    @Test
    fun installModel_withSmallFile_returnsError() = runBlocking {
        // Create a tiny file (smaller than minimum size threshold)
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        val fakeModel = File(modelsDir, "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf")
        fakeModel.writeText("too small to be a real model")

        val result = loader.installModel()
        assertFalse("Install should fail with undersized file", result)
        assertTrue(
            "State should be Error for corrupt file",
            loader.modelState.value is ModelState.Error
        )
    }

    @Test
    fun installModel_withValidSizeFile_attemptsNativeInit() = runBlocking {
        // Create a file larger than the minimum threshold (10 MB)
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        val fakeModel = File(modelsDir, "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf")
        // Write > 10 MB of data
        fakeModel.outputStream().use { out ->
            val buffer = ByteArray(1024 * 1024) // 1 MB buffer
            repeat(11) { out.write(buffer) } // 11 MB total
        }

        val result = loader.installModel()
        // In stub mode, initModel returns true, so this should succeed
        assertTrue("Install should succeed in stub mode with valid-size file", result)
        assertEquals(
            "State should be Ready after successful install",
            ModelState.Ready,
            loader.modelState.value
        )
    }

    @Test
    fun uninstallModel_resetsStateToIdle() = runBlocking {
        // First install (with valid-size file for stub mode)
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        val fakeModel = File(modelsDir, "Qwen2.5-0.5B-Instruct-Q4_K_M.gguf")
        fakeModel.outputStream().use { out ->
            val buffer = ByteArray(1024 * 1024)
            repeat(11) { out.write(buffer) }
        }
        loader.installModel()

        // Then uninstall
        loader.uninstallModel()
        assertEquals(
            "State should be Idle after uninstall",
            ModelState.Idle,
            loader.modelState.value
        )
    }

    @Test
    fun isModelAvailable_withNoFile_returnsFalse() = runBlocking {
        assertFalse("Model should not be available without file", loader.isModelAvailable())
    }

    @Test
    fun modelEviction_setsEvictedState() {
        loader.onModelEvicted()
        assertEquals(
            "State should be Evicted after eviction",
            ModelState.Evicted,
            loader.modelState.value
        )
    }
}
