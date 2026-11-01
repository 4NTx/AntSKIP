package com.artur.antskip.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.artur.antskip.ui.theme.AntTheme

class HeroStatusCard(
    private val context: Context,
    private val version: String,
    private val onPrimaryAction: () -> Unit,
    private val onSecondaryAction: () -> Unit,
) {
    private lateinit var bannerContainer: LinearLayout
    private lateinit var primaryBtn: Button
    private lateinit var secondaryBtn: Button

    fun createView(
        isServiceEnabled: Boolean,
        isAutomationEnabled: Boolean,
        activeAppsCount: Int,
        totalAppsCount: Int,
    ): LinearLayout =
        LinearLayout(context).apply {
            bannerContainer = this
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 16), AntTheme.dp(context, 16), AntTheme.dp(context, 16))
            background = AntTheme.rounded(AntTheme.WARNING_BG, 14f, AntTheme.WARNING_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 16)
            layoutParams = params

            // Header Row with glowing warning indicator
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val hParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                hParams.bottomMargin = AntTheme.dp(context, 6)
                layoutParams = hParams

                val statusDot = View(context).apply {
                    val dotSize = AntTheme.dp(context, 10)
                    background = AntTheme.rounded(AntTheme.WARNING_TEXT, 5f, null, 0, context)
                    layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                        marginEnd = AntTheme.dp(context, 10)
                    }
                    AntTheme.startSubtlePulse(this)
                }

                val title = TextView(context).apply {
                    text = "Acessibilidade necessária"
                    setTextColor(AntTheme.WARNING_TEXT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                addView(statusDot)
                addView(title)
            }

            val desc = TextView(context).apply {
                text = "Para detectar e clicar nos botões de pular automaticamente na Netflix, Crunchyroll e Prime Video, o AntSKIP precisa da permissão de acessibilidade."
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setLineSpacing(AntTheme.dp(context, 2).toFloat(), 1f)
                setPadding(0, 0, 0, AntTheme.dp(context, 12))
            }

            val buttonsColumn = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                // Step 1: Open Accessibility Settings
                primaryBtn = Button(context).apply {
                    text = "1. Ativar nas Configurações"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rippleGradientButton(
                        startColor = AntTheme.ACCENT_GRADIENT_START,
                        endColor = AntTheme.ACCENT_GRADIENT_END,
                        radiusDp = 10f,
                        context = context,
                    )
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 44))
                    AntTheme.addPressFeedback(this)
                    setOnClickListener { onPrimaryAction() }
                }

                val step1Hint = TextView(context).apply {
                    text = "Vá em 'Aplicativos instalados' > toque em AntSKIP e ative."
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(AntTheme.dp(context, 4), AntTheme.dp(context, 4), 0, AntTheme.dp(context, 10))
                }

                // Step 2: Allow Restricted Settings (Android 13+)
                secondaryBtn = Button(context).apply {
                    text = "2. Chave cinza? Desbloquear aqui"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 10f, context = context)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AntTheme.dp(context, 40))
                    AntTheme.addPressFeedback(this)
                    setOnClickListener { onSecondaryAction() }
                }

                val step2Hint = TextView(context).apply {
                    text = "Se o botão estiver cinza no Android 13+, toque acima e depois nos 3 pontinhos > 'Permitir configurações restritas'."
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(AntTheme.dp(context, 4), AntTheme.dp(context, 4), 0, AntTheme.dp(context, 8))
                }

                // ADB Quick Option
                val adbOption = TextView(context).apply {
                    text = "Tem PC? Toque para copiar comando ADB rápido"
                    setTextColor(AntTheme.ACCENT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setPadding(0, AntTheme.dp(context, 6), 0, AntTheme.dp(context, 2))
                    AntTheme.addPressFeedback(this)
                    setOnClickListener {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val cmd = "adb shell settings put secure enabled_accessibility_services com.artur.antskip/com.artur.antskip.accessibility.AntSkipAccessibilityService && adb shell settings put secure accessibility_enabled 1"
                        clip?.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
                        Toast.makeText(context, "Comando ADB copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                    }
                }

                addView(primaryBtn)
                addView(step1Hint)
                addView(secondaryBtn)
                addView(step2Hint)
                addView(adbOption)
            }

            addView(headerRow)
            addView(desc)
            addView(buttonsColumn)

            updateState(isServiceEnabled, isAutomationEnabled, activeAppsCount, totalAppsCount)
        }

    fun updateState(
        isServiceEnabled: Boolean,
        isAutomationEnabled: Boolean,
        activeAppsCount: Int,
        totalAppsCount: Int,
    ) {
        if (isServiceEnabled) {
            bannerContainer.visibility = View.GONE
        } else {
            bannerContainer.visibility = View.VISIBLE
        }
    }
}
