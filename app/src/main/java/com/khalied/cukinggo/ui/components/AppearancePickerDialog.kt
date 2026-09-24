package com.khalied.cukinggo.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.khalied.cukinggo.R
import com.khalied.cukinggo.ui.theme.ThemeMode
import com.khalied.cukinggo.widget.WidgetSkinMode

/**
 * Pilihan tampilan: tema app dan bentuk widget di layar utama.
 *
 * Keduanya digabung dalam satu dialog karena keduanya soal tampilan dan karena
 * header Home sengaja hanya punya dua pill; menambah pill ketiga akan menyempitkan
 * judul di sebelahnya.
 */
@Composable
fun AppearancePickerDialog(
    currentThemeMode: ThemeMode,
    currentWidgetSkin: WidgetSkinMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onWidgetSkinSelected: (WidgetSkinMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.appearance_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            // Tujuh baris pilihan lebih tinggi dari dialog biasa, jadi isinya
            // boleh digulir supaya layar pendek tidak memotong pilihan terakhir.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SectionLabel(stringResource(R.string.appearance_section_theme))
                ThemeMode.entries.forEach { mode ->
                    PickerRow(
                        selected = mode == currentThemeMode,
                        title = stringResource(mode.labelRes()),
                        description = stringResource(mode.descriptionRes()),
                        onClick = { onThemeModeSelected(mode) }
                    )
                }

                Spacer(Modifier.height(10.dp))

                SectionLabel(stringResource(R.string.appearance_section_widget))
                Text(
                    text = stringResource(R.string.appearance_widget_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                WidgetSkinMode.entries.forEach { mode ->
                    PickerRow(
                        selected = mode == currentWidgetSkin,
                        title = stringResource(mode.labelRes()),
                        description = stringResource(mode.descriptionRes()),
                        onClick = { onWidgetSkinSelected(mode) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun PickerRow(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
private fun ThemeMode.descriptionRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system_desc
    ThemeMode.LIGHT -> R.string.theme_light_desc
    ThemeMode.DARK -> R.string.theme_dark_desc
}

@StringRes
private fun WidgetSkinMode.labelRes(): Int = when (this) {
    WidgetSkinMode.AUTO -> R.string.widget_skin_auto
    WidgetSkinMode.STICKER -> R.string.widget_skin_sticker
    WidgetSkinMode.SPEECH -> R.string.widget_skin_speech
    WidgetSkinMode.WINDOW -> R.string.widget_skin_window
}

@StringRes
private fun WidgetSkinMode.descriptionRes(): Int = when (this) {
    WidgetSkinMode.AUTO -> R.string.widget_skin_auto_desc
    WidgetSkinMode.STICKER -> R.string.widget_skin_sticker_desc
    WidgetSkinMode.SPEECH -> R.string.widget_skin_speech_desc
    WidgetSkinMode.WINDOW -> R.string.widget_skin_window_desc
}
