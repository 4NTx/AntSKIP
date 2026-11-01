package com.artur.antskip.domain

enum class SkipAction(
    val preferenceKey: String,
    val label: String,
    val enabledByDefault: Boolean,
    val description: String,
) {
    INTRO(
        preferenceKey = "skip_intro_enabled",
        label = "Pular Aberturas",
        enabledByDefault = true,
        description = "Pula automaticamente aberturas e introduções de episódios.",
    ),
    RECAP(
        preferenceKey = "skip_recap_enabled",
        label = "Pular Resumos",
        enabledByDefault = true,
        description = "Pula resumos do episódio anterior (recaps).",
    ),
    CREDITS(
        preferenceKey = "skip_credits_enabled",
        label = "Pular Créditos",
        enabledByDefault = true,
        description = "Pula os créditos e encerramentos ao final do episódio.",
    ),
    NEXT_EPISODE(
        preferenceKey = "skip_next_episode_enabled",
        label = "Próximo Episódio",
        enabledByDefault = true,
        description = "Avança automaticamente para o próximo episódio quando o botão surgir.",
    ),
}
