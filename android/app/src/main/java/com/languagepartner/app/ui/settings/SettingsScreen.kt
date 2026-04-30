package com.languagepartner.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.languagepartner.app.viewmodel.Language

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sourceLanguage: Language,
    targetLanguage: Language,
    serverAddress: String,
    onNavigateBack: () -> Unit,
    onNavigateToServerSetup: () -> Unit,
    onNavigateToLanguagePicker: (isSource: Boolean) -> Unit,
    onNavigateToDebug: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("ACCOUNT")

            SettingsRow(
                title = "Profile",
                subtitle = null,
                leadingIcon = Icons.Filled.Person,
                onClick = { }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsRow(
                title = "Server",
                subtitle = serverAddress,
                leadingIcon = Icons.Filled.Dns,
                onClick = onNavigateToServerSetup
            )

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("TRANSLATION")

            SettingsRow(
                title = "Source language",
                subtitle = sourceLanguage.name,
                leadingIcon = Icons.Filled.Translate,
                onClick = { onNavigateToLanguagePicker(true) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsRow(
                title = "Target language",
                subtitle = targetLanguage.name,
                leadingIcon = Icons.Filled.Translate,
                onClick = { onNavigateToLanguagePicker(false) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("DEVELOPER")

            SettingsRow(
                title = "Debug console",
                subtitle = "Log level: INFO",
                leadingIcon = Icons.Filled.BugReport,
                onClick = onNavigateToDebug
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
