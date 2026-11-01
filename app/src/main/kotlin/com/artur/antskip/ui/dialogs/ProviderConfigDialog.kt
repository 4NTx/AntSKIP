package com.artur.antskip.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.ui.ProviderRulesCopy
import com.artur.antskip.ui.theme.AntTheme

class ProviderConfigDialog(
    private val context: Context,
    private val preferences: PreferenceStore,
) {
    fun show(provider: StreamingProvider, onDismiss: () -> Unit = {}) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 20), AntTheme.dp(context, 16), AntTheme.dp(context, 20), AntTheme.dp(context, 20))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, context)
        }

        // Header with brand color bar
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, AntTheme.dp(context, 6))

            val colorPill = TextView(context).apply {
                background = AntTheme.rounded(provider.brandColor, 4f, null, 0, context)
                val params = LinearLayout.LayoutParams(AntTheme.dp(context, 4), AntTheme.dp(context, 22))
                params.marginEnd = AntTheme.dp(context, 10)
                layoutParams = params
            }
            val title = TextView(context).apply {
                text = "Regras: ${provider.label}"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(colorPill)
            addView(title)
        }

        val guidance = TextView(context).apply {
            text = ProviderRulesCopy.guidance(provider)
            setTextColor(AntTheme.TEXT_SECONDARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, 0, 0, AntTheme.dp(context, 16))
        }

        container.addView(headerRow)
        container.addView(guidance)

        // Actions toggles
        SkipAction.entries.forEach { action ->
            val actionRow = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(AntTheme.dp(context, 14), AntTheme.dp(context, 12), AntTheme.dp(context, 14), AntTheme.dp(context, 12))
                background = AntTheme.rounded(AntTheme.BG_CANVAS, 10f, AntTheme.CARD_BORDER, 1, context)
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.bottomMargin = AntTheme.dp(context, 10)
                layoutParams = params

                val topRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL

                    val actionTitle = TextView(context).apply {
                        text = action.label
                        setTextColor(AntTheme.TEXT_PRIMARY)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                        typeface = Typeface.DEFAULT_BOLD
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val sw = Switch(context).apply {
                        isChecked = preferences.isActionEnabledForProvider(provider, action)
                        setOnCheckedChangeListener { _, isChecked ->
                            preferences.setActionEnabledForProvider(provider, action, isChecked)
                        }
                    }

                    addView(actionTitle)
                    addView(sw)
                }

                val actionDesc = TextView(context).apply {
                    text = ProviderRulesCopy.actionDescription(provider, action)
                    setTextColor(AntTheme.TEXT_SECONDARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    setPadding(0, AntTheme.dp(context, 4), 0, 0)
                }

                addView(topRow)
                addView(actionDesc)

                val rec = ProviderRulesCopy.actionRecommendation(provider, action)
                if (rec != null) {
                    val recView = TextView(context).apply {
                        text = "• $rec"
                        setTextColor(AntTheme.ACCENT_PRIMARY)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(0, AntTheme.dp(context, 4), 0, 0)
                    }
                    addView(recView)
                }

                val warning = ProviderRulesCopy.actionWarning(provider, action)
                if (warning != null) {
                    val warnView = TextView(context).apply {
                        text = "⚠️ $warning"
                        setTextColor(AntTheme.WARNING_TEXT)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                        setPadding(0, AntTheme.dp(context, 4), 0, 0)
                    }
                    addView(warnView)
                }
            }
            container.addView(actionRow)
        }

        // Sleep mode button for this provider
        val sleepBtn = Button(context).apply {
            text = "Configurar Modo Dormir deste App"
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            background = AntTheme.rounded(AntTheme.CARD_SURFACE_HOVER, 10f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 44))
            params.topMargin = AntTheme.dp(context, 6)
            layoutParams = params
            setOnClickListener {
                SleepModeDialog(context, preferences).show(provider) {
                    onDismiss()
                }
            }
        }
        container.addView(sleepBtn)

        val scroll = ScrollView(context).apply { addView(container) }

        AlertDialog.Builder(context)
            .setView(scroll)
            .setPositiveButton("Concluir") { _, _ -> onDismiss() }
            .show()
    }
}
