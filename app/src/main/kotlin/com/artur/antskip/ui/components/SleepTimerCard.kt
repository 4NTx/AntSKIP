package com.artur.antskip.ui.components

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.service.SleepTimerService
import com.artur.antskip.ui.theme.AntTheme

class SleepTimerCard(
    private val context: Context,
    private val preferences: PreferenceStore,
    private val onStateChanged: () -> Unit,
) {
    private var selectedMinutes = preferences.sleepTimerDurationMinutes

    fun createView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 16), AntTheme.dp(context, 16), AntTheme.dp(context, 16))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 14f, AntTheme.CARD_BORDER, 1, context)
            layoutTransition = LayoutTransition().apply {
                enableTransitionType(LayoutTransition.CHANGING)
                setDuration(200)
            }
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 16)
            layoutParams = params

            // Header Row
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val hParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                hParams.bottomMargin = AntTheme.dp(context, 4)
                layoutParams = hParams

                val badge = AntTheme.createBadge(
                    context = context,
                    text = "SONO",
                    bgColor = AntTheme.CARD_SURFACE_HOVER,
                    textColor = AntTheme.ACCENT_PRIMARY,
                    sizeDp = 34,
                    radiusDp = 9f,
                    textSizeSp = 9.5f,
                ).apply {
                    val bParams = LinearLayout.LayoutParams(AntTheme.dp(context, 34), AntTheme.dp(context, 34))
                    bParams.marginEnd = AntTheme.dp(context, 12)
                    layoutParams = bParams
                }

                val title = TextView(context).apply {
                    text = "Temporizador de sono"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val statusBadge = TextView(context).apply {
                    tag = "timer_status_badge"
                    text = "● ATIVO"
                    setTextColor(AntTheme.SUCCESS_TEXT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rounded(AntTheme.SUCCESS_BG, 6f, AntTheme.SUCCESS_BORDER, 1, context)
                    setPadding(AntTheme.dp(context, 8), AntTheme.dp(context, 3), AntTheme.dp(context, 8), AntTheme.dp(context, 3))
                    visibility = if (SleepTimerService.isRunning) View.VISIBLE else View.GONE
                    if (SleepTimerService.isRunning) AntTheme.startPulsing(this)
                }

                addView(badge)
                addView(title)
                addView(statusBadge)
            }

            val desc = TextView(context).apply {
                text = "Pausa reproduções e bloqueia a tela para você dormir em paz."
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 0, 0, AntTheme.dp(context, 12))
            }

            addView(headerRow)
            addView(desc)

            // Dynamic content container
            val dynamicContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutTransition = LayoutTransition().apply {
                    enableTransitionType(LayoutTransition.CHANGING)
                    setDuration(180)
                }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            fun updateContent() {
                dynamicContainer.removeAllViews()
                val badge = findViewWithTag<TextView>("timer_status_badge")
                if (SleepTimerService.isRunning) {
                    badge?.visibility = View.VISIBLE
                    renderRunningState(dynamicContainer) { updateContent(); onStateChanged() }
                } else {
                    badge?.visibility = View.GONE
                    renderIdleState(dynamicContainer) { updateContent(); onStateChanged() }
                }
            }

            updateContent()

            SleepTimerService.onTickListener = { remaining ->
                (context as? android.app.Activity)?.runOnUiThread {
                    if (SleepTimerService.isRunning) {
                        val countView = dynamicContainer.findViewWithTag<TextView>("countdown_text")
                        countView?.text = SleepTimerService.formatSeconds(remaining)

                        val progressBar = dynamicContainer.findViewWithTag<View>("timer_progress_bar")
                        val total = SleepTimerService.totalDurationSeconds
                        if (progressBar != null && total > 0) {
                            val ratio = (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            val parent = progressBar.parent as? LinearLayout
                            parent?.let { p ->
                                val totalWidth = p.width
                                if (totalWidth > 0) {
                                    val lp = progressBar.layoutParams
                                    lp.width = (totalWidth * ratio).toInt()
                                    progressBar.layoutParams = lp
                                }
                            }
                        }
                    } else {
                        updateContent()
                    }
                }
            }

            SleepTimerService.onFinishListener = {
                (context as? android.app.Activity)?.runOnUiThread {
                    updateContent()
                    onStateChanged()
                }
            }

            addView(dynamicContainer)
        }

    private fun renderRunningState(container: LinearLayout, onRefresh: () -> Unit) {
        val timerDisplay = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 16), AntTheme.dp(context, 16), AntTheme.dp(context, 16))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 12f, AntTheme.ACCENT_SOFT, 1, context)

            val label = TextView(context).apply {
                text = "DESLIGANDO EM"
                setTextColor(AntTheme.TEXT_MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.08f
            }

            val countdown = TextView(context).apply {
                tag = "countdown_text"
                text = SleepTimerService.formatSeconds(SleepTimerService.remainingSeconds)
                setTextColor(AntTheme.ACCENT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
                typeface = Typeface.DEFAULT_BOLD
            }

            // Progress Bar Track
            val progressTrack = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                background = AntTheme.rounded(AntTheme.CARD_BORDER, 3f, null, 0, context)
                val trackParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 5))
                trackParams.topMargin = AntTheme.dp(context, 10)
                layoutParams = trackParams

                val progressBar = View(context).apply {
                    tag = "timer_progress_bar"
                    background = AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 3f, null, 0, context)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
                }
                addView(progressBar)
            }

            val subtext = TextView(context).apply {
                text = "Pausará vídeos e bloqueará a tela ao terminar"
                setTextColor(AntTheme.TEXT_MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(0, AntTheme.dp(context, 10), 0, 0)
            }

            addView(label)
            addView(countdown)
            addView(progressTrack)
            addView(subtext)
        }

        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            bParams.topMargin = AntTheme.dp(context, 12)
            layoutParams = bParams

            val add5 = Button(context).apply {
                text = "+5 min"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rippleButton(AntTheme.CARD_SURFACE_HOVER, radiusDp = 8f, context = context)
                val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 42), 1f)
                params.marginEnd = AntTheme.dp(context, 6)
                layoutParams = params
                AntTheme.addPressFeedback(this)
                setOnClickListener {
                    sendServiceIntent(SleepTimerService.ACTION_ADD_TIME)
                }
            }

            val sub5 = Button(context).apply {
                text = "−5 min"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rippleButton(AntTheme.CARD_SURFACE_HOVER, radiusDp = 8f, context = context)
                val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 42), 1f)
                params.marginEnd = AntTheme.dp(context, 6)
                layoutParams = params
                AntTheme.addPressFeedback(this)
                setOnClickListener {
                    sendServiceIntent(SleepTimerService.ACTION_SUBTRACT_TIME)
                }
            }

            val stopBtn = Button(context).apply {
                text = "Cancelar"
                setTextColor(AntTheme.DANGER_TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rippleButton(AntTheme.DANGER_BG, radiusDp = 8f, context = context)
                layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 42), 1.1f)
                AntTheme.addPressFeedback(this)
                setOnClickListener {
                    sendServiceIntent(SleepTimerService.ACTION_STOP)
                    onRefresh()
                }
            }

            addView(add5)
            addView(sub5)
            addView(stopBtn)
        }

        container.addView(timerDisplay)
        container.addView(buttonsRow)
    }

    private fun renderIdleState(container: LinearLayout, onRefresh: () -> Unit) {
        val presets = listOf(10, 15, 30, 45, 60)
        val chipViews = mutableListOf<TextView>()

        // Stepper: [- 5m]  30 min  [+ 5m]
        val stepperRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(AntTheme.dp(context, 10), AntTheme.dp(context, 6), AntTheme.dp(context, 10), AntTheme.dp(context, 6))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)
            val sParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            sParams.bottomMargin = AntTheme.dp(context, 10)
            layoutParams = sParams
        }

        val startBtn = Button(context).apply {
            text = "Iniciar Temporizador ($selectedMinutes min)"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rippleGradientButton(
                startColor = AntTheme.ACCENT_GRADIENT_START,
                endColor = AntTheme.ACCENT_GRADIENT_END,
                radiusDp = 10f,
                context = context,
            )
            val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 44))
            bParams.topMargin = AntTheme.dp(context, 12)
            layoutParams = bParams
            AntTheme.addPressFeedback(this)
            setOnClickListener {
                startSleepTimer(selectedMinutes * 60L)
                onRefresh()
            }
        }

        val timeLabel = TextView(context).apply {
            text = "$selectedMinutes min"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        fun updateSelectedTime(min: Int) {
            selectedMinutes = min.coerceIn(1, 180)
            preferences.setSleepTimerDurationMinutes(selectedMinutes)
            timeLabel.text = "$selectedMinutes min"
            timeLabel.scaleX = 0.85f
            timeLabel.scaleY = 0.85f
            timeLabel.animate().scaleX(1f).scaleY(1f).setDuration(120).setInterpolator(OvershootInterpolator(1.5f)).start()
            startBtn.text = "Iniciar Temporizador ($selectedMinutes min)"

            chipViews.forEachIndexed { idx, cv ->
                val sel = presets.getOrNull(idx) == selectedMinutes
                cv.setTextColor(if (sel) AntTheme.TEXT_PRIMARY else AntTheme.TEXT_SECONDARY)
                cv.background = if (sel) {
                    AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 8f, null, 0, context)
                } else {
                    AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
                }
            }
        }

        val minusBtn = Button(context).apply {
            text = "− 5m"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 8f, context = context)
            layoutParams = LinearLayout.LayoutParams(AntTheme.dp(context, 62), AntTheme.dp(context, 36))
            AntTheme.addPressFeedback(this)
            setOnClickListener { updateSelectedTime(selectedMinutes - 5) }
        }

        val plusBtn = Button(context).apply {
            text = "+ 5m"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 8f, context = context)
            layoutParams = LinearLayout.LayoutParams(AntTheme.dp(context, 62), AntTheme.dp(context, 36))
            AntTheme.addPressFeedback(this)
            setOnClickListener { updateSelectedTime(selectedMinutes + 5) }
        }

        stepperRow.addView(minusBtn)
        stepperRow.addView(timeLabel)
        stepperRow.addView(plusBtn)

        // Presets chips
        val chipsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val cParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            cParams.bottomMargin = AntTheme.dp(context, 12)
            layoutParams = cParams
        }

        presets.forEach { min ->
            val isSelected = min == selectedMinutes
            val chip = TextView(context).apply {
                text = "${min}m"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, AntTheme.dp(context, 7), 0, AntTheme.dp(context, 7))
                setTextColor(if (isSelected) AntTheme.TEXT_PRIMARY else AntTheme.TEXT_SECONDARY)
                background = if (isSelected) {
                    AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 8f, null, 0, context)
                } else {
                    AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
                }
                val p = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                p.marginEnd = AntTheme.dp(context, 5)
                layoutParams = p

                AntTheme.addPressFeedback(this)
                setOnClickListener { updateSelectedTime(min) }
            }
            chipViews.add(chip)
            chipsRow.addView(chip)
        }

        // Toggles Container
        val optionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 14), AntTheme.dp(context, 6), AntTheme.dp(context, 14), AntTheme.dp(context, 6))
        }

        // Toggle: Pausar mídia
        val pauseRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 6))

            val label = TextView(context).apply {
                text = "Pausar vídeos e músicas ao terminar"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isSleepTimerPauseMediaEnabled
                setOnCheckedChangeListener { _, checked ->
                    preferences.setSleepTimerPauseMediaEnabled(checked)
                }
            }

            addView(label)
            addView(sw)
            setOnClickListener { sw.toggle() }
        }

        // Toggle: Bloquear tela
        val screenRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 6))

            val label = TextView(context).apply {
                text = "Bloquear tela ao terminar"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isSleepTimerTurnOffScreenEnabled
                setOnCheckedChangeListener { _, checked ->
                    preferences.setSleepTimerTurnOffScreenEnabled(checked)
                }
            }

            addView(label)
            addView(sw)
            setOnClickListener { sw.toggle() }
        }

        optionsLayout.addView(pauseRow)
        optionsLayout.addView(AntTheme.divider(context))
        optionsLayout.addView(screenRow)

        val note = TextView(context).apply {
            text = "Alarmes e despertadores nunca são silenciados ou pausados."
            setTextColor(AntTheme.TEXT_MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(0, AntTheme.dp(context, 10), 0, 0)
        }

        container.addView(stepperRow)
        container.addView(chipsRow)
        container.addView(optionsLayout)
        container.addView(startBtn)
        container.addView(note)
    }

    private fun startSleepTimer(durationSeconds: Long) {
        val intent = Intent(context, SleepTimerService::class.java).apply {
            action = SleepTimerService.ACTION_START
            putExtra(SleepTimerService.EXTRA_DURATION_SECONDS, durationSeconds)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun sendServiceIntent(action: String) {
        val intent = Intent(context, SleepTimerService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
