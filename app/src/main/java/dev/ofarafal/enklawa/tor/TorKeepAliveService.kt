package dev.ofarafal.enklawa.tor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.ofarafal.enklawa.MainActivity

/**
 * Usługa pierwszoplanowa: sama nic nie robi, tylko podnosi priorytet procesu, żeby Android nie ubijał
 * demona Tora, gdy ekran jest wyłączony albo aplikacja w tle. Powiadomienie jest ogólne i niewidoczne na ekranie blokady.
 */
class TorKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Połączenie z Torem", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Utrzymuje Tora przy życiu, gdy aplikacja jest w tle"
                    lockscreenVisibility = Notification.VISIBILITY_SECRET
                    setShowBadge(false)
                },
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Microcosm")
            .setContentText("Tor działa w tle")
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(open)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        runCatching { ServiceCompat.startForeground(this, 1, n, type) }.onFailure { stopSelf() }
        return START_NOT_STICKY // po ubiciu procesu świat i tak jest zamknięty, więc nie wracamy sami
    }

    companion object {
        private const val CHANNEL = "tor_keepalive"
    }
}
