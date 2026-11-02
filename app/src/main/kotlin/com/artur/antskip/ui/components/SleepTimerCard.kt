package com.artur.antskip.ui.components

import android.animation.LayoutTransition
import android.app.TimePickerDialog
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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.schedule.ScheduleManager
import com.artur.antskip.service.SleepTimerService
import com.artur.antskip.ui.theme.AntTheme
import java.util.Calendar

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
                    text = "Ativo"
                    setTextColor(AntTheme.SUCCESS_TEXT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rounded(AntTheme.SUCCESS_BG, 6f, AntTheme.SUCCESS_BORDER, 1, context)
                    setPadding(AntTheme.dp(context, 8), AntTheme.dp(context, 3), AntTheme.dp(context, 8), AntTheme.dp(context, 3))
                    visibility = if (SleepTimerService.isRunning) View.VISIBLE else View.GONE
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
        val presets = listOf(15, 30, 45, 60, 90, 120)
        val chipViews = mutableListOf<TextView>()

        fun formatDurationLabel(min: Int): String {
            val h = min / 60
            val m = min % 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h} hora${if (h > 1) "s" else ""}"
                else -> "$min min"
            }
        }

        fun formatDurationShort(min: Int): String {
            val h = min / 60
            val m = min % 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "$min min"
            }
        }

        fun formatChipLabel(min: Int): String {
            val h = min / 60
            val m = min % 60
            return when {
                h > 0 && m > 0 -> "${h}h${m}"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }

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
            text = "Iniciar Temporizador (${formatDurationShort(selectedMinutes)})"
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
            text = formatDurationLabel(selectedMinutes)
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        fun updateSelectedTime(min: Int) {
            selectedMinutes = min.coerceIn(1, 180)
            preferences.setSleepTimerDurationMinutes(selectedMinutes)
            timeLabel.text = formatDurationLabel(selectedMinutes)
            timeLabel.scaleX = 0.85f
            timeLabel.scaleY = 0.85f
            timeLabel.animate().scaleX(1f).scaleY(1f).setDuration(120).setInterpolator(OvershootInterpolator(1.5f)).start()
            startBtn.text = "Iniciar Temporizador (${formatDurationShort(selectedMinutes)})"

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
                text = formatChipLabel(min)
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

        // Toggle: Redução gradual de volume (Fade-out)
        val fadeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 6))

            val labelLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val label = TextView(context).apply {
                    text = "Redução gradual de volume (Fade-out)"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                }
                val sub = TextView(context).apply {
                    text = "Abaixa nos últimos 45s para não assustar"
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                }
                addView(label)
                addView(sub)
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isSleepTimerAudioFadeOutEnabled
                setOnCheckedChangeListener { _, checked ->
                    preferences.setSleepTimerAudioFadeOutEnabled(checked)
                }
            }

            addView(labelLayout)
            addView(sw)
            setOnClickListener { sw.toggle() }
        }

        // Toggle: Balançar celular para estender
        val shakeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 6))

            val labelLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val label = TextView(context).apply {
                    text = "Balançar para estender (+10 min)"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                }
                val sub = TextView(context).apply {
                    text = "Chacoalhe o celular nos últimos 5 min se ainda estiver acordado"
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                }
                addView(label)
                addView(sub)
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isSleepTimerShakeToExtendEnabled
                setOnCheckedChangeListener { _, checked ->
                    preferences.setSleepTimerShakeToExtendEnabled(checked)
                }
            }

            addView(labelLayout)
            addView(sw)
            setOnClickListener { sw.toggle() }
        }

        // Toggle: Iniciar automaticamente em horário programado
        val autoStartRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 6))

            val labelLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val label = TextView(context).apply {
                    text = "Iniciar sozinho em horário programado"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                }
                val formattedTime = "%02d:%02d".format(preferences.sleepRoutineHour, preferences.sleepRoutineMinute)
                val sub = TextView(context).apply {
                    text = if (preferences.isSleepRoutineEnabled) {
                        "Ativo: todo dia às $formattedTime"
                    } else {
                        "Inicia o timer de sono automaticamente sem precisar abrir o app"
                    }
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                }
                addView(label)
                addView(sub)
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isSleepRoutineEnabled
                setOnCheckedChangeListener { _, checked ->
                    preferences.setSleepRoutineEnabled(checked)
                    ScheduleManager.syncSleepRoutineAlarm(context, preferences)
                    onRefresh()
                }
            }

            addView(labelLayout)
            addView(sw)
            setOnClickListener { sw.toggle() }
        }

        optionsLayout.addView(pauseRow)
        optionsLayout.addView(AntTheme.divider(context))
        optionsLayout.addView(screenRow)
        optionsLayout.addView(AntTheme.divider(context))
        optionsLayout.addView(fadeRow)
        optionsLayout.addView(AntTheme.divider(context))
        optionsLayout.addView(shakeRow)
        optionsLayout.addView(AntTheme.divider(context))
        optionsLayout.addView(autoStartRow)

        if (preferences.isSleepRoutineEnabled) {
            // Container for filters, time, days
            val scheduleDetailLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, AntTheme.dp(context, 6), 0, 0)
                layoutTransition = LayoutTransition().apply {
                    enableTransitionType(LayoutTransition.CHANGING)
                    setDuration(160)
                }
            }

            // Quick filter chips
            val presetsScroll = HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                val pParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                pParams.topMargin = AntTheme.dp(context, 6)
                pParams.bottomMargin = AntTheme.dp(context, 8)
                layoutParams = pParams
            }

            val presetsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            data class PresetItem(val id: String, val label: String, val hour: Int, val minute: Int, val days: Set<Int>)

            val presetList = listOf(
                PresetItem("EVERYDAY", "Todo dia (23:30)", 23, 30, setOf(1, 2, 3, 4, 5, 6, 7)),
                PresetItem("WEEKDAYS", "Dias Úteis (Seg-Sex)", 23, 0, setOf(2, 3, 4, 5, 6)),
                PresetItem("WEEKENDS", "Fins de Semana (Sáb-Dom)", 0, 30, setOf(1, 7)),
                PresetItem("CUSTOM", "Personalizado", preferences.sleepRoutineHour, preferences.sleepRoutineMinute, preferences.sleepRoutineDays),
            )

            val currentPreset = preferences.sleepRoutinePreset

            presetList.forEach { preset ->
                val isSelected = currentPreset == preset.id
                val chip = TextView(context).apply {
                    text = preset.label
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 6), AntTheme.dp(context, 12), AntTheme.dp(context, 6))
                    setTextColor(if (isSelected) AntTheme.TEXT_PRIMARY else AntTheme.TEXT_SECONDARY)
                    background = if (isSelected) {
                        AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 8f, null, 0, context)
                    } else {
                        AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
                    }
                    val cParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    cParams.marginEnd = AntTheme.dp(context, 6)
                    layoutParams = cParams
                    AntTheme.addPressFeedback(this)
                    setOnClickListener {
                        preferences.setSleepRoutinePreset(preset.id)
                        if (preset.id != "CUSTOM") {
                            preferences.setSleepRoutineHour(preset.hour)
                            preferences.setSleepRoutineMinute(preset.minute)
                            preferences.setSleepRoutineDays(preset.days)
                        }
                        ScheduleManager.syncSleepRoutineAlarm(context, preferences)
                        onRefresh()
                    }
                }
                presetsRow.addView(chip)
            }
            presetsScroll.addView(presetsRow)
            scheduleDetailLayout.addView(presetsScroll)

            // Time Picker Button
            val formattedTime = "%02d:%02d".format(preferences.sleepRoutineHour, preferences.sleepRoutineMinute)
            val timePickerBtn = Button(context).apply {
                text = "Horário de início: $formattedTime  (Toque para mudar)"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 8f, context = context)
                val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 38))
                bParams.bottomMargin = AntTheme.dp(context, 8)
                layoutParams = bParams
                AntTheme.addPressFeedback(this)
                setOnClickListener {
                    TimePickerDialog(
                        context,
                        android.R.style.Theme_DeviceDefault_Dialog,
                        { _, hour, minute ->
                            preferences.setSleepRoutineHour(hour)
                            preferences.setSleepRoutineMinute(minute)
                            preferences.setSleepRoutinePreset("CUSTOM")
                            ScheduleManager.syncSleepRoutineAlarm(context, preferences)
                            onRefresh()
                        },
                        preferences.sleepRoutineHour,
                        preferences.sleepRoutineMinute,
                        true,
                    ).show()
                }
            }
            scheduleDetailLayout.addView(timePickerBtn)

            // Day Selector Chips
            val daysRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val dayNames = listOf(
                Calendar.SUNDAY to "D",
                Calendar.MONDAY to "S",
                Calendar.TUESDAY to "T",
                Calendar.WEDNESDAY to "Q",
                Calendar.THURSDAY to "Q",
                Calendar.FRIDAY to "S",
                Calendar.SATURDAY to "S",
            )

            val selectedDays = preferences.sleepRoutineDays.toMutableSet()

            dayNames.forEach { (calDay, dayLetter) ->
                val isDaySelected = calDay in selectedDays
                val dayBtn = TextView(context).apply {
                    text = dayLetter
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(if (isDaySelected) AntTheme.TEXT_PRIMARY else AntTheme.TEXT_MUTED)
                    background = if (isDaySelected) {
                        AntTheme.rounded(AntTheme.ACCENT_PRIMARY, 8f, null, 0, context)
                    } else {
                        AntTheme.rounded(AntTheme.CARD_SURFACE, 8f, AntTheme.CARD_BORDER, 1, context)
                    }
                    val dParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 32), 1f)
                    dParams.marginEnd = AntTheme.dp(context, 4)
                    layoutParams = dParams
                    AntTheme.addPressFeedback(this)
                    setOnClickListener {
                        if (isDaySelected && selectedDays.size > 1) {
                            selectedDays.remove(calDay)
                        } else {
                            selectedDays.add(calDay)
                        }
                        preferences.setSleepRoutineDays(selectedDays)
                        preferences.setSleepRoutinePreset("CUSTOM")
                        ScheduleManager.syncSleepRoutineAlarm(context, preferences)
                        onRefresh()
                    }
                }
                daysRow.addView(dayBtn)
            }
            scheduleDetailLayout.addView(daysRow)

            // Summary Label
            val summaryLabel = TextView(context).apply {
                val days = preferences.sleepRoutineDays
                val daysDesc = when {
                    days.size == 7 -> "Todos os dias"
                    days == setOf(2, 3, 4, 5, 6) -> "Segunda a sexta"
                    days == setOf(1, 7) -> "Fins de semana"
                    else -> "${days.size} dias na semana"
                }
                val durationStr = formatDurationShort(selectedMinutes)
                text = "Iniciará sozinho às $formattedTime • $daysDesc ($durationStr)"
                setTextColor(AntTheme.ACCENT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, AntTheme.dp(context, 8), 0, 0)
            }
            scheduleDetailLayout.addView(summaryLabel)

            optionsLayout.addView(scheduleDetailLayout)
        }

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
        if (action == SleepTimerService.ACTION_STOP) {
            context.startService(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
