package com.example.pinpoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pinpoint.model.Contact
import com.example.pinpoint.model.GroupConversation
import com.example.pinpoint.model.GroupRole
import com.example.ui.theme.PinBluePrimary
import com.example.ui.theme.PinCyanAccent
import com.example.ui.theme.StatusFailedRed

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onSendInvite: (pin: String, message: String) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("Hi! Add me on PinPoint.") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Contact by PIN",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your contact's 8-character alphanumeric PIN. No phone number or SIM required.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = pinText,
                    onValueChange = {
                        if (it.length <= 8) {
                            pinText = it.uppercase()
                            errorText = null
                        }
                    },
                    label = { Text("8-Character PIN (e.g. 7F3A9C12)") },
                    singleLine = true,
                    isError = errorText != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_pin")
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = StatusFailedRed,
                        fontSize = 12.sp
                    )
                }

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Invitation Message") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_invite_message")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinText.length != 8) {
                        errorText = "PIN must be exactly 8 characters"
                    } else {
                        onSendInvite(pinText, messageText)
                    }
                },
                modifier = Modifier.testTag("confirm_add_contact_button")
            ) {
                Text("Send Invitation")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_contact_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateGroupDialog(
    availableContacts: List<Contact>,
    onDismiss: () -> Unit,
    onCreateGroup: (name: String, description: String, selectedPins: List<String>) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    val selectedPins = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New PinPoint Group",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_name")
                )

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description / Topic") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_description")
                )

                Text(
                    text = "Select Members:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyColumn(
                    modifier = Modifier
                        .height(140.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(availableContacts) { contact ->
                        val isSelected = selectedPins.contains(contact.pin)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isSelected) selectedPins.remove(contact.pin)
                                    else selectedPins.add(contact.pin)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedPins.add(contact.pin)
                                    else selectedPins.remove(contact.pin)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = contact.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "PIN: ${contact.pin}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onCreateGroup(nameText, descriptionText, selectedPins.toList())
                    }
                },
                enabled = nameText.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_group_button")
            ) {
                Text("Create Group")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_group_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GroupDetailsDialog(
    group: GroupConversation,
    myPin: String,
    onDismiss: () -> Unit,
    onUpdateRole: (memberPin: String, newRole: GroupRole) -> Unit,
    onRemoveMember: (memberPin: String) -> Unit
) {
    val canManage = group.canManageMembers(myPin)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = group.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${group.members.size} members",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (group.description.isNotBlank()) {
                    Text(
                        text = group.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Members & Permissions",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyColumn(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(group.members) { member ->
                        val isSelf = member.pin == myPin
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "PIN: ${member.pin}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Role Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (member.role) {
                                    GroupRole.OWNER -> Color(0xFFE65100)
                                    GroupRole.ADMIN -> PinBluePrimary
                                    GroupRole.MEMBER -> Color(0xFF334155)
                                }
                            ) {
                                Text(
                                    text = member.role.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (canManage && !isSelf && member.role != GroupRole.OWNER) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val nextRole = if (member.role == GroupRole.ADMIN) GroupRole.MEMBER else GroupRole.ADMIN
                                        onUpdateRole(member.pin, nextRole)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (member.role == GroupRole.ADMIN) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = "Toggle Admin",
                                        tint = PinCyanAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveMember(member.pin) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = StatusFailedRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_group_details_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun EditMessageDialog(
    initialText: String,
    onConfirm: (newText: String) -> Unit,
    onDismiss: () -> Unit
) {
    var editedText by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Sent Message",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PinPoint allows in-place message correction. The message will be tagged with (Edited).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_message")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(editedText) },
                enabled = editedText.isNotBlank(),
                modifier = Modifier.testTag("confirm_edit_message_button")
            ) {
                Text("Update Message")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ContactDetailsDialog(
    contact: Contact,
    onStartCall: () -> Unit,
    onStartVideo: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                            } catch (_: Exception) {
                                PinBluePrimary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.displayName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = contact.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "PIN: ${contact.pin}",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = PinCyanAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Clearance Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SECURITY CLEARANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                            Text(
                                text = contact.clearanceLevel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Corporate Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Organization: ${contact.organization}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Department: ${contact.department}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Role: ${contact.jobTitle}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Status: ${contact.statusMessage}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Cryptographic validation banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = PinCyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FIPS 140-2 Ephemeral Key Exchange Verified",
                        fontSize = 11.sp,
                        color = PinCyanAccent
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onDismiss()
                        onStartCall()
                    }
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Voice")
                }
                Button(
                    onClick = {
                        onDismiss()
                        onStartVideo()
                    }
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
