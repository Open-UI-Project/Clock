package org.openui.clock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openui.clock.alarm.AlarmReceiver
import org.openui.clock.alarm.AlarmService
import org.openui.clock.ui.ClockViewModel
import org.openui.clock.ui.components.AboutDialog
import org.openui.clock.ui.components.ClockTab
import org.openui.clock.ui.components.FloatingPillNavBar
import org.openui.clock.ui.components.RingingAlarmDialog
import org.openui.clock.ui.components.TopClockHeader
import org.openui.clock.ui.screens.AlarmScreen
import org.openui.clock.ui.screens.StopwatchScreen
import org.openui.clock.ui.screens.TimerScreen
import org.openui.clock.ui.screens.WorldClockScreen
import org.openui.clock.weather.BreezyWeatherHelper
import org.openui.clock.widget.ClockWidgetManager
import android.provider.Settings
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.openui.clock.ui.theme.ClockAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()
    private var ringingLabelState = mutableStateOf<String?>(null)
    private var requestedTabState = mutableStateOf<ClockTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        checkAndRequestPermissions()
        handleIntent(intent)
        ClockWidgetManager.updateAllWidgets(this)

        setContent {
            ClockAppTheme {
                var selectedTab by remember { mutableStateOf(ClockTab.ALARM) }
                var showAboutDialog by remember { mutableStateOf(false) }

                LaunchedEffect(requestedTabState.value) {
                    requestedTabState.value?.let {
                        selectedTab = it
                        requestedTabState.value = null
                    }
                }

                val alarms by viewModel.alarms.collectAsStateWithLifecycle()
                val cities by viewModel.cities.collectAsStateWithLifecycle()
                val stopwatchState by viewModel.stopwatchState.collectAsStateWithLifecycle()
                val timerState by viewModel.timerState.collectAsStateWithLifecycle()

                val ringingLabel = ringingLabelState.value

                var showAddAlarmSheet by remember { mutableStateOf(false) }
                var showAddCityDialog by remember { mutableStateOf(false) }
                var isSelectionMode by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height * 0.45f)
                        val radius = size.height * 0.48f
                        scale(scaleX = 0.52f, scaleY = 1.0f, pivot = center) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4C245E).copy(alpha = 0.85f),
                                        Color(0xFF2A1B44).copy(alpha = 0.65f),
                                        Color(0xFF15132A).copy(alpha = 0.40f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = radius
                                ),
                                center = center,
                                radius = radius
                            )
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                        topBar = {
                            TopClockHeader(
                                title = stringResource(selectedTab.titleRes),
                                onAddClick = if (!isSelectionMode) {
                                    when (selectedTab) {
                                        ClockTab.ALARM -> { { showAddAlarmSheet = true } }
                                        ClockTab.WORLD_CLOCK -> { { showAddCityDialog = true } }
                                        else -> null
                                    }
                                } else null,
                                onAboutClick = { showAboutDialog = true }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                androidx.compose.animation.AnimatedContent(
                                    targetState = selectedTab,
                                    label = "tab_transition",
                                    transitionSpec = {
                                        androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
                                    }
                                ) { tab ->
                                    when (tab) {
                                        ClockTab.ALARM -> AlarmScreen(
                                            alarms = alarms,
                                            onToggleAlarm = viewModel::toggleAlarm,
                                            onAddAlarm = viewModel::addAlarm,
                                            onUpdateAlarm = viewModel::updateAlarm,
                                            onDeleteAlarm = viewModel::deleteAlarm,
                                            externalShowAddSheet = showAddAlarmSheet,
                                            onExternalShowAddSheetHandled = { showAddAlarmSheet = false },
                                            onSelectionModeChange = { isSelectionMode = it }
                                        )
                                        ClockTab.WORLD_CLOCK -> WorldClockScreen(
                                            cities = cities,
                                            onAddCity = viewModel::addCity,
                                            onDeleteCity = viewModel::deleteCity,
                                            externalShowAddDialog = showAddCityDialog,
                                            onExternalShowAddDialogHandled = { showAddCityDialog = false },
                                            onSelectionModeChange = { isSelectionMode = it }
                                        )
                                        ClockTab.STOPWATCH -> StopwatchScreen(
                                            state = stopwatchState,
                                            onStart = viewModel::startStopwatch,
                                            onPause = viewModel::pauseStopwatch,
                                            onReset = viewModel::resetStopwatch,
                                            onLap = viewModel::lapStopwatch
                                        )
                                        ClockTab.TIMER -> TimerScreen(
                                            state = timerState,
                                            onStart = viewModel::startTimer,
                                            onResume = viewModel::resumeTimer,
                                            onPause = viewModel::pauseTimer,
                                            onReset = viewModel::resetTimer
                                        )
                                    }
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isSelectionMode,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it },
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                FloatingPillNavBar(
                                    selectedTab = selectedTab,
                                    onTabSelected = {
                                        isSelectionMode = false
                                        selectedTab = it
                                    }
                                )
                            }
                        }

                        if (showAboutDialog) {
                            AboutDialog(onDismiss = { showAboutDialog = false })
                        }

                        ringingLabel?.let { label ->
                            RingingAlarmDialog(
                                alarmLabel = label,
                                onSnooze = {
                                    sendBroadcast(Intent(this@MainActivity, AlarmReceiver::class.java).apply {
                                        action = AlarmReceiver.ACTION_SNOOZE
                                    })
                                    ringingLabelState.value = null
                                },
                                onDismiss = {
                                    sendBroadcast(Intent(this@MainActivity, AlarmReceiver::class.java).apply {
                                        action = AlarmReceiver.ACTION_DISMISS
                                    })
                                    ringingLabelState.value = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ClockWidgetManager.updateAllWidgets(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == 201 || requestCode == 301) && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ClockWidgetManager.updateAllWidgets(this)
        }
    }

    private fun handleIntent(intent: Intent?) {
        checkRingingIntent(intent)

        when (intent?.action) {
            "android.intent.action.SHOW_ALARMS",
            "android.intent.action.SET_ALARM" -> requestedTabState.value = ClockTab.ALARM

            "android.intent.action.SHOW_TIMERS",
            "android.intent.action.SET_TIMER" -> requestedTabState.value = ClockTab.TIMER

            "android.intent.action.SHOW_STOPWATCH" -> requestedTabState.value = ClockTab.STOPWATCH

            "android.intent.action.SHOW_CLOCK",
            "android.intent.action.QUICK_CLOCK" -> requestedTabState.value = ClockTab.WORLD_CLOCK
        }

        val tabExtra = intent?.getStringExtra(EXTRA_TAB)
        if (!tabExtra.isNullBlinkOrEmpty()) {
            when (tabExtra) {
                "WORLD_CLOCK", "CLOCK" -> requestedTabState.value = ClockTab.WORLD_CLOCK
                "ALARM" -> requestedTabState.value = ClockTab.ALARM
                "STOPWATCH" -> requestedTabState.value = ClockTab.STOPWATCH
                "TIMER" -> requestedTabState.value = ClockTab.TIMER
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val breezyPermission = "org.breezyweather.READ_PROVIDER"
        if (BreezyWeatherHelper.isBreezyWeatherInstalled(this)) {
            if (ContextCompat.checkSelfPermission(this, breezyPermission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(breezyPermission), 201)
            }
        }

        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                301
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {}
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val notificationManager = getSystemService(android.app.NotificationManager::class.java)
                if (notificationManager != null && !notificationManager.canUseFullScreenIntent()) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            } catch (e: Exception) {}
        }
    }

    private fun checkRingingIntent(intent: Intent?) {
        val ringing = intent?.getStringExtra("RINGING_LABEL")
        if (!ringing.isNullBlinkOrEmpty()) {
            ringingLabelState.value = ringing
        }
    }

    private fun String?.isNullBlinkOrEmpty(): Boolean {
        return this == null || this.isBlank()
    }

    companion object {
        const val EXTRA_TAB = "EXTRA_TAB"
    }
}
