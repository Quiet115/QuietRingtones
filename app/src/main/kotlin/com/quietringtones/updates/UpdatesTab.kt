package com.quietringtones.updates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietringtones.R
import com.quietringtones.UpdateStatus
import com.quietringtones.UpdateUi
import com.quietringtones.core.Release
import java.text.DateFormat
import java.util.Date

@Composable
fun UpdatesTab(
    ui: UpdateUi,
    onAutomatic: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onDownload: (Release) -> Unit,
    onSkip: (Release) -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.auto_updates), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.auto_updates_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = ui.automatic, onCheckedChange = onAutomatic)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.installed_version, ui.installedVersion), style = MaterialTheme.typography.bodyLarge)
            if (ui.lastChecked > 0) {
                val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, context.resources.configuration.locales[0])
                Text(
                    stringResource(R.string.last_checked, format.format(Date(ui.lastChecked))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        StatusBlock(ui.status, onDownload, onSkip) { apk ->
            if (ApkInstaller.canInstall(context)) ApkInstaller.install(context, apk)
            else ApkInstaller.openInstallPermission(context)
        }

        val busy = ui.status is UpdateStatus.Checking || ui.status is UpdateStatus.Downloading
        OutlinedButton(onClick = onCheck, enabled = !busy) { Text(stringResource(R.string.check_updates)) }

        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GitHubRepo.RELEASES_PAGE)))
        }) { Text(stringResource(R.string.all_releases)) }
    }
}

@Composable
private fun StatusBlock(
    status: UpdateStatus,
    onDownload: (Release) -> Unit,
    onSkip: (Release) -> Unit,
    onInstall: (java.io.File) -> Unit,
) {
    when (status) {
        UpdateStatus.Idle -> Unit
        UpdateStatus.Checking -> StatusText(stringResource(R.string.update_checking))
        UpdateStatus.UpToDate -> StatusText(stringResource(R.string.update_up_to_date))
        UpdateStatus.Failed -> StatusText(stringResource(R.string.update_check_failed))
        is UpdateStatus.Available -> {
            StatusText(stringResource(R.string.update_available, status.release.version.toString()))
            Notes(status.release)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onDownload(status.release) }) { Text(stringResource(R.string.download_install)) }
                TextButton(onClick = { onSkip(status.release) }) { Text(stringResource(R.string.skip_version)) }
            }
        }
        is UpdateStatus.Downloading -> {
            StatusText(stringResource(R.string.update_downloading, status.percent))
            LinearProgressIndicator(progress = { status.percent / 100f }, modifier = Modifier.fillMaxWidth())
        }
        is UpdateStatus.Ready -> {
            StatusText(stringResource(R.string.update_ready, status.release.version.toString()))
            Notes(status.release)
            Button(onClick = { onInstall(status.apk) }) { Text(stringResource(R.string.install)) }
        }
        is UpdateStatus.DownloadFailed -> {
            StatusText(stringResource(R.string.update_download_failed))
            Button(onClick = { onDownload(status.release) }) { Text(stringResource(R.string.try_again)) }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Notes(release: Release) {
    if (release.notes.isBlank()) return
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.whats_new), style = MaterialTheme.typography.titleMedium)
            Text(
                release.notes,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
            )
        }
    }
}
