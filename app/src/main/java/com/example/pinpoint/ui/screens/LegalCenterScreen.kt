package com.example.pinpoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BbmTealPrimary
import com.example.ui.theme.PinCyanAccent
import com.example.ui.theme.StatusFailedRed
import com.example.ui.theme.StatusReadGreen

enum class LegalTab {
    PRIVACY_POLICY,
    TERMS_OF_SERVICE,
    DATA_DELETION,
    PLAY_DATA_SAFETY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalCenterScreen(
    onDismiss: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LegalTab.PRIVACY_POLICY) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BbmTealPrimary)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Legal & Privacy Center",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("action_legal_back")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BbmTealPrimary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )

                // Scrollable or fixed Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = BbmTealPrimary,
                    contentColor = Color.White,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        if (selectedTab.ordinal in tabPositions.indices) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                height = 3.dp,
                                color = Color.White
                            )
                        }
                    },
                    divider = {}
                ) {
                    LegalTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = when (tab) {
                                        LegalTab.PRIVACY_POLICY -> "Privacy Policy"
                                        LegalTab.TERMS_OF_SERVICE -> "Terms of Service"
                                        LegalTab.DATA_DELETION -> "Data Deletion"
                                        LegalTab.PLAY_DATA_SAFETY -> "Data Safety"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier.testTag("legal_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Mandatory "Powered by Workrr.in" Hyperlink Footer
            Surface(
                color = Color(0xFFF7F9FA),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Powered by ",
                        fontSize = 13.sp,
                        color = Color(0xFF616161)
                    )
                    Text(
                        text = "Workrr.in",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BbmTealPrimary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                try {
                                    uriHandler.openUri("https://workrr.in")
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Opening workrr.in", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("hyperlink_workrr_footer")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "External link",
                        tint = BbmTealPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                LegalTab.PRIVACY_POLICY -> PrivacyPolicySection(uriHandler)
                LegalTab.TERMS_OF_SERVICE -> TermsOfServiceSection(uriHandler)
                LegalTab.DATA_DELETION -> DataDeletionSection(
                    uriHandler = uriHandler,
                    onTriggerDelete = { showDeleteConfirmDialog = true }
                )
                LegalTab.PLAY_DATA_SAFETY -> PlayDataSafetySection()
            }
        }
    }

    // In-App Permanent Account & Data Deletion Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusFailedRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account & Scrub All Data?")
                }
            },
            text = {
                Text(
                    text = "This action is irreversible. It will permanently scrub and purge your unique PIN, cryptographic keypair, message database, contacts, and all device telemetry. You will be reset to a brand-new blank identity.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF37474F)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                        Toast.makeText(context, "Account & all cryptographic data permanently scrubbed.", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusFailedRed),
                    modifier = Modifier.testTag("button_confirm_scrub_account")
                ) {
                    Text("Permanently Delete & Scrub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PrivacyPolicySection(uriHandler: androidx.compose.ui.platform.UriHandler) {
    LegalCard(
        title = "1. Developer & Organization Information",
        icon = Icons.Default.Business
    ) {
        Text("• Organization: Workrr.in Technologies", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Application: PinPoint Messenger (PPM)", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Official Website: https://workrr.in", fontSize = 13.sp, color = BbmTealPrimary)
        Text("• Privacy Officer Contact: privacy@workrr.in", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• General Inquiries: support@workrr.in", fontSize = 13.sp, color = Color(0xFF424242))
    }

    LegalCard(
        title = "2. Types of Data Collected",
        icon = Icons.Default.FactCheck
    ) {
        Text("PinPoint Messenger is built on a Zero-Knowledge, Privacy-First Architecture:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Spacer(modifier = Modifier.height(4.dp))
        Text("• Unique PIN Identifiers: A randomly generated 8-character alphanumeric string decoupled from phone numbers, email addresses, and SIM cards.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• No Address Book Scraping: The app NEVER harvests, uploads, or syncs your device's address book or contact numbers.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Communications: Messages, voice notes, and file attachments are encrypted client-side using AES-256 and RSA/ECC key cryptography. We cannot read your plaintext content.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Camera & Microphone: Accessed solely when you initiate voice/video calls, capture photos, or record voice memos. Never accessed in the background.", fontSize = 13.sp, color = Color(0xFF424242))
    }

    LegalCard(
        title = "3. Purpose & Data Usage",
        icon = Icons.Default.Shield
    ) {
        Text("We process minimal operational data strictly for core app functionality:", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Delivering end-to-end encrypted messages and attachments.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Facilitating peer-to-peer audio and video calls.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Cryptographic identity verification and public-key exchange.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• We do NOT track users across third-party websites or services.", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusReadGreen)
    }

    LegalCard(
        title = "4. Third-Party Sharing & Advertising",
        icon = Icons.Default.ShareLocation
    ) {
        Text("• Zero Commercial Data Sales: We do NOT sell, rent, monetize, or trade user data or communications under any circumstances.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Zero Third-Party Advertising Networks: There are no ad trackers, profiling SDKs, or targeted ad networks in PinPoint Messenger.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Cloud Infrastructure: Backend signaling and encrypted database storage are secured with Google Cloud / Firebase zero-trust architecture.", fontSize = 13.sp, color = Color(0xFF424242))
    }

    LegalCard(
        title = "5. Data Retention & User Rights",
        icon = Icons.Default.DeleteForever
    ) {
        Text("• Ephemeral Messages: Self-destructing timed messages are permanently deleted after the countdown timer elapses.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Message Retraction: Senders can retract messages at any time, scrubbing them from both devices.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Full Account Erasure: You have the right to delete your account and all associated records at any moment in the app or via our online deletion portal.", fontSize = 13.sp, color = Color(0xFF424242))
    }

    Button(
        onClick = { uriHandler.openUri("https://workrr.in/privacy") },
        colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary),
        modifier = Modifier.fillMaxWidth().testTag("button_open_web_privacy")
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("View Complete Privacy Policy on Workrr.in")
    }
}

@Composable
private fun TermsOfServiceSection(uriHandler: androidx.compose.ui.platform.UriHandler) {
    LegalCard(
        title = "1. Acceptance of Terms",
        icon = Icons.Default.Gavel
    ) {
        Text(
            text = "By accessing or using PinPoint Messenger (developed by Workrr.in), you agree to be bound by these Terms of Service. If you do not agree to these terms, you must not use the application.",
            fontSize = 13.sp,
            color = Color(0xFF424242)
        )
    }

    LegalCard(
        title = "2. User-Generated Content (UGC) & Community Safety",
        icon = Icons.Default.Security
    ) {
        Text("PinPoint Messenger provides secure peer-to-peer and group communications. To protect all users, we strictly enforce zero tolerance for prohibited content:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Spacer(modifier = Modifier.height(4.dp))
        Text("• Harassment, hate speech, defamation, threats, or intimidation.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Child Sexual Abuse Material (CSAM) or any exploitation of minors.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Promotion of terrorism, violence, or illicit acts.", fontSize = 13.sp, color = Color(0xFF424242))
        Text("• Distribution of malware, phishing links, or unauthorized spam.", fontSize = 13.sp, color = Color(0xFF424242))
        Spacer(modifier = Modifier.height(4.dp))
        Text("Violations will result in immediate termination of account access and cryptographic identity revocation.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = StatusFailedRed)
    }

    LegalCard(
        title = "3. Cryptographic Key Management & Disclaimer",
        icon = Icons.Default.Key
    ) {
        Text(
            text = "PinPoint Messenger implements device-generated cryptographic keys. You are responsible for safeguarding your device. Workrr.in cannot recover communications if local cryptographic keys are deleted without a backup.",
            fontSize = 13.sp,
            color = Color(0xFF424242)
        )
    }

    LegalCard(
        title = "4. Limitation of Liability",
        icon = Icons.Default.Balance
    ) {
        Text(
            text = "Workrr.in provides the service 'as is' without warranties of any kind. Workrr.in is not liable for indirect, incidental, or consequential damages resulting from service interruptions or network delays.",
            fontSize = 13.sp,
            color = Color(0xFF424242)
        )
    }

    Button(
        onClick = { uriHandler.openUri("https://workrr.in/terms") },
        colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary),
        modifier = Modifier.fillMaxWidth().testTag("button_open_web_terms")
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("View Complete Terms of Service on Workrr.in")
    }
}

@Composable
private fun DataDeletionSection(
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onTriggerDelete: () -> Unit
) {
    LegalCard(
        title = "Account & Associated Data Deletion Policy",
        icon = Icons.Default.DeleteOutline
    ) {
        Text(
            text = "In compliance with Google Play User Data policies, users can request the permanent deletion of their account and all associated data both from within the app and via our public web deletion portal.",
            fontSize = 13.sp,
            color = Color(0xFF424242)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFFE65100))
                Spacer(modifier = Modifier.width(8.dp))
                Text("In-App Account & Data Deletion", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Instantly scrub your active PIN identity, local cryptographic keypairs, message history, contacts list, and all cached media from this device.",
                fontSize = 12.sp,
                color = Color(0xFF4E342E)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onTriggerDelete,
                colors = ButtonDefaults.buttonColors(containerColor = StatusFailedRed),
                modifier = Modifier.fillMaxWidth().testTag("button_trigger_inapp_delete")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account & Scrub All Data Now")
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF80CBC4))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF00695C))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Public Web Deletion Request Portal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF00695C))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Don't have access to the app? Google Play requires a public web request URL where you can submit a deletion request without needing to reinstall the app:",
                fontSize = 12.sp,
                color = Color(0xFF004D40)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFB2DFDB), RoundedCornerShape(6.dp))
            ) {
                Text(
                    text = "https://workrr.in/privacy/data-deletion",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BbmTealPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { uriHandler.openUri("https://workrr.in/privacy/data-deletion") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00695C)),
                modifier = Modifier.fillMaxWidth().testTag("button_open_web_deletion")
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Web Data Deletion Portal")
            }
        }
    }
}

@Composable
private fun PlayDataSafetySection() {
    LegalCard(
        title = "Google Play Data Safety Form Declarations",
        icon = Icons.Default.VerifiedUser
    ) {
        Text("Summary of official Google Play Console Data Safety disclosures:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusReadGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Data Encryption in Transit: All data transferred using TLS 1.3 & FIPS-validated AES-256-GCM.", fontSize = 12.sp, color = Color(0xFF424242))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusReadGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Data Deletion Available: Users can request data deletion in-app and on the web.", fontSize = 12.sp, color = Color(0xFF424242))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusReadGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("No Third-Party Data Sharing: No data shared with external advertising or tracking brokers.", fontSize = 12.sp, color = Color(0xFF424242))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusReadGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Target Audience & Families Policy: Compliant with Google Play Target Audience standards.", fontSize = 12.sp, color = Color(0xFF424242))
        }
    }
}

@Composable
private fun LegalCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = BbmTealPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF212121))
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
