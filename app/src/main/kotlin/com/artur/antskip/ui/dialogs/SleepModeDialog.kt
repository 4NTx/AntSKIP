package com.artur.antskip.ui.dialogs

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.ui.theme.AntTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SleepModeDialog(
    private val context: Context,
    private val preferences: PreferenceStore,
) {
    fun show(provider: StreamingProvider, onDismiss: () -> Unit = {}) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 16), AntTheme.dp(context, 20), AntTheme.dp(context, 20))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
        }

        val title = TextView(context).apply {
            text = "Modo Dormir: ${provider.label}"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, AntTheme.dp(context, 4))
        }

        val desc = TextView(context).apply {
            text = "Evita que a série continue tocando sozinha a noite toda bloqueando o avanço automático de episódios."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, AntTheme.dp(context, 16))
        }

        // Section 1: Quick Pause
        val pauseHeader = TextView(context).apply {
            text = "Pausar Próximo Episódio Temporariamente"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, AntTheme.dp(context, 8))
        }

        val pauseUntil = preferences.nextEpisodePauseUntilMillis(provider)
        val pauseStatus = TextView(context).apply {
            text = if (pauseUntil > System.currentTimeMillis()) {
                val formatted = formatTimestamp(pauseUntil)
                "Pausa ativa até $formatted"
            } else {
                "Nenhuma pausa temporária ativa no momento."
            }
            setTextColor(if (pauseUntil > System.currentTimeMillis()) AntTheme.WARNING_TEXT else AntTheme.TEXT_MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, AntTheme.dp(context, 10))
        }

        val pauseButtonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(quickPauseButton("1h", 1, provider, pauseStatus))
            addView(quickPauseButton("2h", 2, provider, pauseStatus))
            addView(quickPauseButton("4h", 4, provider, pauseStatus))
            addView(clearPauseButton(provider, pauseStatus))
        }

        // Section 2: Bedtime Schedule
        val scheduleHeader = TextView(context).apply {
            text = "Horário de Sono Recorrente"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, AntTheme.dp(context, 20), 0, AntTheme.dp(context, 8))
        }

        val scheduleSwitchRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, AntTheme.dp(context, 8))
            val label = TextView(context).apply {
                text = "Ativar horário de sono diário"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val sw = Switch(context).apply {
                isChecked = preferences.isNextEpisodeScheduleEnabled(provider)
                setOnCheckedChangeListener { _, isChecked ->
                    preferences.setNextEpisodeScheduleEnabled(provider, isChecked)
                }
            }
            addView(label)
            addView(sw)
        }

        val timeSelectionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val startBtn = Button(context).apply {
                val startMin = preferences.nextEpisodeScheduleStartMinutes(provider)
                text = "Início: ${formatMinutes(startMin)}"
                setTextColor(AntTheme.TEXT_PRIMARY)
                background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
                val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 42), 1f)
                params.marginEnd = AntTheme.dp(context, 6)
                layoutParams = params
                setOnClickListener {
                    val cur = preferences.nextEpisodeScheduleStartMinutes(provider)
                    TimePickerDialog(context, { _, hour, minute ->
                        preferences.setNextEpisodeScheduleStartMinutes(provider, hour * 60 + minute)
                        text = "Início: ${formatMinutes(hour * 60 + minute)}"
                    }, cur / 60, cur % 60, true).show()
                }
            }

            val endBtn = Button(context).apply {
                val endMin = preferences.nextEpisodeScheduleEndMinutes(provider)
                text = "Término: ${formatMinutes(endMin)}"
                setTextColor(AntTheme.TEXT_PRIMARY)
                background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
                layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 42), 1f)
                setOnClickListener {
                    val cur = preferences.nextEpisodeScheduleEndMinutes(provider)
                    TimePickerDialog(context, { _, hour, minute ->
                        preferences.setNextEpisodeScheduleEndMinutes(provider, hour * 60 + minute)
                        text = "Término: ${formatMinutes(hour * 60 + minute)}"
                    }, cur / 60, cur % 60, true).show()
                }
            }

            addView(startBtn)
            addView(endBtn)
        }

        // Section 3: Block Credits during sleep
        val blockCreditsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, AntTheme.dp(context, 16), 0, 0)
            val label = TextView(context).apply {
                text = "Bloquear também botão 'Créditos' no modo dormir"
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val sw = Switch(context).apply {
                isChecked = preferences.blocksCreditsDuringSleep(provider)
                setOnCheckedChangeListener { _, isChecked ->
                    preferences.setBlocksCreditsDuringSleep(provider, isChecked)
                }
            }
            addView(label)
            addView(sw)
        }

        container.addView(title)
        container.addView(desc)
        container.addView(pauseHeader)
        container.addView(pauseStatus)
        container.addView(pauseButtonsRow)
        container.addView(scheduleHeader)
        container.addView(scheduleSwitchRow)
        container.addView(timeSelectionRow)
        container.addView(blockCreditsRow)

        val scroll = ScrollView(context).apply { addView(container) }

        AlertDialog.Builder(context)
            .setView(scroll)
            .setPositiveButton("Concluído") { _, _ -> onDismiss() }
            .show()
    }

    private fun quickPauseButton(label: String, hours: Long, provider: StreamingProvider, statusView: TextView): Button =
        Button(context).apply {
            text = label
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 38), 1f)
            params.marginEnd = AntTheme.dp(context, 6)
            layoutParams = params
            setOnClickListener {
                val until = System.currentTimeMillis() + hours * 3600 * 1000
                preferences.setNextEpisodePauseUntilMillis(provider, until)
                statusView.text = "Pausa ativa até ${formatTimestamp(until)}"
                statusView.setTextColor(AntTheme.WARNING_TEXT)
            }
        }

    private fun clearPauseButton(provider: StreamingProvider, statusView: TextView): Button =
        Button(context).apply {
            text = "Limpar"
            setTextColor(AntTheme.DANGER_TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rounded(AntTheme.DANGER_BG, 8f, AntTheme.DANGER_BORDER, 1, context)
            layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 38), 1.2f)
            setOnClickListener {
                preferences.clearNextEpisodePause(provider)
                statusView.text = "Nenhuma pausa temporária ativa."
                statusView.setTextColor(AntTheme.TEXT_MUTED)
            }
        }

    private fun formatTimestamp(millis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}
