package com.codecroco.tivot.core.memory

/**
 * Represents the current memory pressure level as reported by
 * Android's ComponentCallbacks2.onTrimMemory system.
 */
enum class MemoryPressureLevel {
    /** No memory pressure. Normal operation. */
    NORMAL,
    /** Moderate pressure (TRIM_MEMORY_RUNNING_MODERATE = 5). Log only. */
    MODERATE,
    /** Low memory (TRIM_MEMORY_RUNNING_LOW = 10). Flush KV cache. */
    LOW,
    /** Critical memory (TRIM_MEMORY_RUNNING_CRITICAL = 15). Free model entirely. */
    CRITICAL
}
