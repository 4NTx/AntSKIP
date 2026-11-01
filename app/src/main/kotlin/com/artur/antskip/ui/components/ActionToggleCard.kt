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
    fun createView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 12), AntTheme.dp(context, 16), AntTheme.dp(context, 12))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 12f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 8)
            layoutParams = params

            val topRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val title = TextView(context).apply {
                    text = action.label
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val sw = Switch(context).apply {
                    isChecked = preferences.isActionEnabled(action)
                    setOnCheckedChangeListener { _, isChecked ->
                        preferences.setActionEnabled(action, isChecked)
                        onToggleChanged(isChecked)
                    }
                }

                addView(title)
                addView(sw)
            }

            val desc = TextView(context).apply {
                text = action.description
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding(0, AntTheme.dp(context, 4), 0, 0)
            }

            addView(topRow)
            addView(desc)
        }
}
