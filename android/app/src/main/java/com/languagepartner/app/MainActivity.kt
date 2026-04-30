package com.languagepartner.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.languagepartner.app.ui.main.MainScreen
import com.languagepartner.app.ui.picker.LanguagePickerScreen
import com.languagepartner.app.ui.settings.ServerSetupScreen
import com.languagepartner.app.ui.settings.SettingsScreen
import com.languagepartner.app.ui.debug.DebugScreen
import com.languagepartner.app.ui.theme.LanguagePartnerTheme
import com.languagepartner.app.viewmodel.Language
import com.languagepartner.app.viewmodel.TranslationViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            LanguagePartnerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LanguagePartnerApp()
                }
            }
        }
    }
}

@Composable
fun LanguagePartnerApp() {
    val navController = rememberNavController()
    val translationViewModel: TranslationViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = translationViewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToLanguagePicker = { isSource ->
                    navController.navigate("language_picker/${isSource}")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                sourceLanguage = translationViewModel.sourceLanguage.value,
                targetLanguage = translationViewModel.targetLanguage.value,
                serverAddress = translationViewModel.serverAddress.value,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToServerSetup = { navController.navigate("server_setup") },
                onNavigateToLanguagePicker = { isSource ->
                    navController.navigate("language_picker/${isSource}")
                },
                onNavigateToDebug = { navController.navigate("debug") }
            )
        }
        composable("server_setup") {
            ServerSetupScreen(
                initialAddress = translationViewModel.serverAddress.value,
                onSave = { address ->
                    translationViewModel.saveServerAddress(address)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
                onTestConnection = { address, onResult ->
                    translationViewModel.testConnection(address, onResult)
                }
            )
        }
        composable(
            "language_picker/{isSource}",
            arguments = listOf(navArgument("isSource") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isSource = backStackEntry.arguments?.getBoolean("isSource") ?: true
            val languages = Language.SUPPORTED
            val selectedCode = if (isSource)
                translationViewModel.sourceLanguage.value.code
            else
                translationViewModel.targetLanguage.value.code

            LanguagePickerScreen(
                title = if (isSource) "Source Language" else "Target Language",
                languages = languages,
                selectedCode = selectedCode,
                onLanguageSelected = { code ->
                    if (isSource) {
                        translationViewModel.setSourceLanguage(code)
                    } else {
                        translationViewModel.setTargetLanguage(code)
                    }
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("debug") {
            DebugScreen(
                viewModel = translationViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
