package org.openui.clock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import org.openui.clock.R

enum class ClockTab(@StringRes val titleRes: Int, val icon: ImageVector) {
    ALARM(R.string.tab_alarm, Icons.Default.Alarm),
    WORLD_CLOCK(R.string.tab_world_clock, Icons.Default.Language),
    STOPWATCH(R.string.tab_stopwatch, Icons.Default.Timer),
    TIMER(R.string.tab_timer, Icons.Default.HourglassEmpty)
}

private val PillShape = RoundedCornerShape(32.dp)
private val DarkPillColor = Color(0xFF2C2D35).copy(alpha = 0.75f)
private val SelectedCircleColor = Color(0xFF4A4D59)

@Composable
fun FloatingPillNavBar(
    selectedTab: ClockTab,
    onTabSelected: (ClockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 40.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = PillShape,
            color = DarkPillColor,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClockTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab

                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) SelectedCircleColor else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_bg_color"
                    )

                    val animatedIconColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color(0xFFA0A3AF),
                        label = "tab_icon_color"
                    )

                    Box(
                        modifier = Modifier
                            .testTag("nav_tab_${tab.name.lowercase()}")
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(animatedBgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.titleRes),
                            tint = animatedIconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
