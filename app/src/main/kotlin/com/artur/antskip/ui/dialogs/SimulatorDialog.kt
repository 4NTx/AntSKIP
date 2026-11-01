package com.artur.antskip.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.matcher.SkipMatcher
import com.artur.antskip.matcher.SkipPhraseBank
import com.artur.antskip.matcher.normalizeForMatch
import com.artur.antskip.ui.theme.AntTheme

class SimulatorDialog(
    private val context: Context,
    private val preferences: PreferenceStore,
) {
    fun show() {
        val matcher = SkipMatcher(preferences)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 16), AntTheme.dp(context, 20), AntTheme.dp(context, 20))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
        }

        val title = TextView(context).apply {
            text = "Testar Reconhecimento de Botão"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, AntTheme.dp(context, 4))
        }
        val subtitle = TextView(context).apply {
            text = "Digite o texto de um botão de streaming para testar se o AntSKIP o reconhece."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, AntTheme.dp(context, 14))
        }

        // Provider dropdown
        val providerLabel = TextView(context).apply {
            text = "Streaming:"
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, AntTheme.dp(context, 4))
        }
        val providerSpinner = Spinner(context).apply {
            val providerNames = StreamingProvider.entries.map { it.label }
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, providerNames)
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 10), AntTheme.dp(context, 12), AntTheme.dp(context, 10))
        }

        // Text input
        val inputLabel = TextView(context).apply {
            text = "Texto do Botão (Ex: Pular abertura, Skip Intro):"
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, AntTheme.dp(context, 12), 0, AntTheme.dp(context, 4))
        }
        val input = EditText(context).apply {
            hint = "Ex: Pular abertura"
            setHintTextColor(AntTheme.TEXT_MUTED)
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12), AntTheme.dp(context, 12))
        }

        // Results card
        val resultCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 14), AntTheme.dp(context, 12), AntTheme.dp(context, 14), AntTheme.dp(context, 12))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.topMargin = AntTheme.dp(context, 14)
            layoutParams = params
        }

        val resultHeader = TextView(context).apply {
            text = "Resultado:"
            setTextColor(AntTheme.TEXT_MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val resultText = TextView(context).apply {
            text = "Aguardando teste..."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            setPadding(0, AntTheme.dp(context, 4), 0, 0)
        }
        resultCard.addView(resultHeader)
        resultCard.addView(resultText)

        val testButton = Button(context).apply {
            text = "Verificar Botão"
            setTextColor(AntTheme.TEXT_INVERSE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rippleButton(AntTheme.ACCENT_PRIMARY, radiusDp = 10f, context = context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 44))
            params.topMargin = AntTheme.dp(context, 14)
            layoutParams = params

            setOnClickListener {
                val entered = input.text.toString().trim()
                if (entered.isBlank()) {
                    resultText.text = "Por favor, digite o texto do botão."
                    resultText.setTextColor(AntTheme.WARNING_TEXT)
                    return@setOnClickListener
                }
                val provider = StreamingProvider.entries[providerSpinner.selectedItemPosition]
                val normalized = entered.normalizeForMatch()

                if (matcher.isBlocked(normalized)) {
                    resultText.text = "BLOQUEADO: Esta frase é ignorada intencionalmente."
                    resultText.setTextColor(AntTheme.DANGER_TEXT)
                    return@setOnClickListener
                }

                val matchedAction = SkipPhraseBank.match(normalized)

                if (matchedAction == null) {
                    resultText.text = "NÃO RECONHECIDO\nNenhuma regra conhecida bateu com '$entered'."
                    resultText.setTextColor(AntTheme.DANGER_TEXT)
                } else {
                    val appEnabled = preferences.isProviderEnabled(provider)
                    val actionEnabled = preferences.isActionEnabled(matchedAction)

                    val statusMsg = buildString {
                        append("✅ RECONHECIDO COM SUCESSO!\n")
                        append("• Ação: ${matchedAction.label}\n")
                        append("• Streaming: ${provider.label}\n")
                        append("• Status do Streaming: ${if (appEnabled) "Ativo" else "Desativado nas opções"}\n")
                        append("• Status da Ação: ${if (actionEnabled) "Ativo" else "Desativado nas opções"}")
                    }
                    resultText.text = statusMsg
                    resultText.setTextColor(if (appEnabled && actionEnabled) AntTheme.SUCCESS_TEXT else AntTheme.WARNING_TEXT)
                }
            }
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(providerLabel)
        container.addView(providerSpinner)
        container.addView(inputLabel)
        container.addView(input)
        container.addView(testButton)
        container.addView(resultCard)

        val scroll = ScrollView(context).apply { addView(container) }

        AlertDialog.Builder(context)
            .setView(scroll)
            .setPositiveButton("Fechar", null)
            .show()
    }
}
