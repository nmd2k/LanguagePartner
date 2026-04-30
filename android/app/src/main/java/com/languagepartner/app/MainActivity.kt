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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.languagepartner.app.repository.SettingsRepository
import com.languagepartner.app.ui.main.MainScreen
import com.languagepartner.app.ui.settings.SettingsScreen
import com.languagepartner.app.ui.theme.LanguagePartnerTheme
import com.languagepartner.app.viewmodel.TranslationViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled; AudioCapture checks state at record time
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request RECORD_AUDIO on first launch
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            LanguagePartnerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LanguagePartnerApp(settingsRepository = settingsRepository)
                }
            }
        }
    }
}

@Composable
fun LanguagePartnerApp(settingsRepository: SettingsRepository) {
    val navController = rememberNavController()
    val translationViewModel: TranslationViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = translationViewModel,
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsRepository = settingsRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
