package com.example.pinpoint.model

enum class ProfilePictureVisibility(val title: String, val description: String) {
    PUBLIC(
        title = "Public (Everyone)",
        description = "Visible to anyone who adds your PIN or finds you in the directory."
    ),
    CONTACTS_ONLY(
        title = "My Contacts Only",
        description = "Only visible to verified people in your contacts list."
    ),
    PRIVATE_SELECTED(
        title = "Private (Selected Persons / Groups)",
        description = "Strictly restricted: Only chosen contacts or groups can view your picture."
    )
}

data class UserProfile(
    val pin: String,
    val displayName: String,
    val statusMessage: String,
    val avatarColorHex: String,
    val avatarUri: String? = null,
    val pictureVisibility: ProfilePictureVisibility = ProfilePictureVisibility.PUBLIC,
    val allowedPersonPins: Set<String> = emptySet(),
    val allowedGroupIds: Set<String> = emptySet(),
    val organization: String = "PinPoint Enterprise Hub",
    val department: String = "Information Security & Cryptography",
    val jobTitle: String = "Principal Systems Engineer",
    val clearanceLevel: String = "LEVEL 5 (FIPS 140-2 TOP SECRET)",
    val publicKeyBase64: String,
    val fingerprint: String
) {
    fun canViewerSeePicture(viewerPin: String?, groupId: String? = null): Boolean {
        if (avatarUri.isNullOrBlank()) return false
        return when (pictureVisibility) {
            ProfilePictureVisibility.PUBLIC -> true
            ProfilePictureVisibility.CONTACTS_ONLY -> viewerPin != null
            ProfilePictureVisibility.PRIVATE_SELECTED -> {
                val personMatch = viewerPin != null && allowedPersonPins.contains(viewerPin)
                val groupMatch = groupId != null && allowedGroupIds.contains(groupId)
                personMatch || groupMatch
            }
        }
    }
}

data class Contact(
    val pin: String,
    val displayName: String,
    val statusMessage: String,
    val avatarColorHex: String,
    val organization: String = "Enterprise Directory",
    val department: String = "Engineering",
    val jobTitle: String = "Team Member",
    val clearanceLevel: String = "LEVEL 3 (RESTRICTED)",
    val isOnline: Boolean = true,
    val lastSeenText: String = "Online",
    val unreadCount: Int = 0,
    val isPrivateChat: Boolean = false,
    val isKeyVerified: Boolean = true
)

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

data class ContactInvitation(
    val id: String,
    val senderPin: String,
    val senderName: String,
    val organization: String = "Enterprise Directory",
    val inviteMessage: String,
    val timestamp: Long,
    val status: InvitationStatus = InvitationStatus.PENDING,
    val isIncoming: Boolean = true
)
