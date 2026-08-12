package com.codecroco.tivot.core.memory

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for memory management dependencies.
 * MemoryMonitor uses constructor injection (@Inject @Singleton),
 * so no explicit @Provides is needed.
 */
@Module
@InstallIn(SingletonComponent::class)
object MemoryModule
