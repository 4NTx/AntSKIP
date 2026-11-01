package com.artur.antskip.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import com.artur.antskip.R
import com.artur.antskip.accessibility.AntSkipAccessibilityService
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.tile.SleepTimerTileService
import com.artur.antskip.ui.MainActivity
import kotlin.math.abs
import kotlin.math.sqrt

class SleepTimerService : Service() {
    private val preferences by lazy { PreferenceStore(this) }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private var countDownTimer: CountDownTimer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Audio Fade-out state
    private var originalMusicVolume = -1

    // Shake to Extend state
    private var sensorManager: SensorManager? = null
    private var lastShakeTime = 0L

    private val openAppIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val addIntent by lazy {
        PendingIntent.getService(
            this,
            1,
            Intent(this, SleepTimerService::class.java).apply {
                action = ACTION_ADD_TIME
                putExtra(EXTRA_DELTA_SECONDS, 5 * 60L)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val subIntent by lazy {
        PendingIntent.getService(
            this,
            2,
            Intent(this, SleepTimerService::class.java).apply {
                action = ACTION_SUBTRACT_TIME
                putExtra(EXTRA_DELTA_SECONDS, 5 * 60L)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val stopIntent by lazy {
        PendingIntent.getService(
            this,
            3,
            Intent(this, SleepTimerService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        unregisterShakeDetector()
        restoreAudioVolume()
        SleepTimerTileService.requestUpdate(this)
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

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
            if (gForce > 2.2f) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastShakeTime > 4000L) {
                    lastShakeTime = now
                    Handler(Looper.getMainLooper()).post {
                        if (isRunning) {
                            adjustTimer(10 * 60L)
                            vibrateFeedback()
                        }
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun registerShakeDetector() {
        if (!preferences.isSleepTimerShakeToExtendEnabled) return
        try {
            if (sensorManager == null) {
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            }
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null) {
                sensorManager?.registerListener(
                    shakeListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI,
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun unregisterShakeDetector() {
        try {
            sensorManager?.unregisterListener(shakeListener)
        } catch (_: Exception) {
        }
    }

    private fun vibrateFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE),
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun applyAudioFadeOut(secondsLeft: Long) {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (originalMusicVolume == -1) {
                originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
            if (originalMusicVolume > 0) {
                val factor = (secondsLeft.toFloat() / 45f).coerceIn(0f, 1f)
                val newVolume = (originalMusicVolume * factor).toInt().coerceIn(0, originalMusicVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun restoreAudioVolume() {
        try {
            if (originalMusicVolume != -1) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
                originalMusicVolume = -1
            }
        } catch (_: Exception) {
        }
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

        if (remainingSeconds > 45L) {
            restoreAudioVolume()
        }

        acquireWakeLock(remainingSeconds)
        registerShakeDetector()
        startAsForeground(remainingSeconds)
        onTickListener?.invoke(remainingSeconds)
        SleepTimerTileService.requestUpdate(this)

        countDownTimer = object : CountDownTimer(remainingSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000L) + 1
                if (preferences.isSleepTimerAudioFadeOutEnabled && remainingSeconds in 1..45) {
                    applyAudioFadeOut(remainingSeconds)
                }
                onTickListener?.invoke(remainingSeconds)
                updateNotification(remainingSeconds)
                if (remainingSeconds % 30 == 0L) {
                    SleepTimerTileService.requestUpdate(this@SleepTimerService)
                }
            }

            override fun onFinish() {
                remainingSeconds = 0L
                isRunning = false
                releaseWakeLock()
                unregisterShakeDetector()
                onTickListener?.invoke(0L)
                onFinishListener?.invoke()
                executeTimerFinishedActions()
                restoreAudioVolume()
                stopTimer()
            }
        }.start()
    }

    private fun adjustTimer(deltaSeconds: Long) {
        val newDuration = maxOf(10L, remainingSeconds + deltaSeconds)
        if (newDuration > 45L) {
            restoreAudioVolume()
        }
        startTimer(newDuration)
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        isRunning = false
        remainingSeconds = 0L
        totalDurationSeconds = 0L
        releaseWakeLock()
        unregisterShakeDetector()
        restoreAudioVolume()
        onTickListener?.invoke(0L)
        stopForeground(STOP_FOREGROUND_REMOVE)
        SleepTimerTileService.requestUpdate(this)
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

    private fun updateNotification(seconds: Long) {
        try {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(seconds))
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(seconds: Long): Notification {
        val formattedTime = formatSeconds(seconds)
        val contentText = "Pausando mídia e bloqueando em $formattedTime"

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Temporizador de sono • $formattedTime")
            .setContentText(contentText)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        // Notification action buttons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    "+5 min",
                    addIntent,
                ).build(),
            )
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    "−5 min",
                    subIntent,
                ).build(),
            )
            builder.addAction(
                Notification.Action.Builder(
                    null,
                    "Cancelar",
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
