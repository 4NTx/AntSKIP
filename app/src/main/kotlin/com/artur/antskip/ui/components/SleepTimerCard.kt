package com.artur.antskip.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
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
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 14), AntTheme.dp(context, 16), AntTheme.dp(context, 14))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 12f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 14)
            layoutParams = params

            // Header Row
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val title = TextView(context).apply {
                    text = "Temporizador de sono"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val statusBadge = TextView(context).apply {
                    text = if (SleepTimerService.isRunning) "Ativo" else ""
                    setTextColor(AntTheme.SUCCESS_TEXT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.DEFAULT_BOLD
                    visibility = if (SleepTimerService.isRunning) TextView.VISIBLE else TextView.GONE
                }

                addView(title)
                addView(statusBadge)
            }

            val desc = TextView(context).apply {
                text = "Pausa reproduções e bloqueia a tela após o tempo escolhido."
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding(0, AntTheme.dp(context, 4), 0, AntTheme.dp(context, 12))
            }

            addView(headerRow)
            addView(desc)

            // Dynamic content container
            val dynamicContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            fun updateContent() {
                dynamicContainer.removeAllViews()
                if (SleepTimerService.isRunning) {
                    renderRunningState(dynamicContainer, { updateContent(); onStateChanged() })
                } else {
                    renderIdleState(dynamicContainer, { updateContent(); onStateChanged() })
                }
            }

            updateContent()

            SleepTimerService.onTickListener = {
                (context as? android.app.Activity)?.runOnUiThread {
                    if (SleepTimerService.isRunning) {
                        val countView = dynamicContainer.findViewWithTag<TextView>("countdown_text")
                        countView?.text = SleepTimerService.formatSeconds(it)
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
            setPadding(0, AntTheme.dp(context, 14), 0, AntTheme.dp(context, 14))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)

            val label = TextView(context).apply {
                text = "Desligando em"
                setTextColor(AntTheme.TEXT_MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            }

            val countdown = TextView(context).apply {
                tag = "countdown_text"
                text = SleepTimerService.formatSeconds(SleepTimerService.remainingSeconds)
                setTextColor(AntTheme.ACCENT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 36f)
                typeface = Typeface.DEFAULT_BOLD
            }

            addView(label)
            addView(countdown)
        }

        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            bParams.topMargin = AntTheme.dp(context, 10)
            layoutParams = bParams

            val add5 = Button(context).apply {
                text = "+5 min"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rounded(AntTheme.CARD_SURFACE_HOVER, 8f, AntTheme.CARD_BORDER, 1, context)
                val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 40), 1f)
                params.marginEnd = AntTheme.dp(context, 6)
                layoutParams = params
                setOnClickListener {
                    sendServiceIntent(SleepTimerService.ACTION_ADD_TIME)
                }
            }

            val sub5 = Button(context).apply {
                text = "-5 min"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rounded(AntTheme.CARD_SURFACE_HOVER, 8f, AntTheme.CARD_BORDER, 1, context)
                val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 40), 1f)
                params.marginEnd = AntTheme.dp(context, 6)
                layoutParams = params
                setOnClickListener {
                    sendServiceIntent(SleepTimerService.ACTION_SUBTRACT_TIME)
                }
            }

            val stopBtn = Button(context).apply {
                text = "Cancelar"
                setTextColor(AntTheme.DANGER_TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rounded(AntTheme.DANGER_BG, 8f, AntTheme.DANGER_BORDER, 1, context)
                layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 40), 1.1f)
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

        // Stepper: [-]  15 min  [+]
        val stepperRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(AntTheme.dp(context, 8), AntTheme.dp(context, 4), AntTheme.dp(context, 8), AntTheme.dp(context, 4))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
            val sParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            sParams.bottomMargin = AntTheme.dp(context, 8)
            layoutParams = sParams
        }

        val startBtn = Button(context).apply {
            text = "Iniciar ($selectedMinutes min)"
            setTextColor(AntTheme.TEXT_INVERSE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rippleButton(AntTheme.ACCENT_PRIMARY, radiusDp = 8f, context = context)
            val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 42))
            bParams.topMargin = AntTheme.dp(context, 10)
            layoutParams = bParams
            setOnClickListener {
                startSleepTimer(selectedMinutes * 60L)
                onRefresh()
            }
        }

        val timeLabel = TextView(context).apply {
            text = "$selectedMinutes min"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        fun updateSelectedTime(min: Int) {
            selectedMinutes = min.coerceIn(1, 180)
            preferences.setSleepTimerDurationMinutes(selectedMinutes)
            timeLabel.text = "$selectedMinutes min"
            startBtn.text = "Iniciar ($selectedMinutes min)"

            chipViews.forEachIndexed { idx, cv ->
                val sel = presets.getOrNull(idx) == selectedMinutes
                cv.setTextColor(if (sel) AntTheme.TEXT_INVERSE else AntTheme.TEXT_SECONDARY)
                cv.background = if (sel) {
                    AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 6f, null, 0, context)
                } else {
                    AntTheme.rounded(AntTheme.BG_CANVAS, 6f, AntTheme.CARD_BORDER, 1, context)
                }
            }
        }

        val minusBtn = Button(context).apply {
            text = "− 5m"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 6f, AntTheme.CARD_BORDER, 1, context)
            layoutParams = LinearLayout.LayoutParams(AntTheme.dp(context, 60), AntTheme.dp(context, 34))
            setOnClickListener { updateSelectedTime(selectedMinutes - 5) }
        }

        val plusBtn = Button(context).apply {
            text = "+ 5m"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 6f, AntTheme.CARD_BORDER, 1, context)
            layoutParams = LinearLayout.LayoutParams(AntTheme.dp(context, 60), AntTheme.dp(context, 34))
            setOnClickListener { updateSelectedTime(selectedMinutes + 5) }
        }

        stepperRow.addView(minusBtn)
        stepperRow.addView(timeLabel)
        stepperRow.addView(plusBtn)

        // Presets chips
        val chipsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val cParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            cParams.bottomMargin = AntTheme.dp(context, 10)
            layoutParams = cParams
        }

        presets.forEach { min ->
            val isSelected = min == selectedMinutes
            val chip = TextView(context).apply {
                text = "${min}m"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 6))
                setTextColor(if (isSelected) AntTheme.TEXT_INVERSE else AntTheme.TEXT_SECONDARY)
                background = if (isSelected) {
                    AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 6f, null, 0, context)
                } else {
                    AntTheme.rounded(AntTheme.BG_CANVAS, 6f, AntTheme.CARD_BORDER, 1, context)
                }
                val p = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                p.marginEnd = AntTheme.dp(context, 4)
                layoutParams = p

                setOnClickListener { updateSelectedTime(min) }
            }
            chipViews.add(chip)
            chipsRow.addView(chip)
        }

        // Toggles
        val optionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Toggle: Pausar mídia
        val pauseRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, AntTheme.dp(context, 4), 0, AntTheme.dp(context, 4))

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
        }

        // Toggle: Bloquear tela
        val screenRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, AntTheme.dp(context, 4), 0, AntTheme.dp(context, 4))

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
        }

        optionsLayout.addView(pauseRow)
        optionsLayout.addView(screenRow)

        val note = TextView(context).apply {
            text = "Alarmes e despertadores não são afetados."
            setTextColor(AntTheme.TEXT_MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, AntTheme.dp(context, 6), 0, 0)
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
