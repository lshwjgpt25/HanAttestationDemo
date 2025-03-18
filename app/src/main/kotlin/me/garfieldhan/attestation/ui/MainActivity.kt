package me.garfieldhan.attestation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import me.garfieldhan.attestation.ui.main.MainScreen
import me.garfieldhan.attestation.ui.theme.AppTheme
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Timber.d("onCreate")

        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.d("onDestroy")
    }
}