package com.codecroco.tivot.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the lifecycle state of the Tivot local AI model.
 * Used by TivotModelLoader to communicate model status to the UI layer.
 */
@Serializable
sealed class ModelState {
    /** No model operation in progress. Initial state. */
    @Serializable
    data object Idle : ModelState()

    /** Model file is being downloaded or copied. */
    @Serializable
    data class Downloading(val progress: Float = 0f) : ModelState()

    /** Model file exists, being validated and loaded into native memory. */
    @Serializable
    data object Validating : ModelState()

    /** Model successfully loaded in native memory, ready for inference. */
    @Serializable
    data object Ready : ModelState()

    /** An error occurred during loading, validation, or inference setup. */
    @Serializable
    data class Error(val message: String, val isRecoverable: Boolean = true) : ModelState()

    /** Model was loaded but released due to memory pressure (LMK event). */
    @Serializable
    data object Evicted : ModelState()
}
