package com.example.pinpoint.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PinCyanAccent

@Composable
fun AudioWaveformBar(
    durationText: String = "0:14",
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "audio_anim")
    val animatedHeight by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_height"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("audio_waveform_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PinCyanAccent)
                .clickable { isPlaying = !isPlaying }
                .testTag("audio_play_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }

        // Simulated waveform bars
        Row(
            modifier = Modifier.height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val barHeights = listOf(0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 1.0f, 0.6f, 0.4f, 0.7f, 0.3f, 0.8f, 0.5f)
            barHeights.forEachIndexed { index, baseHeight ->
                val scale = if (isPlaying) {
                    (baseHeight * animatedHeight + (index % 3) * 0.15f).coerceIn(0.2f, 1.0f)
                } else {
                    baseHeight
                }
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight(scale)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isPlaying) PinCyanAccent else Color(0xFF94A3B8))
                )
            }
        }

        Text(
            text = durationText,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
        )
    }
}
