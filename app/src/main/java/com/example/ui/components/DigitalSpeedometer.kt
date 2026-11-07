package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VehicleTelemetry
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DigitalSpeedometer(
    telemetry: VehicleTelemetry,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = telemetry.currentSpeedKmh.coerceIn(0f, 200f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "speed"
    )

    val animatedRpm by animateFloatAsState(
        targetValue = telemetry.engineRpm.toFloat().coerceIn(0f, 6500f),
        animationSpec = spring(dampingRatio = 0.85f),
        label = "rpm"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("digital_speedometer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f) - 24.dp.toPx()
            val startAngle = 140f
            val sweepTotalAngle = 260f

            // 1. Background Arc Track
            drawArc(
                color = Color(0xFF162032),
                startAngle = startAngle,
                sweepAngle = sweepTotalAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Active Speed Glow Arc
            val activeSweep = (animatedSpeed / 200f) * sweepTotalAngle
            if (activeSweep > 0.5f) {
                val speedBrush = Brush.sweepGradient(
                    colors = listOf(NeonCyan, NeonEmerald, NeonAmber, NeonRose),
                    center = center
                )
                drawArc(
                    brush = speedBrush,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Tick Marks (every 20 km/h)
            for (speed in 0..200 step 20) {
                val fraction = speed / 200f
                val angleDeg = startAngle + (fraction * sweepTotalAngle)
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val innerR = radius - 18.dp.toPx()
                val outerR = radius - 8.dp.toPx()
                val isMajor = speed % 40 == 0

                val start = Offset(
                    (center.x + innerR * cos(angleRad)).toFloat(),
                    (center.y + innerR * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + outerR * cos(angleRad)).toFloat(),
                    (center.y + outerR * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = if (isMajor) Color(0xFF94A3B8) else Color(0xFF475569),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. Subtle Inner RPM Arc
            val rpmRadius = radius - 30.dp.toPx()
            val rpmFraction = animatedRpm / 6500f
            val rpmSweep = rpmFraction * sweepTotalAngle
            drawArc(
                color = NeonCyan.copy(alpha = 0.25f),
                startAngle = startAngle,
                sweepAngle = sweepTotalAngle,
                useCenter = false,
                topLeft = Offset(center.x - rpmRadius, center.y - rpmRadius),
                size = Size(rpmRadius * 2, rpmRadius * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            if (rpmSweep > 1f) {
                drawArc(
                    color = if (animatedRpm > 5000f) NeonRose else NeonCyan,
                    startAngle = startAngle,
                    sweepAngle = rpmSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - rpmRadius, center.y - rpmRadius),
                    size = Size(rpmRadius * 2, rpmRadius * 2),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 5. Glowing Needle Indicator
            val needleAngle = startAngle + activeSweep
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val needleLength = radius - 4.dp.toPx()
            val needleEnd = Offset(
                (center.x + needleLength * cos(needleRad)).toFloat(),
                (center.y + needleLength * sin(needleRad)).toFloat()
            )
            val needleStart = Offset(
                (center.x + 20.dp.toPx() * cos(needleRad)).toFloat(),
                (center.y + 20.dp.toPx() * sin(needleRad)).toFloat()
            )

            drawLine(
                color = if (telemetry.isSpeedAlert) NeonRose else NeonCyan,
                start = needleStart,
                end = needleEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center needle hub circle
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 16.dp.toPx(),
                center = center
            )
            drawCircle(
                color = if (telemetry.isSpeedAlert) NeonRose else NeonCyan,
                radius = 7.dp.toPx(),
                center = center
            )
        }

        // Center Content Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Source Badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = telemetry.activeSource.badgeText,
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Numeric Speed
            Text(
                text = String.format(Locale.US, "%.0f", animatedSpeed),
                color = if (telemetry.isSpeedAlert) NeonRose else Color.White,
                fontSize = 58.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 58.sp
            )

            Text(
                text = "KM / H",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Gear & RPM Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(NeonCyan.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = telemetry.activeGear,
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${telemetry.engineRpm} RPM",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
