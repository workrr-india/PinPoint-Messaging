# PinPoint Architecture & Reverse-Engineering Findings Report

## Executive Summary
This document provides an evidence-backed architectural specification for **PinPoint**, an original, clean-room, privacy-first messaging application implementing both consumer BlackBerry Messenger (BBM) and **BBM Enterprise (BBMe)** capabilities.

In accordance with clean-room boundaries, all source code, cryptographic primitives, and user interfaces are original implementations that do not reuse proprietary assets, trademarked logos, or decompiled code.

---

## 1. Feature Analysis: Observed Behavior vs. Original Design

### Area 1: PIN-Based Identity & Key Binding

| Dimension | Observed BBM Architecture (Historical / Static Evidence) | PinPoint Clean-Room Design Choice |
| :--- | :--- | :--- |
| **Identifier Generation** | Originally derived from hardware IMEI/PIN provisioned via BlackBerry Infrastructure (BIS/BES) and BlackBerry Identity (BBID). Format: 8-character hex string (e.g., `2B4F89A1`). | **Deterministic Key Derivation**: PIN is an 8-character uppercase hex string derived from `SHA-256(PublicKey)[0..3]`. Completely decoupled from hardware identifiers or carrier provisioning. |
| **Privacy & Telecom Decoupling** | No phone number was required to chat. Users shared their 8-character alphanumeric PIN or barcode/QR code. | **Zero Telecom Linkage**: Completely eliminates SIM-swap attacks, SMS interception, and contact-book phone scraping. Users cannot be tracked via phone numbers. |
| **Key Mapping & Binding** | Account state mapped via centralized infrastructure to proprietary public key certificates. | **Standard Public Key Infrastructure (PKI)**: User's PIN is cryptographically bound to an RSA-2048/Ed25519 keypair. The full public key fingerprint is verifiable in-app (`CryptoManager`). |
| **Contact Invitation Flow** | Mutual agreement model: Entering a PIN transmits an invitation packet. Both parties must approve before messages can route. | **Zero-Trust Invitation Ledger**: Incoming/outgoing invitations require explicit user confirmation before mutual chat routing is authorized. |

### Area 2: Real-Time State Machine & Status Indicators

| Dimension | Observed BBM Architecture | PinPoint Clean-Room Design Choice |
| :--- | :--- | :--- |
| **Message States** | 1. Clock (Sending)<br>2. Single Checkmark (Sent to server)<br>3. **D** Badge (Delivered to peer device)<br>4. **R** Badge (Opened/Read by recipient) | **Iconic D & R State Machine**: Outgoing messages transition through `SENDING -> SENT -> DELIVERED ('D') -> READ ('R')`. Visual badges rendered with accessible M3 tokens. |
| **Receipt Protocol Model** | Bi-directional ACK messages: Server sends Delivery Ack upon socket delivery; recipient client emits Read Ack upon UI render. | **WebSocket / Push Ack Framing**: Asynchronous acknowledgment frames over real-time transport with state machine updates. |
| **Contact Is Typing** | Ephemeral signaling frame sent when user enters text; expires after ~3 seconds without keystrokes. | **Animated Waveform Bubble**: Real-time typing indicators with debounce and animated pulse indicator. |
| **PING / Nudge Feature** | Sent priority vibration packet that shook the screen and triggered an audible haptic buzz. | **PinPoint PING**: Vibrates device using `VibratorManager` waveform pattern, shakes chat interface, and alerts recipient. |

### Area 3: Multimedia & Calling

| Dimension | Observed BBM Architecture | PinPoint Clean-Room Design Choice |
| :--- | :--- | :--- |
| **1-on-1 & Group Chats** | P2P and reflector-based group routing with Owner/Admin/Member hierarchy. | **Group Hierarchy & Access Control**: Group owner can assign Admin roles, add/remove members, and distribute group encryption parameters. |
| **Voice & Video Calling** | BBM Voice and BBM Video utilized proprietary VoIP engines and SIP/RTP signalling with native codecs (Speex/Opus/H.264). | **WebRTC-Compatible Signaling**: Call state machine (`IDLE -> OUTGOING_RINGING -> CONNECTED -> ENDED`) with full UI for audio waveforms, video stream placeholder, camera flip, and speaker toggle. |
| **File & Media Sharing** | Chunked binary transfers with thumbnail preview and checksum verification. | **Structured Media Payloads**: Distinct message types (`IMAGE`, `VOICE_NOTE`, `FILE`) with inline preview, waveform player, and progress metadata. |

### Area 4: Privacy Controls & BBM Enterprise (BBMe)

| Dimension | Observed BBM / BBMe Architecture | PinPoint Clean-Room Design Choice |
| :--- | :--- | :--- |
| **FIPS 140-2 Per-Message Keys** | BBM Enterprise generates a unique random ephemeral symmetric key (AES-256) per message. | **Per-Message Ephemeral Keys**: `CryptoManager.generateEphemeralMessageKey()` generates `FIPS-AES256#<HEX>` session tags bound to every message transmission. |
| **Message Editing In-Place** | BBMe allows editing sent messages with clear audit markers to maintain corporate compliance. | **Message Editing**: Sender can modify sent messages in-place; UI displays an `(Edited)` audit badge with timestamp. |
| **Message Retraction (Recall)** | Sender long-presses sent message to retract. Recipient client replaces message content with retraction notice. | **Bidirectional Message Retraction**: Retraction command overwrites local and remote copies with `"This message was retracted by sender"` and scrubs attachments. |
| **Data Loss Prevention (DLP)** | BBMe provides watermarking, copy restrictions, and ephemeral session wipe. | **Dynamic DLP Watermark**: Canvas-rendered diagonal low-opacity watermark rendering `PINPOINT SECURE • PIN: <MY_PIN> • CONFIDENTIAL` across chat surfaces. |
| **Enterprise Priority Tagging** | BBMe allows tagging messages as Urgent, Confidential, or Action Required. | **MessagePriority Engine**: Distinct floating badges (`URGENT` red pulse, `CONFIDENTIAL` purple shield, `ACTION_REQUIRED` amber). |
| **Multi-Party Conference Calling** | BBMe provides enterprise audio/video bridge conferencing. | **Enterprise Conference Bridge**: Multi-party active room with host controls, participant speaking wave indicators, and desktop screen sharing simulation. |
| **Enterprise Directory & Clearances** | Integration with corporate directories (BlackBerry UEM) displaying department and clearance levels. | **Corporate Directory Model**: Contact profile cards display organization, department, job title, and verified security clearances (Levels 1 to 5). |

---

## 2. Cryptographic Specification

### PIN Derivation Algorithm
```
Input: PublicKey (bytes)
Digest: D = SHA-256(PublicKey)
PIN = Hex(D[0..3]).toUpperCase()
Length: Exactly 8 alphanumeric characters [0-9A-F]
```

### Digital Signature & Ephemeral Key Scheme
- **Algorithm**: `SHA256withRSA` / `SHA256withECDSA`
- **Payload**: Canonical JSON message payload
- **Ephemeral Session Key**: Unique `FIPS-AES256#<HEX>` generated per message
- **Verification**: Recipient verifies signature against sender's registered public key.

---

## 3. Findings Ledger & Limitations

| Evidence Item | Status | Classification | Notes / Verification |
| :--- | :--- | :--- | :--- |
| **PIN Format** | Answered | Observation | BBM uses 8-char hex strings; clean-room app adopts identical format derived deterministically. |
| **D and R Ticks** | Answered | Observation | Delivery and Read receipts are the cornerstone of the user experience. |
| **Per-Message Ephemeral Keys** | Answered | Observation | BBMe hallmark feature: each message protected with distinct symmetric key. |
| **Message Editing & Recall** | Answered | Observation | BBMe enterprise compliance requires both edit audit trail and remote retraction. |
| **Native VoIP Codecs** | Partially Answered | Inference | Static APKs contain native `.so` libraries (e.g., Opus/WebRTC); runtime verification required on live networks. |
| **Server Reflector Topology** | Unresolved | Unknown | Proprietary server-side routing protocol is proprietary and cannot be queried live under clean-room rules. |
