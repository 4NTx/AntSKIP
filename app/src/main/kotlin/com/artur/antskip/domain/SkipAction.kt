package com.artur.antskip.domain

enum class SkipAction(
    val preferenceKey: String,
    val label: String,
    val enabledByDefault: Boolean,
) {
    INTRO("skip_intro_enabled", "Aberturas / intros", true),
    RECAP("skip_recap_enabled", "Recaps / resumos", true),
    CREDITS("skip_credits_enabled", "Creditos / encerramentos", true),
    PREVIEW("skip_preview_enabled", "Previews", false),
    NEXT_EPISODE("skip_next_episode_enabled", "Proximo episodio", false),
}
