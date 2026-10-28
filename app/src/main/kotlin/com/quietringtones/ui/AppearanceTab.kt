package com.quietringtones.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quietringtones.R

@Composable
fun AppearanceTab(appearance: Appearance, onChange: (Appearance) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(PaletteId.entries, key = { it.name }) { id ->
            val preview = appearance.copy(palette = id).seed()
            PaletteOption(
                name = stringResource(id.nameRes()),
                description = stringResource(id.descriptionRes()),
                seed = preview,
                selected = appearance.palette == id,
                onClick = { onChange(appearance.copy(palette = id)) },
            )
        }
        if (appearance.palette == PaletteId.PERSONALIZADA) {
            item(key = "custom") { CustomControls(appearance, onChange) }
        }
    }
}

@Composable
private fun PaletteOption(name: String, description: String, seed: PaletteSeed, selected: Boolean, onClick: () -> Unit) {
    val colors = derive(seed)
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, outline),
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                listOf(colors.background, colors.surfaceVariant, colors.primary, colors.onBackground).forEach { argb ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CustomControls(appearance: Appearance, onChange: (Appearance) -> Unit) {
    var hex by remember(appearance.customAccent) { mutableStateOf(PaletteMath.toHex(appearance.customAccent)) }
    val parsed = PaletteMath.parseHex(hex)
    val bases = if (appearance.customDark) DarkBases else LightBases

    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(stringResource(R.string.accent_color))
        Swatches(
            colors = AccentChoices,
            selected = appearance.customAccent,
            onSelect = { onChange(appearance.copy(customAccent = it)) },
        )
        OutlinedTextField(
            value = hex,
            onValueChange = { text ->
                hex = text
                PaletteMath.parseHex(text)?.let { onChange(appearance.copy(customAccent = it)) }
            },
            label = { Text(stringResource(R.string.hex_label)) },
            isError = parsed == null,
            supportingText = { if (parsed == null) Text(stringResource(R.string.hex_error)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle(stringResource(R.string.background_color))
        Swatches(
            colors = bases,
            selected = bases[appearance.customBase.coerceIn(bases.indices)],
            onSelect = { onChange(appearance.copy(customBase = bases.indexOf(it))) },
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dark_mode), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = appearance.customDark,
                onCheckedChange = { onChange(appearance.copy(customDark = it, customBase = 0)) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Swatches(colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { argb ->
            val isSelected = argb == selected
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .semantics { contentDescription = PaletteMath.toHex(argb) }
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(argb) }),
            )
        }
    }
}

private fun PaletteId.nameRes() = when (this) {
    PaletteId.OBSIDIANA -> R.string.palette_obsidiana
    PaletteId.FIELTRO -> R.string.palette_fieltro
    PaletteId.PERSONALIZADA -> R.string.palette_custom
}

private fun PaletteId.descriptionRes() = when (this) {
    PaletteId.OBSIDIANA -> R.string.palette_obsidiana_desc
    PaletteId.FIELTRO -> R.string.palette_fieltro_desc
    PaletteId.PERSONALIZADA -> R.string.palette_custom_desc
}
