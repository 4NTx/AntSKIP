package com.artur.antskip.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

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
        content.addView(sleepProtectionStatusPanel())
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
                        "O AntSKIP so toca nos apps e tipos de botao que voce deixou ligados abaixo."
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
                        "Acessibilidade ativa. O app ainda respeita as chaves de automacao, apps e tipos de botao abaixo."
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

    private fun sleepProtectionStatusPanel(): LinearLayout {
        val activeProviders = StreamingProvider.entries.filter { preferences.isSleepProtectionActive(it) }
        return panel("Modo dormir").apply {
            if (activeProviders.isEmpty()) {
                addView(text("Nenhuma pausa de proximo episodio ativa.", 14, color = TEXT_MUTED))
            } else {
                activeProviders.forEach { provider ->
                    val pauseUntil = preferences.nextEpisodePauseUntilMillis(provider)
                    val status = if (pauseUntil > System.currentTimeMillis()) {
                        "${provider.label}: ate ${formatTimestamp(pauseUntil)}"
                    } else {
                        "${provider.label}: horario diario ativo"
                    }
                    addView(statusBadge(status, WARNING_DARK, WARNING_SOFT).withTopMargin(6))
                }
            }
            addView(secondaryButton("Configurar por app") { showProviderPicker() }.withTopMargin(10))
        }
    }

    private fun safeTestPanel(): LinearLayout =
        panel("2. Teste seguro").apply {
            addView(step("A", "Deixe ligado apenas Netflix ou Crunchyroll enquanto testa."))
            addView(step("B", "Em Regras por app, comece com somente Aberturas e intros."))
            addView(step("C", "Abra um episodio com Pular abertura. O app deve mostrar Pulando abertura."))
            addView(
                warningText(
                    "Proximo episodio avanca para outro episodio. Ligue apenas quando quiser continuar sem confirmar.",
                ).withPadding(top = 8),
            )
        }

    private fun appsPanel(): LinearLayout =
        panel("3. Apps monitorados").apply {
            addView(
                switchRow(
                    "Automacao ligada",
                    "Ligado: permite cliques automaticos. Desligado: pausa tudo sem apagar suas regras.",
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
                    "Estes padroes valem para apps sem regra propria. Em Regras por app, a escolha de cada app vence o padrao global.",
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
                    "Use quando quiser decidir app por app, ensinar uma frase nova ou bloquear um texto perigoso.",
                    14,
                    color = TEXT_MUTED,
                ).withPadding(bottom = 10),
            )
            addView(primaryButton("Regras por app") { showProviderPicker() })
            addView(secondaryButton("Ensinar novas frases") { showActionPicker() }.withTopMargin(8))
            addView(secondaryButton("Editar lista de bloqueio") { showBlockListEditor() }.withTopMargin(8))
            addView(secondaryButton("Logs de diagnostico") { showDiagnosticLogs() }.withTopMargin(8))
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
            setPadding(dp(2), dp(4), dp(2), dp(8))
        }
        StreamingProvider.entries.forEach { provider ->
            list.addView(providerPickerRow(provider).withTopMargin(8))
        }

        AlertDialog.Builder(this)
            .setTitle("Regras por app")
            .setMessage("Escolha um app e defina exatamente quais botoes o AntSKIP pode tocar nele.")
            .setView(ScrollView(this).apply { addView(list) })
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showProviderRules(provider: StreamingProvider) {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), 0, dp(2), dp(6))
        }
        list.addView(providerRulesHeader(provider))
        list.addView(sectionTitle("Cliques automaticos").withTopMargin(12))
        SkipAction.entries.forEachIndexed { index, action ->
            val ruleRow = ruleSwitchRow(
                title = action.label,
                description = ProviderRulesCopy.actionDescription(provider, action),
                checked = preferences.isActionEnabledForProvider(provider, action),
                recommendation = ProviderRulesCopy.actionRecommendation(provider, action),
                warning = ProviderRulesCopy.actionWarning(provider, action),
            ) {
                preferences.setActionEnabledForProvider(provider, action, it)
            }
            list.addView(if (index == 0) ruleRow else ruleRow.withTopMargin(8))
        }
        list.addView(sectionTitle("Protecao contra maratona acidental").withTopMargin(12))
        list.addView(nextEpisodeSchedulePanel(provider))

        AlertDialog.Builder(this)
            .setTitle(provider.label)
            .setView(ScrollView(this).apply { addView(list) })
            .setPositiveButton("Fechar") { _, _ -> render() }
            .show()
    }

    private fun providerPickerRow(provider: StreamingProvider): LinearLayout {
        val enabled = preferences.isProviderEnabled(provider)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, dp(10), STROKE)
            elevation = dp(1).toFloat()
            foreground = selectableForeground()
            isClickable = true
            isFocusable = true
            minimumHeight = dp(84)
            contentDescription = "${provider.label}. ${ProviderRulesCopy.summary(provider, preferences)}"
            setOnClickListener { showProviderRules(provider) }

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        text(provider.label, 17, bold = true, color = TEXT_DARK).apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        },
                    )
                    addView(
                        statusBadge(
                            if (enabled) "Monitorado" else "Desligado",
                            if (enabled) SUCCESS_DARK else TEXT_MUTED,
                            if (enabled) SUCCESS_SOFT else DISABLED_SOFT,
                        ),
                    )
                },
            )
            addView(text(ProviderRulesCopy.summary(provider, preferences), 13, color = TEXT_MUTED).withPadding(top = 6))
        }
    }

    private fun providerRulesHeader(provider: StreamingProvider): LinearLayout {
        val enabled = preferences.isProviderEnabled(provider)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(if (enabled) SUCCESS_SOFT else DANGER_SOFT, dp(12), if (enabled) SUCCESS_DARK else DANGER_DARK)
            elevation = dp(1).toFloat()
            addView(text("Controle de automacao", 18, bold = true, color = TEXT_DARK).withPadding(bottom = 8))
            addView(
                statusBadge(
                    if (enabled) "App monitorado" else "App desligado",
                    if (enabled) SUCCESS_DARK else DANGER_DARK,
                    Color.WHITE,
                ),
            )
            addView(text(ProviderRulesCopy.summary(provider, preferences), 14, color = TEXT_DARK).withPadding(top = 8))
            addView(text(ProviderRulesCopy.guidance(provider), 13, color = TEXT_MUTED).withPadding(top = 6))
        }
    }

    private fun ruleSwitchRow(
        title: String,
        description: String,
        checked: Boolean,
        recommendation: String?,
        warning: String?,
        onChanged: (Boolean) -> Unit,
    ): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(if (checked) RULE_ACTIVE_SOFT else Color.WHITE, dp(10), STROKE)
            elevation = dp(1).toFloat()
            var currentChecked = checked
            lateinit var stateLabel: TextView
            val ruleCard = this

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(
                        LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            addView(text(title, 16, bold = true, color = TEXT_DARK))
                            recommendation?.let {
                                addView(text(it, 12, bold = true, color = ACCENT_DARK).withPadding(top = 2))
                            }
                        },
                    )
                    addView(
                        Switch(context).apply {
                            minWidth = dp(56)
                            isChecked = checked
                            contentDescription = "$title em regras por app"
                            setOnCheckedChangeListener { _, value ->
                                currentChecked = value
                                onChanged(value)
                                ruleCard.background = rounded(if (value) RULE_ACTIVE_SOFT else Color.WHITE, dp(10), STROKE)
                                stateLabel.setStatePill(ruleStateText(value), value)
                            }
                        },
                    )
                },
            )
            addView(text(description, 13, color = TEXT_MUTED).withPadding(top = 6))
            warning?.let { addView(warningText(it).withPadding(top = 8)) }
            stateLabel = statePill(ruleStateText(currentChecked), checked).withTopMargin(6)
            addView(stateLabel)
        }

    private fun ruleStateText(checked: Boolean): String =
        if (checked) "Ligado neste app." else "Desligado neste app."

    private fun globalStateText(checked: Boolean): String =
        if (checked) "Estado atual: ativo." else "Estado atual: desligado."

    private fun sectionTitle(value: String): TextView =
        text(value, 14, bold = true, color = TEXT_MUTED).apply {
            setPadding(0, dp(2), 0, dp(6))
        }

    private fun nextEpisodeSchedulePanel(provider: StreamingProvider): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.WHITE, dp(10), STROKE)
            elevation = dp(1).toFloat()
            addView(temporaryNextEpisodePausePanel(provider))
            addView(separator())
            addView(
                switchRow(
                    "Protecao forte ao dormir",
                    "Ligado: enquanto o modo dormir estiver ativo, tambem bloqueia creditos para reduzir avanco acidental.",
                    preferences.blocksCreditsDuringSleep(provider),
                ) {
                    preferences.setBlocksCreditsDuringSleep(provider, it)
                },
            )
            addView(separator())
            addView(
                switchRow(
                    "Pausar Proximo episodio por horario",
                    "Ligado: bloqueia avanco automatico para outro episodio todos os dias neste horario.",
                    preferences.isNextEpisodeScheduleEnabled(provider),
                ) {
                    preferences.setNextEpisodeScheduleEnabled(provider, it)
                },
            )
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(6), 0, 0)
                    addView(
                        secondaryButton("Inicio: ${formatMinutes(preferences.nextEpisodeScheduleStartMinutes(provider))}") {
                            showTimePicker(preferences.nextEpisodeScheduleStartMinutes(provider)) { minutes ->
                                preferences.setNextEpisodeScheduleStartMinutes(provider, minutes)
                                showProviderRules(provider)
                            }
                        }.apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        },
                    )
                    addView(
                        secondaryButton("Fim: ${formatMinutes(preferences.nextEpisodeScheduleEndMinutes(provider))}") {
                            showTimePicker(preferences.nextEpisodeScheduleEndMinutes(provider)) { minutes ->
                                preferences.setNextEpisodeScheduleEndMinutes(provider, minutes)
                                showProviderRules(provider)
                            }
                        }.apply {
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                                leftMargin = dp(8)
                            }
                        },
                    )
                },
            )
        }

    private fun temporaryNextEpisodePausePanel(provider: StreamingProvider): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
            val pauseUntil = preferences.nextEpisodePauseUntilMillis(provider)
            val isPaused = pauseUntil > System.currentTimeMillis()
            addView(
                text(
                    if (isPaused) {
                        "Proximo episodio pausado ate ${formatTimestamp(pauseUntil)}"
                    } else {
                        "Pausar Proximo episodio agora"
                    },
                    16,
                    bold = true,
                    color = TEXT_DARK,
                ).withPadding(bottom = 6),
            )
            addView(text("Bloqueia temporariamente so em ${provider.label}.", 13, color = TEXT_MUTED))
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(8), 0, 0)
                    addView(pauseButton(provider, "1h", 1).weightedButton())
                    addView(pauseButton(provider, "2h", 2).weightedButton(leftMargin = 8))
                    addView(pauseButton(provider, "4h", 4).weightedButton(leftMargin = 8))
                },
            )
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(8), 0, 0)
                    addView(
                        secondaryButton("Ate horario") {
                            showTimePicker(currentMinutesOfDay()) { minutes ->
                                preferences.setNextEpisodePauseUntilMillis(provider, nextOccurrenceMillis(minutes))
                                showProviderRules(provider)
                            }
                        }.weightedButton(),
                    )
                    addView(
                        secondaryButton("Cancelar pausa") {
                            preferences.clearNextEpisodePause(provider)
                            showProviderRules(provider)
                        }.weightedButton(leftMargin = 8),
                    )
                },
            )
        }

    private fun pauseButton(provider: StreamingProvider, label: String, hours: Long): Button =
        secondaryButton(label) {
            preferences.setNextEpisodePauseUntilMillis(provider, System.currentTimeMillis() + hours * 60 * 60 * 1_000)
            showProviderRules(provider)
        }

    private fun showTimePicker(currentMinutes: Int, onSelected: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, hour, minute -> onSelected(hour * 60 + minute) },
            currentMinutes / 60,
            currentMinutes % 60,
            true,
        ).show()
    }

    private fun formatMinutes(minutes: Int): String =
        String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)

    private fun formatTimestamp(millis: Long): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        return formatMinutes(dateTime.hour * 60 + dateTime.minute)
    }

    private fun currentMinutesOfDay(): Int {
        val now = LocalDateTime.now()
        return now.hour * 60 + now.minute
    }

    private fun nextOccurrenceMillis(minutes: Int): Long {
        val now = LocalDateTime.now()
        val selected = LocalDateTime.of(
            LocalDate.now(),
            java.time.LocalTime.of(minutes / 60, minutes % 60),
        )
        val target = if (selected.isAfter(now)) selected else selected.plusDays(1)
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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

    private fun showDiagnosticLogs() {
        val logs = preferences.diagnosticLogs().ifBlank { "Nenhum log registrado ainda." }
        val output = TextView(this).apply {
            text = logs
            textSize = 12f
            setTextColor(TEXT_DARK)
            typeface = Typeface.MONOSPACE
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        AlertDialog.Builder(this)
            .setTitle("Logs de diagnostico")
            .setView(ScrollView(this).apply { addView(output) })
            .setPositiveButton("Copiar tudo") { _, _ -> copyToClipboard("AntSKIP logs", logs) }
            .setNeutralButton("Limpar") { _, _ ->
                preferences.clearDiagnosticLogs()
                render()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
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
            var currentChecked = checked
            lateinit var stateLabel: TextView

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
                            contentDescription = title
                            setOnCheckedChangeListener { _, value ->
                                currentChecked = value
                                onChanged(value)
                                stateLabel.setStatePill(globalStateText(value), value)
                            }
                        },
                    )
                },
            )
            addView(text(description, 13, color = TEXT_MUTED).withPadding(top = 4))
            stateLabel = statePill(globalStateText(currentChecked), checked).withTopMargin(4)
            addView(stateLabel)
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
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(Color.WHITE, dp(10), STROKE)
            elevation = dp(1).toFloat()
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
            background = rounded(Color.WHITE, dp(10), ACCENT)
        }

    private fun button(label: String, backgroundColor: Int, textColor: Int, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(textColor)
            textSize = 14f
            minHeight = dp(50)
            background = rounded(backgroundColor, dp(10))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

    private fun Button.weightedButton(leftMargin: Int = 0): Button = apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            this.leftMargin = dp(leftMargin)
        }
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

    private fun statePill(label: String, active: Boolean): TextView =
        text(label, 12, bold = true, color = if (active) SUCCESS_DARK else TEXT_MUTED).apply {
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = rounded(if (active) SUCCESS_SOFT else DISABLED_SOFT, dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

    private fun TextView.setStatePill(label: String, active: Boolean) {
        text = label
        setTextColor(if (active) SUCCESS_DARK else TEXT_MUTED)
        background = rounded(if (active) SUCCESS_SOFT else DISABLED_SOFT, dp(14))
    }

    private fun warningText(value: String): TextView =
        text(value, 13, bold = true, color = WARNING_DARK).apply {
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = rounded(WARNING_SOFT, dp(10), WARNING_STROKE)
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

    private fun selectableForeground(): android.graphics.drawable.Drawable? {
        val outValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        return getDrawable(outValue.resourceId)
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

    private fun <T : View> T.withTopMargin(value: Int): T = apply {
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
        const val RULE_ACTIVE_SOFT = 0xFFF3FBF6.toInt()
        const val DANGER_DARK = 0xFF9F1D1D.toInt()
        const val DANGER_SOFT = 0xFFFFE4E4.toInt()
        const val WARNING_DARK = 0xFF7A4B00.toInt()
        const val WARNING_SOFT = 0xFFFFF4D8.toInt()
        const val WARNING_STROKE = 0xFFFFD990.toInt()
        const val DISABLED_SOFT = 0xFFF1F3F5.toInt()
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
