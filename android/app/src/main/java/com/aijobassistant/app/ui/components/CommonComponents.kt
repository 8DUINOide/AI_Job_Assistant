package com.aijobassistant.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aijobassistant.app.ui.theme.*

/**
 * Reusable glassmorphic card matching the web dashboard's .glass-card CSS class.
 * Translucent background with blur effect and subtle border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundTranslucent
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

/**
 * Primary gradient button matching the web dashboard's button style.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradientColors: List<Color> = listOf(PrimaryBlue, AccentIndigo)
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = SurfaceElevated
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled && !isLoading) {
                        Brush.horizontalGradient(gradientColors)
                    } else {
                        Brush.horizontalGradient(listOf(SurfaceElevated, SurfaceElevated))
                    },
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * Styled text input field with the dark theme styling.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            isError = isError,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = BorderColor,
                errorBorderColor = StatusDanger,
                cursorColor = PrimaryBlue,
                focusedLabelColor = PrimaryBlue,
                unfocusedLabelColor = TextMuted,
                focusedContainerColor = Color(0x0DFFFFFF),
                unfocusedContainerColor = Color(0x0DFFFFFF)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = StatusDanger,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Animated floating background blobs matching the web dashboard's .blob CSS.
 */
@Composable
fun AnimatedBackgroundBlobs() {
    val infiniteTransition = rememberInfiniteTransition(label = "blob")

    val offsetX1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1X"
    )
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1Y"
    )
    val offsetX2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2X"
    )

    // Blob 1 - Purple (top-left)
    Box(
        modifier = Modifier
            .offset(x = (-50 + offsetX1.toInt()).dp, y = (-50 + offsetY1.toInt()).dp)
            .size(300.dp)
            .clip(CircleShape)
            .blur(80.dp)
            .background(BlobPrimary)
    )

    // Blob 2 - Blue (bottom-right)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.BottomEnd)
            .offset(x = (50 + offsetX2.toInt()).dp, y = 50.dp)
            .size(350.dp)
            .clip(CircleShape)
            .blur(80.dp)
            .background(BlobAccent)
    )
}

/**
 * Pulsing status dot matching the web dashboard's .status-dot CSS.
 */
@Composable
fun StatusDot(
    color: Color = StatusSuccess,
    size: Dp = 10.dp,
    animate: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(contentAlignment = Alignment.Center) {
        // Outer glow
        if (animate) {
            Box(
                modifier = Modifier
                    .size(size + 6.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
        }
        // Inner dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Skill/tag chip matching the web dashboard's .tag CSS class.
 */
@Composable
fun SkillTag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = PrimaryBlueContainer,
    textColor: Color = PrimaryBlueLight,
    borderColor: Color = PrimaryBlue.copy(alpha = 0.3f)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(borderColor, borderColor))
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
