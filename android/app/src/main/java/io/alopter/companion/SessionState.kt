package io.alopter.companion

import kotlinx.coroutines.flow.MutableStateFlow

enum class OverlayState { DISABLED, PERMISSION_MISSING, READY, BUBBLE, PANEL }
enum class ScreenState { OFF, REQUESTING_PERMISSION, STARTING, SHARING, BLOCKED_FOR_APP, STOPPING, ERROR }

val ScreenState.isSessionActive: Boolean
    get() = this == ScreenState.SHARING || this == ScreenState.BLOCKED_FOR_APP

enum class MicState { IDLE, REQUESTING_PERMISSION, LISTENING, TRANSCRIBING, ERROR }

enum class AiState {
    IDLE,
    SENDING,
    STREAMING,
    TOOL_PROPOSED,
    AWAITING_CONFIRMATION,
    EXECUTING,
    COMPLETE,
    CANCELLED,
    ERROR,
}

val AiState.isProcessing: Boolean
    get() = this == AiState.SENDING ||
        this == AiState.STREAMING ||
        this == AiState.TOOL_PROPOSED ||
        this == AiState.AWAITING_CONFIRMATION ||
        this == AiState.EXECUTING

object SessionState {
    val overlay = MutableStateFlow(OverlayState.DISABLED)
    val screen = MutableStateFlow(ScreenState.OFF)
    val mic = MutableStateFlow(MicState.IDLE)
    val ai = MutableStateFlow(AiState.IDLE)
}
