package org.openui.clock.ui.components

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class SoundItem(
    val title: String,
    val uriStr: String,
    val isCustom: Boolean = false
)

@Composable
fun SoundSelectionDialog(
    currentSoundName: String,
    currentSoundUri: String,
    onDismiss: () -> Unit,
    onSoundSelected: (name: String, uri: String) -> Unit
) {
    val context = LocalContext.current
    var selectedName by remember { mutableStateOf(currentSoundName.ifBlank { "По умолчанию" }) }
    var selectedUri by remember { mutableStateOf(currentSoundUri) }

    var playingRingtone by remember { mutableStateOf<Ringtone?>(null) }

    val soundList = remember { mutableStateListOf<SoundItem>() }

    LaunchedEffect(Unit) {
        soundList.clear()
        // Default built-in / system sound
        soundList.add(SoundItem("По умолчанию", ""))
        soundList.add(SoundItem("Cesium", ""))
        soundList.add(SoundItem("Platinum", ""))

        // Query system ringtones
        try {
            val ringtoneManager = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_ALARM)
            }
            val cursor = ringtoneManager.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uriStr = ringtoneManager.getRingtoneUri(cursor.position).toString()
                if (soundList.none { it.title == title }) {
                    soundList.add(SoundItem(title, uriStr))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (currentSoundUri.isNotBlank() && soundList.none { it.uriStr == currentSoundUri }) {
            soundList.add(0, SoundItem(currentSoundName.ifBlank { "Музыка с устройства" }, currentSoundUri, true))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playingRingtone?.stop()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val name = "Моя музыка (${it.lastPathSegment ?: "файл"})"
            selectedName = name
            selectedUri = it.toString()
            val customItem = SoundItem(name, it.toString(), true)
            if (soundList.none { item -> item.uriStr == it.toString() }) {
                soundList.add(0, customItem)
            }
        }
    }

    fun playSound(item: SoundItem) {
        playingRingtone?.stop()
        try {
            val uri = if (item.uriStr.isNotBlank()) {
                Uri.parse(item.uriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            val rt = RingtoneManager.getRingtone(context, uri)
            rt?.play()
            playingRingtone = rt
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(onDismissRequest = {
        playingRingtone?.stop()
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Звук будильника",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        playingRingtone?.stop()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Select from device button
                OutlinedButton(
                    onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбрать с устройства", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(soundList, key = { "${it.title}_${it.uriStr}" }) { item ->
                        val isSelected = selectedName == item.title || (item.uriStr.isNotBlank() && selectedUri == item.uriStr)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedName = item.title
                                    selectedUri = item.uriStr
                                    playSound(item)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedName = item.title
                                    selectedUri = item.uriStr
                                    playSound(item)
                                }
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        playingRingtone?.stop()
                        onDismiss()
                    }) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        playingRingtone?.stop()
                        onSoundSelected(selectedName, selectedUri)
                    }) {
                        Text("Выбрать")
                    }
                }
            }
        }
    }
}
