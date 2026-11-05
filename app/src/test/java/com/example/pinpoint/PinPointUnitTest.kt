package com.example.pinpoint

import com.example.pinpoint.crypto.CryptoManager
import com.example.pinpoint.model.*
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator

class PinPointUnitTest {

    @Test
    fun testPinDerivationAndFormat() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val pin = CryptoManager.derivePinFromPublicKey(keyPair.public.encoded)

        // PIN must be exactly 8 characters long
        assertEquals(8, pin.length)
        // PIN must be valid uppercase hexadecimal
        assertTrue("PIN must match 8-char uppercase hex pattern: $pin", pin.matches(Regex("^[0-9A-F]{8}$")))

        // Deterministic check: re-deriving with same key must yield identical PIN
        val pinSecondTime = CryptoManager.derivePinFromPublicKey(keyPair.public.encoded)
        assertEquals(pin, pinSecondTime)
    }

    @Test
    fun testFingerprintGeneration() {
        val sampleBytes = "test_public_key_payload".toByteArray(Charsets.UTF_8)
        val fingerprint = CryptoManager.generateFingerprint(sampleBytes)
        assertNotNull(fingerprint)
        assertTrue(fingerprint.contains(":"))
    }

    @Test
    fun testDigitalSignatureSigningAndVerification() {
        val testContent = "PinPoint encrypted transmission test"
        val signature = CryptoManager.signMessage(testContent)
        assertNotNull(signature)
        assertTrue(signature.isNotBlank())

        val keyPair = CryptoManager.getOrCreateKeyPair()
        val isValid = CryptoManager.verifySignature(testContent, signature, keyPair.public)
        assertTrue("Signature verification must succeed", isValid)

        val isTamperedValid = CryptoManager.verifySignature("Tampered content", signature, keyPair.public)
        assertFalse("Tampered content signature verification must fail", isTamperedValid)
    }

    @Test
    fun testMessageStateTransitions() {
        // Sent -> Delivered ('D') -> Read ('R')
        var msg = ChatMessage(
            id = "test_msg_1",
            conversationId = "7F3A9C12",
            senderPin = "00112233",
            senderName = "Alex",
            content = "Testing state flow",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
            isOutgoing = true
        )
        assertEquals(MessageStatus.SENDING, msg.status)

        // Transition 1: Sent
        msg = msg.copy(status = MessageStatus.SENT)
        assertEquals(MessageStatus.SENT, msg.status)

        // Transition 2: Delivered (Iconic 'D')
        msg = msg.copy(status = MessageStatus.DELIVERED)
        assertEquals(MessageStatus.DELIVERED, msg.status)

        // Transition 3: Read (Iconic 'R')
        msg = msg.copy(status = MessageStatus.READ)
        assertEquals(MessageStatus.READ, msg.status)
    }

    @Test
    fun testTimedMessageCountdownAndExpiry() {
        val startTime = 1000000L
        val ttlSeconds = 10

        val timedMessage = ChatMessage(
            id = "timed_1",
            conversationId = "7F3A9C12",
            senderPin = "7F3A9C12",
            senderName = "Sarah",
            content = "Self destruct token: 99482",
            timestamp = startTime,
            isTimed = true,
            timeToLiveSeconds = ttlSeconds,
            timerStartedAt = startTime,
            isExpired = false
        )

        // 3 seconds in -> 7 seconds remaining
        val remainingAt3s = timedMessage.remainingSeconds(startTime + 3000L)
        assertEquals(7, remainingAt3s)

        // 10 seconds in -> 0 seconds remaining
        val remainingAt10s = timedMessage.remainingSeconds(startTime + 10000L)
        assertEquals(0, remainingAt10s)

        // 15 seconds in -> 0 seconds remaining
        val remainingAt15s = timedMessage.remainingSeconds(startTime + 15000L)
        assertEquals(0, remainingAt15s)

        // Expire message
        val expiredMessage = timedMessage.copy(
            isExpired = true,
            content = "Message Expired (Self-Destructed)",
            mediaName = null
        )
        assertTrue(expiredMessage.isExpired)
        assertEquals("Message Expired (Self-Destructed)", expiredMessage.content)
    }

    @Test
    fun testMessageRetractionRecall() {
        val originalMessage = ChatMessage(
            id = "retract_test",
            conversationId = "7F3A9C12",
            senderPin = "00112233",
            senderName = "Alex",
            content = "Secret credentials that should not have been sent",
            timestamp = System.currentTimeMillis(),
            mediaName = "secret_file.pdf",
            status = MessageStatus.DELIVERED
        )

        // Simulate retraction
        val retractedMessage = originalMessage.copy(
            isRetracted = true,
            content = "This message was retracted by sender",
            mediaName = null,
            mediaSize = null,
            mediaUrl = null
        )

        assertTrue(retractedMessage.isRetracted)
        assertEquals("This message was retracted by sender", retractedMessage.content)
        assertNull(retractedMessage.mediaName)
    }

    @Test
    fun testGroupMembershipAndRolePermissions() {
        val ownerPin = "OWNER001"
        val adminPin = "ADMIN002"
        val memberPin = "MEMBER03"

        var group = GroupConversation(
            id = "grp_test",
            name = "Security Council",
            description = "E2EE Group",
            creatorPin = ownerPin,
            members = listOf(
                GroupMember(pin = ownerPin, displayName = "Owner", role = GroupRole.OWNER, joinedTimestamp = 1000L)
            ),
            createdTimestamp = 1000L
        )

        assertEquals(1, group.members.size)
        assertTrue(group.canManageMembers(ownerPin))
        assertFalse(group.canManageMembers(memberPin))

        // Add Member
        val newMember = GroupMember(pin = memberPin, displayName = "Bob", role = GroupRole.MEMBER, joinedTimestamp = 2000L)
        group = group.copy(members = group.members + newMember)
        assertEquals(2, group.members.size)
        assertEquals(GroupRole.MEMBER, group.getMemberRole(memberPin))
        assertFalse(group.canManageMembers(memberPin))

        // Promote to Admin
        val promotedMembers = group.members.map {
            if (it.pin == memberPin) it.copy(role = GroupRole.ADMIN) else it
        }
        group = group.copy(members = promotedMembers)
        assertEquals(GroupRole.ADMIN, group.getMemberRole(memberPin))
        assertTrue(group.canManageMembers(memberPin))

        // Demote back to Member
        val demotedMembers = group.members.map {
            if (it.pin == memberPin) it.copy(role = GroupRole.MEMBER) else it
        }
        group = group.copy(members = demotedMembers)
        assertEquals(GroupRole.MEMBER, group.getMemberRole(memberPin))
        assertFalse(group.canManageMembers(memberPin))

        // Remove Member
        group = group.copy(members = group.members.filter { it.pin != memberPin })
        assertEquals(1, group.members.size)
        assertNull(group.getMemberRole(memberPin))
    }

    @Test
    fun testMessageEditingInPlace() {
        var msg = ChatMessage(
            id = "edit_test_1",
            conversationId = "7F3A9C12",
            senderPin = "00112233",
            senderName = "Alex",
            content = "Original typo content",
            timestamp = 1000L,
            isEdited = false
        )
        assertFalse(msg.isEdited)

        // Perform BBM Enterprise edit
        msg = msg.copy(
            content = "Corrected deployment parameters",
            isEdited = true,
            editedTimestamp = 2000L
        )

        assertTrue(msg.isEdited)
        assertEquals("Corrected deployment parameters", msg.content)
        assertEquals(2000L, msg.editedTimestamp)
    }

    @Test
    fun testEphemeralKeyAndPriorityTagging() {
        val ephemeralKey = CryptoManager.generateEphemeralMessageKey()
        assertNotNull(ephemeralKey)
        assertTrue("Ephemeral key must start with FIPS-AES256#", ephemeralKey.startsWith("FIPS-AES256#"))

        val urgentMsg = ChatMessage(
            id = "urgent_1",
            conversationId = "7F3A9C12",
            senderPin = "00112233",
            senderName = "Alex",
            content = "Critical security patch deployment",
            timestamp = 1000L,
            priority = MessagePriority.URGENT,
            ephemeralKeyId = ephemeralKey
        )

        assertEquals(MessagePriority.URGENT, urgentMsg.priority)
        assertEquals(ephemeralKey, urgentMsg.ephemeralKeyId)
    }

    @Test
    fun testEnterpriseConferenceCallSession() {
        val participants = listOf(
            ConferenceParticipant(pin = "PIN01", name = "Host", isSpeaking = false, isVideoOn = true),
            ConferenceParticipant(pin = "PIN02", name = "Sarah", isSpeaking = true, isVideoOn = true)
        )
        val session = CallSession(
            callId = "conf_001",
            peerPin = "ENTERPRISE_BRIDGE",
            peerName = "Global SecOps Incident Bridge",
            isVideo = true,
            isConference = true,
            status = CallStatus.CONNECTED,
            participants = participants
        )

        assertTrue(session.isConference)
        assertEquals(2, session.participants.size)
        assertTrue(session.participants.any { it.isSpeaking })
    }

    @Test
    fun testSecureRandomPinGeneration() {
        val pin = CryptoManager.generateSecureRandomPin()
        assertEquals(8, pin.length)
        assertTrue("PIN must match 8-char uppercase alphanumeric: $pin", pin.matches(Regex("^[0-9A-Z]{8}$")))

        // High entropy test: 100 generated PINs must all be distinct
        val generatedSet = mutableSetOf<String>()
        for (i in 0 until 100) {
            val p = CryptoManager.generateSecureRandomPin()
            assertEquals(8, p.length)
            assertTrue("PIN must be unique: $p", generatedSet.add(p))
        }
    }

    @Test
    fun testUniqueSecurePinCollisionHandling() {
        var attempts = 0
        // Simulate a collision scenario where the first 2 candidate PINs are rejected as taken
        val uniquePin = CryptoManager.generateUniqueSecurePin(
            isUnique = {
                attempts++
                attempts > 2 // Reject first 2, accept 3rd
            },
            maxAttempts = 10
        )

        assertEquals(8, uniquePin.length)
        assertEquals(3, attempts)
    }

    @Test(expected = IllegalStateException::class)
    fun testUniqueSecurePinMaxAttemptsExceededThrows() {
        CryptoManager.generateUniqueSecurePin(
            isUnique = { false }, // Simulates exhaustive collision
            maxAttempts = 5
        )
    }

    @Test
    fun testProfilePictureVisibilityPublic() {
        val profile = UserProfile(
            pin = "A1B2C3D4",
            displayName = "Alex Vance",
            statusMessage = "Online",
            avatarColorHex = "#0091EA",
            avatarUri = "content://media/external/images/media/42",
            pictureVisibility = ProfilePictureVisibility.PUBLIC,
            publicKeyBase64 = "testPub",
            fingerprint = "AA:BB:CC"
        )

        // Public photo must be visible to everyone, even null/unknown viewers
        assertTrue(profile.canViewerSeePicture(viewerPin = null))
        assertTrue(profile.canViewerSeePicture(viewerPin = "00112233"))
        assertTrue(profile.canViewerSeePicture(viewerPin = "99887766", groupId = "grp_123"))
    }

    @Test
    fun testProfilePictureVisibilityContactsOnly() {
        val profile = UserProfile(
            pin = "A1B2C3D4",
            displayName = "Alex Vance",
            statusMessage = "Online",
            avatarColorHex = "#0091EA",
            avatarUri = "content://media/external/images/media/42",
            pictureVisibility = ProfilePictureVisibility.CONTACTS_ONLY,
            publicKeyBase64 = "testPub",
            fingerprint = "AA:BB:CC"
        )

        // Contacts only must reject unauthenticated/unknown null viewers, but allow confirmed contacts
        assertFalse(profile.canViewerSeePicture(viewerPin = null))
        assertTrue(profile.canViewerSeePicture(viewerPin = "00112233"))
    }

    @Test
    fun testProfilePictureVisibilityPrivateSelected() {
        val allowedPin = "7F3A9C12"
        val unauthorizedPin = "99887766"
        val allowedGroup = "grp_security_team"
        val unauthorizedGroup = "grp_general"

        val profile = UserProfile(
            pin = "A1B2C3D4",
            displayName = "Alex Vance",
            statusMessage = "Online",
            avatarColorHex = "#0091EA",
            avatarUri = "content://media/external/images/media/42",
            pictureVisibility = ProfilePictureVisibility.PRIVATE_SELECTED,
            allowedPersonPins = setOf(allowedPin),
            allowedGroupIds = setOf(allowedGroup),
            publicKeyBase64 = "testPub",
            fingerprint = "AA:BB:CC"
        )

        // Unknown viewer
        assertFalse(profile.canViewerSeePicture(viewerPin = null))

        // Unauthorized contact not in allowed list
        assertFalse(profile.canViewerSeePicture(viewerPin = unauthorizedPin))

        // Authorized contact in allowed list
        assertTrue(profile.canViewerSeePicture(viewerPin = allowedPin))

        // Unauthorized group
        assertFalse(profile.canViewerSeePicture(viewerPin = null, groupId = unauthorizedGroup))

        // Authorized group
        assertTrue(profile.canViewerSeePicture(viewerPin = null, groupId = allowedGroup))

        // Either person or group match grants access
        assertTrue(profile.canViewerSeePicture(viewerPin = unauthorizedPin, groupId = allowedGroup))
    }

    @Test
    fun testProfilePictureNullUriNeverVisible() {
        val profile = UserProfile(
            pin = "A1B2C3D4",
            displayName = "Alex Vance",
            statusMessage = "Online",
            avatarColorHex = "#0091EA",
            avatarUri = null, // No picture set
            pictureVisibility = ProfilePictureVisibility.PUBLIC,
            publicKeyBase64 = "testPub",
            fingerprint = "AA:BB:CC"
        )

        assertFalse(profile.canViewerSeePicture(viewerPin = "00112233"))
    }
}
