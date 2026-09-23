package com.khalied.cukinggo.ui.components

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.khalied.cukinggo.R
import com.khalied.cukinggo.location.NearbyAlertState
import com.khalied.cukinggo.location.NearbyRadius
import com.khalied.cukinggo.ui.theme.BlushPink
import com.khalied.cukinggo.ui.theme.InkSoft

/**
 * Setelan fitur "kabar kalau dekat kucing": menyalakan, memilih radius, dan
 * panduan izinnya.
 *
 * Dialog ini sekaligus jadi halaman panduan izin, karena fiturnya butuh dua izin
 * berurutan yang tidak bisa diminta sekaligus: izin notifikasi, lalu izin lokasi,
 * baru izin lokasi latar belakang. Satu tombol selalu menunjuk langkah berikutnya
 * yang memang kurang, jadi pengguna tidak perlu tahu urutannya.
 *
 * Nama opsi pengaturan yang harus dipilih diambil dari sistem lewat
 * getBackgroundPermissionOptionLabel(), supaya kalimatnya cocok dengan yang
 * benar-benar tertulis di HP pengguna (bisa berbeda bahasa dan versi Android).
 *
 * Pemilih radiusnya tetap ditampilkan di semua keadaan, karena itu satu-satunya
 * setelan selain nyala/mati, dan menampilkannya hanya kadang-kadang membuat
 * dialognya terasa berubah-ubah.
 */
@Composable
fun NearbyAlertDialog(
    state: NearbyAlertState,
    radius: NearbyRadius,
    watchedCount: Int,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onRadiusChange: (NearbyRadius) -> Unit,
    onFixPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fallbackLabel = stringResource(R.string.nearby_background_option_fallback)
    val backgroundOptionLabel = remember(context, fallbackLabel) {
        backgroundOptionLabel(context) ?: fallbackLabel
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.nearby_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                Text(
                    text = when (state) {
                        NearbyAlertState.OFF ->
                            stringResource(R.string.nearby_dialog_off, radius.meters)

                        NearbyAlertState.NEED_NOTIFICATION_PERMISSION ->
                            stringResource(R.string.nearby_dialog_need_notification)

                        NearbyAlertState.NEED_FOREGROUND_LOCATION ->
                            stringResource(R.string.nearby_dialog_need_foreground)

                        NearbyAlertState.NEED_BACKGROUND_LOCATION ->
                            stringResource(
                                R.string.nearby_dialog_need_background,
                                backgroundOptionLabel
                            )

                        NearbyAlertState.ACTIVE -> stringResource(
                            R.string.nearby_dialog_active,
                            watchedCount,
                            radius.meters
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.nearby_radius_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    NearbyRadius.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == radius,
                            onClick = { onRadiusChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = NearbyRadius.entries.size
                            ),
                            label = { Text(text = stringResource(option.labelRes())) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(radius.hintRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            val label = when (state) {
                NearbyAlertState.OFF -> stringResource(R.string.nearby_action_turn_on)
                NearbyAlertState.NEED_NOTIFICATION_PERMISSION ->
                    stringResource(R.string.nearby_action_notification)

                NearbyAlertState.NEED_FOREGROUND_LOCATION ->
                    stringResource(R.string.nearby_action_foreground)

                NearbyAlertState.NEED_BACKGROUND_LOCATION ->
                    stringResource(R.string.nearby_action_background)

                NearbyAlertState.ACTIVE -> stringResource(R.string.nearby_action_turn_off)
            }

            Button(
                onClick = when (state) {
                    NearbyAlertState.OFF -> onTurnOn
                    NearbyAlertState.ACTIVE -> onTurnOff
                    else -> onFixPermission
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state == NearbyAlertState.ACTIVE) {
                        BlushPink
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (state == NearbyAlertState.ACTIVE) {
                        InkSoft
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            // Saat izinnya belum lengkap, tombol ini jadi jalan keluar resmi
            // "tidak sekarang": fiturnya dimatikan lagi, bukan dibiarkan menggantung.
            val incomplete = state != NearbyAlertState.OFF && state != NearbyAlertState.ACTIVE
            TextButton(
                onClick = if (incomplete) onTurnOff else onDismiss
            ) {
                Text(
                    text = if (incomplete) {
                        stringResource(R.string.nearby_action_decline)
                    } else {
                        stringResource(R.string.action_close)
                    }
                )
            }
        }
    )
}

@StringRes
private fun NearbyRadius.labelRes(): Int = when (this) {
    NearbyRadius.NEAR -> R.string.nearby_radius_100
    NearbyRadius.NORMAL -> R.string.nearby_radius_200
    NearbyRadius.FAR -> R.string.nearby_radius_500
}

@StringRes
private fun NearbyRadius.hintRes(): Int = when (this) {
    NearbyRadius.NEAR -> R.string.nearby_radius_100_hint
    NearbyRadius.NORMAL -> R.string.nearby_radius_200_hint
    NearbyRadius.FAR -> R.string.nearby_radius_500_hint
}

/** Nama opsi izin lokasi latar belakang menurut sistem, mis. "Izinkan sepanjang waktu". */
private fun backgroundOptionLabel(context: Context): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    return runCatching {
        context.packageManager.getBackgroundPermissionOptionLabel()?.toString()
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
