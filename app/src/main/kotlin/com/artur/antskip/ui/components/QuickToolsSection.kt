package com.artur.antskip.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.artur.antskip.ui.theme.AntTheme

class QuickToolsSection(
    private val context: Context,
    private val onOpenSimulator: () -> Unit,
    private val onOpenDiagnostics: () -> Unit,
    private val onCheckUpdates: () -> Unit,
) {
    fun createView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 12f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 14)
            layoutParams = params

            addView(toolRow("Testar reconhecimento de botões") { onOpenSimulator() })
            addView(AntTheme.divider(context))
            addView(toolRow("Logs de eventos e cliques") { onOpenDiagnostics() })
            addView(AntTheme.divider(context))
            addView(toolRow("Verificar atualizações") { onCheckUpdates() })
        }

    private fun toolRow(title: String, onClick: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 14), AntTheme.dp(context, 16), AntTheme.dp(context, 14))
            background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 0f, context = context)

            val textView = TextView(context).apply {
                text = title
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                typeface = Typeface.DEFAULT
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val arrow = TextView(context).apply {
                text = "›"
                setTextColor(AntTheme.TEXT_MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                typeface = Typeface.DEFAULT_BOLD
            }

            addView(textView)
            addView(arrow)
            setOnClickListener { onClick() }
        }
}
