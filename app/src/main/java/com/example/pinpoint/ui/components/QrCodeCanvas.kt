package com.example.pinpoint.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pinpoint.crypto.CryptoManager

@Composable
fun QrCodeCanvas(
    payload: String,
    sizeDp: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val matrix = remember(payload) {
        CryptoManager.generateQrMatrix(payload)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp)
            .testTag("qr_code_container"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp - 28.dp)) {
            val matrixSize = matrix.size
            val moduleWidth = size.width / matrixSize
            val moduleHeight = size.height / matrixSize

            for (y in 0 until matrixSize) {
                for (x in 0 until matrixSize) {
                    if (matrix[y][x]) {
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(x * moduleWidth, y * moduleHeight),
                            size = Size(moduleWidth * 0.95f, moduleHeight * 0.95f),
                            cornerRadius = CornerRadius(moduleWidth * 0.15f, moduleHeight * 0.15f)
                        )
                    }
                }
            }
        }
    }
}
