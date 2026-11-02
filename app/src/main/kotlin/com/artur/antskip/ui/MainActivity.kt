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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.artur.antskip.accessibility.AntSkipAccessibilityService
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.service.SleepTimerService
import com.artur.antskip.tile.AutoSkipTileService
import com.artur.antskip.tile.SleepTimerTileService
import com.artur.antskip.ui.components.ActionToggleCard
import com.artur.antskip.ui.components.HeroStatusCard
import com.artur.antskip.ui.components.ProviderCard
import com.artur.antskip.ui.components.QuickToolsSection
import com.artur.antskip.ui.components.ScheduleCard
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
    private lateinit var headerStatusBadge: TextView
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
            updateStatus()
        } else {
            render()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SleepTimerService.onTickListener = null
        SleepTimerService.onFinishListener = null
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
            setPadding(AntTheme.dp(this@MainActivity, 16), 0, AntTheme.dp(this@MainActivity, 16), AntTheme.dp(this@MainActivity, 32))
            layoutTransition = android.animation.LayoutTransition().apply {
                enableTransitionType(android.animation.LayoutTransition.CHANGING)
                setDuration(220)
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        val isServiceEnabled = isAccessibilityServiceEnabled()
        val isAutomationEnabled = preferences.isAutomationEnabled
        val activeProviders = StreamingProvider.entries.count { preferences.isProviderEnabled(it) }
        val totalProviders = StreamingProvider.entries.size

        // 1. Top Header Bar (Title, version, pulsing status pill)
        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val hParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            hParams.topMargin = AntTheme.dp(this@MainActivity, 8)
            hParams.bottomMargin = AntTheme.dp(this@MainActivity, 16)
            layoutParams = hParams

            val titleColumn = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val appTitle = TextView(this@MainActivity).apply {
                    text = "AntSKIP"
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val versionText = TextView(this@MainActivity).apply {
                    text = "v${updater.installedVersionName()} • Pular intros automático"
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                }

                addView(appTitle)
                addView(versionText)
            }

            headerStatusBadge = TextView(this@MainActivity).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(AntTheme.dp(this@MainActivity, 10), AntTheme.dp(this@MainActivity, 5), AntTheme.dp(this@MainActivity, 10), AntTheme.dp(this@MainActivity, 5))
                AntTheme.addPressFeedback(this)
                setOnClickListener {
                    if (!isAccessibilityServiceEnabled()) {
                        openAccessibilitySettings()
                    } else {
                        val newState = !preferences.isAutomationEnabled
                        preferences.setAutomationEnabled(newState)
                        updateStatus()
                    }
                }
            }

            addView(titleColumn)
            addView(headerStatusBadge)
        }
        rootLayout.addView(headerBar)

        // 2. Accessibility Banner (ONLY visible when accessibility is disabled!)
        heroCard = HeroStatusCard(
            context = this,
            version = updater.installedVersionName(),
            onPrimaryAction = { openAccessibilitySettings() },
            onSecondaryAction = { openAppInfo() },
        )
        val heroView = heroCard.createView(
            isServiceEnabled = isServiceEnabled,
            isAutomationEnabled = isAutomationEnabled,
            activeAppsCount = activeProviders,
            totalAppsCount = totalProviders,
        )
        rootLayout.addView(heroView)

        // 3. Sleep Timer Card
        val sleepTimerCard = SleepTimerCard(
            context = this,
            preferences = preferences,
            onStateChanged = { updateStatus() },
        )
        rootLayout.addView(sleepTimerCard.createView())

        // 4. Schedule Card (Ativação por Horário / Filtros)
        val scheduleCard = ScheduleCard(
            context = this,
            preferences = preferences,
            onStateChanged = { updateStatus() },
        )
        rootLayout.addView(scheduleCard.createView())

        // 5. Section: Aplicativos (Grouped Card)
        rootLayout.addView(createSectionTitle("APLICATIVOS SUPORTADOS"))
        val providersGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 14f, AntTheme.CARD_BORDER, 1, this@MainActivity)
            val pParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            pParams.bottomMargin = AntTheme.dp(this@MainActivity, 16)
            layoutParams = pParams

            val providersList = StreamingProvider.entries
            providersList.forEachIndexed { index, provider ->
                val card = ProviderCard(
                    context = this@MainActivity,
                    provider = provider,
                    preferences = preferences,
                    isInstalled = isAppInstalled(provider),
                    onToggleChanged = { updateStatus() },
                )
                addView(card.createRowView())
                if (index < providersList.size - 1) {
                    addView(AntTheme.divider(this@MainActivity))
                }
            }
        }
        rootLayout.addView(providersGroup)

        // 6. Section: Ações Automáticas (Grouped Card)
        rootLayout.addView(createSectionTitle("AÇÕES DE PULAR"))
        val actionsGroup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 14f, AntTheme.CARD_BORDER, 1, this@MainActivity)
            val aParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            aParams.bottomMargin = AntTheme.dp(this@MainActivity, 16)
            layoutParams = aParams

            val actionsList = SkipAction.entries
            actionsList.forEachIndexed { index, action ->
                val actionCard = ActionToggleCard(
                    context = this@MainActivity,
                    action = action,
                    preferences = preferences,
                    onToggleChanged = { updateStatus() },
                )
                addView(actionCard.createRowView())
                if (index < actionsList.size - 1) {
                    addView(AntTheme.divider(this@MainActivity))
                }
            }

            // Modo Silencioso / Toast de aviso
            addView(AntTheme.divider(this@MainActivity))
            val toastRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(AntTheme.dp(this@MainActivity, 16), AntTheme.dp(this@MainActivity, 12), AntTheme.dp(this@MainActivity, 16), AntTheme.dp(this@MainActivity, 12))

                val badge = AntTheme.createBadge(
                    context = this@MainActivity,
                    text = "AVISO",
                    bgColor = AntTheme.CARD_SURFACE_HOVER,
                    textColor = AntTheme.TEXT_MUTED,
                    sizeDp = 38,
                    radiusDp = 10f,
                    textSizeSp = 8.5f,
                ).apply {
                    val bParams = LinearLayout.LayoutParams(AntTheme.dp(this@MainActivity, 38), AntTheme.dp(this@MainActivity, 38))
                    bParams.marginEnd = AntTheme.dp(this@MainActivity, 14)
                    layoutParams = bParams
                }

                val textColumn = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                    val titleView = TextView(this@MainActivity).apply {
                        text = "Aviso na tela ao pular"
                        setTextColor(AntTheme.TEXT_PRIMARY)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }

                    val subView = TextView(this@MainActivity).apply {
                        text = "Exibe balão rápido (Toast) confirmando o pulo"
                        setTextColor(AntTheme.TEXT_MUTED)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    }

                    addView(titleView)
                    addView(subView)
                }

                val sw = Switch(this@MainActivity).apply {
                    isChecked = preferences.isToastNotificationEnabled
                    setOnCheckedChangeListener { _, checked ->
                        preferences.setToastNotificationEnabled(checked)
                    }
                }

                addView(badge)
                addView(textColumn)
                addView(sw)
                setOnClickListener { sw.toggle() }
            }
            addView(toastRow)
        }
        rootLayout.addView(actionsGroup)

        // 6. Section: Ferramentas (Grouped Card)
        rootLayout.addView(createSectionTitle("FERRAMENTAS"))
        val tools = QuickToolsSection(
            context = this,
            onOpenSimulator = { SimulatorDialog(this, preferences).show() },
            onOpenDiagnostics = { DiagnosticsDialog(this, preferences).show { updateStatus() } },
            onCheckUpdates = { showUpdateDialog() },
        ).createView()
        rootLayout.addView(tools)

        // 7. Minimalist Footer
        val footer = TextView(this).apply {
            text = "AntSKIP • 100% offline, seguro e privativo"
            setTextColor(AntTheme.TEXT_MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            setPadding(0, AntTheme.dp(this@MainActivity, 12), 0, 0)
        }
        rootLayout.addView(footer)

        updateStatus()

        return ScrollView(this).apply {
            setBackgroundColor(AntTheme.BG_CANVAS)
            clipToPadding = false
            addView(rootLayout)
            applySystemBarPadding()
        }
    }

    private fun createSectionTitle(title: String): TextView =
        TextView(this).apply {
            text = title
            setTextColor(AntTheme.TEXT_MUTED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(AntTheme.dp(this@MainActivity, 4), AntTheme.dp(this@MainActivity, 8), 0, AntTheme.dp(this@MainActivity, 6))
        }

    private fun updateStatus() {
        if (!isViewBuilt) return
        try {
            val isServiceEnabled = isAccessibilityServiceEnabled()
            val isAutomationEnabled = preferences.isAutomationEnabled
            val activeProviders = StreamingProvider.entries.count { preferences.isProviderEnabled(it) }
            val totalProviders = StreamingProvider.entries.size

            heroCard.updateState(isServiceEnabled, isAutomationEnabled, activeProviders, totalProviders)

            headerStatusBadge.animate().cancel()
            headerStatusBadge.scaleX = 1f
            headerStatusBadge.scaleY = 1f
            headerStatusBadge.alpha = 1f

            val isScheduleActive = preferences.isScheduleActiveNow()

            if (!isServiceEnabled) {
                headerStatusBadge.text = "Desativado"
                headerStatusBadge.setTextColor(AntTheme.WARNING_TEXT)
                headerStatusBadge.background = AntTheme.rounded(AntTheme.WARNING_BG, 8f, AntTheme.WARNING_BORDER, 1, this)
            } else if (!isAutomationEnabled) {
                headerStatusBadge.text = "Pausado"
                headerStatusBadge.setTextColor(AntTheme.TEXT_MUTED)
                headerStatusBadge.background = AntTheme.rounded(AntTheme.CARD_SURFACE_HOVER, 8f, AntTheme.CARD_BORDER, 1, this)
            } else if (!isScheduleActive) {
                headerStatusBadge.text = "Fora do Horário"
                headerStatusBadge.setTextColor(AntTheme.WARNING_TEXT)
                headerStatusBadge.background = AntTheme.rounded(AntTheme.WARNING_BG, 8f, AntTheme.WARNING_BORDER, 1, this)
            } else {
                headerStatusBadge.text = "Ativo"
                headerStatusBadge.setTextColor(AntTheme.SUCCESS_TEXT)
                headerStatusBadge.background = AntTheme.rounded(AntTheme.SUCCESS_BG, 8f, AntTheme.SUCCESS_BORDER, 1, this)
            }

            AutoSkipTileService.requestUpdate(this)
            SleepTimerTileService.requestUpdate(this)
        } catch (_: Exception) {
        }
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
        } catch (_: Exception) {
            Toast.makeText(this, "Abra Configurações > Acessibilidade no seu aparelho", Toast.LENGTH_LONG).show()
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
                    topInset + AntTheme.dp(this@MainActivity, 8),
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
                        val d = AlertDialog.Builder(this).create()
                        val container = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(AntTheme.dp(this@MainActivity, 20), AntTheme.dp(this@MainActivity, 18), AntTheme.dp(this@MainActivity, 20), AntTheme.dp(this@MainActivity, 18))
                            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 16f, AntTheme.CARD_BORDER, 1, this@MainActivity)

                            val t = TextView(this@MainActivity).apply {
                                text = "Nova versão: v${state.latestVersion}"
                                setTextColor(AntTheme.TEXT_PRIMARY)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            val m = TextView(this@MainActivity).apply {
                                text = "Uma versão mais recente do AntSKIP está disponível para download no GitHub."
                                setTextColor(AntTheme.TEXT_SECONDARY)
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                                setPadding(0, AntTheme.dp(this@MainActivity, 6), 0, AntTheme.dp(this@MainActivity, 16))
                            }
                            val btnRow = LinearLayout(this@MainActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                                val dlBtn = Button(this@MainActivity).apply {
                                    text = "Baixar Agora"
                                    setTextColor(AntTheme.TEXT_PRIMARY)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    background = AntTheme.rippleGradientButton(
                                        startColor = AntTheme.ACCENT_GRADIENT_START,
                                        endColor = AntTheme.ACCENT_GRADIENT_END,
                                        radiusDp = 8f,
                                        context = this@MainActivity,
                                    )
                                    layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(this@MainActivity, 40), 1f).apply {
                                        marginEnd = AntTheme.dp(this@MainActivity, 8)
                                    }
                                    AntTheme.addPressFeedback(this)
                                    setOnClickListener {
                                        d.dismiss()
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.releaseUrl)))
                                    }
                                }
                                val closeBtn = Button(this@MainActivity).apply {
                                    text = "Depois"
                                    setTextColor(AntTheme.TEXT_MUTED)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    background = AntTheme.rippleButton(AntTheme.CARD_SURFACE_HOVER, radiusDp = 8f, context = this@MainActivity)
                                    layoutParams = LinearLayout.LayoutParams(0, AntTheme.dp(this@MainActivity, 40), 0.8f)
                                    AntTheme.addPressFeedback(this)
                                    setOnClickListener { d.dismiss() }
                                }
                                addView(dlBtn)
                                addView(closeBtn)
                            }
                            addView(t)
                            addView(m)
                            addView(btnRow)
                        }
                        d.setView(container)
                        d.show()
                        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        d.window?.setDimAmount(0.65f)
                    }
                    is UpdateState.UpToDate -> {
                        Toast.makeText(this, "O AntSKIP já está na versão mais recente (v${state.latestVersion})", Toast.LENGTH_SHORT).show()
                    }
                    is UpdateState.CheckFailed -> {
                        Toast.makeText(this, "Não foi possível verificar atualizações no momento", Toast.LENGTH_SHORT).show()
                    }
                    else -> Unit
                }
            }
        } catch (_: Exception) {
        }
    }
}
