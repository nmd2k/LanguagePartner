package com.languagepartner.app.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.languagepartner.app.R
import com.languagepartner.app.websocket.ConnectionStatus
import com.languagepartner.app.viewmodel.Language
import com.languagepartner.app.viewmodel.TranslationMode
import com.languagepartner.app.viewmodel.TranslationViewModel
import com.languagepartner.app.viewmodel.Utterance
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TranslationViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToLanguagePicker: (isSource: Boolean) -> Unit
) {
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val utterances by viewModel.utterances.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val sourceLanguage by viewModel.sourceLanguage.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val isListening = connectionStatus == ConnectionStatus.CONNECTED && !paused

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collectLatest { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(utterances.size) {
        if (utterances.isNotEmpty()) {
            listState.animateScrollToItem(utterances.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_name))
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!isListening) {
                BottomBar(
                    textInput = textInput,
                    onTextInputChange = { textInput = it },
                    onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendTextInput(textInput)
                            textInput = ""
                        }
                    },
                    connectionStatus = connectionStatus,
                    paused = paused,
                    onMicToggle = { viewModel.togglePause() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isListening) {
                LanguageBar(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    onSourceClick = { onNavigateToLanguagePicker(true) },
                    onTargetClick = { onNavigateToLanguagePicker(false) },
                    onSwap = { viewModel.swapLanguages() }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeToggle(
                    currentMode = mode,
                    onToggle = { viewModel.toggleMode() }
                )
                ConnectionStatusChip(status = connectionStatus)
            }

            if (isListening) {
                WaveformVisualizer(isActive = true)

                LiveListeningIndicator()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(utterances) { utterance ->
                        ConversationBubble(utterance = utterance)
                    }
                }
            } else {
                WaveformVisualizer(isActive = false)

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(utterances) { utterance ->
                        ConversationBubble(utterance = utterance)
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageBar(
    sourceLanguage: Language,
    targetLanguage: Language,
    onSourceClick: () -> Unit,
    onTargetClick: () -> Unit,
    onSwap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguagePill(
            language = sourceLanguage,
            onClick = onSourceClick
        )
        IconButton(onClick = onSwap) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = stringResource(R.string.swap_languages)
            )
        }
        LanguagePill(
            language = targetLanguage,
            onClick = onTargetClick
        )
    }
}

@Composable
private fun LanguagePill(
    language: Language,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Text(
            text = language.name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ModeToggle(
    currentMode: TranslationMode,
    onToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = currentMode == TranslationMode.SPEAK,
            onClick = { if (currentMode != TranslationMode.SPEAK) onToggle() },
            label = { Text(stringResource(R.string.mode_speak)) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilterChip(
            selected = currentMode == TranslationMode.READ,
            onClick = { if (currentMode != TranslationMode.READ) onToggle() },
            label = { Text(stringResource(R.string.mode_read)) }
        )
    }
}

@Composable
private fun ConnectionStatusChip(
    status: ConnectionStatus
) {
    val color = when (status) {
        ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
        ConnectionStatus.CONNECTING -> Color(0xFFFFC107)
        ConnectionStatus.DISCONNECTED -> Color(0xFF9E9E9E)
        ConnectionStatus.ERROR -> Color(0xFFF44336)
    }
    val label = when (status) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.status_connected)
        ConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting)
        ConnectionStatus.DISCONNECTED -> stringResource(R.string.status_disconnected)
        ConnectionStatus.ERROR -> stringResource(R.string.status_error)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun WaveformVisualizer(isActive: Boolean) {
    val barCount = 12
    val staticHeights = remember {
        listOf(0.25f, 0.45f, 0.6f, 0.35f, 0.55f, 0.7f, 0.45f, 0.3f, 0.5f, 0.75f, 0.4f, 0.65f)
    }

    val transition = rememberInfiniteTransition(label = "waveform")
    val animatedBarValues = List(barCount) { index ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500,
                    delayMillis = index * 80,
                    easing = FastOutSlowInEasing
                )
            ),
            label = "bar_$index"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val heightRatio = if (isActive) animatedBarValues[index].value else staticHeights[index]
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((28 * heightRatio).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun LiveListeningIndicator() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF4CAF50).copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.listening),
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun ConversationBubble(
    utterance: Utterance
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val bubbleColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = bubbleColor
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = utterance.sourceText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = utterance.translatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeFormat.format(Date(utterance.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun BottomBar(
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSend: () -> Unit,
    connectionStatus: ConnectionStatus,
    paused: Boolean,
    onMicToggle: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = onTextInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.type_to_translate)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
            IconButton(
                onClick = onMicToggle,
                enabled = connectionStatus != ConnectionStatus.DISCONNECTED
            ) {
                when {
                    connectionStatus == ConnectionStatus.CONNECTED && !paused -> Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.pause),
                        tint = Color(0xFFF44336)
                    )
                    connectionStatus == ConnectionStatus.CONNECTED && paused -> Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.resume)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.pause)
                    )
                }
            }
        }
    }
}
