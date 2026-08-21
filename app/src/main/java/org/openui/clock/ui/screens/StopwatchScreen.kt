package org.openui.clock.ui.screens

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openui.clock.ui.StopwatchState
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StopwatchScreen(
    state: StopwatchState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pager for Clock Face
        Box(
            modifier = Modifier.weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                if (page == 0) {
                    AnalogFace(state)
                } else {
                    DigitalFace(state)
                }
            }
        }

        // Pager Indicator
        Row(
            modifier = Modifier.padding(top = 28.dp, bottom = 36.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) { index ->
                val color = if (pagerState.currentPage == index) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
        }

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button (Interval/Reset)
            StopwatchButton(
                text = if (state.isRunning) stringResource(org.openui.clock.R.string.stopwatch_lap) else stringResource(org.openui.clock.R.string.stopwatch_reset),
                backgroundColor = Color(0xFF2A2B36),
                textColor = Color.White,
                onClick = { if (state.isRunning) onLap() else onReset() },
                enabled = state.isRunning || state.elapsedMillis > 0
            )

            // Right Button (Start/Stop)
            StopwatchButton(
                text = if (state.isRunning) stringResource(org.openui.clock.R.string.stopwatch_pause) else stringResource(org.openui.clock.R.string.stopwatch_start),
                backgroundColor = if (state.isRunning) Color(0xFF3D1B1B) else Color(0xFF3B3B6D),
                textColor = if (state.isRunning) Color(0xFFFFB4B4) else Color(0xFFD4D4FF),
                onClick = { if (state.isRunning) onPause() else onStart() }
            )
        }

        // Laps List
        if (state.laps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                itemsIndexed(state.laps, key = { index, _ -> index }) { index, lapMillis ->
                    val lapMin = (lapMillis / 60000) % 60
                    val lapSec = (lapMillis / 1000) % 60
                    val lapMs = (lapMillis % 1000) / 10
                    val lapNum = state.laps.size - index

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(org.openui.clock.R.string.stopwatch_lap_prefix, lapNum),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = String.format("%02d:%02d,%02d", lapMin, lapSec, lapMs),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AnalogFace(state: StopwatchState) {
    val minutes = (state.elapsedMillis / 60000) % 60
    val seconds = (state.elapsedMillis / 1000) % 60
    val millis = (state.elapsedMillis % 1000) / 10

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Background
            drawCircle(
                color = Color(0xFF1E1F28),
                radius = radius,
                center = center
            )

            // Ticks
            for (i in 0 until 60) {
                val angle = (i * 6f - 90f) * (Math.PI / 180f).toFloat()
                val isMajorTick = i % 5 == 0
                val tickLength = if (isMajorTick) 16.dp.toPx() else 8.dp.toPx()
                val tickThickness = if (isMajorTick) 2.dp.toPx() else 1.dp.toPx()
                val tickColor = if (isMajorTick) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f)
                
                val startX = center.x + (radius - tickLength - 8.dp.toPx()) * cos(angle)
                val startY = center.y + (radius - tickLength - 8.dp.toPx()) * sin(angle)
                val endX = center.x + (radius - 8.dp.toPx()) * cos(angle)
                val endY = center.y + (radius - 8.dp.toPx()) * sin(angle)

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickThickness,
                    cap = StrokeCap.Round
                )
            }

            // Sweep hand
            val totalSecondsFloat = (state.elapsedMillis % 60000) / 1000f
            val handAngle = ((totalSecondsFloat / 60f) * 360f - 90f) * (Math.PI / 180f).toFloat()
            
            val handEndX = center.x + (radius - 12.dp.toPx()) * cos(handAngle)
            val handEndY = center.y + (radius - 12.dp.toPx()) * sin(handAngle)

            // Outer hand tail
            val tailEndX = center.x - (20.dp.toPx()) * cos(handAngle)
            val tailEndY = center.y - (20.dp.toPx()) * sin(handAngle)

            drawLine(
                color = Color(0xFF7C3AED),
                start = Offset(tailEndX, tailEndY),
                end = Offset(handEndX, handEndY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            drawCircle(
                color = Color(0xFF7C3AED),
                radius = 5.dp.toPx(),
                center = center
            )
            
            // Inner black dot
            drawCircle(
                color = Color(0xFF1E1F28),
                radius = 2.dp.toPx(),
                center = center
            )
        }

        // Text in the middle
        Text(
            text = String.format("%02d:%02d,%02d", minutes, seconds, millis),
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 44.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White
        )
    }
}

@Composable
fun DigitalFace(state: StopwatchState) {
    val minutes = (state.elapsedMillis / 60000) % 60
    val seconds = (state.elapsedMillis / 1000) % 60
    val millis = (state.elapsedMillis % 1000) / 10

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = String.format("%02d:%02d,%02d", minutes, seconds, millis),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White
        )
    }
}

@Composable
fun StopwatchButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.3f),
            contentColor = textColor,
            disabledContentColor = textColor.copy(alpha = 0.3f)
        ),
        modifier = Modifier.size(90.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text, 
            fontSize = 16.sp, 
            fontWeight = FontWeight.Medium
        )
    }
}
