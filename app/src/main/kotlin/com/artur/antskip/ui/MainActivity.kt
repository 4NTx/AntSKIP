package com.artur.antskip.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.artur.antskip.ui.components.SleepTimerCard
import com.artur.antskip.ui.dialogs.DiagnosticsDialog
import com.artur.antskip.ui.dialogs.SimulatorDialog
import com.artur.antskip.ui.theme.AntTheme
import com.artur.antskip.ui.updater.GitHubUpdateChecker
import com.artur.antskip.ui.updater.UpdateState

class MainActivity : Activity() {
    private val preferences by lazy { PreferenceStore(this) }
    private val updater by lazy { GitHubUpdateChecker(this) }
    private var updateState: UpdateState = UpdateState.NotChecked

    private lateinit var heroCard: HeroStatusCard
    private var isViewBuilt = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyModernSystemBars()
        render()
        requestNotificationPermissionIfNeeded()
        checkUpdatesSilently()
    }

    override fun onResume() {
        super.onResume()
        if (isViewBuilt) {
            updateHeroStatus()
        } else {
            render()
        }
    }

    private fun render() {
        try {
            setContentView(createContent())
            isViewBuilt = true
        } catch (_: Exception) {
        }
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
        heroCard = HeroStatusCard(
            context = this,
            version = updater.installedVersionName(),
            onPrimaryAction = {
                if (!isAccessibilityServiceEnabled()) openAccessibilitySettings()
                else SimulatorDialog(this, preferences).show()
            },
            onSecondaryAction = {
                if (!isAccessibilityServiceEnabled()) openAppInfo()
                else openAccessibilitySettings()
            },
        )
        val heroView = heroCard.createView(
            isServiceEnabled = isServiceEnabled,
            isAutomationEnabled = isAutomationEnabled,
            activeAppsCount = activeProviders,
            totalAppsCount = totalProviders,
        )
        rootLayout.addView(heroView)

        // 2. Sleep Timer Card (Universal media pause & screen lock, alarms 100% protected)
        val sleepTimerCard = SleepTimerCard(
            context = this,
            preferences = preferences,
            onStateChanged = { updateHeroStatus() },
        )
        rootLayout.addView(sleepTimerCard.createView())

        // 3. Quick Tools Section (Test button, Logs, Updates)
        val tools = QuickToolsSection(
            context = this,
            onOpenSimulator = { SimulatorDialog(this, preferences).show() },
            onOpenDiagnostics = { DiagnosticsDialog(this, preferences).show { updateHeroStatus() } },
            onCheckUpdates = { showUpdateDialog() },
        ).createView()
        rootLayout.addView(tools)

        // 4. Section: Streaming Apps (The Big 3)
        rootLayout.addView(createSectionHeader("Streamings Suportados", "Ative os aplicativos que você utiliza para pular botões automaticamente."))
        StreamingProvider.entries.forEach { provider ->
            val card = ProviderCard(
                context = this,
                provider = provider,
                preferences = preferences,
                isInstalled = isAppInstalled(provider),
                onToggleChanged = { updateHeroStatus() },
            )
            rootLayout.addView(card.createView())
        }

        // 5. Section: Global Actions
        rootLayout.addView(createSectionHeader("Ações de Pulo", "Escolha quais tipos de botões devem ser pulados automaticamente."))
        SkipAction.entries.forEach { action ->
            val actionCard = ActionToggleCard(
                context = this,
                action = action,
                preferences = preferences,
                onToggleChanged = { updateHeroStatus() },
            ).createView()
            rootLayout.addView(actionCard)
        }

        // 6. Section: Privacy & Security footer
        rootLayout.addView(createPrivacyFooter())

        return ScrollView(this).apply {
            setBackgroundColor(AntTheme.BG_CANVAS)
            clipToPadding = false
            addView(rootLayout)
            applySystemBarPadding()
        }
    }

    private fun updateHeroStatus() {
        if (!isViewBuilt) return
        try {
            val isServiceEnabled = isAccessibilityServiceEnabled()
            val isAutomationEnabled = preferences.isAutomationEnabled
            val activeProviders = StreamingProvider.entries.count { preferences.isProviderEnabled(it) }
            val totalProviders = StreamingProvider.entries.size
            heroCard.updateState(isServiceEnabled, isAutomationEnabled, activeProviders, totalProviders)
        } catch (_: Exception) {
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
                text = "🛡️ Privacidade & Proteção Total de Alarmes"
                setTextColor(AntTheme.TEXT_PRIMARY)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val pDesc = TextView(this@MainActivity).apply {
                text = "O AntSKIP funciona 100% offline, não grava tela e não acessa dados privados. O temporizador de sono nunca silencia o canal de alarmes (STREAM_ALARM)."
                setTextColor(AntTheme.TEXT_MUTED)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11.5f)
                setPadding(0, AntTheme.dp(this@MainActivity, 4), 0, 0)
            }
            addView(pTitle)
            addView(pDesc)
        }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val expected = ComponentName(this, AntSkipAccessibilityService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    private fun isAppInstalled(provider: StreamingProvider): Boolean =
        try {
            provider.allPackages.any { pkg ->
                packageManager.getLaunchIntentForPackage(pkg) != null
            }
        } catch (_: Exception) {
            false
        }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Abra as Configurações > Acessibilidade do Android", Toast.LENGTH_LONG).show()
        }
    }

    private fun openAppInfo() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null),
                ),
            )
        } catch (_: Exception) {
            openAccessibilitySettings()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun applyModernSystemBars() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            }
        } catch (_: Exception) {
        }
    }

    private fun ScrollView.applySystemBarPadding() {
        setOnApplyWindowInsetsListener { view, insets ->
            try {
                var topInset = 0
                var bottomInset = 0
                var leftInset = 0
                var rightInset = 0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars())
                    topInset = bars.top
                    bottomInset = bars.bottom
                    leftInset = bars.left
                    rightInset = bars.right
                } else {
                    @Suppress("DEPRECATION")
                    run {
                        topInset = insets.systemWindowInsetTop
                        bottomInset = insets.systemWindowInsetBottom
                        leftInset = insets.systemWindowInsetLeft
                        rightInset = insets.systemWindowInsetRight
                    }
                }
                view.setPadding(
                    leftInset,
                    topInset + AntTheme.dp(this@MainActivity, 12),
                    rightInset,
                    bottomInset + AntTheme.dp(this@MainActivity, 16),
                )
            } catch (_: Exception) {
            }
            insets
        }
        try {
            requestApplyInsets()
        } catch (_: Exception) {
        }
    }

    private fun checkUpdatesSilently() {
        try {
            updater.checkForUpdates { state ->
                updateState = state
            }
        } catch (_: Exception) {
        }
    }

    private fun showUpdateDialog() {
        try {
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
        } catch (_: Exception) {
        }
    }
}
