package org.openui.clock.ui.screens

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.openui.clock.ui.TimerState
import kotlin.math.absoluteValue

@Composable
fun TimerScreen(
    state: TimerState,
    onStart: (durationMillis: Long) -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }

    val hasActiveTimer = state.totalMillis > 0

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!hasActiveTimer) {
                Spacer(modifier = Modifier.height(60.dp))
                
                // Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(org.openui.clock.R.string.timer_hours_label), color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(org.openui.clock.R.string.timer_minutes_label), color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(org.openui.clock.R.string.timer_seconds_label), color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pickers and colons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfiniteNumberPicker(hours, 0..99, { hours = it }, Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(20.dp))
                        InfiniteNumberPicker(minutes, 0..59, { minutes = it }, Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(20.dp))
                        InfiniteNumberPicker(seconds, 0..59, { seconds = it }, Modifier.weight(1f))
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(":", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Medium, modifier = Modifier.offset(y = (-4).dp))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(":", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Medium, modifier = Modifier.offset(y = (-4).dp))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PresetButton("00:10:00") { hours = 0; minutes = 10; seconds = 0 }
                    PresetButton("00:15:00") { hours = 0; minutes = 15; seconds = 0 }
                    PresetButton("00:30:00") { hours = 0; minutes = 30; seconds = 0 }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Start Button
                Button(
                    onClick = {
                        val totalMs = (hours * 3600L + minutes * 60L + seconds) * 1000L
                        if (totalMs > 0) {
                            onStart(totalMs)
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B3B6D),
                        contentColor = Color(0xFFD4D4FF)
                    ),
                    modifier = Modifier.size(100.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(stringResource(org.openui.clock.R.string.timer_start), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(modifier = Modifier.height(110.dp))
            } else {
                val remHrs = (state.remainingMillis / 3600000) % 24
                val remMin = (state.remainingMillis / 60000) % 60
                val remSec = (state.remainingMillis / 1000) % 60

                val progress = if (state.totalMillis > 0) state.remainingMillis.toFloat() / state.totalMillis.toFloat() else 0f

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(300.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF7C3AED),
                        trackColor = Color.White.copy(alpha = 0.05f),
                        strokeWidth = 8.dp
                    )

                    Text(
                        text = if (remHrs > 0) String.format("%02d:%02d:%02d", remHrs, remMin, remSec)
                               else String.format("%02d:%02d", remMin, remSec),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onReset,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A2B36),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(90.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(stringResource(org.openui.clock.R.string.timer_cancel), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            if (state.isRunning) onPause() else onResume()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isRunning) Color(0xFF3D1B1B) else Color(0xFF3B3B6D),
                            contentColor = if (state.isRunning) Color(0xFFFFB4B4) else Color(0xFFD4D4FF)
                        ),
                        modifier = Modifier.size(90.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (state.isRunning) stringResource(org.openui.clock.R.string.timer_pause) else stringResource(org.openui.clock.R.string.timer_start), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
fun PresetButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2A2B36),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfiniteNumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = range.last - range.first + 1
    val itemCount = 10000 * count
    val initialPage = remember(count, range.first) { (itemCount / 2) - ((itemCount / 2) % count) + (value - range.first) }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { itemCount }
    )
    val coroutineScope = rememberCoroutineScope()

    val itemHeight = 70.dp
    val visibleHeight = 210.dp

    LaunchedEffect(pagerState.currentPage) {
        val num = range.first + ((pagerState.currentPage % count + count) % count)
        onValueChange(num)
    }

    val flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = androidx.compose.foundation.pager.PagerSnapDistance.atMost(50)
    )

    Box(
        modifier = modifier.height(visibleHeight),
        contentAlignment = Alignment.Center
    ) {
        VerticalPager(
            state = pagerState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val num = range.first + ((page % count + count) % count)
            val isSelected = page == pagerState.currentPage

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d", num),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (isSelected) 56.sp else 38.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}
