package org.openui.clock.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openui.clock.R
import org.openui.clock.data.Alarm
import org.openui.clock.ui.components.AddAlarmBottomSheet
import org.openui.clock.util.AlarmTimeUtils

private val AlarmCardShape = RoundedCornerShape(24.dp)
private val SelectionPillShape = RoundedCornerShape(32.dp)

@Composable
fun AlarmScreen(
    alarms: List<Alarm>,
    onToggleAlarm: (Alarm, Boolean) -> Unit,
    onAddAlarm: (Alarm) -> Unit,
    onUpdateAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    externalShowAddSheet: Boolean = false,
    onExternalShowAddSheetHandled: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }
    var alarmToDelete by remember { mutableStateOf<Alarm?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedAlarmIds by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(isSelectionMode) {
        onSelectionModeChange(isSelectionMode)
    }

    LaunchedEffect(externalShowAddSheet) {
        if (externalShowAddSheet) {
            alarmToEdit = null
            showAddSheet = true
            onExternalShowAddSheetHandled()
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedAlarmIds = emptySet()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (alarms.isEmpty()) {
            EmptyAlarmsState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    val isSelected = selectedAlarmIds.contains(alarm.id)
                    AlarmItemCard(
                        alarm = alarm,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onToggle = { enabled ->
                            onToggleAlarm(alarm, enabled)
                            if (enabled) {
                                val daysSet = AlarmTimeUtils.parseDaysOfWeek(alarm.daysOfWeek)
                                val msg = AlarmTimeUtils.getAlarmRingsInMessage(context, alarm.hour, alarm.minute, daysSet)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                selectedAlarmIds = if (isSelected) {
                                    selectedAlarmIds - alarm.id
                                } else {
                                    selectedAlarmIds + alarm.id
                                }
                            } else {
                                alarmToEdit = alarm
                                showAddSheet = true
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedAlarmIds = setOf(alarm.id)
                            }
                        }
                    )
                }
            }
        }

        // Animated Bottom Selection Action Pill
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Surface(
                shape = SelectionPillShape,
                color = Color(0xFF252733).copy(alpha = 0.95f),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cancel button
                    TextButton(
                        onClick = {
                            isSelectionMode = false
                            selectedAlarmIds = emptySet()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    // Select / Deselect All
                    val allSelected = alarms.isNotEmpty() && selectedAlarmIds.size == alarms.size
                    TextButton(
                        onClick = {
                            selectedAlarmIds = if (allSelected) {
                                emptySet()
                            } else {
                                alarms.map { it.id }.toSet()
                            }
                        }
                    ) {
                        Text(
                            text = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                            color = Color(0xFFC4B5FD),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    // Delete Button
                    Button(
                        onClick = {
                            val toDelete = alarms.filter { selectedAlarmIds.contains(it.id) }
                            toDelete.forEach { onDeleteAlarm(it) }
                            isSelectionMode = false
                            selectedAlarmIds = emptySet()
                        },
                        enabled = selectedAlarmIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            disabledContainerColor = Color(0xFFEF4444).copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = if (selectedAlarmIds.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stringResource(R.string.delete)} (${selectedAlarmIds.size})",
                            color = if (selectedAlarmIds.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddAlarmBottomSheet(
            initialAlarm = alarmToEdit,
            onDismiss = { showAddSheet = false },
            onSave = { savedAlarm ->
                if (alarmToEdit != null) {
                    onUpdateAlarm(savedAlarm)
                } else {
                    onAddAlarm(savedAlarm)
                }
                showAddSheet = false
            }
        )
    }

    alarmToDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = { Text(stringResource(R.string.alarm_delete_title)) },
            text = { Text(stringResource(R.string.alarm_delete_message, alarm.getFormattedTime())) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAlarm(alarm)
                    alarmToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmItemCard(
    alarm: Alarm,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2A2044) else Color(0xFF1E1F28).copy(alpha = 0.7f),
        label = "alarm_card_bg"
    )

    Card(
        shape = AlarmCardShape,
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(AlarmCardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Selection Checkbox on the Left
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF7C3AED) else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 0.dp else 2.dp,
                                color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.getFormattedTime(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (alarm.isEnabled) Color.White
                    else Color.White.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val subtitle = buildString {
                    append(alarm.getRepeatDaysText(LocalContext.current))
                    if (alarm.label.isNotBlank()) {
                        append(" • ")
                        append(alarm.label)
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (alarm.isEnabled) Color(0xFFC4B5FD)
                    else Color.White.copy(alpha = 0.4f)
                )
            }

            // Animated Switch on the Right
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF7C3AED)
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyAlarmsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.alarms_empty),
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFB0B3C0).copy(alpha = 0.85f)
        )
    }
}

