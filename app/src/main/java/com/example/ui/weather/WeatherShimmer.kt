package com.example.ui.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SkyPalette

@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "weather_shimmer")
    val offset by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.08f),
        ),
        start = Offset(offset, 0f),
        end = Offset(offset + 360f, 220f),
    )
}

@Composable
private fun ShimmerBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}

/**
 * Animated Bento Grid loading skeleton displayed during initial GPS lookup or city transitions.
 */
@Composable
fun WeatherLoadingSkeleton(
    palette: SkyPalette,
    modifier: Modifier = Modifier,
) {
    val brush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_loading_skeleton"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero card skeleton
        BentoGlassCard(palette = palette, highlighted = true, contentPadding = 20.dp) {
            ShimmerBlock(brush, Modifier.width(180.dp).height(28.dp))
            Spacer(Modifier.height(10.dp))
            ShimmerBlock(brush, Modifier.width(240.dp).height(20.dp), shape = CircleShape)
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBlock(brush, Modifier.width(126.dp).height(64.dp))
                    ShimmerBlock(brush, Modifier.width(150.dp).height(18.dp))
                }
                ShimmerBlock(brush, Modifier.width(72.dp).height(72.dp), shape = CircleShape)
            }
            Spacer(Modifier.height(18.dp))
            ShimmerBlock(brush, Modifier.fillMaxWidth(0.75f).height(24.dp), shape = CircleShape)
        }

        // Hourly strip skeleton
        BentoGlassCard(palette = palette) {
            ShimmerBlock(brush, Modifier.width(170.dp).height(18.dp))
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(4) {
                    ShimmerBlock(
                        brush = brush,
                        modifier = Modifier
                            .weight(1f)
                            .height(106.dp),
                        shape = RoundedCornerShape(18.dp),
                    )
                }
            }
        }

        // 7-day forecast skeleton
        BentoGlassCard(palette = palette) {
            ShimmerBlock(brush, Modifier.width(150.dp).height(18.dp))
            Spacer(Modifier.height(14.dp))
            repeat(4) { idx ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ShimmerBlock(brush, Modifier.width(48.dp).height(18.dp))
                    ShimmerBlock(brush, Modifier.width(24.dp).height(24.dp), shape = CircleShape)
                    ShimmerBlock(brush, Modifier.weight(1f).height(10.dp), shape = CircleShape)
                    ShimmerBlock(brush, Modifier.width(40.dp).height(18.dp))
                }
                if (idx < 3) Spacer(Modifier.height(14.dp))
            }
        }

        // Bento detail grid skeleton (2x2 preview rows)
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(2) {
                    BentoGlassCard(palette = palette, modifier = Modifier.weight(1f)) {
                        ShimmerBlock(brush, Modifier.width(80.dp).height(14.dp))
                        Spacer(Modifier.height(16.dp))
                        ShimmerBlock(brush, Modifier.width(96.dp).height(30.dp))
                        Spacer(Modifier.height(10.dp))
                        ShimmerBlock(brush, Modifier.fillMaxWidth().height(12.dp))
                    }
                }
            }
        }
    }
}
