package com.codecroco.tivot

import android.app.Application
import android.util.Log
import com.codecroco.tivot.core.memory.MemoryMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point for Tivot.
 * - Initializes Hilt dependency injection.
 * - Registers the MemoryMonitor as a ComponentCallbacks2 listener
 *   to respond to system memory pressure events (LMK).
 */
@HiltAndroidApp
class TivotApplication : Application() {

    companion object {
        private const val TAG = "TivotApplication"
    }

    @Inject
    lateinit var memoryMonitor: MemoryMonitor

    override fun onCreate() {
        super.onCreate()
        registerComponentCallbacks(memoryMonitor)
        Log.i(TAG, "Tivot Application initialized. MemoryMonitor registered.")
    }

    override fun onTerminate() {
        unregisterComponentCallbacks(memoryMonitor)
        super.onTerminate()
    }
}
