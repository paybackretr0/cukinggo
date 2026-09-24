package com.khalied.cukinggo

import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** Kucing yang diminta dibuka, misalnya dari tap widget di layar utama. */
    private val pendingCatId = MutableStateFlow<Long?>(null)

    /** Permintaan memotret dari tombol jepret di widget. */
    private val pendingCapture = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingCatId.value = intent.requestedCatId()
        pendingCapture.value = intent.wantsCapture()
        val displayPreferences = appContainer.displayPreferences

        setContent {
            val themeModeKey by displayPreferences.themeModeKey.collectAsStateWithLifecycle()
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
                    val openCatId by pendingCatId.collectAsStateWithLifecycle()
                    val openCapture by pendingCapture.collectAsStateWithLifecycle()
                    CukingGoNavHost(
                        openCatId = openCatId,
                        onOpenCatConsumed = { pendingCatId.value = null },
                        openCapture = openCapture,
                        onOpenCaptureConsumed = { pendingCapture.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // launchMode singleTop: app yang sudah terbuka menerima intent baru di
        // sini, bukan lewat onCreate.
        setIntent(intent)
        pendingCatId.value = intent.requestedCatId()
        pendingCapture.value = intent.wantsCapture()
    }

    private fun Intent?.requestedCatId(): Long? =
        this?.getLongExtra(EXTRA_CAT_ID, NO_CAT_ID)?.takeIf { it > 0 }

    private fun Intent?.wantsCapture(): Boolean =
        this?.getBooleanExtra(EXTRA_CAPTURE_NOW, false) == true

    companion object {
        /** Extra dari widget, supaya tap-nya langsung mendarat di detail kucing. */
        const val EXTRA_CAT_ID = "extra_cat_id"

        /** Widget tanpa data kucing mengirim ini, artinya cukup buka Home. */
        const val NO_CAT_ID = -1L

        /** Extra dari tombol jepret di widget: langsung buka kamera dan jepret. */
        const val EXTRA_CAPTURE_NOW = "extra_capture_now"
    }
}
