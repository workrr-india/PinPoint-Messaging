package com.example.pinpoint.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pinpoint.data.PinPointRepository
import com.example.pinpoint.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab {
    CHATS,
    FEEDS,
    CONTACTS,
    GROUPS,
    PROFILE
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PinPointRepository.getInstance(application)

    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val contacts: StateFlow<List<Contact>> = repository.contacts
    val groups: StateFlow<List<GroupConversation>> = repository.groups
    val invitations: StateFlow<List<ContactInvitation>> = repository.invitations
    val peerTypingStatus: StateFlow<Map<String, Boolean>> = repository.peerTypingStatus
    val activeCall: StateFlow<CallSession?> = repository.activeCall
    val pingTrigger: StateFlow<Long?> = repository.pingTrigger

    private val _selectedTab = MutableStateFlow(MainTab.CHATS)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isPrivateChatActive = MutableStateFlow(false)
    val isPrivateChatActive: StateFlow<Boolean> = _isPrivateChatActive.asStateFlow()

    private val _timedDurationSeconds = MutableStateFlow(0) // 0 means disabled
    val timedDurationSeconds: StateFlow<Int> = _timedDurationSeconds.asStateFlow()

    private val _selectedPriority = MutableStateFlow(MessagePriority.NORMAL)
    val selectedPriority: StateFlow<MessagePriority> = _selectedPriority.asStateFlow()

    private val _showContactDetails = MutableStateFlow<Contact?>(null)
    val showContactDetails: StateFlow<Contact?> = _showContactDetails.asStateFlow()

    private val _showAddContactDialog = MutableStateFlow(false)
    val showAddContactDialog: StateFlow<Boolean> = _showAddContactDialog.asStateFlow()

    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()

    private val _showGroupDetails = MutableStateFlow<GroupConversation?>(null)
    val showGroupDetails: StateFlow<GroupConversation?> = _showGroupDetails.asStateFlow()

    private val _showLegalCenter = MutableStateFlow(false)
    val showLegalCenter: StateFlow<Boolean> = _showLegalCenter.asStateFlow()

    private val _showPicturePrivacyDialog = MutableStateFlow(false)
    val showPicturePrivacyDialog: StateFlow<Boolean> = _showPicturePrivacyDialog.asStateFlow()

    private val _permissionDisclosure = MutableStateFlow<String?>(null)
    val permissionDisclosure: StateFlow<String?> = _permissionDisclosure.asStateFlow()

    // Filtered chats list
    val conversationItems: StateFlow<List<ConversationPreview>> = combine(
        repository.contacts,
        repository.groups,
        repository.messages,
        _searchQuery
    ) { contactsList, groupsList, messagesMap, query ->
        val contactPreviews = contactsList.map { contact ->
            val msgs = messagesMap[contact.pin] ?: emptyList()
            val lastMsg = msgs.lastOrNull()
            ConversationPreview(
                id = contact.pin,
                title = contact.displayName,
                subtitle = contact.statusMessage,
                pin = contact.pin,
                avatarColorHex = contact.avatarColorHex,
                isGroup = false,
                lastMessage = lastMsg?.content ?: "No messages yet",
                lastMessageTime = lastMsg?.timestamp ?: 0L,
                lastMessageStatus = lastMsg?.status,
                isLastMessageOutgoing = lastMsg?.isOutgoing ?: false,
                unreadCount = contact.unreadCount,
                isOnline = contact.isOnline
            )
        }

        val groupPreviews = groupsList.map { grp ->
            val msgs = messagesMap[grp.id] ?: emptyList()
            val lastMsg = msgs.lastOrNull()
            ConversationPreview(
                id = grp.id,
                title = grp.name,
                subtitle = "${grp.members.size} members",
                pin = null,
                avatarColorHex = "#7C4DFF",
                isGroup = true,
                lastMessage = lastMsg?.let { "${it.senderName}: ${it.content}" } ?: "Group created",
                lastMessageTime = lastMsg?.timestamp ?: grp.createdTimestamp,
                lastMessageStatus = lastMsg?.status,
                isLastMessageOutgoing = lastMsg?.isOutgoing ?: false,
                unreadCount = 0,
                isOnline = true
            )
        }

        val combined = (contactPreviews + groupPreviews).sortedByDescending { it.lastMessageTime }

        if (query.isBlank()) {
            combined
        } else {
            combined.filter {
                it.title.contains(query, ignoreCase = true) ||
                        (it.pin?.contains(query, ignoreCase = true) == true) ||
                        it.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active conversation messages
    val activeMessages: StateFlow<List<ChatMessage>> = combine(
        repository.messages,
        _activeConversationId
    ) { messagesMap, activeId ->
        if (activeId == null) emptyList() else messagesMap[activeId] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun openConversation(id: String) {
        _activeConversationId.value = id
    }

    fun closeConversation() {
        if (_isPrivateChatActive.value) {
            // Scrub private messages on exit
            _activeConversationId.value?.let { repository.clearPrivateChatHistory(it) }
            _isPrivateChatActive.value = false
        }
        _activeConversationId.value = null
        _timedDurationSeconds.value = 0
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun togglePrivateChat() {
        _isPrivateChatActive.value = !_isPrivateChatActive.value
    }

    fun setTimedDuration(seconds: Int) {
        _timedDurationSeconds.value = seconds
    }

    fun setShowAddContactDialog(show: Boolean) {
        _showAddContactDialog.value = show
    }

    fun setShowCreateGroupDialog(show: Boolean) {
        _showCreateGroupDialog.value = show
    }

    fun openGroupDetails(group: GroupConversation?) {
        _showGroupDetails.value = group
    }

    fun setSelectedPriority(priority: MessagePriority) {
        _selectedPriority.value = priority
    }

    fun openContactDetails(contact: Contact?) {
        _showContactDetails.value = contact
    }

    // Message actions
    fun sendTextMessage(text: String) {
        val convId = _activeConversationId.value ?: return
        if (text.isBlank()) return

        val ttl = _timedDurationSeconds.value
        val isTimed = ttl > 0
        val priority = _selectedPriority.value

        repository.sendMessage(
            conversationId = convId,
            content = text.trim(),
            type = MessageType.TEXT,
            isTimed = isTimed,
            ttlSeconds = ttl,
            priority = priority
        )

        // Reset priority after sending
        _selectedPriority.value = MessagePriority.NORMAL
    }

    fun editMessage(messageId: String, newContent: String) {
        val convId = _activeConversationId.value ?: return
        if (newContent.isBlank()) return
        repository.editMessage(convId, messageId, newContent.trim())
    }

    fun sendPing() {
        val convId = _activeConversationId.value ?: return
        repository.sendPing(convId)
    }

    fun sendAttachment(type: MessageType, name: String, size: String, caption: String) {
        val convId = _activeConversationId.value ?: return
        repository.sendMessage(
            conversationId = convId,
            content = caption.ifBlank { name },
            type = type,
            isTimed = false,
            ttlSeconds = 0,
            priority = _selectedPriority.value,
            mediaName = name,
            mediaSize = size
        )
        _selectedPriority.value = MessagePriority.NORMAL
    }

    fun retractMessage(messageId: String) {
        val convId = _activeConversationId.value ?: return
        repository.retractMessage(convId, messageId)
    }

    fun deleteMessage(messageId: String) {
        val convId = _activeConversationId.value ?: return
        repository.deleteMessage(convId, messageId)
    }

    fun revealTimedMessage(messageId: String) {
        val convId = _activeConversationId.value ?: return
        repository.startTimedMessageTimer(convId, messageId)
    }

    // Contact and Group actions
    fun sendContactInvitation(pin: String, message: String) {
        repository.sendInvitation(pin.trim().uppercase(), message)
        _showAddContactDialog.value = false
    }

    fun acceptInvitation(invitationId: String) {
        repository.acceptInvitation(invitationId)
    }

    fun rejectInvitation(invitationId: String) {
        repository.rejectInvitation(invitationId)
    }

    fun createGroup(name: String, description: String, selectedMemberPins: List<String>) {
        val group = repository.createGroup(name, description, selectedMemberPins)
        _showCreateGroupDialog.value = false
        openConversation(group.id)
    }

    fun addMemberToGroup(groupId: String, contactPin: String) {
        repository.addMemberToGroup(groupId, contactPin)
    }

    fun removeMemberFromGroup(groupId: String, memberPin: String) {
        repository.removeMemberFromGroup(groupId, memberPin)
    }

    fun updateMemberRole(groupId: String, memberPin: String, newRole: GroupRole) {
        repository.updateMemberRole(groupId, memberPin, newRole)
    }

    // Calls
    fun startCall(peerPin: String, peerName: String, isVideo: Boolean) {
        repository.startCall(peerPin, peerName, isVideo)
    }

    fun endCall() {
        repository.endCall()
    }

    fun toggleMute() {
        repository.toggleMute()
    }

    fun toggleSpeaker() {
        repository.toggleSpeaker()
    }

    fun toggleCamera() {
        repository.toggleCamera()
    }

    fun startConferenceCall(roomName: String = "Enterprise Secure Bridge") {
        repository.startConferenceCall(roomName)
    }

    fun toggleScreenShare() {
        repository.toggleScreenShare()
    }

    fun updateDisplayName(newName: String) {
        repository.updateDisplayName(newName)
    }

    fun updateStatusMessage(newStatus: String) {
        repository.updateStatusMessage(newStatus)
    }

    fun setupNewUserAccount(displayName: String, statusMessage: String) {
        repository.setupNewUserAccount(displayName, statusMessage)
    }

    fun setShowPicturePrivacyDialog(show: Boolean) {
        _showPicturePrivacyDialog.value = show
    }

    fun updateProfilePicture(avatarUri: String?) {
        repository.updateProfilePicture(avatarUri)
    }

    fun updatePictureVisibility(
        visibility: ProfilePictureVisibility,
        allowedPersons: Set<String> = emptySet(),
        allowedGroups: Set<String> = emptySet()
    ) {
        repository.updatePictureVisibility(visibility, allowedPersons, allowedGroups)
    }

    fun togglePersonPictureAccess(pin: String) {
        repository.togglePersonPictureAccess(pin)
    }

    fun toggleGroupPictureAccess(groupId: String) {
        repository.toggleGroupPictureAccess(groupId)
    }

    fun setShowLegalCenter(show: Boolean) {
        _showLegalCenter.value = show
    }

    fun showPermissionDisclosure(type: String) {
        _permissionDisclosure.value = type
    }

    fun dismissPermissionDisclosure() {
        _permissionDisclosure.value = null
    }

    fun deleteAccountAndScrubAllData() {
        repository.deleteAccountAndScrubAllData()
    }
}

data class ConversationPreview(
    val id: String,
    val title: String,
    val subtitle: String,
    val pin: String?,
    val avatarColorHex: String,
    val isGroup: Boolean,
    val lastMessage: String,
    val lastMessageTime: Long,
    val lastMessageStatus: MessageStatus?,
    val isLastMessageOutgoing: Boolean,
    val unreadCount: Int,
    val isOnline: Boolean
)
