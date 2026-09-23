package com.khalied.cukinggo.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Dijalankan saat alarm tengah malam berbunyi.
 *
 * Tugasnya dua: menggambar ulang kedua varian widget (supaya "Kucing hari ini"
 * berganti kucing, dan lencana rentetan di keduanya ikut dihitung ulang), lalu
 * memasang alarm untuk tengah malam berikutnya.
 *
 * Pemanggilan [CatWidgets.scheduleMidnightRefresh] aman walau ternyata sudah tidak
 * ada widget yang terpasang: fungsi itu berhenti sendiri dan alarmnya tidak
 * dipasang lagi, jadi tidak ada alarm yang menyala tanpa ada yang menampilkannya.
 */
class WidgetMidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        CatWidgets.scheduleMidnightRefresh(context)
        CatWidgets.refreshAll(context)
    }
}
