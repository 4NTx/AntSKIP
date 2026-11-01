package com.artur.antskip.ui.dialogs

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.ui.theme.AntTheme

class DiagnosticsDialog(
    private val context: Context,
    private val preferences: PreferenceStore,
) {
    fun show(onLogsCleared: () -> Unit = {}) {
        val dialog = AlertDialog.Builder(context).create()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 18), AntTheme.dp(context, 20), AntTheme.dp(context, 18))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
        }

        // Top Header with Title and Close 'X'
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val hParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            hParams.bottomMargin = AntTheme.dp(context, 4)
            layoutParams = hParams

            val title = TextView(context).apply {
                text = "Logs de eventos"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val closeX = TextView(context).apply {
                text = "✕"
                setTextColor(AntTheme.TEXT_MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(AntTheme.dp(context, 8), AntTheme.dp(context, 4), AntTheme.dp(context, 8), AntTheme.dp(context, 4))
                AntTheme.addPressFeedback(this)
                setOnClickListener { dialog.dismiss() }
            }

            addView(title)
            addView(closeX)
        }

        val subtitle = TextView(context).apply {
            text = "Histórico recente de botões pulados pelo AntSKIP."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, AntTheme.dp(context, 12))
        }

        val logs = preferences.diagnosticLogs()
        val logsDisplay = TextView(context).apply {
            text = if (logs.isBlank()) "Nenhum evento registrado ainda.\nAbra a Netflix, Crunchyroll ou Prime Video para começar a pular." else logs
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.MONOSPACE
            setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
        }

        val logScroll = ScrollView(context).apply {
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 220))
            params.bottomMargin = AntTheme.dp(context, 14)
            layoutParams = params
            addView(logsDisplay)
        }

        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val copyButton = Button(context).apply {
                text = "Copiar"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rippleGradientButton(
                    startColor = AntTheme.ACCENT_GRADIENT_START,
                    endColor = AntTheme.ACCENT_GRADIENT_END,
                    radiusDp = 8f,
                    context = context,
                )
                val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 40), 1f)
                params.marginEnd = AntTheme.dp(context, 8)
                layoutParams = params
                AntTheme.addPressFeedback(this)
                setOnClickListener {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("AntSKIP Logs", preferences.diagnosticLogs()))
                    Toast.makeText(context, "Copiado para a área de transferência", Toast.LENGTH_SHORT).show()
                }
            }

            val clearButton = Button(context).apply {
                text = "Limpar"
                setTextColor(AntTheme.DANGER_TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rippleButton(AntTheme.DANGER_BG, radiusDp = 8f, context = context)
                layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 40), 1f)
                AntTheme.addPressFeedback(this)
                setOnClickListener {
                    preferences.clearDiagnosticLogs()
                    logsDisplay.text = "Logs limpos com sucesso."
                    onLogsCleared()
                }
            }
            addView(copyButton)
            addView(clearButton)
        }

        container.addView(headerRow)
        container.addView(subtitle)
        container.addView(logScroll)
        container.addView(buttonsRow)

        val outerScroll = ScrollView(context).apply {
            addView(container)
        }

        dialog.setView(outerScroll)
        dialog.show()

        // Remove ugly default gray background and add smooth dimming!
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.65f)
    }
}
