package com.codecroco.tivot.core.jni

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for native inference dependencies.
 * NativeInferenceEngine uses constructor injection (@Inject),
 * so no explicit @Provides needed at this stage.
 * This module serves as the anchor point for future bindings
 * (e.g., binding NativeInferenceEngine to an InferenceEngine interface in Etapa 2).
 */
@Module
@InstallIn(SingletonComponent::class)
object NativeModule
