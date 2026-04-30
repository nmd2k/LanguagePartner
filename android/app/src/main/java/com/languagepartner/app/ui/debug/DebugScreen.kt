package com.languagepartner.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.languagepartner.app.viewmodel.LogEntry
import com.languagepartner.app.viewmodel.TranslationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: TranslationViewModel,
    onNavigateBack: () -> Unit
) {
    val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()

    val stats = rememberLogStats(logEntries)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug") },
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
                .background(Color(0xFF0F0F0E))
        ) {
            StatRow(stats = stats)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(logEntries) { entry ->
                    LogRow(entry = entry)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

data class LogStats(
    val total: Int,
    val errors: Int,
    val warns: Int,
    val lastRtt: String
)

@Composable
private fun rememberLogStats(entries: List<LogEntry>): LogStats {
    val total = entries.size
    val errors = entries.count { it.level == "ERROR" }
    val warns = entries.count { it.level == "WARN" }
    return LogStats(
        total = total,
        errors = errors,
        warns = warns,
        lastRtt = "—"
    )
}

@Composable
private fun StatRow(stats: LogStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(label = "Logs", value = stats.total.toString(), tone = "neutral")
        StatChip(label = "Errors", value = stats.errors.toString(), tone = "err")
        StatChip(label = "Warns", value = stats.warns.toString(), tone = "warn")
        StatChip(label = "RTT", value = stats.lastRtt, tone = "neutral")
    }
}

@Composable
private fun RowScope.StatChip(label: String, value: String, tone: String) {
    val valueColor = when (tone) {
        "err" -> Color(0xFFFF503C)
        "warn" -> Color(0xFFFFB43C)
        "ok" -> Color(0xFF4CAF50)
        else -> Color(0xBBFFFFFF)
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .background(Color(0x0DFFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0x59FFFFFF),
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        "INFO" -> Color(0xFF4CAF50)
        "DEBUG" -> Color(0x99FFFFFF)
        "WARN" -> Color(0xFFFFB43C)
        "ERROR" -> Color(0xFFFF503C)
        else -> Color(0x99FFFFFF)
    }
    val bgColor = when (entry.level) {
        "ERROR" -> Color(0x11FF503C)
        "WARN" -> Color(0x0DFFB43C)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.timestamp,
            fontSize = 9.sp,
            color = Color(0x40FFFFFF),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 1.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = entry.level,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = levelColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(38.dp)
        )
        Text(
            text = entry.message,
            fontSize = 11.sp,
            color = Color(0xA6FFFFFF),
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp
        )
    }
}
