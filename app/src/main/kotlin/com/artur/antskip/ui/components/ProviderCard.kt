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
import com.artur.antskip.ui.theme.AntTheme

class ProviderCard(
    private val context: Context,
    private val provider: StreamingProvider,
    private val preferences: PreferenceStore,
    private val isInstalled: Boolean,
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
                    text = provider.label
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(title)

                if (!isInstalled) {
                    val sub = TextView(context).apply {
                        text = "Não instalado neste aparelho"
                        setTextColor(AntTheme.TEXT_MUTED)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    }
                    addView(sub)
                }
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isProviderEnabled(provider)
                setOnCheckedChangeListener { _, isChecked ->
                    preferences.setProviderEnabled(provider, isChecked)
                    onToggleChanged(isChecked)
                }
            }

            addView(textColumn)
            addView(sw)
        }
}
