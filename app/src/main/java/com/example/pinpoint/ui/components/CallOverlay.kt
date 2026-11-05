package com.example.pinpoint.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pinpoint.model.CallSession
import com.example.pinpoint.model.CallStatus
import com.example.ui.theme.BbmTealPrimary
import com.example.ui.theme.StatusFailedRed

/**
 * Screen 4 Authentic BBM Calling UI:
 * - Top deep teal header banner with back arrow, contact name, "Calling" status, speakerphone toggle
 * - Center large portrait / photo of the contact
 * - Bottom floating pill with Teal Mute button, Red End Call button, and Teal Camera switch button
 */
@Composable
fun CallOverlay(
    session: CallSession?,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleScreenShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = session != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (session == null) return@AnimatedVisibility

        val durationFormatted = String.format(
            "%02d:%02d",
            session.durationSeconds / 60,
            session.durationSeconds % 60
        )

        val peerName = if (session.isConference) session.conferenceRoomName else session.peerName.ifBlank { "Gideon Smith" }
        val statusText = if (session.status == CallStatus.OUTGOING_RINGING || session.status == CallStatus.INCOMING_RINGING) "Calling" else durationFormatted

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White)
                .testTag("call_screen_overlay")
        ) {
            // Screen 4: Top Deep Teal Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(BbmTealPrimary)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    // Top Bar Actions: Back Arrow (Left), Speakerphone Toggle (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEndCall,
                            modifier = Modifier.testTag("action_call_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier.testTag("action_call_speaker")
                        ) {
                            Icon(
                                imageVector = if (session.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Speakerphone",
                                tint = Color.White
                            )
                        }
                    }

                    // Contact Name & Status (Screen 4: "Gideon Smith" / "Calling")
                    Column(modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)) {
                        Text(
                            text = peerName,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusText,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Screen 4: Center Portrait Section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF7F9FA)),
                contentAlignment = Alignment.Center
            ) {
                if (session.isConference) {
                    // Multi-party Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(session.participants) { p ->
                            Box(
                                modifier = Modifier
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFECEFF1))
                                    .border(1.5.dp, if (p.isSpeaking) BbmTealPrimary else Color(0xFFCFD8DC), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(BbmTealPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(p.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                                }
                            }
                        }
                    }
                } else {
                    // Screen 4: Portrait Photo of Contact (Gideon Smith)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFECEFF1))
                                .border(4.dp, BbmTealPrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(CircleShape)
                                    .background(BbmTealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = peerName,
                                        tint = Color.White,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFECEFF1)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = BbmTealPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Protected Call • FIPS 140-2 Encrypted",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BbmTealPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Screen 4: Bottom Floating Controls Dock
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF263238))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Teal Mute Button
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (session.isMuted) Color(0xFF78909C) else BbmTealPrimary)
                            .testTag("action_call_mute")
                    ) {
                        Icon(
                            imageVector = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Large Red End Call Button
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(StatusFailedRed)
                            .testTag("action_call_end")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Teal Video / Camera Flip Button
                    IconButton(
                        onClick = onToggleCamera,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (session.isCameraOn) BbmTealPrimary else Color(0xFF78909C))
                            .testTag("action_call_camera")
                    ) {
                        Icon(
                            imageVector = if (session.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Camera Switch",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
