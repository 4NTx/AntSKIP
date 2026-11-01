package com.artur.antskip.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.artur.antskip.ui.theme.AntTheme

class QuickToolsSection(
    private val context: Context,
    private val onOpenSimulator: () -> Unit,
    private val onOpenPhrases: () -> Unit,
    private val onOpenDiagnostics: () -> Unit,
    private val onCheckUpdates: () -> Unit,
) {
    fun createView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 16)
            layoutParams = params

            val row1 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val rParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                rParams.bottomMargin = AntTheme.dp(context, 8)
                layoutParams = rParams

                addView(toolButton("🎯 Simulador de Teste", 0) { onOpenSimulator() })
                addView(toolButton("✏️ Editor de Frases", 8) { onOpenPhrases() })
            }

            val row2 = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

                addView(toolButton("📊 Logs & Diagnóstico", 0) { onOpenDiagnostics() })
                addView(toolButton("🔄 Atualizações", 8) { onCheckUpdates() })
            }

            addView(row1)
            addView(row2)
        }

    private fun toolButton(title: String, marginStartDp: Int, onClick: () -> Unit): Button =
        Button(context).apply {
            text = title
            setTextColor(AntTheme.TEXT_PRIMARY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 10f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(0, AntTheme.dp(context, 44), 1f)
            params.marginStart = AntTheme.dp(context, marginStartDp)
            layoutParams = params
            setOnClickListener { onClick() }
        }
}
