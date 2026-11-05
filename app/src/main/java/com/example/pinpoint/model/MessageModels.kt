package com.example.pinpoint.model

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

enum class MessageType {
    TEXT,
    PING,        // BBM-style Nudge/PING with vibration & alert
    IMAGE,       // Photo attachment
    VOICE_NOTE,  // Audio recording
    FILE,        // Document/file
    SYSTEM       // System notifications (joined group, promoted admin, etc.)
}

enum class MessagePriority {
    NORMAL,
    URGENT,          // Critical security/operations alert
    CONFIDENTIAL,    // Restricted dissemination
    ACTION_REQUIRED  // Requires explicit acknowledgement
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderPin: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENT,
    val isOutgoing: Boolean = true,
    val isTimed: Boolean = false,
    val timeToLiveSeconds: Int = 0,
    val timerStartedAt: Long? = null,
    val isExpired: Boolean = false,
    val isRetracted: Boolean = false,
    val isEdited: Boolean = false,
    val editedTimestamp: Long? = null,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val ephemeralKeyId: String? = null,
    val mediaName: String? = null,
    val mediaSize: String? = null,
    val mediaUrl: String? = null
) {
    /**
     * Calculates remaining time for a timed self-destructing message.
     * Returns 0 if expired or not a timed message.
     */
    fun remainingSeconds(currentTimeMs: Long): Int {
        if (!isTimed || isExpired || timerStartedAt == null) return 0
        val elapsedMs = currentTimeMs - timerStartedAt
        val elapsedSeconds = (elapsedMs / 1000).toInt()
        val remaining = timeToLiveSeconds - elapsedSeconds
        return remaining.coerceAtLeast(0)
    }
}
