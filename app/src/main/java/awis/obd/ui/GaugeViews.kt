package awis.obd.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular radial dial gauge with sweep gradient, animated needle and ticks.
 */
@Composable
fun CircularDialGauge(
    value: Float,
    maxValue: Float,
    title: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    redlineStartValue: Float? = null,
    subText: String? = null
) {
    val progress = (value / maxValue).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "dial_progress")

    // Arc from 135 deg to 405 deg (270 degree total sweep)
    val startAngle = 135f
    val sweepAngle = 270f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                val strokeWidth = 10.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                // Background track arc
                drawArc(
                    color = Color(0xFF1E2228),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active progress arc
                val activeSweep = sweepAngle * animatedProgress
                if (activeSweep > 0) {
                    val brush = if (redlineStartValue != null) {
                        val redlineRatio = redlineStartValue / maxValue
                        Brush.sweepGradient(
                            0.375f to accentColor,
                            (0.375f + (sweepAngle / 360f) * redlineRatio) to accentColor,
                            (0.375f + (sweepAngle / 360f)) to Color(0xFFFF5252),
                            center = center
                        )
                    } else {
                        Brush.sweepGradient(
                            listOf(accentColor.copy(alpha = 0.6f), accentColor),
                            center = center
                        )
                    }

                    drawArc(
                        brush = brush,
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Dial needle / indicator
                val currentAngle = (startAngle + (sweepAngle * animatedProgress)) * (Math.PI / 180.0)
                val radius = (size.width - strokeWidth) / 2
                val needleLength = radius * 0.75f
                val needleEnd = Offset(
                    x = (center.x + needleLength * cos(currentAngle)).toFloat(),
                    y = (center.y + needleLength * sin(currentAngle)).toFloat()
                )

                drawLine(
                    color = Color.White,
                    start = center,
                    end = needleEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center hub
                drawCircle(
                    color = accentColor,
                    radius = 5.dp.toPx(),
                    center = center
                )
            }

            // Numeric value overlay in center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 22.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.0f", value),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }

        if (subText != null) {
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Modern implementation of the 2012 AccelGaugeView:
 * Reads device linear acceleration sensors in real-time, displays G-Force with dynamic Soft to Hard indicators.
 */
@Composable
fun AccelGForceGauge(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var gForceX by remember { mutableFloatStateOf(0f) }
    var gForceY by remember { mutableFloatStateOf(0f) }
    var totalGForce by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    val ax = event.values[0] / SensorManager.GRAVITY_EARTH
                    val ay = event.values[1] / SensorManager.GRAVITY_EARTH
                    // Simple low-pass smoothing
                    gForceX = gForceX * 0.7f + ax * 0.3f
                    gForceY = gForceY * 0.7f + ay * 0.3f
                    val total = kotlin.math.sqrt(gForceX * gForceX + gForceY * gForceY)
                    totalGForce = totalGForce * 0.8f + total * 0.2f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager?.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val animatedGForce by animateFloatAsState(targetValue = totalGForce, label = "gforce_anim")
    val gRating = when {
        animatedGForce < 0.25f -> "Gentle / Soft"
        animatedGForce < 0.60f -> "Moderate"
        animatedGForce < 0.90f -> "Firm / Hard"
        else -> "Aggressive / Extreme"
    }

    val ratingColor = when {
        animatedGForce < 0.25f -> Color(0xFF00E676)
        animatedGForce < 0.60f -> Color(0xFF00E5FF)
        animatedGForce < 0.90f -> Color(0xFFFF9100)
        else -> Color(0xFFFF5252)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "G-FORCE & ACCELERATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = gRating,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ratingColor
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.2f", animatedGForce),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = " G",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ratingColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lateral / Longitudinal Crosshair Bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Target Crosshair Canvas
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14171C)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    val r = size.width / 2
                    // Outer ring
                    drawCircle(color = Color(0xFF2B303A), style = Stroke(width = 1.dp.toPx()))
                    // Inner ring
                    drawCircle(color = Color(0xFF2B303A), radius = r / 2, style = Stroke(width = 1.dp.toPx()))
                    // Cross lines
                    drawLine(Color(0xFF2B303A), Offset(0f, r), Offset(size.width, r), 1.dp.toPx())
                    drawLine(Color(0xFF2B303A), Offset(r, 0f), Offset(r, size.height), 1.dp.toPx())

                    // Clamped bubble offset
                    val bx = (center.x - (gForceX * (r * 0.7f))).coerceIn(4f, size.width - 4f)
                    val by = (center.y + (gForceY * (r * 0.7f))).coerceIn(4f, size.height - 4f)
                    drawCircle(
                        color = ratingColor,
                        radius = 4.dp.toPx(),
                        center = Offset(bx, by)
                    )
                }
            }

            // Horizontal Intensity Bar (replacing AccelGaugeView Soft/Hard bar)
            Column(modifier = Modifier.weight(1f)) {
                val clampedProgress = (animatedGForce / 1.5f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF14171C))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(clampedProgress)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E676),
                                        Color(0xFF00E5FF),
                                        Color(0xFFFF9100),
                                        Color(0xFFFF5252)
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Soft (0G)", fontSize = 10.sp, color = Color(0xFF00E676))
                    Text("Moderate (0.5G)", fontSize = 10.sp, color = Color(0xFF00E5FF))
                    Text("Hard (1.0G+)", fontSize = 10.sp, color = Color(0xFFFF5252))
                }
            }
        }
    }
}

@Composable
fun CoolantGauge(
    tempValue: Int,
    tempUnit: String,
    modifier: Modifier = Modifier,
    minTemp: Int = if (tempUnit == "°F" || tempUnit == "F") 95 else 35,
    maxTemp: Int = if (tempUnit == "°F" || tempUnit == "F") 280 else 138
) {
    val progress = ((tempValue - minTemp).toFloat() / (maxTemp - minTemp).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "gauge_progress")

    val tempColor = when {
        progress < 0.25f -> Color(0xFF29B6F6) // Cold blue
        progress < 0.75f -> Color(0xFF66BB6A) // Normal green
        progress < 0.88f -> Color(0xFFFFA726) // Warm orange
        else -> Color(0xFFEF5350) // Hot red
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ENGINE COOLANT TEMPERATURE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (tempValue > 0) "$tempValue $tempUnit" else "--",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tempColor
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF1E2228))
        ) {
            // Gradient track
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF00E676),
                                Color(0xFFFF9100),
                                Color(0xFFFF5252)
                            )
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("C ($minTemp$tempUnit)", fontSize = 11.sp, color = Color(0xFF00E5FF))
            Text("Normal", fontSize = 11.sp, color = Color(0xFF00E676))
            Text("H ($maxTemp$tempUnit)", fontSize = 11.sp, color = Color(0xFFFF5252))
        }
    }
}
