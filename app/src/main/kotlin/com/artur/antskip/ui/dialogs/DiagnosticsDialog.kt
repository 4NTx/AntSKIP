package com.artur.antskip.ui.dialogs

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
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
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 16), AntTheme.dp(context, 20), AntTheme.dp(context, 20))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
        }

        val title = TextView(context).apply {
            text = "Logs de Diagnóstico"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, AntTheme.dp(context, 6))
        }

        val subtitle = TextView(context).apply {
            text = "Histórico recente de eventos de acessibilidade, detecções e cliques de botões."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, AntTheme.dp(context, 14))
        }

        val logs = preferences.diagnosticLogs()
        val logsDisplay = TextView(context).apply {
            text = if (logs.isBlank()) "Nenhum log registrado ainda.\nAbra um app de streaming para começar a monitorar." else logs
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.MONOSPACE
            setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
        }

        val logScroll = ScrollView(context).apply {
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 260))
            params.bottomMargin = AntTheme.dp(context, 14)
            layoutParams = params
            addView(logsDisplay)
        }

        val buttonsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val copyButton = Button(context).apply {
                text = "Copiar Logs"
                setTextColor(AntTheme.TEXT_INVERSE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rippleButton(AntTheme.ACCENT_PRIMARY, radiusDp = 8f, context = context)
                val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 42), 1f)
                params.marginEnd = AntTheme.dp(context, 8)
                layoutParams = params
                setOnClickListener {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("AntSKIP Diagnostics", preferences.diagnosticLogs()))
                    Toast.makeText(context, "Logs copiados para a área de transferência", Toast.LENGTH_SHORT).show()
                }
            }

            val clearButton = Button(context).apply {
                text = "Limpar"
                setTextColor(AntTheme.DANGER_TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = Typeface.DEFAULT_BOLD
                background = AntTheme.rounded(AntTheme.DANGER_BG, 8f, AntTheme.DANGER_BORDER, 1, context)
                layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(context, 42), 1f)
                setOnClickListener {
                    preferences.clearDiagnosticLogs()
                    logsDisplay.text = "Logs limpos com sucesso."
                    onLogsCleared()
                }
            }
            addView(copyButton)
            addView(clearButton)
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(logScroll)
        container.addView(buttonsRow)

        AlertDialog.Builder(context)
            .setView(container)
            .setPositiveButton("Fechar", null)
            .show()
    }
}
