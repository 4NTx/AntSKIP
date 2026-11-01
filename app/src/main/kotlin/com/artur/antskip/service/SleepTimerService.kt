package com.artur.antskip.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.KeyEvent
import com.artur.antskip.R
import com.artur.antskip.accessibility.AntSkipAccessibilityService
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.ui.MainActivity

class SleepTimerService : Service() {
    private val preferences by lazy { PreferenceStore(this) }
    private var countDownTimer: CountDownTimer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationSec = intent.getLongExtra(EXTRA_DURATION_SECONDS, 15 * 60L)
                startTimer(durationSec)
            }
            ACTION_ADD_TIME -> {
                val addSec = intent.getLongExtra(EXTRA_DELTA_SECONDS, 5 * 60L)
                adjustTimer(addSec)
            }
            ACTION_SUBTRACT_TIME -> {
                val subSec = intent.getLongExtra(EXTRA_DELTA_SECONDS, 5 * 60L)
                adjustTimer(-subSec)
            }
            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLock(seconds: Long) {
        releaseWakeLock()
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AntSKIP:SleepTimerWakeLock",
            ).apply {
                setReferenceCounted(false)
                acquire((seconds * 1000L) + 15_000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    private fun startAsForeground(seconds: Long) {
        val notification = buildNotification(seconds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                0,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer(seconds: Long) {
        countDownTimer?.cancel()
        remainingSeconds = seconds
        if (totalDurationSeconds <= 0L || seconds > totalDurationSeconds) {
            totalDurationSeconds = seconds
        }
        isRunning = true

        acquireWakeLock(remainingSeconds)
        startAsForeground(remainingSeconds)
        onTickListener?.invoke(remainingSeconds)

        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000L) + 1
                onTickListener?.invoke(remainingSeconds)
            }

            override fun onFinish() {
                remainingSeconds = 0L
                isRunning = false
                releaseWakeLock()
                onTickListener?.invoke(0L)
                onFinishListener?.invoke()
                executeTimerFinishedActions()
                stopTimer()
            }
        }.start()
    }

    private fun adjustTimer(deltaSeconds: Long) {
        val newDuration = maxOf(10L, remainingSeconds + deltaSeconds)
        startTimer(newDuration)
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isRunning = false
        remainingSeconds = 0L
        totalDurationSeconds = 0L
        releaseWakeLock()
        onTickListener?.invoke(0L)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun executeTimerFinishedActions() {
        // 1. Pause media if enabled (SAFELY: Alarms are NEVER touched!)
        if (preferences.isSleepTimerPauseMediaEnabled) {
            pauseActiveMedia()
        }

        // 2. Turn off / Lock screen if enabled
        if (preferences.isSleepTimerTurnOffScreenEnabled) {
            AntSkipAccessibilityService.instance?.lockScreen()
        }
    }

    /**
     * Pauses media playback universally across Spotify, YouTube, Netflix, podcasts, etc.
     * GUARANTEE: Does NOT silence, mute, or affect STREAM_ALARM in any way.
     */
    private fun pauseActiveMedia() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        // 1. Dispatch standard MEDIA_PAUSE key events
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)

        // 2. Request transient audio focus exclusively on USAGE_MEDIA / STREAM_MUSIC
        // This prompts video and music players to immediately halt playback.
        // STREAM_ALARM is completely isolated and will continue firing normally!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .build()
            audioManager.requestAudioFocus(focusRequest)

            // Abandon after a short moment so playback can be resumed anytime later
            Handler(Looper.getMainLooper()).postDelayed({
                audioManager.abandonAudioFocusRequest(focusRequest)
            }, 800)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.sleep_timer_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.sleep_timer_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(seconds: Long): Notification {
        val formattedTime = formatSeconds(seconds)
        val contentText = "Pausando mídia e desligando em $formattedTime"

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val addIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, SleepTimerService::class.java).apply {
                action = ACTION_ADD_TIME
                putExtra(EXTRA_DELTA_SECONDS, 5 * 60L)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val subIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, SleepTimerService::class.java).apply {
                action = ACTION_SUBTRACT_TIME
                putExtra(EXTRA_DELTA_SECONDS, 5 * 60L)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, SleepTimerService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val stopTimeMs = System.currentTimeMillis() + (seconds * 1000L)

        builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.sleep_timer_notification_title))
            .setContentText(contentText)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(stopTimeMs)
            .setShowWhen(true)
            .setUsesChronometer(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }

        // Notification action buttons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.action_add_five_min),
                    addIntent,
                ).build(),
            )
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.action_sub_five_min),
                    subIntent,
                ).build(),
            )
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.action_stop_timer),
                    stopIntent,
                ).build(),
            )
        }

        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "ant_skip_sleep_timer"
        const val NOTIFICATION_ID = 4040

        const val ACTION_START = "com.artur.antskip.ACTION_START_SLEEP_TIMER"
        const val ACTION_STOP = "com.artur.antskip.ACTION_STOP_SLEEP_TIMER"
        const val ACTION_ADD_TIME = "com.artur.antskip.ACTION_ADD_TIME"
        const val ACTION_SUBTRACT_TIME = "com.artur.antskip.ACTION_SUBTRACT_TIME"

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_DELTA_SECONDS = "extra_delta_seconds"

        var remainingSeconds: Long = 0L
            private set
        var totalDurationSeconds: Long = 0L
            private set
        var isRunning: Boolean = false
            private set

        var onTickListener: ((Long) -> Unit)? = null
        var onFinishListener: (() -> Unit)? = null

        fun formatSeconds(totalSec: Long): String {
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%02d:%02d".format(min, sec)
        }
    }
}
