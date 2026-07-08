package com.artur.antskip.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private val preferences by lazy { PreferenceStore(this) }
    private var updateState: UpdateState = UpdateState.NotChecked
    private var updateCheckStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BACKGROUND
        window.navigationBarColor = BACKGROUND
        render()
        checkForUpdates()
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
            setPadding(dp(16), dp(12), dp(16), dp(18))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        content.addView(headerPanel())
        content.addView(versionPanel())
        content.addView(enablePanel())
        content.addView(safeTestPanel())
        content.addView(appsPanel())
        content.addView(actionsPanel())
        content.addView(advancedPanel())
        content.addView(privacyNote())

        return ScrollView(this).apply {
            setBackgroundColor(BACKGROUND)
            clipToPadding = false
            isFillViewport = false
            addView(content)
            applySystemBarPadding()
        }
    }

    private fun headerPanel(): LinearLayout {
        val enabled = isAccessibilityServiceEnabled()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text("AntSKIP", 30, bold = true, color = TEXT_DARK))
            addView(
                statusBadge(
                    if (enabled) "Acessibilidade ativa" else "Acessibilidade desligada",
                    if (enabled) SUCCESS_DARK else DANGER_DARK,
                    if (enabled) SUCCESS_SOFT else DANGER_SOFT,
                ).withTopMargin(8),
            )
            addView(
                text(
                    if (enabled) {
                        "O app esta pronto para tocar nos botoes permitidos quando eles aparecerem."
                    } else {
                        "Ative o servico de acessibilidade para o AntSKIP conseguir detectar e tocar nos botoes."
                    },
                    15,
                    color = TEXT_MUTED,
                ).withPadding(top = 10, bottom = 14),
            )
        }
    }

    private fun versionPanel(): LinearLayout =
        panel("Versao").apply {
            val state = updateState
            addView(
                text(
                    "Instalada: ${installedVersionName()}",
                    14,
                    bold = true,
                    color = TEXT_DARK,
                ).withPadding(bottom = 6),
            )
            when (state) {
                UpdateState.NotChecked,
                UpdateState.Checking -> {
                    addView(text("Verificando atualizacao...", 14, color = TEXT_MUTED))
                }
                is UpdateState.UpToDate -> {
                    addView(statusBadge("Atualizado", SUCCESS_DARK, SUCCESS_SOFT))
                    addView(text("Ultima versao: ${state.latestVersion}", 13, color = TEXT_MUTED).withPadding(top = 8))
                }
                is UpdateState.UpdateAvailable -> {
                    addView(statusBadge("Atualizacao disponivel", WARNING_DARK, WARNING_SOFT))
                    addView(
                        text(
                            "Nova versao: ${state.latestVersion}",
                            13,
                            color = TEXT_MUTED,
                        ).withPadding(top = 8, bottom = 10),
                    )
                    addView(primaryButton("Abrir download") { openUrl(state.releaseUrl) })
                }
                is UpdateState.CheckFailed -> {
                    addView(statusBadge("Nao foi possivel verificar", DANGER_DARK, DANGER_SOFT))
                    addView(text(state.message, 13, color = TEXT_MUTED).withPadding(top = 8, bottom = 10))
                    addView(secondaryButton("Ver releases") { openUrl(RELEASES_URL) })
                }
            }
        }

    private fun enablePanel(): LinearLayout {
        val enabled = isAccessibilityServiceEnabled()
        return panel("1. Ativacao").apply {
            addView(
                text(
                    if (enabled) {
                        "Acessibilidade ja esta ativa. Use estes botoes se precisar revisar permissao ou liberar configuracoes restritas."
                    } else {
                        "Primeiro ative o servico AntSKIP. Se o Android mostrar configuracoes restritas, abra as informacoes do app e permita."
                    },
                    14,
                    color = TEXT_MUTED,
                ).withPadding(bottom = 12),
            )
            addView(primaryButton(if (enabled) "Abrir acessibilidade" else "Ativar acessibilidade") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            })
            addView(secondaryButton("Permitir configuracoes restritas") {
                openAppInfo()
            }.withTopMargin(8))
        }
    }

    private fun safeTestPanel(): LinearLayout =
        panel("2. Teste seguro").apply {
            addView(step("A", "Deixe ligado apenas Netflix ou Crunchyroll enquanto testa."))
            addView(step("B", "Em Regras por app, comece com somente Aberturas e intros."))
            addView(step("C", "Abra um episodio com Pular abertura. O app deve mostrar Pulando abertura."))
            addView(
                warningText(
                    "Proximo episodio e mais arriscado porque alguns apps usam textos genericos como Proximo ou Next.",
                ).withPadding(top = 8),
            )
        }

    private fun appsPanel(): LinearLayout =
        panel("3. Apps monitorados").apply {
            addView(
                switchRow(
                    "Automacao ligada",
                    "Pausa ou libera todos os cliques automaticos sem apagar suas regras.",
                    preferences.isAutomationEnabled,
                ) { preferences.setAutomationEnabled(it) },
            )
            addView(separator())
            StreamingProvider.entries.forEachIndexed { index, provider ->
                addView(
                    switchRow(provider.label, provider.description, preferences.isProviderEnabled(provider)) {
                        preferences.setProviderEnabled(provider, it)
                    },
                )
                if (index != StreamingProvider.entries.lastIndex) addView(separator())
            }
        }

    private fun actionsPanel(): LinearLayout =
        panel("4. Padroes globais").apply {
            addView(
                text(
                    "Estes sao os padroes. Regras por app podem liberar ou bloquear uma acao so em um streaming.",
                    14,
                    color = TEXT_MUTED,
                ).withPadding(bottom = 8),
            )
            SkipAction.entries.forEachIndexed { index, action ->
                addView(
                    switchRow(action.label, action.description, preferences.isActionEnabled(action)) {
                        preferences.setActionEnabled(action, it)
                    },
                )
                if (index != SkipAction.entries.lastIndex) addView(separator())
            }
        }

    private fun advancedPanel(): LinearLayout =
        panel("5. Ajustes finos").apply {
            addView(
                text(
                    "Use quando algo nao clicar ou quando clicar no botao errado.",
                    14,
                    color = TEXT_MUTED,
                ).withPadding(bottom = 10),
            )
            addView(primaryButton("Regras por app") { showProviderPicker() })
            addView(secondaryButton("Ensinar novas frases") { showActionPicker() }.withTopMargin(8))
            addView(secondaryButton("Editar lista de bloqueio") { showBlockListEditor() }.withTopMargin(8))
        }

    private fun privacyNote(): TextView =
        text(
            "Sem captura de tela, overlay, gravacao de video ou inspecao de rede. O AntSKIP usa apenas textos de acessibilidade.",
            13,
            color = TEXT_MUTED,
        ).apply {
            gravity = Gravity.CENTER
        }.withPadding(top = 12, bottom = 4)

    private fun showProviderPicker() {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        StreamingProvider.entries.forEach { provider ->
            list.addView(secondaryButton(provider.label) { showProviderRules(provider) }.withTopMargin(8))
        }

        AlertDialog.Builder(this)
            .setTitle("Regras por app")
            .setMessage("Escolha um app para decidir quais acoes ele pode executar.")
            .setView(ScrollView(this).apply { addView(list) })
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showProviderRules(provider: StreamingProvider) {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), 0, dp(4), 0)
        }
        SkipAction.entries.forEachIndexed { index, action ->
            list.addView(
                switchRow(
                    action.label,
                    action.description,
                    preferences.isActionEnabledForProvider(provider, action),
                ) {
                    preferences.setActionEnabledForProvider(provider, action, it)
                },
            )
            if (index != SkipAction.entries.lastIndex) list.addView(separator())
        }

        AlertDialog.Builder(this)
            .setTitle("Regras: ${provider.label}")
            .setMessage("Estas regras valem so para ${provider.label}.")
            .setView(ScrollView(this).apply { addView(list) })
            .setPositiveButton("Fechar") { _, _ -> render() }
            .show()
    }

    private fun showActionPicker() {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        SkipAction.entries.forEach { action ->
            list.addView(secondaryButton(action.label) { showPhraseEditor(action) }.withTopMargin(8))
        }

        AlertDialog.Builder(this)
            .setTitle("Ensinar novas frases")
            .setMessage("Escolha a acao e adicione o texto exatamente como aparece no streaming.")
            .setView(ScrollView(this).apply { addView(list) })
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showPhraseEditor(action: SkipAction) {
        val input = EditText(this).apply {
            setText(preferences.customPhrases(action).joinToString("\n"))
            minLines = 6
            gravity = Gravity.TOP
            hint = "Pular abertura\nSkip Intro\nProximo"
        }

        AlertDialog.Builder(this)
            .setTitle("Frases: ${action.label}")
            .setMessage("Uma frase por linha. Repetidas sao removidas. Acentos e maiusculas sao ignorados.")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val phrases = input.toCleanSet()
                preferences.setCustomPhrases(action, phrases)
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showBlockListEditor() {
        val input = EditText(this).apply {
            setText(preferences.blockedPhrases().joinToString("\n"))
            minLines = 7
            gravity = Gravity.TOP
            hint = "Trailer\nReiniciar\nMais informacoes"
        }

        AlertDialog.Builder(this)
            .setTitle("Lista de bloqueio")
            .setMessage("O AntSKIP nunca clica em botoes que contenham estas frases. A lista de bloqueio vence as frases ensinadas.")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                preferences.setBlockedPhrases(input.toCleanSet())
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
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(10))

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        text(title, 16, bold = true, color = TEXT_DARK).apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        },
                    )
                    addView(
                        Switch(context).apply {
                            minWidth = dp(56)
                            isChecked = checked
                            setOnCheckedChangeListener { _, value -> onChanged(value) }
                        },
                    )
                },
            )
            addView(text(description, 13, color = TEXT_MUTED).withPadding(top = 4))
        }

    private fun step(number: String, value: String): LinearLayout =
        LinearLayout(this).apply {
            gravity = Gravity.TOP
            setPadding(0, dp(7), 0, dp(7))
            addView(
                text(number, 12, bold = true, color = Color.WHITE).apply {
                    gravity = Gravity.CENTER
                    background = rounded(ACCENT, dp(12))
                    layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { topMargin = dp(1) }
                },
            )
            addView(
                text(value, 14, color = TEXT_DARK).apply {
                    setPadding(dp(10), 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                },
            )
        }

    private fun panel(title: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = rounded(Color.WHITE, dp(8), STROKE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { setMargins(0, 0, 0, dp(12)) }
            addView(text(title, 18, bold = true, color = TEXT_DARK).withPadding(bottom = 8))
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
            textSize = 14f
            minHeight = dp(48)
            background = rounded(backgroundColor, dp(8))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

    private fun statusBadge(label: String, textColor: Int, backgroundColor: Int): TextView =
        text(label, 13, bold = true, color = textColor).apply {
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = rounded(backgroundColor, dp(16))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

    private fun warningText(value: String): TextView =
        text(value, 13, bold = true, color = WARNING_DARK).apply {
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(WARNING_SOFT, dp(8), WARNING_STROKE)
        }

    private fun separator(): View =
        View(this).apply {
            setBackgroundColor(STROKE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1),
            )
        }

    private fun text(value: String, sizeSp: Int, bold: Boolean = false, color: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = sizeSp.toFloat()
            setTextColor(color)
            includeFontPadding = true
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun openAppInfo() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            },
        )
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun checkForUpdates() {
        if (updateCheckStarted) return
        updateCheckStarted = true
        updateState = UpdateState.Checking
        Thread {
            val result = runCatching {
                val connection = (URL(LATEST_RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = UPDATE_TIMEOUT_MS
                    readTimeout = UPDATE_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "AntSKIP/${installedVersionName()}")
                }
                try {
                    if (connection.responseCode !in 200..299) {
                        error("HTTP ${connection.responseCode}")
                    }
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val latestVersion = json.getString("tag_name").removePrefix("v")
                    val releaseUrl = json.getString("html_url")
                    if (isNewerVersion(latestVersion, installedVersionName())) {
                        UpdateState.UpdateAvailable(latestVersion, releaseUrl)
                    } else {
                        UpdateState.UpToDate(latestVersion)
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrElse {
                UpdateState.CheckFailed("Confira sua conexao ou abra a pagina de releases manualmente.")
            }

            runOnUiThread {
                updateState = result
                render()
            }
        }.start()
    }

    private fun installedVersionName(): String =
        try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (_: PackageManager.NameNotFoundException) {
            "?"
        }

    private fun isNewerVersion(latest: String, installed: String): Boolean {
        val latestParts = latest.toVersionParts()
        val installedParts = installed.toVersionParts()
        val maxSize = maxOf(latestParts.size, installedParts.size)
        repeat(maxSize) { index ->
            val latestPart = latestParts.getOrElse(index) { 0 }
            val installedPart = installedParts.getOrElse(index) { 0 }
            if (latestPart != installedPart) return latestPart > installedPart
        }
        return false
    }

    private fun String.toVersionParts(): List<Int> =
        removePrefix("v")
            .split('.')
            .map { value -> value.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

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

    private fun ScrollView.applySystemBarPadding() {
        setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop + dp(8),
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom + dp(8),
            )
            insets
        }
        requestApplyInsets()
    }

    private fun EditText.toCleanSet(): Set<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toCollection(linkedSetOf())

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
        const val LATEST_RELEASE_API_URL = "https://api.github.com/repos/4NTx/AntSKIP/releases/latest"
        const val RELEASES_URL = "https://github.com/4NTx/AntSKIP/releases/latest"
        const val UPDATE_TIMEOUT_MS = 5_000
        const val BACKGROUND = 0xFFF6F7F9.toInt()
        const val TEXT_DARK = 0xFF1F2933.toInt()
        const val TEXT_MUTED = 0xFF637083.toInt()
        const val ACCENT = 0xFFF47521.toInt()
        const val ACCENT_DARK = 0xFFB84A0D.toInt()
        const val SUCCESS_DARK = 0xFF127A3A.toInt()
        const val SUCCESS_SOFT = 0xFFE1F6E8.toInt()
        const val DANGER_DARK = 0xFF9F1D1D.toInt()
        const val DANGER_SOFT = 0xFFFFE4E4.toInt()
        const val WARNING_DARK = 0xFF7A4B00.toInt()
        const val WARNING_SOFT = 0xFFFFF4D8.toInt()
        const val WARNING_STROKE = 0xFFFFD990.toInt()
        const val STROKE = 0xFFE3E7ED.toInt()
    }

    private sealed interface UpdateState {
        data object NotChecked : UpdateState
        data object Checking : UpdateState
        data class UpToDate(val latestVersion: String) : UpdateState
        data class UpdateAvailable(val latestVersion: String, val releaseUrl: String) : UpdateState
        data class CheckFailed(val message: String) : UpdateState
    }
}
