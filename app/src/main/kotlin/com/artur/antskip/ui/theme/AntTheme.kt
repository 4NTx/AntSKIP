package com.artur.antskip.ui.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

object AntTheme {
    // Premium Dark OLED Palette
    const val BG_CANVAS = 0xFF0B0E14.toInt()
    const val CARD_SURFACE = 0xFF141923.toInt()
    const val CARD_SURFACE_HOVER = 0xFF1A212E.toInt()
    const val CARD_BORDER = 0xFF232B3A.toInt()
    const val HERO_BG = 0xFF161E2D.toInt()
    const val HERO_BORDER = 0xFF2B3A54.toInt()

    // Accents
    const val ACCENT_PRIMARY = 0xFFFF7A00.toInt()
    const val ACCENT_HOVER = 0xFFE56900.toInt()
    const val ACCENT_SOFT = 0x26FF7A00.toInt()

    // Status Colors
    const val SUCCESS_TEXT = 0xFF34D399.toInt()
    const val SUCCESS_BG = 0x2210B981.toInt()
    const val SUCCESS_BORDER = 0x4410B981.toInt()

    const val WARNING_TEXT = 0xFFFBBF24.toInt()
    const val WARNING_BG = 0x22F59E0B.toInt()
    const val WARNING_BORDER = 0x44F59E0B.toInt()

    const val DANGER_TEXT = 0xFFF87171.toInt()
    const val DANGER_BG = 0x22EF4444.toInt()
    const val DANGER_BORDER = 0x44EF4444.toInt()

    // Typography Colors
    const val TEXT_PRIMARY = 0xFFF1F5F9.toInt()
    const val TEXT_SECONDARY = 0xFF94A3B8.toInt()
    const val TEXT_MUTED = 0xFF64748B.toInt()
    const val TEXT_INVERSE = 0xFF0B0E14.toInt()

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    fun rounded(
        fillColor: Int,
        radiusDp: Float,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 1,
        context: Context? = null,
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(fillColor)
            val density = context?.resources?.displayMetrics?.density ?: 2f
            cornerRadius = radiusDp * density
            if (strokeColor != null) {
                setStroke((strokeWidthDp * density).toInt(), strokeColor)
            }
        }

    fun gradient(
        startColor: Int,
        endColor: Int,
        radiusDp: Float,
        context: Context,
        orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.TL_BR,
    ): GradientDrawable =
        GradientDrawable(orientation, intArrayOf(startColor, endColor)).apply {
            val density = context.resources.displayMetrics.density
            cornerRadius = radiusDp * density
        }

    fun rippleButton(
        normalColor: Int,
        rippleColor: Int = Color.WHITE,
        radiusDp: Float = 12f,
        context: Context,
    ): RippleDrawable {
        val normal = rounded(normalColor, radiusDp, null, 0, context)
        val mask = rounded(Color.WHITE, radiusDp, null, 0, context)
        return RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            normal,
            mask,
        )
    }

    fun badge(
        context: Context,
        text: String,
        textColor: Int,
        bgColor: Int,
        borderColor: Int? = null,
    ): TextView =
        TextView(context).apply {
            this.text = text
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(context, 10), dp(context, 4), dp(context, 10), dp(context, 4))
            background = rounded(bgColor, 8f, borderColor, 1, context)
        }
}
