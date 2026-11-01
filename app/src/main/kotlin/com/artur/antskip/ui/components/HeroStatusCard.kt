package com.artur.antskip.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.artur.antskip.ui.theme.AntTheme

class HeroStatusCard(
    private val context: Context,
    private val isServiceEnabled: Boolean,
    private val isAutomationEnabled: Boolean,
    private val version: String,
    private val activeAppsCount: Int,
    private val totalAppsCount: Int,
    private val onPrimaryAction: () -> Unit,
    private val onSecondaryAction: () -> Unit,
) {
    fun createView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 20), AntTheme.dp(context, 20), AntTheme.dp(context, 20))
            background = AntTheme.rounded(AntTheme.HERO_BG, 18f, AntTheme.HERO_BORDER, 1, context)
            elevation = AntTheme.dp(context, 6).toFloat()
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 16)
            layoutParams = params

            // Top row: Title + Version + Automation status badge
            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val dot = TextView(context).apply {
                    val dotColor = when {
                        !isServiceEnabled -> AntTheme.DANGER_TEXT
                        !isAutomationEnabled -> AntTheme.WARNING_TEXT
                        else -> AntTheme.SUCCESS_TEXT
                    }
                    background = AntTheme.rounded(dotColor, 6f, null, 0, context)
                    val dotParams = LinearLayout.LayoutParams(AntTheme.dp(context, 8), AntTheme.dp(context, 8))
                    dotParams.marginEnd = AntTheme.dp(context, 8)
                    layoutParams = dotParams
                }

                val title = TextView(context).apply {
                    text = "AntSKIP"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                    typeface = Typeface.DEFAULT_BOLD
                }

                val versionBadge = AntTheme.badge(
                    context,
                    "v$version",
                    AntTheme.TEXT_SECONDARY,
                    AntTheme.BG_CANVAS,
                    AntTheme.CARD_BORDER,
                ).apply {
                    val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    bParams.marginStart = AntTheme.dp(context, 8)
                    layoutParams = bParams
                }

                val spacer = LinearLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }

                val statusBadge = AntTheme.badge(
                    context,
                    if (!isServiceEnabled) "Acessibilidade desligada"
                    else if (!isAutomationEnabled) "Pausado"
                    else "Automacao ativa",
                    if (!isServiceEnabled) AntTheme.DANGER_TEXT
                    else if (!isAutomationEnabled) AntTheme.WARNING_TEXT
                    else AntTheme.SUCCESS_TEXT,
                    if (!isServiceEnabled) AntTheme.DANGER_BG
                    else if (!isAutomationEnabled) AntTheme.WARNING_BG
                    else AntTheme.SUCCESS_BG,
                    if (!isServiceEnabled) AntTheme.DANGER_BORDER
                    else if (!isAutomationEnabled) AntTheme.WARNING_BORDER
                    else AntTheme.SUCCESS_BORDER,
                )

                addView(dot)
                addView(title)
                addView(versionBadge)
                addView(spacer)
                addView(statusBadge)
            }

            // Description
            val desc = TextView(context).apply {
                text = if (!isServiceEnabled) {
                    "Ative a acessibilidade para que o AntSKIP detecte e aperte automaticamente os botões de pular no streaming."
                } else if (!isAutomationEnabled) {
                    "A automação está pausada manualmente. Reative para voltar a pular aberturas e créditos."
                } else {
                    "Pula aberturas, resumos, créditos e avança episódios instantaneamente com resposta em ~120ms."
                }
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                setPadding(0, AntTheme.dp(context, 10), 0, AntTheme.dp(context, 14))
            }

            // Metrics row
            val metricsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 8), AntTheme.dp(context, 12), AntTheme.dp(context, 8))
                background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)

                val metricText = TextView(context).apply {
                    text = "Streamings Monitorados: $activeAppsCount de $totalAppsCount ativos"
                    setTextColor(AntTheme.TEXT_SECONDARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(metricText)
            }

            // Action buttons
            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.topMargin = AntTheme.dp(context, 14)
                layoutParams = params

                val primaryBtn = Button(context).apply {
                    text = if (!isServiceEnabled) "Ativar Acessibilidade" else "Simulador de Reconhecimento"
                    setTextColor(AntTheme.TEXT_INVERSE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rippleButton(AntTheme.ACCENT_PRIMARY, radiusDp = 10f, context = context)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 44))
                    setOnClickListener { onPrimaryAction() }
                }

                val secondaryBtn = Button(context).apply {
                    text = if (!isServiceEnabled) "Permitir Configurações Restritas" else "Gerenciar Acessibilidade do Android"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rounded(AntTheme.CARD_SURFACE, 10f, AntTheme.CARD_BORDER, 1, context)
                    val sParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 40))
                    sParams.topMargin = AntTheme.dp(context, 8)
                    layoutParams = sParams
                    setOnClickListener { onSecondaryAction() }
                }

                addView(primaryBtn)
                addView(secondaryBtn)
            }

            addView(topRow)
            addView(desc)
            addView(metricsRow)
            addView(buttonsRow)
        }
}
