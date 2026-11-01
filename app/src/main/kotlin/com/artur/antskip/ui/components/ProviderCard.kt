package com.artur.antskip.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.ui.ProviderRulesCopy
import com.artur.antskip.ui.theme.AntTheme

class ProviderCard(
    private val context: Context,
    private val provider: StreamingProvider,
    private val preferences: PreferenceStore,
    private val isInstalled: Boolean,
    private val onCardClicked: () -> Unit,
    private val onToggleChanged: (Boolean) -> Unit,
) {
    fun createView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 14), AntTheme.dp(context, 16), AntTheme.dp(context, 14))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 14f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 10)
            layoutParams = params

            // Top row: Brand pill, Title, Installed Badge, Switch
            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val brandPill = TextView(context).apply {
                    background = AntTheme.rounded(provider.brandColor, 4f, null, 0, context)
                    val pParams = LinearLayout.LayoutParams(AntTheme.dp(context, 4), AntTheme.dp(context, 20))
                    pParams.marginEnd = AntTheme.dp(context, 10)
                    layoutParams = pParams
                }

                val title = TextView(context).apply {
                    text = provider.label
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    typeface = Typeface.DEFAULT_BOLD
                }

                val installedBadge = AntTheme.badge(
                    context,
                    if (isInstalled) "Instalado" else "Nao detectado",
                    if (isInstalled) AntTheme.SUCCESS_TEXT else AntTheme.TEXT_MUTED,
                    if (isInstalled) AntTheme.SUCCESS_BG else AntTheme.BG_CANVAS,
                    if (isInstalled) AntTheme.SUCCESS_BORDER else AntTheme.CARD_BORDER,
                ).apply {
                    val bParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    bParams.marginStart = AntTheme.dp(context, 8)
                    layoutParams = bParams
                }

                val spacer = LinearLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }

                val sw = Switch(context).apply {
                    isChecked = preferences.isProviderEnabled(provider)
                    setOnCheckedChangeListener { _, isChecked ->
                        preferences.setProviderEnabled(provider, isChecked)
                        onToggleChanged(isChecked)
                    }
                }

                addView(brandPill)
                addView(title)
                addView(installedBadge)
                addView(spacer)
                addView(sw)
            }

            // Summary of rules
            val summaryText = TextView(context).apply {
                text = ProviderRulesCopy.summary(provider, preferences)
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 6))
            }

            // Bottom bar: Tap hint and Sleep protection status if active
            val bottomRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val tapHint = TextView(context).apply {
                    text = "Toque para configurar regras e modo dormir ›"
                    setTextColor(AntTheme.ACCENT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(tapHint)

                if (preferences.isSleepProtectionActive(provider)) {
                    val sleepBadge = AntTheme.badge(
                        context,
                        "Modo dormir ativo",
                        AntTheme.WARNING_TEXT,
                        AntTheme.WARNING_BG,
                        AntTheme.WARNING_BORDER,
                    )
                    addView(sleepBadge)
                }
            }

            addView(topRow)
            addView(summaryText)
            addView(bottomRow)

            setOnClickListener { onCardClicked() }
        }
}
