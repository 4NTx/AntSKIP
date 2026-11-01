package com.artur.antskip.ui.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object AntTheme {
    // Clean Modern Dark Palette (GitHub/Pixel Dark style)
    const val BG_CANVAS = 0xFF0E1117.toInt()
    const val CARD_SURFACE = 0xFF161B22.toInt()
    const val CARD_SURFACE_HOVER = 0xFF21262D.toInt()
    const val CARD_BORDER = 0xFF2B313A.toInt()
    const val DIVIDER = 0xFF242930.toInt()

    // Accent
    const val ACCENT_PRIMARY = 0xFFF97316.toInt()
    const val ACCENT_HOVER = 0xFFEA580C.toInt()
    const val ACCENT_SOFT = 0x22F97316.toInt()

    // Status Colors
    const val SUCCESS_TEXT = 0xFF3FB950.toInt()
    const val SUCCESS_BG = 0x1A3FB950.toInt()
    const val SUCCESS_BORDER = 0x333FB950.toInt()

    const val WARNING_TEXT = 0xFFD29922.toInt()
    const val WARNING_BG = 0x1AD29922.toInt()
    const val WARNING_BORDER = 0x33D29922.toInt()

    const val DANGER_TEXT = 0xFFF85149.toInt()
    const val DANGER_BG = 0x1AF85149.toInt()
    const val DANGER_BORDER = 0x33F85149.toInt()

    // Typography Colors
    const val TEXT_PRIMARY = 0xFFF0F6FC.toInt()
    const val TEXT_SECONDARY = 0xFF8B949E.toInt()
    const val TEXT_MUTED = 0xFF6E7681.toInt()
    const val TEXT_INVERSE = 0xFF0E1117.toInt()

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

    fun rippleButton(
        normalColor: Int,
        rippleColor: Int = Color.WHITE,
        radiusDp: Float = 10f,
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

    fun divider(context: Context): View =
        View(context).apply {
            setBackgroundColor(DIVIDER)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 1),
            )
            layoutParams = params
        }
}
