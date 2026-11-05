package com.example.pinpoint.model

enum class CallStatus {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTED,
    ENDED
}

data class ConferenceParticipant(
    val pin: String,
    val name: String,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val isVideoOn: Boolean = true
)

data class CallSession(
    val callId: String,
    val peerPin: String,
    val peerName: String,
    val isVideo: Boolean = false,
    val isConference: Boolean = false,
    val conferenceRoomName: String = "Enterprise Secure Bridge",
    val status: CallStatus = CallStatus.IDLE,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = true,
    val isScreenSharing: Boolean = false,
    val participants: List<ConferenceParticipant> = emptyList(),
    val encryptionStandard: String = "AES-256-GCM (FIPS 140-2)"
)
