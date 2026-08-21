package org.openui.clock.ui.components

import androidx.compose.ui.res.stringResource
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.openui.clock.data.Alarm
import org.openui.clock.util.AlarmTimeUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardShape = RoundedCornerShape(28.dp)
private val BottomPillShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmBottomSheet(
    initialAlarm: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        AddAlarmContent(
            initialAlarm = initialAlarm,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

@Composable
private fun AddAlarmContent(
    initialAlarm: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialAlarm?.hour ?: 6) }
    var selectedMinute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }

    var label by remember { mutableStateOf(initialAlarm?.label ?: "") }
    var soundEnabled by remember { mutableStateOf(initialAlarm?.soundEnabled ?: true) }
    var soundName by remember { mutableStateOf(initialAlarm?.soundName ?: "default") }
    var soundUri by remember { mutableStateOf(initialAlarm?.soundUri ?: "") }

    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }
    var vibratePattern by remember { mutableStateOf(initialAlarm?.vibratePattern ?: "default") }

    var snoozeEnabled by remember { mutableStateOf(initialAlarm?.snoozeEnabled ?: true) }
    var snoozeSubtitle by remember { mutableStateOf("5 минут, 3 раза") }

    var showSoundDialog by remember { mutableStateOf(false) }
    var showVibrationDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }

    // Selected repeat days (1=Mon..7=Sun)
    val initialDays = remember(initialAlarm) {
        initialAlarm?.daysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
    }
    var selectedDays by remember { mutableStateOf(initialDays) }

    // Compute preview date (e.g. "Сегодня-пн, 21 авг." or "Пт, 25 авг.")
    val dateText by remember(selectedHour, selectedMinute, selectedDays) {
        derivedStateOf {
            AlarmTimeUtils.getAlarmDatePreviewText(selectedHour, selectedMinute, selectedDays)
        }
    }

    val context = LocalContext.current

    val ringsInText by remember(selectedHour, selectedMinute, selectedDays) {
        derivedStateOf {
            AlarmTimeUtils.getAlarmRingsInMessage(context, selectedHour, selectedMinute, selectedDays)
        }
    }

    val daysMap = remember {
        listOf(
            1 to "П",
            2 to "В",
            3 to "С",
            4 to "Ч",
            5 to "П",
            6 to "С",
            7 to "В"
        )
    }

    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    val handleSave = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "Включите разрешение на точные будильники", Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    } catch (_: Exception) {}
                }
            }
        }

        val alarmToSave = Alarm(
            id = initialAlarm?.id ?: 0,
            hour = selectedHour,
            minute = selectedMinute,
            label = label,
            isEnabled = true,
            daysOfWeek = selectedDays.sorted().joinToString(","),
            vibrate = vibrate,
            vibratePattern = vibratePattern,
            soundName = soundName,
            soundUri = soundUri,
            soundEnabled = soundEnabled,
            snoozeEnabled = snoozeEnabled,
            snoozeDurationMinutes = 5,
            snoozeTimes = 3
        )
        
        Toast.makeText(
            context,
            AlarmTimeUtils.getAlarmRingsInMessage(context, selectedHour, selectedMinute, selectedDays),
            Toast.LENGTH_SHORT
        ).show()

        onSave(alarmToSave)
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar with Back Arrow ONLY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (initialAlarm != null) stringResource(org.openui.clock.R.string.edit_alarm_title) else stringResource(org.openui.clock.R.string.add_alarm_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time Wheel Picker Display (05 59 / 06 : 00 / 07 01)
                TimeWheelPicker(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Remaining time indicator pill
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2C2D35).copy(alpha = 0.6f)
                ) {
                    AnimatedContent(
                        targetState = ringsInText,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "rings_in_anim"
                    ) { targetText ->
                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFFC4B5FD),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Settings Card
                Card(
                    shape = CardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E22)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header Row: Date & Calendar Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )

                            IconButton(
                                onClick = {
                                    selectedDays = when {
                                        selectedDays.size == 7 -> emptySet()
                                        selectedDays.containsAll(listOf(1, 2, 3, 4, 5)) && selectedDays.size == 5 -> setOf(6, 7)
                                        selectedDays.containsAll(listOf(6, 7)) && selectedDays.size == 2 -> setOf(1, 2, 3, 4, 5, 6, 7)
                                        else -> setOf(1, 2, 3, 4, 5)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Календарь",
                                    tint = if (selectedDays.isNotEmpty()) Color(0xFFC4B5FD) else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Day of Week Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            daysMap.forEach { (dayNum, dayChar) ->
                                val isSelected = selectedDays.contains(dayNum)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF7C3AED)
                                            else Color(0xFF252630)
                                        )
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                selectedDays - dayNum
                                            } else {
                                                selectedDays + dayNum
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayChar,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = if (isSelected) {
                                            Color.White
                                        } else if (dayNum == 7) {
                                            Color(0xFFEF4444).copy(alpha = 0.75f) // Sunday
                                        } else {
                                            Color.White.copy(alpha = 0.45f)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Alarm Label Field
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            placeholder = { Text(stringResource(org.openui.clock.R.string.alarm_label_hint), color = Color.White.copy(alpha = 0.4f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedBorderColor = Color(0xFF8B5CF6),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Item 1: Sound
                        SettingToggleRow(
                            title = stringResource(org.openui.clock.R.string.alarm_sound),
                            subtitle = soundName,
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it },
                            onClick = { showSoundDialog = true }
                        )

                        Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Item 2: Vibration
                        SettingToggleRow(
                            title = stringResource(org.openui.clock.R.string.alarm_vibrate),
                            subtitle = vibratePattern,
                            checked = vibrate,
                            onCheckedChange = { vibrate = it },
                            onClick = { showVibrationDialog = true }
                        )

                        Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        // Item 3: Snooze / Pause
                        SettingToggleRow(
                            title = stringResource(org.openui.clock.R.string.alarm_snooze),
                            subtitle = snoozeSubtitle,
                            checked = snoozeEnabled,
                            onCheckedChange = { snoozeEnabled = it },
                            onClick = { showSnoozeDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Fixed Bottom Action Pill [ Отмена | Сохранить ]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 56.dp, top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = BottomPillShape,
                    color = Color(0xFF2C2C34),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        ) {
                            Text(
                                text = stringResource(org.openui.clock.R.string.cancel),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Text(
                            text = "|",
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        TextButton(
                            onClick = handleSave,
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .testTag("save_alarm_button")
                        ) {
                            Text(
                                text = stringResource(org.openui.clock.R.string.save),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (showSoundDialog) {
            SoundSelectionDialog(
                currentSoundName = soundName,
                currentSoundUri = soundUri,
                onDismiss = { showSoundDialog = false },
                onSoundSelected = { name, uri ->
                    soundName = name
                    soundUri = uri
                    soundEnabled = true
                    showSoundDialog = false
                }
            )
        }

        if (showVibrationDialog) {
            SimpleOptionSelectionDialog(
                title = stringResource(org.openui.clock.R.string.alarm_vibrate),
                options = listOf(context.getString(org.openui.clock.R.string.sound_default), "Базовая", "Сердцебиение", "Сигнал", "Непрерывная"),
                selectedOption = vibratePattern,
                onDismiss = { showVibrationDialog = false },
                onSelect = {
                    vibratePattern = it
                    vibrate = true
                    showVibrationDialog = false
                }
            )
        }

        if (showSnoozeDialog) {
            SimpleOptionSelectionDialog(
                title = stringResource(org.openui.clock.R.string.alarm_snooze),
                options = listOf(
                    "5 минут, 3 раза",
                    "5 минут, 5 раз",
                    "10 минут, 3 раза",
                    "10 минут, 5 раз",
                    "15 минут, 3 раза"
                ),
                selectedOption = snoozeSubtitle,
                onDismiss = { showSnoozeDialog = false },
                onSelect = {
                    snoozeSubtitle = it
                    snoozeEnabled = true
                    showSnoozeDialog = false
                }
            )
        }
    }
}

@Composable
private fun TimeWheelPicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hours Column (Independently Scrollable with LazyColumn + snap fling)
        TimeWheelColumn(
            value = hour,
            count = 24,
            onValueChange = onHourChange,
            alignment = Alignment.End
        )

        // Center Colon
        Box(
            modifier = Modifier
                .height(64.dp)
                .width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ":",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Minutes Column (Independently Scrollable with LazyColumn + snap fling)
        TimeWheelColumn(
            value = minute,
            count = 60,
            onValueChange = onMinuteChange,
            alignment = Alignment.Start
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeWheelColumn(
    value: Int,
    count: Int,
    onValueChange: (Int) -> Unit,
    alignment: Alignment.Horizontal
) {
    val itemCount = 10000 * count
    val initialValue = remember { value }
    val initialPage = remember(count) { (itemCount / 2) - ((itemCount / 2) % count) + initialValue }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { itemCount }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        val selectedValue = (pagerState.currentPage % count + count) % count
        onValueChange(selectedValue)
    }

    val flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = androidx.compose.foundation.pager.PagerSnapDistance.atMost(50)
    )

    val itemHeight = 64.dp
    val visibleHeight = itemHeight * 3

    Box(
        modifier = Modifier
            .height(visibleHeight)
            .width(110.dp),
        contentAlignment = Alignment.Center
    ) {
        VerticalPager(
            state = pagerState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val itemValue = (page % count + count) % count
            val isSelected = pagerState.currentPage == page

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    },
                contentAlignment = if (alignment == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Text(
                    text = String.format("%02d", itemValue),
                    fontSize = if (isSelected) 52.sp else 34.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                    textAlign = if (alignment == Alignment.End) TextAlign.End else TextAlign.Start,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA78BFA)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SimpleOptionSelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1E24),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color(0xFF8B5CF6) else Color.White
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(option) }
                        )
                    }
                    Divider(color = Color.White.copy(alpha = 0.1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(org.openui.clock.R.string.cancel), color = Color.White)
                    }
                }
            }
        }
    }
}
