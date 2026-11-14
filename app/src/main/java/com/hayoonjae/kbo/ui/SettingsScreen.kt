package com.hayoonjae.kbo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hayoonjae.kbo.R
import com.hayoonjae.kbo.data.Settings
import com.hayoonjae.kbo.data.Team
import com.hayoonjae.kbo.ui.theme.CardSurface
import com.hayoonjae.kbo.ui.theme.InkMuted

@Composable
fun SettingsScreen(
    viewModel: KboViewModel,
    settings: Settings,
    notificationsAllowed: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onChangeTeam: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val myTeam: Team? = settings.myTeam
    var keyInput by remember(settings.userApiKey) { mutableStateOf(settings.userApiKey) }
    var saved by remember { mutableStateOf(false) }

    LazyColumn(modifier.fillMaxSize()) {
        item {
            GradientHeader(
                team = myTeam,
                title = stringResource(R.string.tab_settings),
                subtitle = myTeam?.fullName ?: stringResource(R.string.no_team),
            )
        }

        item { SectionTitle(stringResource(R.string.my_team)) }
        item {
            Card {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = onChangeTeam).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (myTeam != null) {
                        TeamEmblem(myTeam, size = 32.dp)
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        myTeam?.fullName ?: stringResource(R.string.no_team),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.change_team),
                        style = MaterialTheme.typography.labelLarge,
                        color = InkMuted,
                    )
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = InkMuted)
                }
            }
        }

        item { SectionTitle(stringResource(R.string.settings_notifications)) }

        item {
            Card {
                Column {
                    if (!notificationsAllowed) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onRequestNotificationPermission)
                                .padding(16.dp),
                        ) {
                            Text(
                                stringResource(R.string.settings_permission_needed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Divider()
                    }
                    SwitchRow(
                        title = stringResource(R.string.settings_notify_result),
                        description = stringResource(R.string.settings_notify_result_desc),
                        checked = settings.notifyResult,
                        enabled = myTeam != null,
                        onChange = viewModel::setNotifyResult,
                    )
                    Divider()
                    SwitchRow(
                        title = stringResource(R.string.settings_notify_video),
                        description = stringResource(R.string.settings_notify_video_desc),
                        checked = settings.notifyVideo,
                        enabled = myTeam != null && settings.hasApiKey,
                        onChange = viewModel::setNotifyVideo,
                    )
                }
            }
        }

        item { SectionTitle(stringResource(R.string.settings_youtube)) }
        item {
            Card {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it; saved = false },
                        label = { Text(stringResource(R.string.settings_api_key)) },
                        placeholder = { Text(stringResource(R.string.settings_api_key_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (settings.userApiKey.isBlank() && settings.hasApiKey) {
                        Text(
                            stringResource(R.string.settings_api_key_from_build),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkMuted,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { viewModel.setApiKey(keyInput); saved = true }) {
                            Text(stringResource(R.string.settings_save))
                        }
                        if (saved) {
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.settings_saved),
                                style = MaterialTheme.typography.labelLarge,
                                color = InkMuted,
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.settings_about)) }
        item {
            Card {
                Text(
                    stringResource(R.string.settings_source),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurface),
    ) { content() }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.labelMedium, color = InkMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked && enabled, onCheckedChange = onChange, enabled = enabled)
    }
}
