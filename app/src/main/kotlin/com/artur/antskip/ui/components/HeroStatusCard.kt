package com.artur.antskip.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.artur.antskip.ui.theme.AntTheme

class HeroStatusCard(
    private val context: Context,
    private val version: String,
    private val onPrimaryAction: () -> Unit,
    private val onSecondaryAction: () -> Unit,
) {
    private lateinit var dotView: TextView
    private lateinit var statusBadgeView: TextView
    private lateinit var descView: TextView
    private lateinit var metricTextView: TextView
    private lateinit var primaryBtn: Button
    private lateinit var secondaryBtn: Button

    fun createView(
        isServiceEnabled: Boolean,
        isAutomationEnabled: Boolean,
        activeAppsCount: Int,
        totalAppsCount: Int,
    ): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 18), AntTheme.dp(context, 18), AntTheme.dp(context, 18), AntTheme.dp(context, 18))
            background = AntTheme.rounded(AntTheme.HERO_BG, 18f, AntTheme.HERO_BORDER, 1, context)
            elevation = AntTheme.dp(context, 6).toFloat()
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 16)
            layoutParams = params

            // Top row: Dot + Title + Version + Spacer + Status pill badge
            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                dotView = TextView(context).apply {
                    val dotParams = LinearLayout.LayoutParams(AntTheme.dp(context, 9), AntTheme.dp(context, 9))
                    dotParams.marginEnd = AntTheme.dp(context, 8)
                    layoutParams = dotParams
                }

                val title = TextView(context).apply {
                    text = "AntSKIP"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
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

                statusBadgeView = TextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(AntTheme.dp(context, 10), AntTheme.dp(context, 4), AntTheme.dp(context, 10), AntTheme.dp(context, 4))
                }

                addView(dotView)
                addView(title)
                addView(versionBadge)
                addView(spacer)
                addView(statusBadgeView)
            }

            descView = TextView(context).apply {
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, AntTheme.dp(context, 10), 0, AntTheme.dp(context, 12))
            }

            val metricsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(AntTheme.dp(context, 12), AntTheme.dp(context, 8), AntTheme.dp(context, 12), AntTheme.dp(context, 8))
                background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)

                metricTextView = TextView(context).apply {
                    setTextColor(AntTheme.TEXT_SECONDARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(metricTextView)
            }

            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                bParams.topMargin = AntTheme.dp(context, 14)
                layoutParams = bParams

                primaryBtn = Button(context).apply {
                    setTextColor(AntTheme.TEXT_INVERSE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rippleButton(AntTheme.ACCENT_PRIMARY, radiusDp = 10f, context = context)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 44))
                    setOnClickListener { onPrimaryAction() }
                }

                secondaryBtn = Button(context).apply {
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
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
            addView(descView)
            addView(metricsRow)
            addView(buttonsRow)

            updateState(isServiceEnabled, isAutomationEnabled, activeAppsCount, totalAppsCount)
        }

    fun updateState(
        isServiceEnabled: Boolean,
        isAutomationEnabled: Boolean,
        activeAppsCount: Int,
        totalAppsCount: Int,
    ) {
        val dotColor = when {
            !isServiceEnabled -> AntTheme.DANGER_TEXT
            !isAutomationEnabled -> AntTheme.WARNING_TEXT
            else -> AntTheme.SUCCESS_TEXT
        }
        dotView.background = AntTheme.rounded(dotColor, 6f, null, 0, context)

        statusBadgeView.text = when {
            !isServiceEnabled -> "Desativado"
            !isAutomationEnabled -> "Pausado"
            else -> "Ativo"
        }
        val textColor = when {
            !isServiceEnabled -> AntTheme.DANGER_TEXT
            !isAutomationEnabled -> AntTheme.WARNING_TEXT
            else -> AntTheme.SUCCESS_TEXT
        }
        val bgColor = when {
            !isServiceEnabled -> AntTheme.DANGER_BG
            !isAutomationEnabled -> AntTheme.WARNING_BG
            else -> AntTheme.SUCCESS_BG
        }
        val borderColor = when {
            !isServiceEnabled -> AntTheme.DANGER_BORDER
            !isAutomationEnabled -> AntTheme.WARNING_BORDER
            else -> AntTheme.SUCCESS_BORDER
        }
        statusBadgeView.setTextColor(textColor)
        statusBadgeView.background = AntTheme.rounded(bgColor, 8f, borderColor, 1, context)

        descView.text = when {
            !isServiceEnabled -> "Ative a acessibilidade para que o AntSKIP detecte e aperte automaticamente os botões de pular no streaming."
            !isAutomationEnabled -> "A automação está pausada manualmente. Reative para voltar a pular aberturas e créditos."
            else -> "Pula aberturas, resumos, créditos e avança episódios instantaneamente com resposta em ~100ms."
        }

        metricTextView.text = "Streamings Monitorados: $activeAppsCount de $totalAppsCount ativos"

        primaryBtn.text = if (!isServiceEnabled) "Ativar Acessibilidade" else "Testar Reconhecimento"
        secondaryBtn.text = if (!isServiceEnabled) "Permitir Configurações Restritas" else "Gerenciar Acessibilidade do Android"
    }
}
