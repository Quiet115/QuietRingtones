package com.quietringtones.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quietringtones.R

@Composable
fun SettingsTab(
    appearance: Appearance,
    onAppearance: (Appearance) -> Unit,
    language: Language,
    onLanguage: (Language) -> Unit,
    updatesPage: @Composable () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = page == 0,
                onClick = { page = 0 },
                label = { Text(stringResource(R.string.settings_appearance)) },
            )
            FilterChip(
                selected = page == 1,
                onClick = { page = 1 },
                label = { Text(stringResource(R.string.settings_language)) },
            )
            FilterChip(
                selected = page == 2,
                onClick = { page = 2 },
                label = { Text(stringResource(R.string.settings_updates)) },
            )
        }
        when (page) {
            0 -> AppearanceTab(appearance, onAppearance)
            1 -> LanguageTab(language, onLanguage)
            else -> updatesPage()
        }
    }
}

@Composable
private fun LanguageTab(language: Language, onLanguage: (Language) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.language_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Language.entries.forEach { option ->
            val selected = option == language
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = { onLanguage(option) }),
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(12.dp))
                    Text(stringResource(option.labelRes()), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun Language.labelRes() = when (this) {
    Language.ENGLISH -> R.string.language_english
    Language.SPANISH -> R.string.language_spanish
}
