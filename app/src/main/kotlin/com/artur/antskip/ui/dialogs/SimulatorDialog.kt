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
        val dialog = AlertDialog.Builder(context).create()
        val matcher = SkipMatcher(preferences)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 18), AntTheme.dp(context, 20), AntTheme.dp(context, 18))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
        }

        // Top Header with Title and Close 'X'
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val hParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            hParams.bottomMargin = AntTheme.dp(context, 4)
            layoutParams = hParams

            val title = TextView(context).apply {
                text = "Testar reconhecimento"
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
                setPadding(AntTheme.dp(context, 8), AntTheme.dp(context, 4), AntTheme.dp(context, 8), AntTheme.dp(context, 4))
                AntTheme.addPressFeedback(this)
                setOnClickListener { dialog.dismiss() }
            }

            addView(title)
            addView(closeX)
        }

        val subtitle = TextView(context).apply {
            text = "Verifique se o AntSKIP identifica a frase de um botão de streaming."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, AntTheme.dp(context, 12))
        }

        val providerLabel = TextView(context).apply {
            text = "Aplicativo:"
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, AntTheme.dp(context, 4))
        }
        val providerSpinner = Spinner(context).apply {
            val providerNames = StreamingProvider.entries.map { it.label }
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, providerNames)
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 6f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 10), AntTheme.dp(context, 8), AntTheme.dp(context, 10), AntTheme.dp(context, 8))
        }

        val inputLabel = TextView(context).apply {
            text = "Texto do botão:"
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, AntTheme.dp(context, 10), 0, AntTheme.dp(context, 4))
        }
        val input = EditText(context).apply {
            hint = "Ex: Pular abertura"
            setHintTextColor(AntTheme.TEXT_MUTED)
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 6f, AntTheme.CARD_BORDER, 1, context)
            setPadding(AntTheme.dp(context, 10), AntTheme.dp(context, 10), AntTheme.dp(context, 10), AntTheme.dp(context, 10))
        }

        val resultCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 10), AntTheme.dp(context, 12), AntTheme.dp(context, 10))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 8f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.topMargin = AntTheme.dp(context, 12)
            layoutParams = params
        }

        val resultText = TextView(context).apply {
            text = "Digite uma frase e toque em Verificar."
            setTextColor(AntTheme.TEXT_MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        }
        resultCard.addView(resultText)

        val testButton = Button(context).apply {
            text = "Verificar"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rippleGradientButton(
                startColor = AntTheme.ACCENT_GRADIENT_START,
                endColor = AntTheme.ACCENT_GRADIENT_END,
                radiusDp = 8f,
                context = context,
            )
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 42))
            params.topMargin = AntTheme.dp(context, 12)
            layoutParams = params
            AntTheme.addPressFeedback(this)

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
                    resultText.text = "Bloqueado: Esta frase é de controle do player e é ignorada intencionalmente."
                    resultText.setTextColor(AntTheme.WARNING_TEXT)
                    return@setOnClickListener
                }

                val matchedAction = SkipPhraseBank.match(normalized)
                if (matchedAction == null) {
                    resultText.text = "Não reconhecido: Nenhuma regra encontrada para '$entered'."
                    resultText.setTextColor(AntTheme.DANGER_TEXT)
                } else {
                    val appEnabled = preferences.isProviderEnabled(provider)
                    val actionEnabled = preferences.isActionEnabled(matchedAction)
                    val statusDesc = if (appEnabled && actionEnabled) "habilitado" else "desabilitado nas opções"
                    resultText.text = "Reconhecido como: ${matchedAction.label} ($statusDesc no ${provider.label})"
                    resultText.setTextColor(if (appEnabled && actionEnabled) AntTheme.SUCCESS_TEXT else AntTheme.WARNING_TEXT)
                }
            }
        }

        container.addView(headerRow)
        container.addView(subtitle)
        container.addView(providerLabel)
        container.addView(providerSpinner)
        container.addView(inputLabel)
        container.addView(input)
        container.addView(testButton)
        container.addView(resultCard)

        val scroll = ScrollView(context).apply { addView(container) }

        dialog.setView(scroll)
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setDimAmount(0.65f)
    }
}
