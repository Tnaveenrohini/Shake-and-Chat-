package com.flikk.shakechat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val bg = Color(0xFF0E0B14)
    val card = Color(0xFF1A1622)
    val card2 = Color(0xFF241F2E)
    val line = Color(0xFF332C40)
    val pink = Color(0xFFFF3D7F)
    val orange = Color(0xFFFFB23D)
    val text = Color(0xFFF4F1F8)
    val dim = Color(0xFF9A93A8)
    val green = Color(0xFF3DDC97)
    val red = Color(0xFFFF5A5A)
    val grad = Brush.linearGradient(listOf(pink, orange))
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.pink,
            secondary = C.orange,
            background = C.bg,
            surface = C.card,
            onPrimary = Color.White,
            onBackground = C.text,
            onSurface = C.text,
            surfaceVariant = C.card2,
            outline = C.line,
        ),
        content = content,
    )
}

@Composable
fun GradButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(C.grad)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(C.grad)
                else Modifier.background(C.card2).border(1.dp, C.line, shape)
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 8.dp)),
    ) {
        Text(
            text,
            color = if (selected) Color.White else C.text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = C.pink,
    unfocusedBorderColor = C.line,
    focusedContainerColor = C.card,
    unfocusedContainerColor = C.card,
    cursorColor = C.pink,
    focusedLabelColor = C.pink,
    unfocusedLabelColor = C.dim,
)
