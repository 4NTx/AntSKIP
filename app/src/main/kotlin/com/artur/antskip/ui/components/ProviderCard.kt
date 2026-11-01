package com.artur.antskip.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.StreamingProvider
import com.artur.antskip.ui.theme.AntTheme

class ProviderCard(
    private val context: Context,
    private val provider: StreamingProvider,
    private val preferences: PreferenceStore,
    private val isInstalled: Boolean,
    private val onToggleChanged: (Boolean) -> Unit,
) {
    fun createRowView(): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(AntTheme.dp(context, 16), AntTheme.dp(context, 12), AntTheme.dp(context, 16), AntTheme.dp(context, 12))
            background = AntTheme.rippleButton(AntTheme.CARD_SURFACE, radiusDp = 0f, context = context)

            // Brand Badge Icon (e.g. Red 'N', Orange 'CR', Blue 'P', Deep Blue 'MAX', Indigo 'D+', Red 'YT')
            val badgeSymbol = when (provider) {
                StreamingProvider.NETFLIX -> "N"
                StreamingProvider.CRUNCHYROLL -> "CR"
                StreamingProvider.PRIME_VIDEO -> "P"
                StreamingProvider.MAX -> "MAX"
                StreamingProvider.DISNEY_PLUS -> "D+"
                StreamingProvider.YOUTUBE -> "YT"
            }
            val brandBadge = AntTheme.createBadge(
                context = context,
                text = badgeSymbol,
                bgColor = provider.brandColor,
                sizeDp = 38,
                radiusDp = 10f,
                textSizeSp = when {
                    badgeSymbol.length >= 3 -> 10.5f
                    badgeSymbol.length == 2 -> 12f
                    else -> 15f
                },
            ).apply {
                val bParams = LinearLayout.LayoutParams(AntTheme.dp(context, 38), AntTheme.dp(context, 38))
                bParams.marginEnd = AntTheme.dp(context, 14)
                layoutParams = bParams
            }

            // Text info
            val textColumn = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val title = TextView(context).apply {
                    text = provider.label
                    setTextColor(AntTheme.TEXT_PRIMARY)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(title)

                val sub = TextView(context).apply {
                    val isEnabled = preferences.isProviderEnabled(provider)
                    text = when {
                        !isInstalled -> "Não instalado no dispositivo"
                        isEnabled -> "Monitoramento automático ativo"
                        else -> "Desativado"
                    }
                    setTextColor(if (isEnabled && isInstalled) AntTheme.SUCCESS_TEXT else AntTheme.TEXT_MUTED)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                }
                addView(sub)
            }

            val sw = Switch(context).apply {
                isChecked = preferences.isProviderEnabled(provider)
                setOnCheckedChangeListener { _, isChecked ->
                    preferences.setProviderEnabled(provider, isChecked)
                    val sub = textColumn.getChildAt(1) as? TextView
                    sub?.text = when {
                        !isInstalled -> "Não instalado no dispositivo"
                        isChecked -> "Monitoramento automático ativo"
                        else -> "Desativado"
                    }
                    sub?.setTextColor(if (isChecked && isInstalled) AntTheme.SUCCESS_TEXT else AntTheme.TEXT_MUTED)
                    onToggleChanged(isChecked)
                }
            }

            addView(brandBadge)
            addView(textColumn)
            addView(sw)

            AntTheme.addPressFeedback(this)
            setOnClickListener {
                sw.toggle()
            }
        }
}
