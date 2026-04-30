package com.languagepartner.app.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val keyboardController = LocalSoftwareKeyboardController.current

    val isListening = connectionStatus == ConnectionStatus.CONNECTED && !paused && mode == TranslationMode.SPEAK

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collectLatest { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(utterances.size) {
        if (utterances.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                LanguageBar(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    onSourceClick = { onNavigateToLanguagePicker(true) },
                    onTargetClick = { onNavigateToLanguagePicker(false) },
                    onSwap = { viewModel.swapLanguages() }
                )
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
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            BottomBar(
                textInput = textInput,
                onTextInputChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendTextInput(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                },
                connectionStatus = connectionStatus,
                paused = paused,
                isListening = isListening,
                onMicToggle = { viewModel.togglePause() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                items = utterances.asReversed(),
                key = { it.id }
            ) { utterance ->
                ConversationBubble(
                    utterance = utterance,
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                    )
                )
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
        LanguagePill(language = sourceLanguage, onClick = onSourceClick)
        IconButton(onClick = onSwap) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = stringResource(R.string.swap_languages),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        LanguagePill(language = targetLanguage, onClick = onTargetClick)
    }
}

@Composable
private fun LanguagePill(
    language: Language,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp
    ) {
        Text(
            text = language.name,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
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
private fun ConnectionStatusChip(status: ConnectionStatus) {
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
                modifier = Modifier.size(8.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun LiveListeningIndicator() {
    val activeColor = Color(0xFF4CAF50)
    val barHeights = remember {
        listOf(0.4f, 0.7f, 0.9f, 0.5f, 0.3f, 0.8f, 1.0f, 0.6f, 0.4f, 0.7f, 0.5f, 0.8f, 0.9f, 0.4f, 0.6f, 0.5f, 0.3f, 0.7f, 0.45f, 0.75f)
    }
    val barCount = barHeights.size
    val transition = rememberInfiniteTransition(label = "listenWave")
    val animatedBars = List(barCount) { index ->
        transition.animateFloat(
            initialValue = barHeights[index] * 0.3f,
            targetValue = barHeights[index],
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 280,
                    delayMillis = index * 40,
                    easing = FastOutSlowInEasing
                )
            ),
            label = "bar_$index"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = activeColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        repeat(barCount) { index ->
            val heightRatio = animatedBars[index].value
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((20 * heightRatio).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor.copy(alpha = 0.85f))
            )
            if (index < barCount - 1) {
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.listening),
            color = activeColor,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ConversationBubble(
    utterance: Utterance,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = utterance.sourceText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (utterance.translatedText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = utterance.translatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeFormat.format(Date(utterance.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp)
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
    isListening: Boolean,
    onMicToggle: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Column {
            AnimatedVisibility(
                visible = isListening,
                enter = slideInVertically { it } + fadeIn(tween(180)),
                exit = slideOutVertically { it } + fadeOut(tween(180))
            ) {
                LiveListeningIndicator()
            }

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
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onSend,
                    enabled = textInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (textInput.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline
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
                            contentDescription = stringResource(R.string.resume),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        else -> Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.pause),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
