package com.artur.antskip.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
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
    private lateinit var bannerContainer: LinearLayout
    private lateinit var primaryBtn: Button
    private lateinit var secondaryBtn: Button

    fun createView(
        isServiceEnabled: Boolean,
        isAutomationEnabled: Boolean,
        activeAppsCount: Int,
        totalAppsCount: Int,
    ): LinearLayout =
        LinearLayout(context).apply {
            bannerContainer = this
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 14), AntTheme.dp(context, 16), AntTheme.dp(context, 14))
            background = AntTheme.rounded(AntTheme.WARNING_BG, 12f, AntTheme.WARNING_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 14)
            layoutParams = params

            val title = TextView(context).apply {
                text = "Acessibilidade desativada"
                setTextColor(AntTheme.WARNING_TEXT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
            }

            val desc = TextView(context).apply {
                text = "O AntSKIP precisa da permissão de acessibilidade para detectar e tocar nos botões de pular."
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding(0, AntTheme.dp(context, 4), 0, AntTheme.dp(context, 10))
            }

            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                primaryBtn = Button(context).apply {
                    text = "Ativar nas Configurações"
                    setTextColor(AntTheme.TEXT_INVERSE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rippleButton(AntTheme.ACCENT_PRIMARY, radiusDp = 8f, context = context)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 40))
                    setOnClickListener { onPrimaryAction() }
                }

                secondaryBtn = Button(context).apply {
                    text = "Permitir Configurações Restritas"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rounded(AntTheme.CARD_SURFACE, 8f, AntTheme.CARD_BORDER, 1, context)
                    val sParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 36))
                    sParams.topMargin = AntTheme.dp(context, 6)
                    layoutParams = sParams
                    setOnClickListener { onSecondaryAction() }
                }

                addView(primaryBtn)
                addView(secondaryBtn)
            }

            addView(title)
            addView(desc)
            addView(buttonsRow)

            updateState(isServiceEnabled, isAutomationEnabled, activeAppsCount, totalAppsCount)
        }

    fun updateState(
        isServiceEnabled: Boolean,
        isAutomationEnabled: Boolean,
        activeAppsCount: Int,
        totalAppsCount: Int,
    ) {
        // If accessibility is enabled, the warning banner disappears completely!
        if (isServiceEnabled) {
            bannerContainer.visibility = View.GONE
        } else {
            bannerContainer.visibility = View.VISIBLE
        }
    }
}
