package com.artur.antskip.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.artur.antskip.ui.theme.AntTheme

class QuickToolsSection(
    private val context: Context,
    private val onOpenSimulator: () -> Unit,
    private val onOpenDiagnostics: () -> Unit,
    private val onCheckUpdates: () -> Unit,
) {
    fun createView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AntTheme.rounded(AntTheme.CARD_SURFACE, 14f, AntTheme.CARD_BORDER, 1, context)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = AntTheme.dp(context, 16)
            layoutParams = params

            addView(
                toolRow(
                    tag = "TEST",
                    title = "Simulador de Reconhecimento",
                    subtitle = "Teste se os botões do player são reconhecidos",
                    onClick = onOpenSimulator,
                ),
            )
            addView(AntTheme.divider(context))
            addView(
                toolRow(
                    tag = "LOG",
                    title = "Logs e Diagnóstico",
                    subtitle = "Histórico de cliques e status do serviço",
                    onClick = onOpenDiagnostics,
                ),
            )
            addView(AntTheme.divider(context))
            addView(
                toolRow(
                    tag = "BAT",
                    title = "Execução em 2º Plano",
                    subtitle = "Evitar que Xiaomi/Samsung matem o app de noite",
                    onClick = {
                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
                            if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(fallback)
                                }
                            } else {
                                Toast.makeText(context, "O AntSKIP já está liberado de restrições de bateria!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                ),
            )
            addView(AntTheme.divider(context))
            addView(
                toolRow(
                    tag = "ATT",
                    title = "Verificar Atualizações",
                    subtitle = "Buscar versão mais recente no GitHub",
                    onClick = onCheckUpdates,
                ),
            )
        }

    private fun toolRow(tag: String, title: String, subtitle: String, onClick: () -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 12), AntTheme.dp(context, 16), AntTheme.dp(context, 12))
            background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 0f, context = context)

            val badge = AntTheme.createBadge(
                context = context,
                text = tag,
                bgColor = AntTheme.CARD_SURFACE_HOVER,
                textColor = AntTheme.TEXT_MUTED,
                sizeDp = 38,
                radiusDp = 10f,
                textSizeSp = 10f,
            ).apply {
                val bParams = LinearLayout.LayoutParams(AntTheme.dp(context, 38), AntTheme.dp(context, 38))
                bParams.marginEnd = AntTheme.dp(context, 14)
                layoutParams = bParams
            }

            val textColumn = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val titleView = TextView(context).apply {
                    text = title
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    typeface = Typeface.DEFAULT_BOLD
                }

                val subView = TextView(context).apply {
                    text = subtitle
                    setTextColor(AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }

                addView(titleView)
                addView(subView)
            }

            val arrow = TextView(context).apply {
                text = "›"
                setTextColor(AntTheme.TEXT_MUTED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                typeface = Typeface.DEFAULT_BOLD
            }

            addView(badge)
            addView(textColumn)
            addView(arrow)

            AntTheme.addPressFeedback(this)
            setOnClickListener { onClick() }
        }
}
