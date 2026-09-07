package awis.obd.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Coolant Temp",
                style = MaterialTheme.typography.labelLarge,
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
