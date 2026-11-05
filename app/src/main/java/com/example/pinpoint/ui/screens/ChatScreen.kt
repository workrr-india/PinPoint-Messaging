package com.example.pinpoint.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pinpoint.model.ChatMessage
import com.example.pinpoint.model.MessagePriority
import com.example.pinpoint.model.MessageType
import com.example.pinpoint.ui.components.DeliveryTickBadge
import com.example.pinpoint.ui.components.EditMessageDialog
import com.example.pinpoint.viewmodel.ChatViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val timedDuration by viewModel.timedDurationSeconds.collectAsState()
    val selectedPriority by viewModel.selectedPriority.collectAsState()
    val peerTypingMap by viewModel.peerTypingStatus.collectAsState()
    val isPeerTyping = peerTypingMap[conversationId] == true

    val isGroup = conversationId.startsWith("grp_")
    val contact = contacts.find { it.pin == conversationId }
    val group = groups.find { it.id == conversationId }

    val title = if (isGroup) "[${group?.members?.size ?: 3}] ${group?.name ?: "sales team"}" else (contact?.displayName ?: "Gideon Smith")
    val subtitle = if (isGroup) "${group?.members?.size ?: 3} members" else "Available"

    var inputText by remember { mutableStateOf("") }
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    var messageToEdit by remember { mutableStateOf<ChatMessage?>(null) }
    var showTimedDial by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val listState = rememberLazyListState()

    BackHandler(enabled = showTimedDial) {
        showTimedDial = false
    }
    BackHandler(enabled = !showTimedDial) {
        viewModel.closeConversation()
    }

    LaunchedEffect(messages.size, isPeerTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (contact != null) viewModel.openContactDetails(contact)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isGroup) Color(0xFF7C4DFF)
                                    else try {
                                        Color(android.graphics.Color.parseColor(contact?.avatarColorHex ?: "#007399"))
                                    } catch (_: Exception) {
                                        BbmTealPrimary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGroup) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = title.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeConversation() },
                        modifier = Modifier.testTag("action_chat_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Video Call Button
                    IconButton(
                        onClick = {
                            viewModel.startCall(
                                peerPin = conversationId,
                                peerName = contact?.displayName ?: title,
                                isVideo = true
                            )
                        },
                        modifier = Modifier.testTag("action_video_call")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                    }

                    // Voice Call Button
                    IconButton(
                        onClick = {
                            viewModel.startCall(
                                peerPin = conversationId,
                                peerName = contact?.displayName ?: title,
                                isVideo = false
                            )
                        },
                        modifier = Modifier.testTag("action_voice_call")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White)
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("action_chat_overflow")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View PIN Details") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    if (contact != null) viewModel.openContactDetails(contact)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Timed Messages") },
                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showTimedDial = !showTimedDial
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Message Priority") },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showPriorityMenu = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Send PING!") },
                                leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PingOrange) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.sendPing()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Legal & Privacy Policy") },
                                leadingIcon = { Icon(Icons.Default.Policy, contentDescription = null, tint = BbmTealPrimary) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setShowLegalCenter(true)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BbmTealPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .background(BbmChatBackground)
        ) {
            // Chat canvas: Light gray background (Screen 1)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    // System Notice for Group Chat (Screen 5)
                    if (isGroup) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFCFD8DC),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Gideon Smith joined the chat",
                                        fontSize = 12.sp,
                                        color = Color(0xFF37474F),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(messages, key = { it.id }) { message ->
                        BbmMessageItem(
                            message = message,
                            onOpenMenu = { selectedMessageForMenu = message },
                            onRevealTimed = { viewModel.revealTimedMessage(message.id) }
                        )
                    }

                    if (isPeerTyping) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$title is typing...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input Panel (Always visible above keyboard or navigation bar)
            Surface(
                color = Color(0xFFF7F9FA),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Screen 5: Timed Message Dial Drawer
                    AnimatedVisibility(visible = showTimedDial) {
                        TimedDialDrawer(
                            selectedSeconds = timedDuration,
                            onSelectDuration = { sec ->
                                viewModel.setTimedDuration(sec)
                                showTimedDial = false
                            },
                            onClose = { showTimedDial = false }
                        )
                    }

                    // Upper input row: Red exclamation badge + "Protected: Enter a message" + Mic/Send icon (Screen 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Red Exclamation Badge (!)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Protected text input field
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = if (timedDuration > 0) "Timed (${timedDuration}s): Enter message" else "Protected: Enter a message",
                                    color = Color(0xFF757575),
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = false,
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendTextMessage(inputText)
                                        inputText = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BbmTealPrimary,
                                unfocusedBorderColor = Color(0xFFCFD8DC),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                cursorColor = BbmTealPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_text_input")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Single Primary Action: Send Button when typing, Microphone when empty
                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendTextMessage(inputText)
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier.testTag("action_send_primary")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(BbmTealPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Message",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    viewModel.sendAttachment(
                                        type = MessageType.VOICE_NOTE,
                                        name = "voice_note.m4a",
                                        size = "0:14",
                                        caption = "Voice Note"
                                    )
                                    Toast.makeText(context, "Voice note recorded and sent!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("action_mic")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Note",
                                    tint = Color(0xFF546E7A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)

                    // Bottom toolbar row: Settings, Gallery, Camera, G (Stickers), Document, Timer (Clean 6 Feature Icons)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Settings / Security Options
                        IconButton(
                            onClick = { showPriorityMenu = true },
                            modifier = Modifier.size(38.dp).testTag("action_toolbar_settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF546E7A))
                        }

                        // Gallery / Photo
                        IconButton(
                            onClick = {
                                viewModel.sendAttachment(
                                    type = MessageType.IMAGE,
                                    name = "enterprise_photo.jpg",
                                    size = "1.2 MB",
                                    caption = "Shared photo"
                                )
                            },
                            modifier = Modifier.size(38.dp).testTag("action_toolbar_gallery")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Gallery", tint = Color(0xFF546E7A))
                        }

                        // Camera
                        IconButton(
                            onClick = {
                                viewModel.sendAttachment(
                                    type = MessageType.IMAGE,
                                    name = "camera_snapshot.jpg",
                                    size = "2.4 MB",
                                    caption = "Snapshot"
                                )
                            },
                            modifier = Modifier.size(38.dp).testTag("action_toolbar_camera")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera", tint = Color(0xFF546E7A))
                        }

                        // "G" Sticker / PING Button
                        IconButton(
                            onClick = { viewModel.sendPing() },
                            modifier = Modifier.size(38.dp).testTag("action_toolbar_stickers")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(1.5.dp, Color(0xFF546E7A), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color(0xFF546E7A)
                                )
                            }
                        }

                        // Document / File Attachment
                        IconButton(
                            onClick = {
                                viewModel.sendAttachment(
                                    type = MessageType.FILE,
                                    name = "da-bbm-enterprise-sdk.pdf",
                                    size = "734.55 KB",
                                    caption = "da-bbm-enterprise-sdk.pdf"
                                )
                            },
                            modifier = Modifier.size(38.dp).testTag("action_toolbar_file")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "Document", tint = Color(0xFF546E7A))
                        }

                        // Timer / Dial Toggle
                        IconButton(
                            onClick = { showTimedDial = !showTimedDial },
                            modifier = Modifier.size(38.dp).testTag("action_toolbar_timer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timed Message Dial",
                                tint = if (timedDuration > 0) BbmTealPrimary else Color(0xFF546E7A)
                            )
                        }
                    }
                }
            }
        }
    }

    // Screen 2: Authentic BBM "Message" Long-Press Dialog
    selectedMessageForMenu?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForMenu = null },
            title = {
                Text(
                    text = "Message",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Retract
                    Text(
                        text = "Retract",
                        fontSize = 16.sp,
                        color = Color(0xFF212121),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForMenu = null
                                viewModel.retractMessage(msg.id)
                                Toast.makeText(context, "Message retracted", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp)
                            .testTag("action_retract")
                    )

                    // Delete
                    Text(
                        text = "Delete",
                        fontSize = 16.sp,
                        color = Color(0xFF212121),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForMenu = null
                                viewModel.deleteMessage(msg.id)
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp)
                            .testTag("action_delete")
                    )

                    // Edit
                    if (msg.isOutgoing && !msg.isRetracted) {
                        Text(
                            text = "Edit",
                            fontSize = 16.sp,
                            color = Color(0xFF212121),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMessageForMenu = null
                                    messageToEdit = msg
                                }
                                .padding(vertical = 10.dp)
                                .testTag("action_edit")
                        )
                    }

                    // Copy
                    Text(
                        text = "Copy",
                        fontSize = 16.sp,
                        color = Color(0xFF212121),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForMenu = null
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("PinPoint Message", msg.content))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp)
                            .testTag("action_copy")
                    )

                    // Quote
                    Text(
                        text = "Quote",
                        fontSize = 16.sp,
                        color = Color(0xFF212121),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForMenu = null
                                inputText = "> \"${msg.content}\"\n"
                            }
                            .padding(vertical = 10.dp)
                            .testTag("action_quote")
                    )

                    // Forward
                    Text(
                        text = "Forward",
                        fontSize = 16.sp,
                        color = Color(0xFF212121),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageForMenu = null
                                Toast.makeText(context, "Select contact to forward to", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp)
                            .testTag("action_forward")
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedMessageForMenu = null }) {
                    Text("Cancel", color = BbmTealPrimary)
                }
            },
            containerColor = Color.White
        )
    }

    // Edit Dialog
    messageToEdit?.let { msg ->
        EditMessageDialog(
            initialText = msg.content,
            onConfirm = { updatedText ->
                viewModel.editMessage(msg.id, updatedText)
                messageToEdit = null
            },
            onDismiss = { messageToEdit = null }
        )
    }

    // Priority Menu Dialog
    if (showPriorityMenu) {
        AlertDialog(
            onDismissRequest = { showPriorityMenu = false },
            title = { Text("Message Priority", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MessagePriority.entries.forEach { priority ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedPriority(priority)
                                    showPriorityMenu = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPriority == priority,
                                onClick = {
                                    viewModel.setSelectedPriority(priority)
                                    showPriorityMenu = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (priority) {
                                    MessagePriority.NORMAL -> "Normal Delivery"
                                    MessagePriority.URGENT -> "🚨 Urgent (High Priority)"
                                    MessagePriority.CONFIDENTIAL -> "🔒 Confidential"
                                    MessagePriority.ACTION_REQUIRED -> "⚠️ Action Required"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPriorityMenu = false }) { Text("Close") }
            }
        )
    }
}

/**
 * Single Message Row matching Screen 1 (Chat with Gideon Smith):
 * - Incoming: Round avatar on left, pure white bubble, dark text
 * - Outgoing: Right-aligned, deep teal bubble, white text, delivery D/R badge
 * - PDF Attachment: Distinctive blue container with red PDF badge and CANCEL button
 */
@Composable
private fun BbmMessageItem(
    message: ChatMessage,
    onOpenMenu: () -> Unit,
    onRevealTimed: () -> Unit
) {
    val isOutgoing = message.isOutgoing
    val timeFormatted = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectTapGestures(
                    onLongPress = { onOpenMenu() },
                    onTap = { onOpenMenu() }
                )
            }
    ) {
        if (!isOutgoing) {
            // Incoming row: Round avatar on left + pure white bubble (Screen 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Round Avatar (Gideon Smith)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BbmTealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.senderName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = Color(0xFF78909C),
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 10.dp),
                        color = BbmBubbleIncoming,
                        shadowElevation = 1.dp,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (message.isRetracted) {
                                Text(
                                    text = "This message was retracted by sender",
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            } else if (message.isTimed && message.timerStartedAt == null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onRevealTimed() }
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = BbmTealPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Timed Message (Tap to reveal)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BbmTealPrimary
                                    )
                                }
                            } else {
                                Text(
                                    text = message.content,
                                    fontSize = 14.sp,
                                    color = Color(0xFF212121)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Outgoing row: Right-aligned, deep teal bubble or distinctive PDF attachment card
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = Color(0xFF78909C)
                    )
                }

                // Check if this is the prominent PDF attachment card from Screen 1!
                if (message.type == MessageType.FILE || message.content.contains(".pdf")) {
                    BbmFileAttachmentCard(
                        fileName = message.mediaName ?: message.content,
                        fileSize = message.mediaSize ?: "734.55 KB",
                        statusText = "Waiting for recipient to accept",
                        onCancel = { onOpenMenu() }
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 2.dp, bottomStart = 10.dp, bottomEnd = 10.dp),
                        color = BbmBubbleOutgoing,
                        shadowElevation = 1.dp,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (message.isRetracted) {
                                Text(
                                    text = "This message was retracted by sender",
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            } else {
                                Text(
                                    text = message.content,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (message.isEdited) {
                                    Text(
                                        text = "(Edited)",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                DeliveryTickBadge(status = message.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 1 Iconic PDF Attachment Card:
 * - Blue container (`#00829B`)
 * - Red square badge with "PDF"
 * - Title "da-bbm-enterprise-sdk.pdf"
 * - "734.55 KB"
 * - "Waiting for recipient to accept"
 * - CANCEL button
 */
@Composable
private fun BbmFileAttachmentCard(
    fileName: String,
    fileSize: String,
    statusText: String,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = BbmFileCardBlue,
        shadowElevation = 2.dp,
        modifier = Modifier
            .widthIn(max = 300.dp)
            .testTag("file_attachment_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Red square badge with white bold "PDF" text
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BbmPdfBadgeRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = fileSize,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CANCEL Button
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("button_cancel_attachment")
            ) {
                Text(
                    text = "CANCEL",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Screen 5: Timed Message Dial Drawer
 * Shows interactive duration picker with circular dial and "Share until 11:49 AM"
 */
@Composable
private fun TimedDialDrawer(
    selectedSeconds: Int,
    onSelectDuration: (Int) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share until 11:49 AM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Circular Dial representation
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F4F8))
                    .border(5.dp, Color(0xFFFF9800), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedSeconds > 0) "${selectedSeconds}" else "30",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = "SECONDS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757575)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duration Pill Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(5, 10, 30, 60).forEach { sec ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedSeconds == sec) BbmTealPrimary else Color(0xFFECEFF1),
                        modifier = Modifier.clickable { onSelectDuration(sec) }
                    ) {
                        Text(
                            text = "${sec}s",
                            color = if (selectedSeconds == sec) Color.White else Color(0xFF37474F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = { onSelectDuration(0) }) {
                Text("Disable Timer", color = Color(0xFF757575), fontSize = 12.sp)
            }
        }
    }
}
