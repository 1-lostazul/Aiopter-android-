package io.alopter.companion.ai

data class ChatMessage(val role: String, val content: String)
data class ProposedAction(val id: String, val label: String, val uri: String, val risk: Int = 1)
sealed interface StreamEvent {
    data class Delta(val text: String) : StreamEvent
    data class Action(val action: ProposedAction) : StreamEvent
    data class Error(val message: String) : StreamEvent
    data object Done : StreamEvent
}
