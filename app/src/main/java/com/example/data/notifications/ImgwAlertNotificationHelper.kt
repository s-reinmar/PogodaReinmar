package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.WeatherWarning

interface AlertNotificationGateway {
    fun hasNotificationPermission(): Boolean
    fun postImgwAlertNotification(
        cityName: String,
        warning: WeatherWarning,
        totalActiveCount: Int,
    ): Boolean
}

/**
 * Posts Android system notifications for active IMGW-PIB meteorological warnings,
 * respecting runtime POST_NOTIFICATIONS permission on Android 13+ (API 33+).
 */
class AndroidImgwAlertNotifier(
    private val context: Context,
) : AlertNotificationGateway {

    override fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun postImgwAlertNotification(
        cityName: String,
        warning: WeatherWarning,
        totalActiveCount: Int,
    ): Boolean {
        if (!hasNotificationPermission()) return false
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        ensureChannelCreated()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = context.getString(
            R.string.notification_imgw_title,
            warning.level,
            cityName,
            warning.eventName,
        )
        val summary = if (totalActiveCount > 1) {
            context.getString(
                R.string.notification_imgw_multi_summary,
                warning.validFrom,
                warning.validTo,
                totalActiveCount - 1,
            )
        } else {
            context.getString(
                R.string.warning_validity,
                warning.validFrom,
                warning.validTo,
            )
        }

        val bigText = buildString {
            append(summary)
            if (warning.description.isNotBlank()) {
                append("\n")
                append(warning.description)
            }
        }

        val priority = when (warning.level) {
            3 -> NotificationCompat.PRIORITY_MAX
            2 -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            val notificationId = BASE_NOTIFICATION_ID + (warning.id.hashCode() and 0x0FFF)
            manager.notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    private fun ensureChannelCreated() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val existing = systemManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_imgw_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_imgw_desc)
            enableVibration(true)
        }
        systemManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "imgw_weather_alerts"
        private const val NOTIFICATION_REQUEST_CODE = 4201
        private const val BASE_NOTIFICATION_ID = 7100
    }
}
