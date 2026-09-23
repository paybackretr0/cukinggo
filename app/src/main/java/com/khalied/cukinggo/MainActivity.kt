package com.khalied.cukinggo

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khalied.cukinggo.navigation.CukingGoNavHost
import com.khalied.cukinggo.ui.theme.CreamBg
import com.khalied.cukinggo.ui.theme.CukingGoTheme
import com.khalied.cukinggo.ui.theme.NightBg
import com.khalied.cukinggo.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themePreferences = appContainer.themePreferences

        setContent {
            val themeModeKey by themePreferences.themeModeKey.collectAsStateWithLifecycle()
            val darkTheme = ThemeMode.fromKey(themeModeKey).isDark(isSystemInDarkTheme())

            // Latar window ikut pilihan tema, supaya tidak ada kedipan warna
            // yang tidak sesuai saat pindah layar.
            SideEffect {
                window.setBackgroundDrawable(
                    ColorDrawable((if (darkTheme) NightBg else CreamBg).toArgb())
                )
            }

            CukingGoTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CukingGoNavHost()
                }
            }
        }
    }
}
