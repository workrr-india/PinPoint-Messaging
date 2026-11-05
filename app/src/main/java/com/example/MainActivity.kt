package com.example

import android.Manifest
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pinpoint.notification.NotificationHelper
import com.example.pinpoint.ui.components.AddContactDialog
import com.example.pinpoint.ui.components.CallOverlay
import com.example.pinpoint.ui.components.CreateGroupDialog
import com.example.pinpoint.ui.components.GroupDetailsDialog
import com.example.pinpoint.ui.components.PermissionDisclosureDialog
import com.example.pinpoint.ui.components.ProfilePicturePrivacyDialog
import com.example.pinpoint.ui.screens.ChatScreen
import com.example.pinpoint.ui.screens.ConversationListScreen
import com.example.pinpoint.ui.screens.LegalCenterScreen
import com.example.pinpoint.viewmodel.ChatViewModel
import com.example.ui.theme.PinPointTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFormat(PixelFormat.RGBA_8888)
        enableEdgeToEdge()
        setContent {
            PinPointTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Open directly into PinPoint Messenger (Sign in with Google removed)
                    PinPointApp()
                }
            }
        }
    }
}

@Composable
fun PinPointApp(viewModel: ChatViewModel = viewModel()) {
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val showAddContact by viewModel.showAddContactDialog.collectAsState()
    val showCreateGroup by viewModel.showCreateGroupDialog.collectAsState()
    val showGroupDetails by viewModel.showGroupDetails.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val showLegalCenter by viewModel.showLegalCenter.collectAsState()
    val showPicturePrivacyDialog by viewModel.showPicturePrivacyDialog.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val permissionDisclosure by viewModel.permissionDisclosure.collectAsState()

    val context = LocalContext.current

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully */ }

    LaunchedEffect(Unit) {
        // Create high-priority notification channel
        NotificationHelper.createNotificationChannel(context)

        // Request POST_NOTIFICATIONS runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Handle tap on notification
        val activity = context as? ComponentActivity
        val extraConvId = activity?.intent?.getStringExtra(NotificationHelper.EXTRA_CONVERSATION_ID)
        if (extraConvId != null) {
            viewModel.openConversation(extraConvId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val showContactDetails by viewModel.showContactDetails.collectAsState()

        AnimatedContent(
            targetState = activeConversationId,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_nav"
        ) { convId ->
            if (convId == null) {
                ConversationListScreen(viewModel = viewModel)
            } else {
                ChatScreen(
                    conversationId = convId,
                    viewModel = viewModel
                )
            }
        }

        // Active Voice / Video / Enterprise Conference Call Overlay
        CallOverlay(
            session = activeCall,
            onEndCall = { viewModel.endCall() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleSpeaker = { viewModel.toggleSpeaker() },
            onToggleCamera = { viewModel.toggleCamera() },
            onToggleScreenShare = { viewModel.toggleScreenShare() }
        )

        // Legal & Privacy Center Screen
        if (showLegalCenter) {
            LegalCenterScreen(
                onDismiss = { viewModel.setShowLegalCenter(false) },
                onDeleteAccount = { viewModel.deleteAccountAndScrubAllData() }
            )
        }

        // Prominent Disclosure & Explicit Consent Dialog (Google Play Compliance)
        permissionDisclosure?.let { permType ->
            PermissionDisclosureDialog(
                permissionType = permType,
                onConsentGranted = {
                    viewModel.dismissPermissionDisclosure()
                },
                onDismiss = { viewModel.dismissPermissionDisclosure() }
            )
        }

        // Profile Picture & Privacy Options Dialog
        if (showPicturePrivacyDialog) {
            ProfilePicturePrivacyDialog(
                userProfile = userProfile,
                contacts = contacts,
                groups = groups,
                onUpdatePicture = { viewModel.updateProfilePicture(it) },
                onUpdateVisibility = { vis, persons, grps ->
                    viewModel.updatePictureVisibility(vis, persons, grps)
                },
                onTogglePersonAccess = { viewModel.togglePersonPictureAccess(it) },
                onToggleGroupAccess = { viewModel.toggleGroupPictureAccess(it) },
                onDismiss = { viewModel.setShowPicturePrivacyDialog(false) }
            )
        }

        // Dialogs
        if (showAddContact) {
            AddContactDialog(
                onDismiss = { viewModel.setShowAddContactDialog(false) },
                onSendInvite = { pin, msg ->
                    viewModel.sendContactInvitation(pin, msg)
                }
            )
        }

        if (showCreateGroup) {
            CreateGroupDialog(
                availableContacts = contacts,
                onDismiss = { viewModel.setShowCreateGroupDialog(false) },
                onCreateGroup = { name, desc, pins ->
                    viewModel.createGroup(name, desc, pins)
                }
            )
        }

        showGroupDetails?.let { grp ->
            GroupDetailsDialog(
                group = grp,
                myPin = userProfile.pin,
                onDismiss = { viewModel.openGroupDetails(null) },
                onUpdateRole = { memberPin, role ->
                    viewModel.updateMemberRole(grp.id, memberPin, role)
                },
                onRemoveMember = { memberPin ->
                    viewModel.removeMemberFromGroup(grp.id, memberPin)
                }
            )
        }

        showContactDetails?.let { contact ->
            com.example.pinpoint.ui.components.ContactDetailsDialog(
                contact = contact,
                onStartCall = {
                    viewModel.startCall(contact.pin, contact.displayName, false)
                },
                onStartVideo = {
                    viewModel.startCall(contact.pin, contact.displayName, true)
                },
                onDismiss = { viewModel.openContactDetails(null) }
            )
        }
    }
}
