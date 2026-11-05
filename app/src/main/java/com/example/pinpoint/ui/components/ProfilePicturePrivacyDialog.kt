package com.example.pinpoint.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.pinpoint.model.Contact
import com.example.pinpoint.model.GroupConversation
import com.example.pinpoint.model.ProfilePictureVisibility
import com.example.pinpoint.model.UserProfile
import com.example.ui.theme.BbmTealDark
import com.example.ui.theme.BbmTealPrimary
import com.example.ui.theme.PinCyanAccent

/**
 * Curated preset avatar options for quick selection in demo / enterprise environments.
 */
data class PresetAvatarOption(
    val id: String,
    val title: String,
    val resourceId: Int? = null,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val colorHex: String
)

val PRESET_AVATARS = listOf(
    PresetAvatarOption("ppm_logo", "PPM Official", resourceId = R.drawable.ppm_logo, colorHex = "#007399"),
    PresetAvatarOption("cyber_shield", "Cyber SecOps", icon = Icons.Default.Security, colorHex = "#0D47A1"),
    PresetAvatarOption("tech_lead", "Tech Lead", icon = Icons.Default.Terminal, colorHex = "#00695C"),
    PresetAvatarOption("executive", "Executive", icon = Icons.Default.WorkspacePremium, colorHex = "#E65100"),
    PresetAvatarOption("stealth", "Stealth Ops", icon = Icons.Default.VisibilityOff, colorHex = "#263238")
)

@Composable
fun ProfilePicturePrivacyDialog(
    userProfile: UserProfile,
    contacts: List<Contact>,
    groups: List<GroupConversation>,
    onUpdatePicture: (String?) -> Unit,
    onUpdateVisibility: (ProfilePictureVisibility, Set<String>, Set<String>) -> Unit,
    onTogglePersonAccess: (String) -> Unit,
    onToggleGroupAccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var selectedVisibility by remember { mutableStateOf(userProfile.pictureVisibility) }
    var selectedPersonPins by remember { mutableStateOf(userProfile.allowedPersonPins) }
    var selectedGroupIds by remember { mutableStateOf(userProfile.allowedGroupIds) }
    var privateTab by remember { mutableStateOf(0) } // 0: Persons, 1: Groups
    var previewContactPin by remember { mutableStateOf<String?>(contacts.firstOrNull()?.pin) }

    // Modern Zero-Permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdatePicture(uri.toString())
            Toast.makeText(context, "Profile picture updated from gallery!", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = {
            onUpdateVisibility(selectedVisibility, selectedPersonPins, selectedGroupIds)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("dialog_profile_picture_privacy"),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BbmTealPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = BbmTealPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Profile Picture & Privacy",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Add picture and control who can view it",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onUpdateVisibility(selectedVisibility, selectedPersonPins, selectedGroupIds)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("dialog_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Picture Avatar Showcase & Actions
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Picture preview with status badge
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .border(3.dp, BbmTealPrimary, CircleShape)
                                            .background(Color(0xFFE0F2FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val avatar = userProfile.avatarUri
                                        if (avatar != null) {
                                            when {
                                                avatar.startsWith("preset://ppm_logo") -> {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.ppm_logo),
                                                        contentDescription = "Profile Picture",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                avatar.startsWith("preset://") -> {
                                                    val preset = PRESET_AVATARS.find { "preset://${it.id}" == avatar }
                                                    if (preset?.icon != null) {
                                                        Icon(
                                                            imageVector = preset.icon,
                                                            contentDescription = preset.title,
                                                            tint = Color.White,
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color(android.graphics.Color.parseColor(preset.colorHex)))
                                                                .padding(20.dp)
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = BbmTealPrimary, modifier = Modifier.size(54.dp))
                                                    }
                                                }
                                                else -> {
                                                    AsyncImage(
                                                        model = avatar,
                                                        contentDescription = "Profile Picture",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = userProfile.displayName.take(1).uppercase(),
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BbmTealPrimary
                                            )
                                        }
                                    }

                                    // Visibility badge icon at bottom right of avatar
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (selectedVisibility) {
                                                    ProfilePictureVisibility.PUBLIC -> Color(0xFF00897B)
                                                    ProfilePictureVisibility.CONTACTS_ONLY -> BbmTealPrimary
                                                    ProfilePictureVisibility.PRIVATE_SELECTED -> Color(0xFFD32F2F)
                                                }
                                            )
                                            .border(2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (selectedVisibility) {
                                                ProfilePictureVisibility.PUBLIC -> Icons.Default.Public
                                                ProfilePictureVisibility.CONTACTS_ONLY -> Icons.Default.People
                                                ProfilePictureVisibility.PRIVATE_SELECTED -> Icons.Default.Lock
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = userProfile.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )

                                Text(
                                    text = "PIN: ${userProfile.pin}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = BbmTealPrimary
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Image Actions: Photo Picker & Remove
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("button_upload_photo")
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Choose Photo", fontSize = 13.sp)
                                    }

                                    if (userProfile.avatarUri != null) {
                                        OutlinedButton(
                                            onClick = {
                                                onUpdatePicture(null)
                                                Toast.makeText(context, "Profile picture removed", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("button_remove_photo")
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove", fontSize = 13.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick-Select Preset Avatars Row
                                Text(
                                    text = "Or choose a preset security avatar:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(PRESET_AVATARS) { preset ->
                                        val isCurrent = userProfile.avatarUri == "preset://${preset.id}"
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isCurrent) BbmTealPrimary.copy(alpha = 0.15f) else Color.White,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                if (isCurrent) BbmTealPrimary else Color(0xFFCBD5E1)
                                            ),
                                            modifier = Modifier.clickable {
                                                onUpdatePicture("preset://${preset.id}")
                                                Toast.makeText(context, "Selected ${preset.title}", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (preset.resourceId != null) {
                                                    Image(
                                                        painter = painterResource(id = preset.resourceId),
                                                        contentDescription = preset.title,
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                    )
                                                } else if (preset.icon != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(android.graphics.Color.parseColor(preset.colorHex))),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(preset.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = preset.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) BbmTealPrimary else Color(0xFF334155)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Visibility Selector Section
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = BbmTealPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Visibility & Audience Options",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                text = "Control which users or groups can see your profile picture",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Radio 1: Public (Everyone)
                    item {
                        VisibilityOptionCard(
                            title = "Public (Everyone)",
                            description = "Visible to anyone who enters your PIN or contacts you in public chats.",
                            icon = Icons.Default.Public,
                            badgeColor = Color(0xFF00897B),
                            isSelected = selectedVisibility == ProfilePictureVisibility.PUBLIC,
                            onClick = {
                                selectedVisibility = ProfilePictureVisibility.PUBLIC
                                onUpdateVisibility(ProfilePictureVisibility.PUBLIC, selectedPersonPins, selectedGroupIds)
                            }
                        )
                    }

                    // Radio 2: My Contacts Only
                    item {
                        VisibilityOptionCard(
                            title = "My Contacts Only",
                            description = "Only people in your approved Contacts list can view your picture.",
                            icon = Icons.Default.People,
                            badgeColor = BbmTealPrimary,
                            isSelected = selectedVisibility == ProfilePictureVisibility.CONTACTS_ONLY,
                            onClick = {
                                selectedVisibility = ProfilePictureVisibility.CONTACTS_ONLY
                                onUpdateVisibility(ProfilePictureVisibility.CONTACTS_ONLY, selectedPersonPins, selectedGroupIds)
                            }
                        )
                    }

                    // Radio 3: Private View (Selected Person or Group)
                    item {
                        VisibilityOptionCard(
                            title = "Private View (Selected Person or Group)",
                            description = "Strictly restricted: Only chosen individuals or teams can view your photo.",
                            icon = Icons.Default.Lock,
                            badgeColor = Color(0xFFD32F2F),
                            isSelected = selectedVisibility == ProfilePictureVisibility.PRIVATE_SELECTED,
                            onClick = {
                                selectedVisibility = ProfilePictureVisibility.PRIVATE_SELECTED
                                onUpdateVisibility(ProfilePictureVisibility.PRIVATE_SELECTED, selectedPersonPins, selectedGroupIds)
                            }
                        )
                    }

                    // Specific Audience Selector (Only visible when PRIVATE_SELECTED is active)
                    if (selectedVisibility == ProfilePictureVisibility.PRIVATE_SELECTED) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, BbmTealPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Allowed Viewers Selection",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = BbmTealPrimary
                                        ) {
                                            Text(
                                                text = "${selectedPersonPins.size} Persons • ${selectedGroupIds.size} Groups",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Tab Row: Persons vs Groups
                                    TabRow(
                                        selectedTabIndex = privateTab,
                                        containerColor = Color.White,
                                        contentColor = BbmTealPrimary,
                                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    ) {
                                        Tab(
                                            selected = privateTab == 0,
                                            onClick = { privateTab = 0 },
                                            text = {
                                                Text("Selected Persons (${selectedPersonPins.size}/${contacts.size})", fontSize = 12.sp)
                                            },
                                            icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                        Tab(
                                            selected = privateTab == 1,
                                            onClick = { privateTab = 1 },
                                            text = {
                                                Text("Selected Groups (${selectedGroupIds.size}/${groups.size})", fontSize = 12.sp)
                                            },
                                            icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Content for Tab 0: Persons
                                    if (privateTab == 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Choose individual contacts:",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            TextButton(
                                                onClick = {
                                                    selectedPersonPins = if (selectedPersonPins.size == contacts.size) {
                                                        emptySet()
                                                    } else {
                                                        contacts.map { it.pin }.toSet()
                                                    }
                                                    onUpdateVisibility(selectedVisibility, selectedPersonPins, selectedGroupIds)
                                                }
                                            ) {
                                                Text(
                                                    text = if (selectedPersonPins.size == contacts.size) "Deselect All" else "Select All",
                                                    fontSize = 12.sp,
                                                    color = BbmTealPrimary
                                                )
                                            }
                                        }

                                        if (contacts.isEmpty()) {
                                            Text("No contacts added yet.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            contacts.forEach { contact ->
                                                val isChecked = selectedPersonPins.contains(contact.pin)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isChecked) Color(0xFFE0F2FE) else Color.White,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        0.8.dp,
                                                        if (isChecked) BbmTealPrimary else Color(0xFFE2E8F0)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clickable {
                                                            selectedPersonPins = if (isChecked) {
                                                                selectedPersonPins - contact.pin
                                                            } else {
                                                                selectedPersonPins + contact.pin
                                                            }
                                                            onTogglePersonAccess(contact.pin)
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Checkbox(
                                                            checked = isChecked,
                                                            onCheckedChange = { checked ->
                                                                selectedPersonPins = if (checked) {
                                                                    selectedPersonPins + contact.pin
                                                                } else {
                                                                    selectedPersonPins - contact.pin
                                                                }
                                                                onTogglePersonAccess(contact.pin)
                                                            },
                                                            colors = CheckboxDefaults.colors(checkedColor = BbmTealPrimary)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    try {
                                                                        Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                                                                    } catch (_: Exception) {
                                                                        BbmTealPrimary
                                                                    }
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = contact.displayName.take(1).uppercase(),
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = contact.displayName,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 13.sp,
                                                                color = Color(0xFF0F172A)
                                                            )
                                                            Text(
                                                                text = "PIN: ${contact.pin} • ${contact.jobTitle}",
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF64748B)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Content for Tab 1: Groups
                                    if (privateTab == 1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Choose authorized groups:",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            TextButton(
                                                onClick = {
                                                    selectedGroupIds = if (selectedGroupIds.size == groups.size) {
                                                        emptySet()
                                                    } else {
                                                        groups.map { it.id }.toSet()
                                                    }
                                                    onUpdateVisibility(selectedVisibility, selectedPersonPins, selectedGroupIds)
                                                }
                                            ) {
                                                Text(
                                                    text = if (selectedGroupIds.size == groups.size) "Deselect All" else "Select All",
                                                    fontSize = 12.sp,
                                                    color = BbmTealPrimary
                                                )
                                            }
                                        }

                                        if (groups.isEmpty()) {
                                            Text("No enterprise groups active.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            groups.forEach { group ->
                                                val isChecked = selectedGroupIds.contains(group.id)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isChecked) Color(0xFFEDE7F6) else Color.White,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        0.8.dp,
                                                        if (isChecked) Color(0xFF7C4DFF) else Color(0xFFE2E8F0)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp)
                                                        .clickable {
                                                            selectedGroupIds = if (isChecked) {
                                                                selectedGroupIds - group.id
                                                            } else {
                                                                selectedGroupIds + group.id
                                                            }
                                                            onToggleGroupAccess(group.id)
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Checkbox(
                                                            checked = isChecked,
                                                            onCheckedChange = { checked ->
                                                                selectedGroupIds = if (checked) {
                                                                    selectedGroupIds + group.id
                                                                } else {
                                                                    selectedGroupIds - group.id
                                                                }
                                                                onToggleGroupAccess(group.id)
                                                            },
                                                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7C4DFF))
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF7C4DFF)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = group.name,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 13.sp,
                                                                color = Color(0xFF0F172A)
                                                            )
                                                            Text(
                                                                text = "${group.members.size} team members",
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF64748B)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Access Verification & Privacy Simulator
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = BbmTealPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Privacy Simulator: Test How Others See You",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Select viewer to simulate
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(contacts) { contact ->
                                        val isSimulated = previewContactPin == contact.pin
                                        FilterChip(
                                            selected = isSimulated,
                                            onClick = { previewContactPin = contact.pin },
                                            label = { Text(contact.displayName.split(" ").first(), fontSize = 11.sp) }
                                        )
                                    }
                                    item {
                                        val isUnknown = previewContactPin == "UNKNOWN_PUBLIC"
                                        FilterChip(
                                            selected = isUnknown,
                                            onClick = { previewContactPin = "UNKNOWN_PUBLIC" },
                                            label = { Text("Unknown User", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val testViewerPin = if (previewContactPin == "UNKNOWN_PUBLIC") null else previewContactPin
                                val isViewerAllowed = when (selectedVisibility) {
                                    ProfilePictureVisibility.PUBLIC -> true
                                    ProfilePictureVisibility.CONTACTS_ONLY -> testViewerPin != null
                                    ProfilePictureVisibility.PRIVATE_SELECTED -> testViewerPin != null && selectedPersonPins.contains(testViewerPin)
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isViewerAllowed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isViewerAllowed) Color(0xFFA5D6A7) else Color(0xFFFFCDD2)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isViewerAllowed) Icons.Default.CheckCircle else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isViewerAllowed) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isViewerAllowed) "Access Granted" else "Access Restricted",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isViewerAllowed) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                            Text(
                                                text = if (isViewerAllowed) {
                                                    "Viewer can view your high-resolution profile picture."
                                                } else {
                                                    "Viewer only sees privacy-masked monogram initial."
                                                },
                                                fontSize = 11.sp,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                // Bottom Save & Powered by Workrr.in Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Powered by Workrr.in
                    Row(
                        modifier = Modifier
                            .clickable {
                                try {
                                    uriHandler.openUri("https://workrr.in")
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Opening workrr.in", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("privacy_dialog_workrr_link"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Powered by ", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(
                            "Workrr.in",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BbmTealPrimary,
                            textDecoration = TextDecoration.Underline
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = BbmTealPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    Button(
                        onClick = {
                            onUpdateVisibility(selectedVisibility, selectedPersonPins, selectedGroupIds)
                            Toast.makeText(context, "Picture & privacy settings updated!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("button_save_picture_privacy")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Settings", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VisibilityOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) BbmTealPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) BbmTealPrimary else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = BbmTealPrimary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
