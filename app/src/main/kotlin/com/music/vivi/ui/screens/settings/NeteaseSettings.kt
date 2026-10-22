/**
 * vivimusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.music.vivi.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.music.vivi.LocalPlayerAwareWindowInsets
import com.music.vivi.R
import com.music.vivi.constants.EnableNeteaseStreamingKey
import com.music.vivi.constants.NeteaseAudioQuality
import com.music.vivi.constants.NeteaseAudioQualityKey
import com.music.vivi.ui.component.AnimatedRadioButton
import com.music.vivi.ui.component.ExpressiveSettingGroup
import com.music.vivi.ui.component.IconButton
import com.music.vivi.ui.component.Material3SettingsItem
import com.music.vivi.ui.component.ModernSwitch
import com.music.vivi.ui.utils.backToMain
import com.music.vivi.utils.rememberEnumPreference
import com.music.vivi.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeteaseSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (neteaseEnabled, onNeteaseEnabledChange) = rememberPreference(
        EnableNeteaseStreamingKey,
        defaultValue = false
    )
    val (neteaseQuality, onNeteaseQualityChange) = rememberEnumPreference(
        NeteaseAudioQualityKey,
        defaultValue = NeteaseAudioQuality.LOSSLESS
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Description text
        Text(
            text = stringResource(R.string.enable_netease_streaming_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Large capsule banner for main toggle
        val containerColor by animateColorAsState(
            targetValue = if (neteaseEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            label = "containerColor"
        )

        val contentColor = if (neteaseEnabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            onClick = { onNeteaseEnabledChange(!neteaseEnabled) },
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.enable_netease_streaming),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                ModernSwitch(
                    checked = neteaseEnabled,
                    onCheckedChange = onNeteaseEnabledChange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options settings group
        ExpressiveSettingGroup(
            title = stringResource(R.string.netease_audio_quality),
            items = listOf(
                Material3SettingsItem(
                    leadingContent = {
                        AnimatedRadioButton(
                            selected = neteaseQuality == NeteaseAudioQuality.LOSSLESS,
                            onClick = null,
                            enabled = neteaseEnabled
                        )
                    },
                    title = { Text(NeteaseAudioQuality.LOSSLESS.toLabel()) },
                    enabled = neteaseEnabled,
                    onClick = { onNeteaseQualityChange(NeteaseAudioQuality.LOSSLESS) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        AnimatedRadioButton(
                            selected = neteaseQuality == NeteaseAudioQuality.QUALITY_320,
                            onClick = null,
                            enabled = neteaseEnabled
                        )
                    },
                    title = { Text(NeteaseAudioQuality.QUALITY_320.toLabel()) },
                    enabled = neteaseEnabled,
                    onClick = { onNeteaseQualityChange(NeteaseAudioQuality.QUALITY_320) }
                ),
                Material3SettingsItem(
                    leadingContent = {
                        AnimatedRadioButton(
                            selected = neteaseQuality == NeteaseAudioQuality.QUALITY_128,
                            onClick = null,
                            enabled = neteaseEnabled
                        )
                    },
                    title = { Text(NeteaseAudioQuality.QUALITY_128.toLabel()) },
                    enabled = neteaseEnabled,
                    onClick = { onNeteaseQualityChange(NeteaseAudioQuality.QUALITY_128) }
                )
            )
        )

        Row(
            modifier = Modifier.padding(top = 24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.netease_beta_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.netease_settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}
