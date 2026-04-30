package com.languagepartner.app.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.languagepartner.app.R
import com.languagepartner.app.viewmodel.TranslationMode
import com.languagepartner.app.viewmodel.TranslationViewModel
import com.languagepartner.app.viewmodel.Utterance
import com.languagepartner.app.websocket.ConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TranslationViewModel,
    onNavigateToSettings: () -> Unit
) {
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val utterances by viewModel.utterances.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val serverAddress by viewModel.serverAddress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Auto-connect when a server address is available
    LaunchedEffect(serverAddress) {
        if (serverAddress.isNotEmpty()) {
            viewModel.connect(serverAddress)
        }
    }

    // Show snackbar on error events
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { errorMessage: String ->
            snackbarHostState.showSnackbar(message = errorMessage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Status row: connection chip + mode toggle + mic indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectionStatusChip(status = connectionStatus)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModeToggle(mode = mode, onToggle = { viewModel.toggleMode() })
                    if (connectionStatus == ConnectionStatus.CONNECTED) {
                        MicIndicator()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Utterance list
            UtteranceList(utterances = utterances)
        }
    }
}

@Composable
private fun ConnectionStatusChip(status: ConnectionStatus) {
    val (label, color) = when (status) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.status_connected) to Color(0xFF4CAF50)
        ConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting) to Color(0xFFFFC107)
        ConnectionStatus.DISCONNECTED -> stringResource(R.string.status_disconnected) to Color(0xFF9E9E9E)
        ConnectionStatus.ERROR -> stringResource(R.string.status_error) to Color(0xFFF44336)
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(8.dp)
            ) {}
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
private fun ModeToggle(mode: TranslationMode, onToggle: () -> Unit) {
    FilterChip(
        selected = mode == TranslationMode.SPEAK,
        onClick = onToggle,
        label = {
            Text(
                if (mode == TranslationMode.SPEAK)
                    stringResource(R.string.mode_speak)
                else
                    stringResource(R.string.mode_read)
            )
        },
        modifier = Modifier.padding(end = 8.dp)
    )
}

@Composable
private fun MicIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )
    Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = "Microphone active",
        tint = Color(0xFFF44336),
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
    )
}

@Composable
private fun UtteranceList(utterances: List<Utterance>) {
    val listState = rememberLazyListState()

    // Auto-scroll to top when new utterance arrives (newest at top)
    LaunchedEffect(utterances.size) {
        if (utterances.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    if (utterances.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Waiting for translations...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(utterances, key = { it.id.ifEmpty { it.timestamp.toString() } }) { utterance ->
                UtteranceCard(utterance = utterance)
            }
        }
    }
}

@Composable
private fun UtteranceCard(utterance: Utterance) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ZH source text — small, muted
            Text(
                text = utterance.sourceText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // EN translation — large, prominent
            Text(
                text = utterance.translatedText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
