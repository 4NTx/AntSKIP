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
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.ui.theme.AntTheme

class ActionToggleCard(
    private val context: Context,
    private val action: SkipAction,
    private val preferences: PreferenceStore,
    private val onToggleChanged: (Boolean) -> Unit,
) {
    fun createRowView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 12), AntTheme.dp(context, 16), AntTheme.dp(context, 12))
            background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 0f, context = context)

            // Visual Action Badge
            val badgeLabel = when (action) {
                SkipAction.INTRO -> "INTRO"
                SkipAction.RECAP -> "RECAP"
                SkipAction.CREDITS -> "FIM"
                SkipAction.NEXT_EPISODE -> "PROX"
            }
            val actionBadge = AntTheme.createBadge(
                context = context,
                text = badgeLabel,
                bgColor = AntTheme.CARD_SURFACE_HOVER,
                textColor = AntTheme.ACCENT_PRIMARY,
                sizeDp = 38,
                radiusDp = 10f,
                textSizeSp = 10f,
            ).apply {
                val bParams = LinearLayout.LayoutParams(AntTheme.dp(context, 38), AntTheme.dp(context, 38))
                bParams.marginEnd = AntTheme.dp(context, 14)
                layoutParams = bParams
            }

            val textColumn = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val title = TextView(context).apply {
                    text = action.label
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                    typeface = Typeface.DEFAULT_BOLD
                }

                val desc = TextView(context).apply {
                    text = action.description
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }

                addView(title)
                addView(desc)
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isActionEnabled(action)
                setOnCheckedChangeListener { _, isChecked ->
                    preferences.setActionEnabled(action, isChecked)
                    onToggleChanged(isChecked)
                }
            }

            addView(actionBadge)
            addView(textColumn)
            addView(sw)

            AntTheme.addPressFeedback(this)
            setOnClickListener {
                sw.toggle()
            }
        }
}
