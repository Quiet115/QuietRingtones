package com.quietringtones.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quietringtones.R

@Composable
fun AppearanceTab(appearance: Appearance, onChange: (Appearance) -> Unit) {
    var editing by remember { mutableStateOf<CustomPalette?>(null) }
    var creating by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(Palettes.builtIn, key = { it.id }) { palette ->
            PaletteOption(
                palette = palette,
                selected = appearance.selectedId == palette.id,
                onClick = { onChange(appearance.copy(selectedId = palette.id)) },
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item(key = Palettes.WALLPAPER) {
                WallpaperOption(
                    selected = appearance.isWallpaper,
                    onClick = { onChange(appearance.copy(selectedId = Palettes.WALLPAPER)) },
                )
            }
        }
        item(key = "mine") {
            Text(
                stringResource(R.string.your_palettes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        items(appearance.customs, key = { it.id }) { custom ->
            PaletteOption(
                palette = Palettes.custom(custom),
                selected = appearance.selectedId == custom.id,
                onClick = { onChange(appearance.copy(selectedId = custom.id)) },
                trailing = {
                    TextButton(onClick = { editing = custom }) { Text(stringResource(R.string.edit)) }
                    TextButton(onClick = {
                        val rest = appearance.customs.filterNot { it.id == custom.id }
                        val selected = if (appearance.selectedId == custom.id) Palettes.DEFAULT else appearance.selectedId
                        onChange(Appearance(selected, rest))
                    }) { Text(stringResource(R.string.delete)) }
                },
            )
        }
        item(key = "create") {
            OutlinedButton(
                onClick = { creating = true },
                enabled = appearance.customs.size < Palettes.MAX_CUSTOM,
            ) { Text(stringResource(R.string.create_palette)) }
        }
    }

    val target = editing
    if (creating || target != null) {
        PaletteEditor(
            initial = target ?: CustomPalette("custom:${System.currentTimeMillis()}", "", true, 190f, 250f, 0.4f),
            onDismiss = {
                creating = false
                editing = null
            },
            onSave = { saved ->
                val customs = appearance.customs.filterNot { it.id == saved.id } + saved
                onChange(Appearance(saved.id, customs))
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun PaletteOption(
    palette: Palette,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    OptionCard(selected, onClick) {
        Swatches(listOf(palette.background, palette.card, palette.accent, palette.accent2))
        Column(Modifier.weight(1f)) {
            Text(palette.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(palette.group.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

@Composable
private fun WallpaperOption(selected: Boolean, onClick: () -> Unit) {
    OptionCard(selected, onClick) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.palette_wallpaper), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.palette_wallpaper_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OptionCard(selected: Boolean, onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, outline),
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun Swatches(colors: List<Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        colors.forEach { argb ->
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
    }
}

@Composable
private fun PaletteEditor(initial: CustomPalette, onDismiss: () -> Unit, onSave: (CustomPalette) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var dark by remember { mutableStateOf(initial.dark) }
    var accentHue by remember { mutableStateOf(initial.accentHue) }
    var backgroundHue by remember { mutableStateOf(initial.backgroundHue) }
    var tint by remember { mutableStateOf(initial.tint) }
    val draft = CustomPalette(initial.id, name.trim(), dark, accentHue, backgroundHue, tint)
    val preview = Palettes.custom(draft)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.palette_editor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text(stringResource(R.string.palette_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dark_mode), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = dark, onCheckedChange = { dark = it })
                }
                SliderRow(stringResource(R.string.accent_hue), accentHue, 0f..360f) { accentHue = it }
                SliderRow(stringResource(R.string.background_hue), backgroundHue, 0f..360f) { backgroundHue = it }
                SliderRow(stringResource(R.string.background_tint), tint, 0f..1f) { tint = it }
                Swatches(listOf(preview.background, preview.card, preview.accent, preview.accent2))
            }
        },
        confirmButton = {
            Button(enabled = draft.name.isNotEmpty(), onClick = { onSave(draft) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

private fun PaletteGroup.labelRes() = when (this) {
    PaletteGroup.DARK -> R.string.palette_group_dark
    PaletteGroup.COLORFUL -> R.string.palette_group_colorful
    PaletteGroup.LIGHT -> R.string.palette_group_light
    PaletteGroup.SYSTEM -> R.string.palette_group_system
}
