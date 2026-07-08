package com.artur.antskip.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.artur.antskip.accessibility.AntSkipAccessibilityService
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider

class MainActivity : Activity() {
    private val preferences by lazy { PreferenceStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        setContentView(createContent())
    }

    private fun createContent(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            setBackgroundColor(BACKGROUND)
        }

        content.addView(header())
        content.addView(statusPanel())
        content.addView(setupPanel())
        content.addView(providerPanel())
        content.addView(actionPanel())
        content.addView(customPhrasePanel())
        content.addView(privacyNote())

        return ScrollView(this).apply { addView(content) }
    }

    private fun header(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("AntSKIP", 30, bold = true, color = TEXT_DARK))
            addView(
                text(
                    "Pula aberturas, resumos, creditos e proximo episodio quando o botao aparece no app de streaming.",
                    15,
                    color = TEXT_MUTED,
                ).withPadding(top = 6, bottom = 14),
            )
        }

    private fun statusPanel(): LinearLayout {
        val enabled = isAccessibilityServiceEnabled()
        return panel().apply {
            addView(rowTitle("Status", if (enabled) "Acessibilidade ativa" else "Precisa ativar"))
            addView(
                text(
                    if (enabled) {
                        "Pronto para detectar botoes nos apps selecionados."
                    } else {
                        "Ative o servico AntSKIP em Acessibilidade. Se aparecer configuracoes restritas, libere na tela de informacoes do app."
                    },
                    14,
                    color = TEXT_MUTED,
                ).withPadding(top = 8, bottom = 12),
            )
            addView(primaryButton(if (enabled) "Abrir acessibilidade" else "Ativar acessibilidade") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
            addView(secondaryButton("Abrir informacoes do app") {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    },
                )
            }.withTopMargin(8))
        }
    }

    private fun setupPanel(): LinearLayout =
        panel("Como testar").apply {
            addView(step("1", "Deixe Automacao ligada."))
            addView(step("2", "Ative Netflix ou Crunchyroll em Apps monitorados."))
            addView(step("3", "No app de streaming, abra um episodio com Pular abertura ou Proximo."))
            addView(step("4", "Quando o AntSKIP tocar no botao, aparece uma mensagem na tela."))
        }

    private fun providerPanel(): LinearLayout =
        panel("Apps monitorados").apply {
            addView(
                switchRow(
                    "Automacao ligada",
                    "Chave geral. Desligue para pausar tudo sem perder suas escolhas.",
                    preferences.isAutomationEnabled,
                ) { preferences.setAutomationEnabled(it) },
            )
            StreamingProvider.entries.forEach { provider ->
                addView(
                    switchRow(provider.label, provider.description, preferences.isProviderEnabled(provider)) {
                        preferences.setProviderEnabled(provider, it)
                    },
                )
            }
        }

    private fun actionPanel(): LinearLayout =
        panel("O que pular").apply {
            SkipAction.entries.forEach { action ->
                addView(
                    switchRow(action.label, action.description, preferences.isActionEnabled(action)) {
                        preferences.setActionEnabled(action, it)
                    },
                )
            }
        }

    private fun customPhrasePanel(): LinearLayout =
        panel("Ensinar frases").apply {
            addView(
                text(
                    "Se um app mostrar outro texto, adicione aqui exatamente como aparece. Uma frase por linha.",
                    14,
                    color = TEXT_MUTED,
                ).withPadding(bottom = 10),
            )
            SkipAction.entries.forEach { action ->
                addView(secondaryButton("Editar frases: ${action.label}") { showPhraseEditor(action) }.withTopMargin(8))
            }
        }

    private fun privacyNote(): TextView =
        text(
            "Sem captura de tela, sem overlay e sem gravar video. O app reage somente aos textos de acessibilidade dos apps selecionados.",
            13,
            color = TEXT_MUTED,
        ).apply { gravity = Gravity.CENTER }.withPadding(top = 16)

    private fun showPhraseEditor(action: SkipAction) {
        val input = EditText(this).apply {
            setText(preferences.customPhrases(action).joinToString("\n"))
            minLines = 5
            gravity = Gravity.TOP
            hint = "Ex.: Pular abertura\nSkip Intro\nProximo"
        }

        AlertDialog.Builder(this)
            .setTitle("Frases para ${action.label}")
            .setMessage("Uma frase por linha. O AntSKIP ignora acentos e maiusculas.")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val phrases = input.text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toCollection(linkedSetOf())
                preferences.setCustomPhrases(action, phrases)
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun switchRow(
        title: String,
        description: String,
        checked: Boolean,
        onChanged: (Boolean) -> Unit,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))

            val copy = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(title, 16, bold = true, color = TEXT_DARK))
                addView(text(description, 13, color = TEXT_MUTED).withPadding(top = 3))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            addView(copy)
            addView(
                Switch(context).apply {
                    isChecked = checked
                    setOnCheckedChangeListener { _, value -> onChanged(value) }
                },
            )
        }

    private fun rowTitle(title: String, badge: String): LinearLayout =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(
                text(title, 18, bold = true, color = TEXT_DARK).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                },
            )
            addView(
                text(badge, 12, bold = true, color = ACCENT_DARK).apply {
                    setPadding(dp(10), dp(5), dp(10), dp(5))
                    background = rounded(ACCENT_SOFT, dp(16))
                },
            )
        }

    private fun step(number: String, value: String): LinearLayout =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(7), 0, dp(7))
            addView(
                text(number, 13, bold = true, color = Color.WHITE).apply {
                    gravity = Gravity.CENTER
                    background = rounded(ACCENT, dp(14))
                    layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
                },
            )
            addView(
                text(value, 14, color = TEXT_DARK).apply {
                    setPadding(dp(10), 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                },
            )
        }

    private fun panel(title: String? = null): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(Color.WHITE, dp(8), STROKE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(0, 0, 0, dp(12)) }
            if (title != null) addView(text(title, 18, bold = true, color = TEXT_DARK).withPadding(bottom = 8))
        }

    private fun primaryButton(label: String, onClick: () -> Unit): Button =
        button(label, ACCENT, Color.WHITE, onClick)

    private fun secondaryButton(label: String, onClick: () -> Unit): Button =
        button(label, Color.WHITE, ACCENT_DARK, onClick).apply {
            background = rounded(Color.WHITE, dp(8), ACCENT)
        }

    private fun button(label: String, backgroundColor: Int, textColor: Int, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(textColor)
            background = rounded(backgroundColor, dp(8))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

    private fun text(value: String, sizeSp: Int, bold: Boolean = false, color: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = sizeSp.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, AntSkipAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }

    private fun <T : TextView> T.withPadding(top: Int = 0, bottom: Int = 0): T = apply {
        setPadding(paddingLeft, dp(top), paddingRight, dp(bottom))
    }

    private fun <T : TextView> T.withTopMargin(value: Int): T = apply {
        layoutParams = (layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )).apply { topMargin = dp(value) }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        const val BACKGROUND = 0xFFF6F7F9.toInt()
        const val TEXT_DARK = 0xFF1F2933.toInt()
        const val TEXT_MUTED = 0xFF637083.toInt()
        const val ACCENT = 0xFFF47521.toInt()
        const val ACCENT_DARK = 0xFFB84A0D.toInt()
        const val ACCENT_SOFT = 0xFFFFE9DC.toInt()
        const val STROKE = 0xFFE3E7ED.toInt()
    }
}
