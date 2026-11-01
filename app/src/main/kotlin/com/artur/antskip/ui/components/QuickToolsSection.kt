package com.artur.antskip.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.artur.antskip.ui.theme.AntTheme

class QuickToolsSection(
    private val context: Context,
    private val onOpenSimulator: () -> Unit,
    private val onOpenDiagnostics: () -> Unit,
    private val onCheckUpdates: () -> Unit,
) {
    fun createView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 16)
            layoutParams = params

            addView(toolButton("🎯 Testar Botão", 0) { onOpenSimulator() })
            addView(toolButton("📊 Logs", 6) { onOpenDiagnostics() })
            addView(toolButton("🔄 Atualizações", 6) { onCheckUpdates() })
        }

    private fun toolButton(title: String, marginStartDp: Int, onClick: () -> Unit): Button =
        Button(context).apply {
            text = title
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 10f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 42), 1f)
            params.marginStart = AntTheme.dp(context, marginStartDp)
            layoutParams = params
            setOnClickListener { onClick() }
        }
}
