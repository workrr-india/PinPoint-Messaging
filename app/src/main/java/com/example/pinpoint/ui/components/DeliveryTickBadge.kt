package com.example.pinpoint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pinpoint.model.MessageStatus
import com.example.ui.theme.StatusDeliveredBlue
import com.example.ui.theme.StatusFailedRed
import com.example.ui.theme.StatusReadGreen
import com.example.ui.theme.StatusSentGrey

/**
 * Iconic BBM-style delivery and read receipts:
 * - 'D' badge for DELIVERED
 * - 'R' badge for READ
 * - Checkmark for SENT
 * - Clock for SENDING
 */
@Composable
fun DeliveryTickBadge(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    when (status) {
        MessageStatus.SENDING -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                tint = StatusSentGrey,
                modifier = modifier
                    .size(14.dp)
                    .testTag("status_sending")
            )
        }
        MessageStatus.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = StatusSentGrey,
                modifier = modifier
                    .size(14.dp)
                    .testTag("status_sent")
            )
        }
        MessageStatus.DELIVERED -> {
            Box(
                modifier = modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(StatusDeliveredBlue)
                    .testTag("status_delivered"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "D",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 10.sp
                )
            }
        }
        MessageStatus.READ -> {
            Box(
                modifier = modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(StatusReadGreen)
                    .testTag("status_read"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 10.sp
                )
            }
        }
        MessageStatus.FAILED -> {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Failed",
                tint = StatusFailedRed,
                modifier = modifier
                    .size(14.dp)
                    .testTag("status_failed")
            )
        }
    }
}
