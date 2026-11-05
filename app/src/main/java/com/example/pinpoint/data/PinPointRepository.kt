package com.example.pinpoint.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import com.example.pinpoint.crypto.CryptoManager
import com.example.pinpoint.model.*
import com.example.pinpoint.notification.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class PinPointRepository private constructor(private val appContext: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val firestoreDb: FirebaseFirestore by lazy {
        val dbId = try {
            appContext.getString(com.example.R.string.firestore_database_id)
        } catch (_: Exception) {
            "ai-studio-android-pinpoint-18cfd724-c834-4e5e-8706-80f69f7e112b"
        }
        FirebaseFirestore.getInstance(dbId)
    }

    private val assignedPins = mutableSetOf("2B4F89A1", "9C1288B4", "7F3A9C12", "4E81A20D", "3D98E5C1", "9B20C47E")

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _userProfile = MutableStateFlow(createInitialUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    private val _groups = MutableStateFlow<List<GroupConversation>>(emptyList())
    val groups: StateFlow<List<GroupConversation>> = _groups.asStateFlow()

    private val _invitations = MutableStateFlow<List<ContactInvitation>>(emptyList())
    val invitations: StateFlow<List<ContactInvitation>> = _invitations.asStateFlow()

    private val _peerTypingStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val peerTypingStatus: StateFlow<Map<String, Boolean>> = _peerTypingStatus.asStateFlow()

    private val _activeCall = MutableStateFlow<CallSession?>(null)
    val activeCall: StateFlow<CallSession?> = _activeCall.asStateFlow()

    private val _pingTrigger = MutableStateFlow<Long?>(null)
    val pingTrigger: StateFlow<Long?> = _pingTrigger.asStateFlow()

    private var callTimerJob: Job? = null
    private var timedMessageWatcherJob: Job? = null

    init {
        seedInitialData()
        startTimedMessageWatcher()
    }

    /**
     * Verifies whether a candidate PIN is already registered or assigned to an account/contact.
     */
    fun isPinRegistered(pin: String): Boolean {
        val uppercasePin = pin.uppercase()
        val isAssigned = assignedPins.contains(uppercasePin)
        val isContactPin = _contacts.value.any { it.pin.equals(uppercasePin, ignoreCase = true) }
        return isAssigned || isContactPin
    }

    /**
     * Securely generates and assigns a unique 8-character alphanumeric PIN to a user account
     * upon initial setup or profile regeneration, verifying uniqueness before assignment.
     */
    fun setupNewUserAccount(
        displayName: String = "Alex Vance",
        statusMessage: String = "Available | Security & Privacy First 🔒"
    ): UserProfile {
        val uniquePin = CryptoManager.generateUniqueSecurePin(
            isUnique = { candidate -> !isPinRegistered(candidate) }
        )
        assignedPins.add(uniquePin)

        val keyPair = CryptoManager.getOrCreateKeyPair()
        val pubBytes = keyPair.public.encoded
        val pubBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
        val fingerprint = CryptoManager.generateFingerprint(pubBytes)

        val profile = UserProfile(
            pin = uniquePin,
            displayName = displayName,
            statusMessage = statusMessage,
            avatarColorHex = "#0091EA",
            organization = "PinPoint Enterprise Hub",
            department = "Information Security & Cryptography",
            jobTitle = "Principal Systems Engineer",
            clearanceLevel = "LEVEL 5 (FIPS 140-2 TOP SECRET)",
            publicKeyBase64 = pubBase64,
            fingerprint = fingerprint
        )
        _userProfile.value = profile
        return profile
    }

    /**
     * Google Play User Data Policy Compliance:
     * Permanently scrubs and purges all cryptographic messages, media, contacts,
     * calls, and re-initializes a fresh anonymous identity.
     */
    fun deleteAccountAndScrubAllData() {
        _messages.value = emptyMap()
        _contacts.value = emptyList()
        _groups.value = emptyList()
        _invitations.value = emptyList()
        _activeCall.value = null
        setupNewUserAccount("New User", "Account scrubbed & privacy keys regenerated.")
    }

    private fun createInitialUserProfile(): UserProfile {
        val uniquePin = CryptoManager.generateUniqueSecurePin(
            isUnique = { candidate -> !isPinRegistered(candidate) }
        )
        assignedPins.add(uniquePin)

        val keyPair = CryptoManager.getOrCreateKeyPair()
        val pubBytes = keyPair.public.encoded
        val pubBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
        val fingerprint = CryptoManager.generateFingerprint(pubBytes)

        return UserProfile(
            pin = uniquePin,
            displayName = "Alex Vance",
            statusMessage = "Available | Security & Privacy First 🔒",
            avatarColorHex = "#0091EA",
            publicKeyBase64 = pubBase64,
            fingerprint = fingerprint
        )
    }

    private fun seedInitialData() {
        val myPin = _userProfile.value.pin

        val seedContacts = listOf(
            Contact(
                pin = "2B4F89A1",
                displayName = "Gideon Smith",
                statusMessage = "Available",
                avatarColorHex = "#00829B",
                organization = "PinPoint Enterprise Hub",
                department = "Global Architecture & Cloud",
                jobTitle = "Principal Solutions Engineer",
                clearanceLevel = "LEVEL 5 (FIPS 140-2 TOP SECRET)",
                isOnline = true,
                unreadCount = 0,
                isKeyVerified = true
            ),
            Contact(
                pin = "9C1288B4",
                displayName = "Patricia Padill",
                statusMessage = "This chat is now protected.",
                avatarColorHex = "#00897B",
                organization = "PinPoint Enterprise Hub",
                department = "Enterprise Security Operations",
                jobTitle = "Compliance Officer",
                clearanceLevel = "LEVEL 4 (CONFIDENTIAL)",
                isOnline = true,
                unreadCount = 0,
                isKeyVerified = true
            ),
            Contact(
                pin = "4E81A20D",
                displayName = "Michael Accettura",
                statusMessage = "great, thanks! :)",
                avatarColorHex = "#3F51B5",
                organization = "PinPoint Enterprise Hub",
                department = "Global SecOps & Infrastructure",
                jobTitle = "Senior Infrastructure Incident Lead",
                clearanceLevel = "LEVEL 4 (CONFIDENTIAL)",
                isOnline = false,
                lastSeenText = "Wed 2:03 PM",
                unreadCount = 0,
                isKeyVerified = true
            ),
            Contact(
                pin = "7F3A9C12",
                displayName = "Sarah Chen",
                statusMessage = "Auditing cryptography protocols 🛡️",
                avatarColorHex = "#7C4DFF",
                organization = "PinPoint Enterprise Hub",
                department = "Security Architecture & Cryptography",
                jobTitle = "Lead Cryptographic Architect",
                clearanceLevel = "LEVEL 5 (FIPS 140-2 TOP SECRET)",
                isOnline = true,
                unreadCount = 0,
                isKeyVerified = true
            )
        )
        _contacts.value = seedContacts

        val salesGroup = GroupConversation(
            id = "grp_sales_team",
            name = "sales team",
            description = "Commercial negotiations, contract delivery, and regional sales.",
            creatorPin = "2B4F89A1",
            members = listOf(
                GroupMember(pin = myPin, displayName = "Alex Vance (You)", role = GroupRole.OWNER, joinedTimestamp = System.currentTimeMillis() - 864000000L),
                GroupMember(pin = "2B4F89A1", displayName = "Gideon Smith", role = GroupRole.ADMIN, joinedTimestamp = System.currentTimeMillis() - 764000000L),
                GroupMember(pin = "9C1288B4", displayName = "Patricia Padill", role = GroupRole.MEMBER, joinedTimestamp = System.currentTimeMillis() - 664000000L)
            ),
            createdTimestamp = System.currentTimeMillis() - 864000000L
        )

        val seedGroup = GroupConversation(
            id = "grp_core_infra",
            name = "Protocol Core Architecture",
            description = "E2EE signaling, zero-trust PIN routing & state machines.",
            creatorPin = myPin,
            members = listOf(
                GroupMember(pin = myPin, displayName = "Alex Vance (You)", role = GroupRole.OWNER, joinedTimestamp = System.currentTimeMillis() - 864000000L),
                GroupMember(pin = "7F3A9C12", displayName = "Sarah Chen", role = GroupRole.ADMIN, joinedTimestamp = System.currentTimeMillis() - 764000000L),
                GroupMember(pin = "4E81A20D", displayName = "Marcus Brody", role = GroupRole.MEMBER, joinedTimestamp = System.currentTimeMillis() - 664000000L)
            ),
            createdTimestamp = System.currentTimeMillis() - 864000000L
        )
        _groups.value = listOf(salesGroup, seedGroup)

        val now = System.currentTimeMillis()

        // Seed messages for Gideon Smith ("2B4F89A1") - Matching Screenshot 1 & 2!
        val gideonMessages = listOf(
            ChatMessage(
                id = "g1",
                conversationId = "2B4F89A1",
                senderPin = myPin,
                senderName = "Alex Vance",
                content = "Hey Gideon",
                timestamp = now - 360000L,
                status = MessageStatus.READ,
                isOutgoing = true
            ),
            ChatMessage(
                id = "g2",
                conversationId = "2B4F89A1",
                senderPin = "2B4F89A1",
                senderName = "Gideon Smith",
                content = "Hi",
                timestamp = now - 300000L,
                status = MessageStatus.READ,
                isOutgoing = false
            ),
            ChatMessage(
                id = "g3",
                conversationId = "2B4F89A1",
                senderPin = "2B4F89A1",
                senderName = "Gideon Smith",
                content = "What Can I do for you?",
                timestamp = now - 260000L,
                status = MessageStatus.READ,
                isOutgoing = false
            ),
            ChatMessage(
                id = "g4",
                conversationId = "2B4F89A1",
                senderPin = myPin,
                senderName = "Alex Vance",
                content = "Can you review this file?",
                timestamp = now - 200000L,
                status = MessageStatus.READ,
                isOutgoing = true
            ),
            ChatMessage(
                id = "g5",
                conversationId = "2B4F89A1",
                senderPin = myPin,
                senderName = "Alex Vance",
                content = "da-bbm-enterprise-sdk.pdf",
                timestamp = now - 180000L,
                status = MessageStatus.READ,
                isOutgoing = true,
                type = MessageType.FILE,
                mediaName = "da-bbm-enterprise-sdk.pdf",
                mediaSize = "734.55 KB"
            ),
            ChatMessage(
                id = "g6",
                conversationId = "2B4F89A1",
                senderPin = "2B4F89A1",
                senderName = "Gideon Smith",
                content = "Sure",
                timestamp = now - 120000L,
                status = MessageStatus.READ,
                isOutgoing = false
            ),
            ChatMessage(
                id = "g7",
                conversationId = "2B4F89A1",
                senderPin = myPin,
                senderName = "Alex Vance",
                content = "By today?",
                timestamp = now - 70000L,
                status = MessageStatus.DELIVERED,
                isOutgoing = true
            ),
            ChatMessage(
                id = "g8",
                conversationId = "2B4F89A1",
                senderPin = myPin,
                senderName = "Alex Vance",
                content = "Actually you can send my tomorrow",
                timestamp = now - 30000L,
                status = MessageStatus.DELIVERED,
                isOutgoing = true
            )
        )

        // Seed messages for sales team group
        val salesTeamMessages = listOf(
            ChatMessage(
                id = "stm1",
                conversationId = "grp_sales_team",
                senderPin = "2B4F89A1",
                senderName = "Gideon Smith",
                content = "Gideon Smith joined the chat",
                timestamp = now - 600000L,
                status = MessageStatus.READ,
                isOutgoing = false
            ),
            ChatMessage(
                id = "stm2",
                conversationId = "grp_sales_team",
                senderPin = "2B4F89A1",
                senderName = "Gideon Smith",
                content = "Hey everyone",
                timestamp = now - 300000L,
                status = MessageStatus.READ,
                isOutgoing = false
            ),
            ChatMessage(
                id = "stm3",
                conversationId = "grp_sales_team",
                senderPin = myPin,
                senderName = "Alex Vance",
                content = "Can you join the call?",
                timestamp = now - 200000L,
                status = MessageStatus.DELIVERED,
                isOutgoing = true
            ),
            ChatMessage(
                id = "stm4",
                conversationId = "grp_sales_team",
                senderPin = "9C1288B4",
                senderName = "Patricia Padill",
                content = "Sure",
                timestamp = now - 120000L,
                status = MessageStatus.READ,
                isOutgoing = false
            ),
            ChatMessage(
                id = "stm5",
                conversationId = "grp_sales_team",
                senderPin = "2B4F89A1",
                senderName = "Gideon Smith",
                content = "Where at?",
                timestamp = now - 60000L,
                status = MessageStatus.READ,
                isOutgoing = false
            )
        )

        // Seed messages for Patricia Padill
        val patriciaMessages = listOf(
            ChatMessage(
                id = "pm1",
                conversationId = "9C1288B4",
                senderPin = "9C1288B4",
                senderName = "Patricia Padill",
                content = "This chat is now protected.",
                timestamp = now - 300000L,
                status = MessageStatus.READ,
                isOutgoing = false
            )
        )

        // Seed messages for Michael Accettura
        val michaelMessages = listOf(
            ChatMessage(
                id = "mm1",
                conversationId = "4E81A20D",
                senderPin = "4E81A20D",
                senderName = "Michael Accettura",
                content = "great, thanks! :)",
                timestamp = now - 86400000L,
                status = MessageStatus.READ,
                isOutgoing = false
            )
        )

        // Seed messages for Sarah Chen ("7F3A9C12")
        val sarahMessages = listOf(
            ChatMessage(
                id = "m1",
                conversationId = "7F3A9C12",
                senderPin = "7F3A9C12",
                senderName = "Sarah Chen",
                content = "Got it",
                timestamp = now - 3600000L,
                status = MessageStatus.READ,
                isOutgoing = false
            )
        )

        _messages.value = mapOf(
            "2B4F89A1" to gideonMessages,
            "grp_sales_team" to salesTeamMessages,
            "9C1288B4" to patriciaMessages,
            "4E81A20D" to michaelMessages,
            "7F3A9C12" to sarahMessages
        )

        // Seed pending invitation
        _invitations.value = listOf(
            ContactInvitation(
                id = "inv_1",
                senderPin = "9B20C47E",
                senderName = "Devin Miller",
                inviteMessage = "Hi Alex, please add me on PinPoint! Worked together on the crypto audit.",
                timestamp = now - 86400000L,
                status = InvitationStatus.PENDING,
                isIncoming = true
            )
        )
    }

    // ==================== Real-Time Messaging & Status Transitions ====================

    fun sendMessage(
        conversationId: String,
        content: String,
        type: MessageType = MessageType.TEXT,
        isTimed: Boolean = false,
        ttlSeconds: Int = 0,
        priority: MessagePriority = MessagePriority.NORMAL,
        mediaName: String? = null,
        mediaSize: String? = null,
        mediaUrl: String? = null
    ): String {
        val myProfile = _userProfile.value
        val messageId = "msg_${UUID.randomUUID()}"
        val initialMessage = ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderPin = myProfile.pin,
            senderName = myProfile.displayName,
            content = content,
            timestamp = System.currentTimeMillis(),
            type = type,
            status = MessageStatus.SENDING,
            isOutgoing = true,
            isTimed = isTimed,
            timeToLiveSeconds = ttlSeconds,
            timerStartedAt = if (isTimed) System.currentTimeMillis() else null,
            priority = priority,
            ephemeralKeyId = CryptoManager.generateEphemeralMessageKey(),
            mediaName = mediaName,
            mediaSize = mediaSize,
            mediaUrl = mediaUrl
        )

        // Append initial message
        val currentList = _messages.value[conversationId] ?: emptyList()
        _messages.value = _messages.value + (conversationId to (currentList + initialMessage))

        // Trigger real-time transitions: SENDING -> SENT -> DELIVERED -> READ
        scope.launch {
            delay(350)
            updateMessageStatus(conversationId, messageId, MessageStatus.SENT)

            delay(800)
            updateMessageStatus(conversationId, messageId, MessageStatus.DELIVERED)

            delay(1500)
            updateMessageStatus(conversationId, messageId, MessageStatus.READ)

            // If it's a 1-on-1 contact chat, simulate smart peer interaction
            val contact = _contacts.value.find { it.pin == conversationId }
            if (contact != null) {
                simulatePeerResponse(conversationId, contact.displayName, type, content)
            }
        }

        return messageId
    }

    fun sendPing(conversationId: String) {
        val myProfile = _userProfile.value
        val messageId = "ping_${UUID.randomUUID()}"
        val pingMessage = ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderPin = myProfile.pin,
            senderName = myProfile.displayName,
            content = "PING!",
            timestamp = System.currentTimeMillis(),
            type = MessageType.PING,
            status = MessageStatus.SENDING,
            isOutgoing = true
        )

        val currentList = _messages.value[conversationId] ?: emptyList()
        _messages.value = _messages.value + (conversationId to (currentList + pingMessage))

        triggerHapticPing()

        scope.launch {
            delay(300)
            updateMessageStatus(conversationId, messageId, MessageStatus.SENT)
            delay(600)
            updateMessageStatus(conversationId, messageId, MessageStatus.DELIVERED)
            delay(1000)
            updateMessageStatus(conversationId, messageId, MessageStatus.READ)

            // Peer responds to PING
            delay(1500)
            setPeerTyping(conversationId, true)
            delay(1800)
            setPeerTyping(conversationId, false)

            val reply = ChatMessage(
                id = "resp_${UUID.randomUUID()}",
                conversationId = conversationId,
                senderPin = conversationId,
                senderName = _contacts.value.find { it.pin == conversationId }?.displayName ?: "Contact",
                content = "PING received! 👋 What's on your mind?",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                status = MessageStatus.READ,
                isOutgoing = false
            )
            appendIncomingMessage(conversationId, reply)
        }
    }

    private fun updateMessageStatus(conversationId: String, messageId: String, newStatus: MessageStatus) {
        val list = _messages.value[conversationId] ?: return
        val updated = list.map { msg ->
            if (msg.id == messageId) msg.copy(status = newStatus) else msg
        }
        _messages.value = _messages.value + (conversationId to updated)
    }

    private fun simulatePeerResponse(conversationId: String, peerName: String, sentType: MessageType, sentContent: String) {
        scope.launch {
            delay(1800)
            setPeerTyping(conversationId, true)
            delay(2200)
            setPeerTyping(conversationId, false)

            val responseText = when (sentType) {
                MessageType.IMAGE -> "Great visual! Received and decrypted successfully."
                MessageType.VOICE_NOTE -> "Heard your voice note clearly, good audio quality."
                MessageType.FILE -> "File received intact. Checking the checksum now."
                else -> {
                    when {
                        sentContent.contains("hello", ignoreCase = true) || sentContent.contains("hi", ignoreCase = true) ->
                            "Hey Alex! Good to hear from you."
                        sentContent.contains("pin", ignoreCase = true) ->
                            "Yes! The 8-character PIN architecture keeps everything private without linking to any phone numbers."
                        sentContent.contains("call", ignoreCase = true) ->
                            "Ready whenever you want to test the voice or video stream!"
                        else -> "Got your message on PinPoint! E2EE verified with your key fingerprint."
                    }
                }
            }

            val incoming = ChatMessage(
                id = "inc_${UUID.randomUUID()}",
                conversationId = conversationId,
                senderPin = conversationId,
                senderName = peerName,
                content = responseText,
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                status = MessageStatus.READ,
                isOutgoing = false
            )
            appendIncomingMessage(conversationId, incoming)
        }
    }

    private fun appendIncomingMessage(conversationId: String, message: ChatMessage) {
        val list = _messages.value[conversationId] ?: emptyList()
        _messages.value = _messages.value + (conversationId to (list + message))

        // Trigger WhatsApp-style high priority notification for incoming message
        NotificationHelper.showWhatsAppStyleNotification(appContext, message)
    }

    fun setPeerTyping(conversationId: String, isTyping: Boolean) {
        _peerTypingStatus.value = _peerTypingStatus.value + (conversationId to isTyping)
    }

    // ==================== Privacy: Retraction & Timed Messages ====================

    fun retractMessage(conversationId: String, messageId: String) {
        val list = _messages.value[conversationId] ?: return
        val updated = list.map { msg ->
            if (msg.id == messageId) {
                msg.copy(
                    isRetracted = true,
                    content = "This message was retracted by sender",
                    mediaName = null,
                    mediaSize = null,
                    mediaUrl = null
                )
            } else msg
        }
        _messages.value = _messages.value + (conversationId to updated)
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        val list = _messages.value[conversationId] ?: return
        val updated = list.filterNot { it.id == messageId }
        _messages.value = _messages.value + (conversationId to updated)
    }

    fun editMessage(conversationId: String, messageId: String, newContent: String) {
        val list = _messages.value[conversationId] ?: return
        val updated = list.map { msg ->
            if (msg.id == messageId && msg.isOutgoing && !msg.isRetracted && !msg.isExpired) {
                msg.copy(
                    content = newContent,
                    isEdited = true,
                    editedTimestamp = System.currentTimeMillis()
                )
            } else msg
        }
        _messages.value = _messages.value + (conversationId to updated)
    }

    fun startTimedMessageTimer(conversationId: String, messageId: String) {
        val list = _messages.value[conversationId] ?: return
        val updated = list.map { msg ->
            if (msg.id == messageId && msg.isTimed && msg.timerStartedAt == null) {
                msg.copy(timerStartedAt = System.currentTimeMillis())
            } else msg
        }
        _messages.value = _messages.value + (conversationId to updated)
    }

    private fun startTimedMessageWatcher() {
        timedMessageWatcherJob?.cancel()
        timedMessageWatcherJob = scope.launch {
            while (isActive) {
                delay(1000)
                val currentMessages = _messages.value
                val now = System.currentTimeMillis()
                var hasChanges = false

                val updatedMap = currentMessages.mapValues { (_, list) ->
                    list.map { msg ->
                        if (msg.isTimed && !msg.isExpired && msg.timerStartedAt != null) {
                            if (msg.remainingSeconds(now) <= 0) {
                                hasChanges = true
                                msg.copy(
                                    isExpired = true,
                                    content = "Message Expired (Self-Destructed)",
                                    mediaName = null,
                                    mediaSize = null,
                                    mediaUrl = null
                                )
                            } else {
                                msg
                            }
                        } else {
                            msg
                        }
                    }
                }

                if (hasChanges) {
                    _messages.value = updatedMap
                }
            }
        }
    }

    fun clearPrivateChatHistory(conversationId: String) {
        _messages.value = _messages.value + (conversationId to emptyList())
    }

    // ==================== Groups & Role Permissions ====================

    fun createGroup(name: String, description: String, selectedMemberPins: List<String>): GroupConversation {
        val myProfile = _userProfile.value
        val groupId = "grp_${UUID.randomUUID().toString().take(8)}"

        val members = mutableListOf<GroupMember>()
        members.add(
            GroupMember(
                pin = myProfile.pin,
                displayName = "${myProfile.displayName} (You)",
                role = GroupRole.OWNER,
                joinedTimestamp = System.currentTimeMillis()
            )
        )

        selectedMemberPins.forEach { pin ->
            val contact = _contacts.value.find { it.pin == pin }
            if (contact != null) {
                members.add(
                    GroupMember(
                        pin = pin,
                        displayName = contact.displayName,
                        role = GroupRole.MEMBER,
                        joinedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        val newGroup = GroupConversation(
            id = groupId,
            name = name,
            description = description,
            creatorPin = myProfile.pin,
            members = members,
            createdTimestamp = System.currentTimeMillis()
        )

        _groups.value = _groups.value + newGroup

        val welcomeMsg = ChatMessage(
            id = "sys_${UUID.randomUUID()}",
            conversationId = groupId,
            senderPin = myProfile.pin,
            senderName = "System",
            content = "Group \"$name\" created by ${myProfile.displayName}. End-to-end encrypted.",
            timestamp = System.currentTimeMillis(),
            type = MessageType.SYSTEM,
            status = MessageStatus.READ,
            isOutgoing = true
        )
        _messages.value = _messages.value + (groupId to listOf(welcomeMsg))

        return newGroup
    }

    fun addMemberToGroup(groupId: String, contactPin: String) {
        val contact = _contacts.value.find { it.pin == contactPin } ?: return
        val currentGroup = _groups.value.find { it.id == groupId } ?: return
        if (currentGroup.members.any { it.pin == contactPin }) return

        val newMember = GroupMember(
            pin = contactPin,
            displayName = contact.displayName,
            role = GroupRole.MEMBER,
            joinedTimestamp = System.currentTimeMillis()
        )

        val updatedGroup = currentGroup.copy(members = currentGroup.members + newMember)
        _groups.value = _groups.value.map { if (it.id == groupId) updatedGroup else it }

        val sysMsg = ChatMessage(
            id = "sys_${UUID.randomUUID()}",
            conversationId = groupId,
            senderPin = _userProfile.value.pin,
            senderName = "System",
            content = "${contact.displayName} joined the group.",
            timestamp = System.currentTimeMillis(),
            type = MessageType.SYSTEM,
            status = MessageStatus.READ,
            isOutgoing = true
        )
        appendIncomingMessage(groupId, sysMsg)
    }

    fun removeMemberFromGroup(groupId: String, memberPin: String) {
        val currentGroup = _groups.value.find { it.id == groupId } ?: return
        val member = currentGroup.members.find { it.pin == memberPin } ?: return

        val updatedGroup = currentGroup.copy(members = currentGroup.members.filter { it.pin != memberPin })
        _groups.value = _groups.value.map { if (it.id == groupId) updatedGroup else it }

        val sysMsg = ChatMessage(
            id = "sys_${UUID.randomUUID()}",
            conversationId = groupId,
            senderPin = _userProfile.value.pin,
            senderName = "System",
            content = "${member.displayName} was removed from the group.",
            timestamp = System.currentTimeMillis(),
            type = MessageType.SYSTEM,
            status = MessageStatus.READ,
            isOutgoing = true
        )
        appendIncomingMessage(groupId, sysMsg)
    }

    fun updateMemberRole(groupId: String, memberPin: String, newRole: GroupRole) {
        val currentGroup = _groups.value.find { it.id == groupId } ?: return
        val updatedMembers = currentGroup.members.map {
            if (it.pin == memberPin) it.copy(role = newRole) else it
        }
        val updatedGroup = currentGroup.copy(members = updatedMembers)
        _groups.value = _groups.value.map { if (it.id == groupId) updatedGroup else it }

        val memberName = currentGroup.members.find { it.pin == memberPin }?.displayName ?: memberPin
        val sysMsg = ChatMessage(
            id = "sys_${UUID.randomUUID()}",
            conversationId = groupId,
            senderPin = _userProfile.value.pin,
            senderName = "System",
            content = "$memberName role was changed to $newRole.",
            timestamp = System.currentTimeMillis(),
            type = MessageType.SYSTEM,
            status = MessageStatus.READ,
            isOutgoing = true
        )
        appendIncomingMessage(groupId, sysMsg)
    }

    // ==================== Contact Invitations ====================

    fun sendInvitation(targetPin: String, message: String) {
        val myProfile = _userProfile.value
        val inv = ContactInvitation(
            id = "inv_${UUID.randomUUID().toString().take(8)}",
            senderPin = targetPin,
            senderName = "PIN $targetPin",
            inviteMessage = message.ifBlank { "Let's connect on PinPoint!" },
            timestamp = System.currentTimeMillis(),
            status = InvitationStatus.PENDING,
            isIncoming = false
        )
        _invitations.value = _invitations.value + inv
    }

    fun acceptInvitation(invitationId: String) {
        val inv = _invitations.value.find { it.id == invitationId } ?: return
        _invitations.value = _invitations.value.map {
            if (it.id == invitationId) it.copy(status = InvitationStatus.ACCEPTED) else it
        }

        // Add as contact
        val newContact = Contact(
            pin = inv.senderPin,
            displayName = inv.senderName,
            statusMessage = "Connected via PIN invitation",
            avatarColorHex = "#00B0FF",
            isOnline = true
        )
        if (_contacts.value.none { it.pin == inv.senderPin }) {
            _contacts.value = _contacts.value + newContact
        }
    }

    fun rejectInvitation(invitationId: String) {
        _invitations.value = _invitations.value.map {
            if (it.id == invitationId) it.copy(status = InvitationStatus.REJECTED) else it
        }
    }

    // ==================== Voice & Video Calling ====================

    fun startCall(peerPin: String, peerName: String, isVideo: Boolean) {
        val session = CallSession(
            callId = "call_${UUID.randomUUID().toString().take(8)}",
            peerPin = peerPin,
            peerName = peerName,
            isVideo = isVideo,
            status = CallStatus.OUTGOING_RINGING,
            durationSeconds = 0
        )
        _activeCall.value = session

        // Simulate remote answering after 2 seconds
        scope.launch {
            delay(2000)
            if (_activeCall.value?.status == CallStatus.OUTGOING_RINGING) {
                _activeCall.value = _activeCall.value?.copy(status = CallStatus.CONNECTED)
                startCallTimer()
            }
        }
    }

    fun startConferenceCall(roomName: String) {
        val myProfile = _userProfile.value
        val participants = listOf(
            ConferenceParticipant(pin = myProfile.pin, name = "${myProfile.displayName} (Host)", isSpeaking = false, isVideoOn = true),
            ConferenceParticipant(pin = "7F3A9C12", name = "Sarah Chen", isSpeaking = true, isVideoOn = true),
            ConferenceParticipant(pin = "4E81A20D", name = "Marcus Brody", isSpeaking = false, isVideoOn = true),
            ConferenceParticipant(pin = "3D98E5C1", name = "Elena Rostova", isSpeaking = false, isVideoOn = false)
        )
        val session = CallSession(
            callId = "conf_${UUID.randomUUID().toString().take(8)}",
            peerPin = "ENTERPRISE_BRIDGE",
            peerName = roomName,
            isVideo = true,
            isConference = true,
            conferenceRoomName = roomName,
            status = CallStatus.CONNECTED,
            durationSeconds = 0,
            participants = participants
        )
        _activeCall.value = session
        startCallTimer()
    }

    fun toggleScreenShare() {
        _activeCall.value = _activeCall.value?.let { it.copy(isScreenSharing = !it.isScreenSharing) }
    }

    fun endCall() {
        callTimerJob?.cancel()
        _activeCall.value = _activeCall.value?.copy(status = CallStatus.ENDED)
        scope.launch {
            delay(800)
            _activeCall.value = null
        }
    }

    fun toggleMute() {
        _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeaker() {
        _activeCall.value = _activeCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun toggleCamera() {
        _activeCall.value = _activeCall.value?.let { it.copy(isCameraOn = !it.isCameraOn) }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = scope.launch {
            while (isActive && _activeCall.value?.status == CallStatus.CONNECTED) {
                delay(1000)
                _activeCall.value = _activeCall.value?.let {
                    it.copy(durationSeconds = it.durationSeconds + 1)
                }
            }
        }
    }

    // ==================== User Profile Updates ====================

    fun updateDisplayName(newName: String) {
        _userProfile.value = _userProfile.value.copy(displayName = newName)
    }

    fun updateStatusMessage(newStatus: String) {
        _userProfile.value = _userProfile.value.copy(statusMessage = newStatus)
    }

    fun updateProfilePicture(avatarUri: String?) {
        _userProfile.value = _userProfile.value.copy(avatarUri = avatarUri)
    }

    fun updatePictureVisibility(
        visibility: ProfilePictureVisibility,
        allowedPersons: Set<String>,
        allowedGroups: Set<String>
    ) {
        _userProfile.value = _userProfile.value.copy(
            pictureVisibility = visibility,
            allowedPersonPins = allowedPersons,
            allowedGroupIds = allowedGroups
        )
    }

    fun togglePersonPictureAccess(pin: String) {
        val currentSet = _userProfile.value.allowedPersonPins.toMutableSet()
        if (currentSet.contains(pin)) {
            currentSet.remove(pin)
        } else {
            currentSet.add(pin)
        }
        _userProfile.value = _userProfile.value.copy(
            allowedPersonPins = currentSet,
            pictureVisibility = ProfilePictureVisibility.PRIVATE_SELECTED
        )
    }

    fun toggleGroupPictureAccess(groupId: String) {
        val currentSet = _userProfile.value.allowedGroupIds.toMutableSet()
        if (currentSet.contains(groupId)) {
            currentSet.remove(groupId)
        } else {
            currentSet.add(groupId)
        }
        _userProfile.value = _userProfile.value.copy(
            allowedGroupIds = currentSet,
            pictureVisibility = ProfilePictureVisibility.PRIVATE_SELECTED
        )
    }

    private fun triggerHapticPing() {
        try {
            _pingTrigger.value = System.currentTimeMillis()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 200), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 150, 80, 200), -1)
            }
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: PinPointRepository? = null

        fun getInstance(context: Context): PinPointRepository {
            return instance ?: synchronized(this) {
                instance ?: PinPointRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
