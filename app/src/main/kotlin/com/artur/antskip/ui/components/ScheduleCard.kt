package com.artur.antskip.ui.components

import android.animation.LayoutTransition
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.schedule.ScheduleManager
import com.artur.antskip.ui.theme.AntTheme
import java.util.Calendar

class ScheduleCard(
    private val context: Context,
    private val preferences: PreferenceStore,
    private val onStateChanged: () -> Unit,
) {
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

            // 1. Header Row
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val hParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                hParams.bottomMargin = AntTheme.dp(context, 4)
                layoutParams = hParams

                val badge = AntTheme.createBadge(
                    context = context,
                    text = "HORÁRIO",
                    bgColor = AntTheme.CARD_SURFACE_HOVER,
                    textColor = AntTheme.ACCENT_PRIMARY,
                    sizeDp = 34,
                    radiusDp = 9f,
                    textSizeSp = 8.5f,
                ).apply {
                    val bParams = LinearLayout.LayoutParams(AntTheme.dp(context, 34), AntTheme.dp(context, 34))
                    bParams.marginEnd = AntTheme.dp(context, 12)
                    layoutParams = bParams
                }

                val title = TextView(context).apply {
                    text = "Ativação por Horário"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val statusBadge = TextView(context).apply {
                    tag = "schedule_status_badge"
                    updateStatusBadge(this)
                }

                addView(badge)
                addView(title)
                addView(statusBadge)
            }

            val desc = TextView(context).apply {
                text = "Programe dias e horários para o pulador automático e rotina de sono."
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, 0, 0, AntTheme.dp(context, 12))
            }

            addView(headerRow)
            addView(desc)

            // Dynamic Container
            val dynamicContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutTransition = LayoutTransition().apply {
                    enableTransitionType(LayoutTransition.CHANGING)
                    setDuration(180)
                }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            fun refresh() {
                val badge = findViewWithTag<TextView>("schedule_status_badge")
                badge?.let { updateStatusBadge(it) }
                dynamicContainer.removeAllViews()
                renderContent(dynamicContainer) { refresh(); onStateChanged() }
                onStateChanged()
            }

            renderContent(dynamicContainer) { refresh(); onStateChanged() }
            addView(dynamicContainer)
        }

    private fun updateStatusBadge(badge: TextView) {
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        badge.typeface = Typeface.DEFAULT_BOLD
        badge.setPadding(AntTheme.dp(context, 8), AntTheme.dp(context, 3), AntTheme.dp(context, 8), AntTheme.dp(context, 3))

        if (!preferences.isScheduleEnabled) {
            badge.text = "Sempre ativo"
            badge.setTextColor(AntTheme.TEXT_MUTED)
            badge.background = AntTheme.rounded(AntTheme.CARD_SURFACE_HOVER, 6f, AntTheme.CARD_BORDER, 1, context)
        } else if (preferences.isScheduleActiveNow()) {
            badge.text = "● Ativo agora"
            badge.setTextColor(AntTheme.SUCCESS_TEXT)
            badge.background = AntTheme.rounded(AntTheme.SUCCESS_BG, 6f, AntTheme.SUCCESS_BORDER, 1, context)
        } else {
            badge.text = "○ Fora do horário"
            badge.setTextColor(AntTheme.WARNING_TEXT)
            badge.background = AntTheme.rounded(AntTheme.WARNING_BG, 6f, AntTheme.WARNING_BORDER, 1, context)
        }
    }

    private fun renderContent(container: LinearLayout, onRefresh: () -> Unit) {
        // Toggle 1: Ativação por Horário Programado
        val masterSwitchRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 14), AntTheme.dp(context, 10), AntTheme.dp(context, 14), AntTheme.dp(context, 10))
            val mParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            mParams.bottomMargin = AntTheme.dp(context, 10)
            layoutParams = mParams

            val labelLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val label = TextView(context).apply {
                    text = "Restringir a horário programado"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    typeface = Typeface.DEFAULT_BOLD
                }
                val sub = TextView(context).apply {
                    text = if (preferences.isScheduleEnabled) "Ativo apenas no período configurado abaixo" else "Desativado (AntSKIP funciona 24 horas por dia)"
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                }
                addView(label)
                addView(sub)
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isScheduleEnabled
                setOnCheckedChangeListener { _, checked ->
                    preferences.setScheduleEnabled(checked)
                    if (checked && (preferences.schedulePreset == "ALWAYS" || preferences.schedulePreset.isBlank())) {
                        preferences.setSchedulePreset("NIGHT")
                        preferences.setScheduleStartHour(22)
                        preferences.setScheduleStartMinute(0)
                        preferences.setScheduleEndHour(6)
                        preferences.setScheduleEndMinute(0)
                        preferences.setScheduleDays(setOf(1, 2, 3, 4, 5, 6, 7))
                    }
                    onRefresh()
                }
            }

            addView(labelLayout)
            addView(sw)
            setOnClickListener { sw.toggle() }
        }
        container.addView(masterSwitchRow)

        if (preferences.isScheduleEnabled) {
            // Presets Horizontal Filter Chips
            val presetsScroll = HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                val pParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                pParams.bottomMargin = AntTheme.dp(context, 10)
                layoutParams = pParams
            }

            val presetsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            data class PresetItem(val id: String, val label: String, val startH: Int, val startM: Int, val endH: Int, val endM: Int, val days: Set<Int>)

            val presetList = listOf(
                PresetItem("NIGHT", "Noturno (22h - 06h)", 22, 0, 6, 0, setOf(1, 2, 3, 4, 5, 6, 7)),
                PresetItem("PRIME", "Horário Nobre (19h - 01h)", 19, 0, 1, 0, setOf(1, 2, 3, 4, 5, 6, 7)),
                PresetItem("WEEKDAYS", "Dias Úteis (Seg-Sex • 20h - 02h)", 20, 0, 2, 0, setOf(2, 3, 4, 5, 6)),
                PresetItem("WEEKENDS", "Fins de Semana (Dia todo)", 0, 0, 23, 59, setOf(1, 7)),
                PresetItem("CUSTOM", "Personalizado", preferences.scheduleStartHour, preferences.scheduleStartMinute, preferences.scheduleEndHour, preferences.scheduleEndMinute, preferences.scheduleDays),
            )

            val currentPreset = preferences.schedulePreset

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
                        preferences.setSchedulePreset(preset.id)
                        if (preset.id != "CUSTOM") {
                            preferences.setScheduleStartHour(preset.startH)
                            preferences.setScheduleStartMinute(preset.startM)
                            preferences.setScheduleEndHour(preset.endH)
                            preferences.setScheduleEndMinute(preset.endM)
                            preferences.setScheduleDays(preset.days)
                        }
                        onRefresh()
                    }
                }
                presetsRow.addView(chip)
            }
            presetsScroll.addView(presetsRow)
            container.addView(presetsScroll)

            // Time Selector Card
            val timeBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)
                setPadding(AntTheme.dp(context, 14), AntTheme.dp(context, 10), AntTheme.dp(context, 14), AntTheme.dp(context, 10))
                val tParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                tParams.bottomMargin = AntTheme.dp(context, 10)
                layoutParams = tParams

                val titleRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    val trParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    trParams.bottomMargin = AntTheme.dp(context, 8)
                    layoutParams = trParams

                    val label = TextView(context).apply {
                        text = "INTERVALO DE FUNCIONAMENTO"
                        setTextColor(AntTheme.TEXT_MUTED)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                        typeface = Typeface.DEFAULT_BOLD
                        letterSpacing = 0.05f
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    addView(label)
                }

                val timePickersRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL

                    val startTimeBtn = Button(context).apply {
                        val formattedStart = "%02d:%02d".format(preferences.scheduleStartHour, preferences.scheduleStartMinute)
                        text = "Início\n$formattedStart"
                        setTextColor(AntTheme.TEXT_PRIMARY)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        typeface = Typeface.DEFAULT_BOLD
                        background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 8f, context = context)
                        layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 48), 1f)
                        AntTheme.addPressFeedback(this)
                        setOnClickListener {
                            TimePickerDialog(
                                context,
                                android.R.style.Theme_DeviceDefault_Dialog,
                                { _, hour, minute ->
                                    preferences.setScheduleStartHour(hour)
                                    preferences.setScheduleStartMinute(minute)
                                    preferences.setSchedulePreset("CUSTOM")
                                    onRefresh()
                                },
                                preferences.scheduleStartHour,
                                preferences.scheduleStartMinute,
                                true,
                            ).show()
                        }
                    }

                    val arrow = TextView(context).apply {
                        text = "➔"
                        setTextColor(AntTheme.ACCENT_PRIMARY)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                        gravity = Gravity.CENTER
                        setPadding(AntTheme.dp(context, 8), 0, AntTheme.dp(context, 8), 0)
                    }

                    val endTimeBtn = Button(context).apply {
                        val formattedEnd = "%02d:%02d".format(preferences.scheduleEndHour, preferences.scheduleEndMinute)
                        text = "Término\n$formattedEnd"
                        setTextColor(AntTheme.TEXT_PRIMARY)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        typeface = Typeface.DEFAULT_BOLD
                        background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 8f, context = context)
                        layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 48), 1f)
                        AntTheme.addPressFeedback(this)
                        setOnClickListener {
                            TimePickerDialog(
                                context,
                                android.R.style.Theme_DeviceDefault_Dialog,
                                { _, hour, minute ->
                                    preferences.setScheduleEndHour(hour)
                                    preferences.setScheduleEndMinute(minute)
                                    preferences.setSchedulePreset("CUSTOM")
                                    onRefresh()
                                },
                                preferences.scheduleEndHour,
                                preferences.scheduleEndMinute,
                                true,
                            ).show()
                        }
                    }

                    addView(startTimeBtn)
                    addView(arrow)
                    addView(endTimeBtn)
                }

                // Days of week selector
                val daysTitle = TextView(context).apply {
                    text = "DIAS ATIVOS"
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    letterSpacing = 0.05f
                    setPadding(0, AntTheme.dp(context, 10), 0, AntTheme.dp(context, 6))
                }

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

                val selectedDays = preferences.scheduleDays.toMutableSet()

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
                        val dParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 34), 1f)
                        dParams.marginEnd = AntTheme.dp(context, 4)
                        layoutParams = dParams
                        AntTheme.addPressFeedback(this)
                        setOnClickListener {
                            if (isDaySelected && selectedDays.size > 1) {
                                selectedDays.remove(calDay)
                            } else {
                                selectedDays.add(calDay)
                            }
                            preferences.setScheduleDays(selectedDays)
                            preferences.setSchedulePreset("CUSTOM")
                            onRefresh()
                        }
                    }
                    daysRow.addView(dayBtn)
                }

                val summaryLabel = TextView(context).apply {
                    val start = "%02d:%02d".format(preferences.scheduleStartHour, preferences.scheduleStartMinute)
                    val end = "%02d:%02d".format(preferences.scheduleEndHour, preferences.scheduleEndMinute)
                    val days = preferences.scheduleDays
                    val daysDesc = when {
                        days.size == 7 -> "Todos os dias"
                        days == setOf(2, 3, 4, 5, 6) -> "Segunda a sexta"
                        days == setOf(1, 7) -> "Fins de semana"
                        else -> "${days.size} dias na semana"
                    }
                    val isOvernight = preferences.scheduleStartHour > preferences.scheduleEndHour ||
                        (preferences.scheduleStartHour == preferences.scheduleEndHour && preferences.scheduleStartMinute > preferences.scheduleEndMinute)
                    val overnightText = if (isOvernight) " (vira a noite)" else ""
                    text = "Resumo: $daysDesc • $start até $end$overnightText"
                    setTextColor(AntTheme.ACCENT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, AntTheme.dp(context, 10), 0, 0)
                }

                addView(titleRow)
                addView(timePickersRow)
                addView(daysTitle)
                addView(daysRow)
                addView(summaryLabel)
            }
            container.addView(timeBox)
        }

        // Section 2: Rotina de Sono Automática (Auto Sleep Timer)
        val sleepRoutineBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 14), AntTheme.dp(context, 10), AntTheme.dp(context, 14), AntTheme.dp(context, 10))
            val sParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            sParams.topMargin = AntTheme.dp(context, 4)
            layoutParams = sParams

            val routineRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val labelLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                    val label = TextView(context).apply {
                        text = "Rotina de Sono Automática"
                        setTextColor(AntTheme.TEXT_PRIMARY)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    val formattedRoutine = "%02d:%02d".format(preferences.sleepRoutineHour, preferences.sleepRoutineMinute)
                    val sub = TextView(context).apply {
                        text = if (preferences.isSleepRoutineEnabled) {
                            "Inicia o temporizador (${preferences.sleepTimerDurationMinutes}m) todo dia às $formattedRoutine"
                        } else {
                            "Disparar temporizador de sono automaticamente"
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

            addView(routineRow)

            if (preferences.isSleepRoutineEnabled) {
                addView(AntTheme.divider(context))

                val routineTimeBtn = Button(context).apply {
                    val formatted = "%02d:%02d".format(preferences.sleepRoutineHour, preferences.sleepRoutineMinute)
                    text = "Horário de Dormir: $formatted  (Toque para mudar)"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 8f, context = context)
                    val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 38))
                    bParams.topMargin = AntTheme.dp(context, 8)
                    layoutParams = bParams
                    AntTheme.addPressFeedback(this)
                    setOnClickListener {
                        TimePickerDialog(
                            context,
                            android.R.style.Theme_DeviceDefault_Dialog,
                            { _, hour, minute ->
                                preferences.setSleepRoutineHour(hour)
                                preferences.setSleepRoutineMinute(minute)
                                ScheduleManager.syncSleepRoutineAlarm(context, preferences)
                                onRefresh()
                            },
                            preferences.sleepRoutineHour,
                            preferences.sleepRoutineMinute,
                            true,
                        ).show()
                    }
                }
                addView(routineTimeBtn)
            }
        }
        container.addView(sleepRoutineBox)
    }
}
