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
            setPadding(AntTheme.dp(context, 18), AntTheme.dp(context, 16), AntTheme.dp(context, 18), AntTheme.dp(context, 16))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 16)
            layoutParams = params

            // Header Row
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val icon = TextView(context).apply {
                    text = "🌙"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setPadding(0, 0, AntTheme.dp(context, 8), 0)
                }

                val title = TextView(context).apply {
                    text = "Temporizador de Sono"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val liveBadge = AntTheme.badge(
                    context,
                    if (SleepTimerService.isRunning) "Ativo" else "Parado",
                    if (SleepTimerService.isRunning) AntTheme.SUCCESS_TEXT else AntTheme.TEXT_MUTED,
                    if (SleepTimerService.isRunning) AntTheme.SUCCESS_BG else AntTheme.BG_CANVAS,
                    if (SleepTimerService.isRunning) AntTheme.SUCCESS_BORDER else AntTheme.CARD_BORDER,
                )

                addView(icon)
                addView(title)
                addView(liveBadge)
            }

            val desc = TextView(context).apply {
                text = "Pausa vídeos ou músicas e desliga a tela do celular após o tempo escolhido. Seus alarmes continuam funcionando normalmente."
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 12))
            }

            addView(headerRow)
            addView(desc)

            // Content container: switch between running mode and idle mode
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

            // Register live tick listener
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

            // Alarm Safety Reassurance
            val safetyNote = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(AntTheme.dp(context, 10), AntTheme.dp(context, 8), AntTheme.dp(context, 10), AntTheme.dp(context, 8))
                background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
                val sParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                sParams.topMargin = AntTheme.dp(context, 12)
                layoutParams = sParams

                val shield = TextView(context).apply {
                    text = "🛡️"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, 0, AntTheme.dp(context, 6), 0)
                }

                val noteText = TextView(context).apply {
                    text = "Proteção de Alarmes: O despertador e alarmes do sistema nunca são afetados ou silenciados."
                    setTextColor(AntTheme.SUCCESS_TEXT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    typeface = Typeface.DEFAULT_BOLD
                }

                addView(shield)
                addView(noteText)
            }

            addView(safetyNote)
        }

    private fun renderRunningState(container: LinearLayout, onRefresh: () -> Unit) {
        val timerDisplay = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, AntTheme.dp(context, 14), 0, AntTheme.dp(context, 14))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 12f, AntTheme.CARD_BORDER, 1, context)

            val label = TextView(context).apply {
                text = "Desligando em:"
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
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
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rounded(AntTheme.DANGER_BG, 8f, AntTheme.DANGER_BORDER, 1, context)
                layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 40), 1.2f)
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
        // Quick preset chips row
        val chipsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val cParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            cParams.bottomMargin = AntTheme.dp(context, 10)
            layoutParams = cParams
        }

        val presets = listOf(10, 15, 30, 45, 60)
        val chipViews = mutableListOf<TextView>()

        val startBtn = Button(context).apply {
            text = "Iniciar Temporizador ($selectedMinutes min)"
            setTextColor(AntTheme.TEXT_INVERSE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rippleButton(AntTheme.ACCENT_PRIMARY, radiusDp = 10f, context = context)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 44))
            setOnClickListener {
                startSleepTimer(selectedMinutes * 60L)
                onRefresh()
            }
        }

        presets.forEach { min ->
            val isSelected = min == selectedMinutes
            val chip = TextView(context).apply {
                text = "${min}m"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, AntTheme.dp(context, 8), 0, AntTheme.dp(context, 8))
                setTextColor(if (isSelected) AntTheme.TEXT_INVERSE else AntTheme.TEXT_SECONDARY)
                background = if (isSelected) {
                    AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 8f, null, 0, context)
                } else {
                    AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
                }
                val p = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                p.marginEnd = AntTheme.dp(context, 4)
                layoutParams = p

                setOnClickListener {
                    selectedMinutes = min
                    preferences.setSleepTimerDurationMinutes(min)
                    chipViews.forEachIndexed { idx, cv ->
                        val sel = presets[idx] == selectedMinutes
                        cv.setTextColor(if (sel) AntTheme.TEXT_INVERSE else AntTheme.TEXT_SECONDARY)
                        cv.background = if (sel) {
                            AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 8f, null, 0, context)
                        } else {
                            AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
                        }
                    }
                    startBtn.text = "Iniciar Temporizador ($selectedMinutes min)"
                }
            }
            chipViews.add(chip)
            chipsRow.addView(chip)
        }

        // Toggles
        val optionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val oParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            oParams.bottomMargin = AntTheme.dp(context, 10)
            layoutParams = oParams

            // Toggle: Pause Media
            val pauseRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, AntTheme.dp(context, 4), 0, AntTheme.dp(context, 4))

                val label = TextView(context).apply {
                    text = "Pausar vídeos e músicas ao finalizar"
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

            // Toggle: Turn off screen
            val screenRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, AntTheme.dp(context, 4), 0, AntTheme.dp(context, 4))

                val label = TextView(context).apply {
                    text = "Desligar / Bloquear tela ao finalizar"
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

            addView(pauseRow)
            addView(screenRow)
        }

        container.addView(chipsRow)
        container.addView(optionsLayout)
        container.addView(startBtn)
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
        context.startService(intent)
    }
}
