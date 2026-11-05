

# PinPoint Messenger

**PinPoint** is a privacy-first, enterprise-grade Android messaging application built with **Jetpack Compose** and **Material Design 3**, combining the fast consumer experience with the security and compliance features of **PinPoint Enterprise**.

---
<img width="377" height="786" alt="image" src="https://github.com/user-attachments/assets/e655e659-8943-42b9-bec4-2e0d12223180" />
<img width="367" height="776" alt="image" src="https://github.com/user-attachments/assets/ddf3a92d-ed28-4f9d-8044-b24cb8f5db24" />
<img width="367" height="778" alt="image" src="https://github.com/user-attachments/assets/77aab923-8367-4ea0-a4f9-50174f125391" />
<img width="363" height="792" alt="image" src="https://github.com/user-attachments/assets/eaa24c10-e4a4-490d-ac26-2c02fddfa18e" />
<img width="357" height="787" alt="image" src="https://github.com/user-attachments/assets/65707f3c-9cf7-4c8b-ba5e-9423174533eb" />


## Key Features

### 1. Cryptographic 8-Character PIN Identity
- Zero reliance on phone numbers, SIM cards, or telecom SMS verification.
- PINs (e.g. `7F3A9C12`) are derived deterministically from the SHA-256 digest of the user's public key.
- Built-in dynamic QR code generator canvas for quick PIN scanning and sharing.
- Mutual invitation ledger: zero unsolicited message routing.

### 2. Real-Time Indicators & Feedback 
- **Sent**: Single tick
- **Delivered**: Iconic **D** badge (blue)
- **Read**: Iconic **R** badge (green)
- Real-time animated "Contact is typing..." waveform bubble.
- **PING / Nudge**: Priority vibration alert pattern (`VibratorManager`) with visual screen alert.

### 3. PinPoint Enterprise Security & Compliance
- **Per-Message Ephemeral Symmetric Keys**: Each message is sealed with a unique ephemeral key tag (`FIPS-AES256#<HEX>`) adhering to the FIPS 140-2 encryption model.
- **In-Place Message Editing**: Senders can edit sent messages; an `(Edited)` badge is rendered with timestamp.
- **Message Retraction (Recall)**: Sender can retract any sent message, wiping content and media across all recipient devices.
- **Enterprise Priority Tagging**: Send messages tagged as `URGENT` (crimson alert), `CONFIDENTIAL` (restricted shield), or `ACTION REQUIRED` (amber caution).
- **Dynamic DLP Watermark**: Canvas-rendered diagonal low-opacity security watermark (`PINPOINT SECURE • PIN: <PIN> • CONFIDENTIAL`) across chat surfaces to protect against unauthorized screenshots and leaks.
- **Corporate Directory & Security Clearances**: Contact profile cards display organization, department, role, and security clearance levels (`LEVEL 1` to `LEVEL 5 FIPS 140-2 TOP SECRET`).

### 4. Multimedia & Enterprise Calling
- **Enterprise Conference Bridge**: Multi-party encrypted conference room with host controls, active speaker detection, and desktop screen sharing simulation.
- 1-on-1 Voice and Video calls with camera flip, mute, and speakerphone controls.
- Voice Notes with animated waveform visualizer.
- Photo attachments with inline thumbnail previews.
- Document and file sharing with transfer progress and download state.

---

## Architecture & Code Structure

```
com.example
├── MainActivity.kt                      # Edge-to-edge entrypoint, navigation & active overlays
com.example.pinpoint
├── crypto/
│   └── CryptoManager.kt                 # RSA keypair generation, PIN derivation, ephemeral keys & QR matrix
├── data/
│   └── PinPointRepository.kt            # Reactive StateFlows, D & R status transitions, PinPoint editing & calls
├── model/
│   ├── MessageModels.kt                 # ChatMessage, MessagePriority, MessageStatus, MessageType
│   ├── UserModels.kt                    # UserProfile, Contact with clearance levels, ContactInvitation
│   ├── GroupModels.kt                   # GroupConversation, GroupMember, GroupRole
│   └── CallModels.kt                    # CallSession, ConferenceParticipant, CallStatus
├── viewmodel/
│   └── ChatViewModel.kt                 # MVVM ViewModel coordinating chats, editing, priority, and calling
└── ui/
    ├── components/
    │   ├── DeliveryTickBadge.kt         # Iconic 'D' and 'R' receipt badge composables
    │   ├── QrCodeCanvas.kt              # Native 21x21 QR code canvas renderer for PIN sharing
    │   ├── AudioWaveformBar.kt          # Animated audio waveform player for voice notes
    │   ├── CallOverlay.kt               # Voice, video, and multi-party conference calling overlay
    │   └── Dialogs.kt                   # Add contact, create group, edit message, and contact details dialogs
    ├── screens/
    │   ├── ConversationListScreen.kt    # Chats, Contacts, Groups, Profile & PIN card
    │   └── ChatScreen.kt                # Conversation screen with DLP watermark, priority chips & retraction
    └── theme/
        ├── Color.kt                     # Modern obsidian/slate palette with cyan & status colors
        └── Theme.kt                     # PinPoint Material 3 dark/light theme
```

---

## Running Unit Tests

Run local JVM unit tests via Gradle:

```bash
gradle :app:testDebugUnitTest
```

Unit tests cover:
- Cryptographic 8-character PIN derivation from public key SHA-256.
- Digital signature generation and verification.
- Message status transitions (`SENDING -> SENT -> DELIVERED ('D') -> READ ('R')`).
- Timed message countdown calculation and automatic expiration.
- Message retraction (recall) and content scrubbing.
- In-place message editing with `isEdited` audit state.
- Ephemeral session key generation (`FIPS-AES256#...`) and priority classification.
- Multi-party enterprise conference calling sessions.
- Group membership permissions and role management (`OWNER`, `ADMIN`, `MEMBER`).
