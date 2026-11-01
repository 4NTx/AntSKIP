package com.artur.antskip.ui.theme

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView

object AntTheme {
    // Clean Modern Dark Palette (Deep Midnight Obsidian)
    const val BG_CANVAS = 0xFF0A0D14.toInt()
    const val CARD_SURFACE = 0xFF121721.toInt()
    const val CARD_SURFACE_HOVER = 0xFF1B2230.toInt()
    const val CARD_BORDER = 0xFF222B3A.toInt()
    const val DIVIDER = 0xFF1E2633.toInt()

    // Brand Colors for Streamings
    const val BRAND_NETFLIX = 0xFFE50914.toInt()
    const val BRAND_CRUNCHYROLL = 0xFFF47521.toInt()
    const val BRAND_PRIME = 0xFF00A8E1.toInt()

    // Accent (Vibrant Flame Orange / Sunset)
    const val ACCENT_PRIMARY = 0xFFF97316.toInt()
    const val ACCENT_HOVER = 0xFFEA580C.toInt()
    const val ACCENT_GRADIENT_START = 0xFFFF6B2B.toInt()
    const val ACCENT_GRADIENT_END = 0xFFEA580C.toInt()
    const val ACCENT_SOFT = 0x22F97316.toInt()

    // Status Colors
    const val SUCCESS_TEXT = 0xFF3FB950.toInt()
    const val SUCCESS_BG = 0x1A3FB950.toInt()
    const val SUCCESS_BORDER = 0x333FB950.toInt()

    const val WARNING_TEXT = 0xFFE3B341.toInt()
    const val WARNING_BG = 0x1AE3B341.toInt()
    const val WARNING_BORDER = 0x33E3B341.toInt()

    const val DANGER_TEXT = 0xFFF85149.toInt()
    const val DANGER_BG = 0x1AF85149.toInt()
    const val DANGER_BORDER = 0x33F85149.toInt()

    // Typography Colors
    const val TEXT_PRIMARY = 0xFFF0F6FC.toInt()
    const val TEXT_SECONDARY = 0xFF9DA7B3.toInt()
    const val TEXT_MUTED = 0xFF656F7D.toInt()
    const val TEXT_INVERSE = 0xFF0A0D14.toInt()

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
        orientation: GradientDrawable.Orientation = GradientDrawable.Orientation.LEFT_RIGHT,
        context: Context? = null,
    ): GradientDrawable =
        GradientDrawable(orientation, intArrayOf(startColor, endColor)).apply {
            val density = context?.resources?.displayMetrics?.density ?: 2f
            cornerRadius = radiusDp * density
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

    fun rippleGradientButton(
        startColor: Int,
        endColor: Int,
        rippleColor: Int = Color.WHITE,
        radiusDp: Float = 10f,
        context: Context,
    ): RippleDrawable {
        val normal = gradient(startColor, endColor, radiusDp, context = context)
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

    fun createBadge(
        context: Context,
        text: String,
        bgColor: Int,
        textColor: Int = Color.WHITE,
        sizeDp: Int = 38,
        radiusDp: Float = 10f,
        textSizeSp: Float = 13.5f,
    ): TextView =
        TextView(context).apply {
            this.text = text
            this.setTextColor(textColor)
            this.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            this.typeface = Typeface.DEFAULT_BOLD
            this.gravity = Gravity.CENTER
            this.background = rounded(bgColor, radiusDp, null, 0, context)
            this.layoutParams = LinearLayout.LayoutParams(dp(context, sizeDp), dp(context, sizeDp))
        }

    @SuppressLint("ClickableViewAccessibility")
    fun addPressFeedback(view: View, scale: Float = 0.96f) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(scale)
                        .scaleY(scale)
                        .setDuration(90)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(130)
                        .setInterpolator(OvershootInterpolator(1.3f))
                        .start()
                }
            }
            false
        }
    }

    fun startPulsing(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.85f, 1.25f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1000
        }
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.85f, 1.25f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1000
        }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0.65f, 1.0f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1000
        }
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            start()
        }
    }
}
