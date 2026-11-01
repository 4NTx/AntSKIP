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
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
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

            addView(textColumn)
            addView(sw)
        }
}
