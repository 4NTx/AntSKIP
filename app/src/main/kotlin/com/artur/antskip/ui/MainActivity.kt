package com.artur.antskip.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.artur.antskip.accessibility.AntSkipAccessibilityService
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.ui.components.ActionToggleCard
import com.artur.antskip.ui.components.HeroStatusCard
import com.artur.antskip.ui.components.ProviderCard
import com.artur.antskip.ui.components.QuickToolsSection
import com.artur.antskip.ui.dialogs.CustomPhrasesDialog
import com.artur.antskip.ui.dialogs.DiagnosticsDialog
import com.artur.antskip.ui.dialogs.ProviderConfigDialog
import com.artur.antskip.ui.dialogs.SimulatorDialog
import com.artur.antskip.ui.theme.AntTheme
import com.artur.antskip.ui.updater.GitHubUpdateChecker
import com.artur.antskip.ui.updater.UpdateState

class MainActivity : Activity() {
    private val preferences by lazy { PreferenceStore(this) }
    private val updater by lazy { GitHubUpdateChecker(this) }
    private var updateState: UpdateState = UpdateState.NotChecked

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyModernSystemBars()
        render()
        checkUpdatesSilently()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        setContentView(createContent())
    }

    private fun createContent(): ScrollView {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AntTheme.dp(this@MainActivity, 16), 0, AntTheme.dp(this@MainActivity, 16), AntTheme.dp(this@MainActivity, 24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val isServiceEnabled = isAccessibilityServiceEnabled()
        val isAutomationEnabled = preferences.isAutomationEnabled
        val activeProviders = StreamingProvider.entries.count { preferences.isProviderEnabled(it) }
        val totalProviders = StreamingProvider.entries.size

        // 1. Hero Status Card
        val hero = HeroStatusCard(
            context = this,
            isServiceEnabled = isServiceEnabled,
            isAutomationEnabled = isAutomationEnabled,
            version = updater.installedVersionName(),
            activeAppsCount = activeProviders,
            totalAppsCount = totalProviders,
            onPrimaryAction = {
                if (!isServiceEnabled) openAccessibilitySettings()
                else SimulatorDialog(this, preferences).show()
            },
            onSecondaryAction = {
                if (!isServiceEnabled) openAppInfo()
                else openAccessibilitySettings()
            },
        ).createView()
        rootLayout.addView(hero)

        // 2. Quick Tools Section
        val tools = QuickToolsSection(
            context = this,
            onOpenSimulator = { SimulatorDialog(this, preferences).show() },
            onOpenPhrases = { CustomPhrasesDialog(this, preferences).show { render() } },
            onOpenDiagnostics = { DiagnosticsDialog(this, preferences).show { render() } },
            onCheckUpdates = { showUpdateDialog() },
        ).createView()
        rootLayout.addView(tools)

        // 3. Section: Streaming Apps
        rootLayout.addView(createSectionHeader("Streamings Monitorados", "Toque em qualquer streaming para ajustar regras específicas e modo sono."))
        StreamingProvider.entries.forEach { provider ->
            val card = ProviderCard(
                context = this,
                provider = provider,
                preferences = preferences,
                isInstalled = isAppInstalled(provider),
                onCardClicked = {
                    ProviderConfigDialog(this, preferences).show(provider) {
                        render()
                    }
                },
                onToggleChanged = { render() },
            ).createView()
            rootLayout.addView(card)
        }

        // 4. Section: Global Actions
        rootLayout.addView(createSectionHeader("Ações Globais de Pulo", "Configurações padrão aplicadas a todos os aplicativos de streaming."))
        SkipAction.entries.forEach { action ->
            val actionCard = ActionToggleCard(
                context = this,
                action = action,
                preferences = preferences,
                onToggleChanged = { render() },
            ).createView()
            rootLayout.addView(actionCard)
        }

        // 5. Section: Privacy & Security footer
        rootLayout.addView(createPrivacyFooter())

        return ScrollView(this).apply {
            setBackgroundColor(AntTheme.BG_CANVAS)
            clipToPadding = false
            addView(rootLayout)
            applySystemBarPadding()
        }
    }

    private fun createSectionHeader(title: String, subtitle: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.topMargin = AntTheme.dp(this@MainActivity, 14)
            params.bottomMargin = AntTheme.dp(this@MainActivity, 10)
            layoutParams = params

            val titleView = TextView(this@MainActivity).apply {
                text = title
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val subtitleView = TextView(this@MainActivity).apply {
                text = subtitle
                setTextColor(AntTheme.TEXT_SECONDARY)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setPadding(0, AntTheme.dp(this@MainActivity, 3), 0, 0)
            }
            addView(titleView)
            addView(subtitleView)
        }

    private fun createPrivacyFooter(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.topMargin = AntTheme.dp(this@MainActivity, 18)
            params.bottomMargin = AntTheme.dp(this@MainActivity, 20)
            layoutParams = params
            setPadding(AntTheme.dp(this@MainActivity, 14), AntTheme.dp(this@MainActivity, 12), AntTheme.dp(this@MainActivity, 14), AntTheme.dp(this@MainActivity, 12))
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 12f, AntTheme.CARD_BORDER, 1, this@MainActivity)

            val pTitle = TextView(this@MainActivity).apply {
                text = "🛡️ Privacidade & Segurança Total"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val pDesc = TextView(this@MainActivity).apply {
                text = "O AntSKIP não grava a tela, não lê vídeos, não acessa credenciais e funciona 100% offline em segundo plano sem enviar dados para a internet."
                setTextColor(AntTheme.TEXT_MUTED)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(0, AntTheme.dp(this@MainActivity, 4), 0, 0)
            }
            addView(pTitle)
            addView(pDesc)
        }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, AntSkipAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    private fun isAppInstalled(provider: StreamingProvider): Boolean =
        provider.allPackages.any { pkg ->
            packageManager.getLaunchIntentForPackage(pkg) != null
        }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openAppInfo() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    private fun applyModernSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        }
    }

    private fun ScrollView.applySystemBarPadding() {
        setOnApplyWindowInsetsListener { view, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars()).top
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetTop
            }
            val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            view.setPadding(
                0,
                topInset + AntTheme.dp(this@MainActivity, 12),
                0,
                bottomInset + AntTheme.dp(this@MainActivity, 16),
            )
            insets
        }
        requestApplyInsets()
    }

    private fun checkUpdatesSilently() {
        updater.checkForUpdates { state ->
            updateState = state
        }
    }

    private fun showUpdateDialog() {
        updater.checkForUpdates { state ->
            updateState = state
            when (state) {
                is UpdateState.UpdateAvailable -> {
                    AlertDialog.Builder(this)
                        .setTitle("Nova Versão Disponível: v${state.latestVersion}")
                        .setMessage("Uma nova atualização do AntSKIP foi encontrada no GitHub.")
                        .setPositiveButton("Baixar") { _, _ ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.releaseUrl)))
                        }
                        .setNegativeButton("Mais tarde", null)
                        .show()
                }
                is UpdateState.UpToDate -> {
                    Toast.makeText(this, "AntSKIP está atualizado (v${state.latestVersion})!", Toast.LENGTH_SHORT).show()
                }
                is UpdateState.CheckFailed -> {
                    Toast.makeText(this, "Não foi possível verificar atualizações: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }
    }
}
