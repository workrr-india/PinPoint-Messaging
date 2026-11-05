package com.example.pinpoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BbmTealPrimary
import com.example.ui.theme.StatusReadGreen

/**
 * Google Play Policy Compliant Prominent Disclosure & Explicit Consent Dialog:
 * - Appears as a standalone disclosure before initiating sensitive runtime permissions (Camera / Microphone)
 * - Details specific usage (E2EE calling & voice notes only)
 * - Requires affirmative user action ("I Consent & Agree")
 * - Features "Powered by Workrr.in" footer hyperlink
 */
@Composable
fun PermissionDisclosureDialog(
    permissionType: String, // "CAMERA", "MICROPHONE", or "CALLS"
    onConsentGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BbmTealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (permissionType == "MICROPHONE") Icons.Default.Mic else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Permission Disclosure",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE0F2F1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = BbmTealPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Prominent User Disclosure & Consent",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BbmTealPrimary
                        )
                    }
                }

                Text(
                    text = when (permissionType) {
                        "MICROPHONE" -> "PinPoint Messenger requires Microphone access strictly to record voice notes and transmit your audio during end-to-end encrypted voice/video calls."
                        "CAMERA" -> "PinPoint Messenger requires Camera access strictly to capture photo attachments and stream video during end-to-end encrypted video calls."
                        else -> "PinPoint Messenger requires Camera and Microphone access strictly to enable peer-to-peer end-to-end encrypted voice and video calling."
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF37474F)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = StatusReadGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zero Background Access & Zero Advertising Tracking",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusReadGreen
                    )
                }

                Text(
                    text = "Your audio and camera feeds are never accessed in the background and are never collected or shared with third-party advertising networks.",
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                )

                HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))

                // Footer Hyperlink
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Powered by ",
                        fontSize = 11.sp,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = "Workrr.in",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BbmTealPrimary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { uriHandler.openUri("https://workrr.in") }
                            .testTag("hyperlink_disclosure_workrr")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = BbmTealPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConsentGranted,
                colors = ButtonDefaults.buttonColors(containerColor = BbmTealPrimary),
                modifier = Modifier.testTag("button_consent_agree")
            ) {
                Text("I Consent & Agree")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("button_consent_decline")
            ) {
                Text("Not Now", color = Color(0xFF757575))
            }
        },
        containerColor = Color.White
    )
}
