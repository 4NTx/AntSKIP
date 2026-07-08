package com.artur.antskip.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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

class MainActivity : Activity() {
    private val preferences by lazy { PreferenceStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContent())
    }

    private fun createContent(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        content.addView(text("AntSKIP", sizeSp = 26, bold = true))
        content.addView(
            text(
                "Ative a acessibilidade, escolha os apps de streaming e os botoes que o AntSKIP deve tocar automaticamente.",
                sizeSp = 15,
            ).withPadding(bottom = 16, top = 8),
        )

        content.addView(
            switchRow("Automacao ligada", preferences.isAutomationEnabled) {
                preferences.setAutomationEnabled(it)
            }.withPadding(vertical = 8),
        )

        content.addView(sectionTitle("Apps"))
        StreamingProvider.entries.forEach { provider ->
            content.addView(
                switchRow(provider.label, preferences.isProviderEnabled(provider)) {
                    preferences.setProviderEnabled(provider, it)
                },
            )
        }

        content.addView(sectionTitle("Skips"))
        SkipAction.entries.forEach { action ->
            content.addView(
                switchRow(action.label, preferences.isActionEnabled(action)) {
                    preferences.setActionEnabled(action, it)
                },
            )
        }

        content.addView(button("Abrir acessibilidade") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(button("Abrir configuracoes do app") {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        })
        content.addView(
            text(
                "Leve por desenho: reage apenas a eventos de acessibilidade dos apps selecionados, sem overlay, captura de tela ou verificacao continua.",
                sizeSp = 13,
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }.withPadding(top = 18),
        )

        return ScrollView(this).apply { addView(content) }
    }

    private fun switchRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit): Switch =
        Switch(this).apply {
            text = label
            textSize = 16f
            isChecked = checked
            setPadding(0, dp(6), 0, dp(6))
            setOnCheckedChangeListener { _, value -> onChanged(value) }
        }

    private fun sectionTitle(value: String): TextView =
        text(value, sizeSp = 18, bold = true).withPadding(top = 18, bottom = 6)

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { onClick() }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

    private fun text(value: String, sizeSp: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = sizeSp.toFloat()
            setTextColor(Color.rgb(32, 32, 32))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun <T : TextView> T.withPadding(
        top: Int = 0,
        bottom: Int = 0,
        vertical: Int = 0,
    ): T = apply {
        val resolvedTop = if (vertical > 0) vertical else top
        val resolvedBottom = if (vertical > 0) vertical else bottom
        setPadding(0, dp(resolvedTop), 0, dp(resolvedBottom))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
