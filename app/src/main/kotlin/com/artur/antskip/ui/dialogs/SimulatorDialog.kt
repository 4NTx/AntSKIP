package com.artur.antskip.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
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
            text = "Simulador de Reconhecimento"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, AntTheme.dp(context, 6))
        }
        val subtitle = TextView(context).apply {
            text = "Digite o texto de um botão de streaming para testar instantaneamente se o AntSKIP o reconhece."
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, AntTheme.dp(context, 16))
        }

        // Provider dropdown
        val providerLabel = TextView(context).apply {
            text = "Aplicativo de Streaming:"
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
            setPadding(0, AntTheme.dp(context, 14), 0, AntTheme.dp(context, 4))
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
            setPadding(AntTheme.dp(context, 14), AntTheme.dp(context, 14), AntTheme.dp(context, 14), AntTheme.dp(context, 14))
            background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.topMargin = AntTheme.dp(context, 16)
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, AntTheme.dp(context, 4), 0, 0)
        }
        resultCard.addView(resultHeader)
        resultCard.addView(resultText)

        val testButton = Button(context).apply {
            text = "Testar Reconhecimento"
            setTextColor(AntTheme.TEXT_INVERSE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rippleButton(AntTheme.ACCENT_PRIMARY, radiusDp = 10f, context = context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 46))
            params.topMargin = AntTheme.dp(context, 16)
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

                // Check blocked
                if (matcher.isBlocked(normalized)) {
                    resultText.text = "BLOQUEADO: A frase está na lista de bloqueios."
                    resultText.setTextColor(AntTheme.DANGER_TEXT)
                    return@setOnClickListener
                }

                // Check match
                val matchedAction = when (provider) {
                    StreamingProvider.CRUNCHYROLL -> {
                        if (normalized in setOf("pular", "skip")) SkipAction.INTRO
                        else SkipAction.entries.firstOrNull { action ->
                            val phrases = when (action) {
                                SkipAction.INTRO -> setOf("pular abertura", "pular introducao", "skip intro", "skip opening")
                                SkipAction.RECAP -> setOf("pular resumo", "skip recap")
                                SkipAction.CREDITS -> setOf("pular creditos", "skip credits", "skip ending")
                                SkipAction.NEXT_EPISODE -> setOf("proximo episodio", "next episode", "play next")
                                else -> emptySet()
                            }
                            phrases.any { normalized == it || normalized.contains(it) }
                        } ?: com.artur.antskip.matcher.SkipPhraseBank.match(normalized) ?: matcher.matchCustomPhrase(normalized)
                    }
                    else -> com.artur.antskip.matcher.SkipPhraseBank.match(normalized) ?: matcher.matchCustomPhrase(normalized)
                }

                if (matchedAction == null) {
                    resultText.text = "NÃO RECONHECIDO\nNenhuma regra ou frase cadastrada bateu com '$entered'."
                    resultText.setTextColor(AntTheme.DANGER_TEXT)
                } else {
                    val enabledGlobally = preferences.isActionEnabled(matchedAction)
                    val enabledForProvider = preferences.isActionEnabledForProvider(provider, matchedAction)
                    val sleepBlocked = matchedAction == SkipAction.NEXT_EPISODE && preferences.isNextEpisodeBlocked(provider)

                    val statusMsg = buildString {
                        append("RECONHECIDO COM SUCESSO!\n")
                        append("• Ação: ${matchedAction.label} (${matchedAction.name})\n")
                        append("• App: ${provider.label}\n")
                        append("• Status no app: ${if (enabledForProvider) "Ativo" else "Desativado nas regras"}\n")
                        if (sleepBlocked) {
                            append("• Modo dormir: Bloqueado no momento para este app!\n")
                        }
                    }
                    resultText.text = statusMsg
                    resultText.setTextColor(if (enabledForProvider && !sleepBlocked) AntTheme.SUCCESS_TEXT else AntTheme.WARNING_TEXT)
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

        val scroll = ScrollView(context).apply {
            addView(container)
        }

        AlertDialog.Builder(context)
            .setView(scroll)
            .setPositiveButton("Fechar", null)
            .show()
    }
}
