package com.example.pinpoint.model

enum class GroupRole {
    OWNER,
    ADMIN,
    MEMBER
}

data class GroupMember(
    val pin: String,
    val displayName: String,
    val role: GroupRole,
    val joinedTimestamp: Long
)

data class GroupConversation(
    val id: String,
    val name: String,
    val description: String,
    val creatorPin: String,
    val members: List<GroupMember>,
    val createdTimestamp: Long
) {
    fun getMemberRole(pin: String): GroupRole? {
        return members.find { it.pin == pin }?.role
    }

    fun canManageMembers(pin: String): Boolean {
        val role = getMemberRole(pin)
        return role == GroupRole.OWNER || role == GroupRole.ADMIN
    }
}
