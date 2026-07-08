package com.artur.antskip.domain

enum class SkipAction(
    val preferenceKey: String,
    val label: String,
    val enabledByDefault: Boolean,
    val description: String,
) {
    INTRO(
        "skip_intro_enabled",
        "Aberturas e intros",
        true,
        "Ex.: Pular abertura, Skip Intro, Skip Opening.",
    ),
    RECAP(
        "skip_recap_enabled",
        "Recaps e resumos",
        true,
        "Ex.: Pular resumo, Pular recapitulacao, Skip Recap.",
    ),
    CREDITS(
        "skip_credits_enabled",
        "Creditos e encerramentos",
        true,
        "Ex.: Pular creditos, Skip Credits, Watch next.",
    ),
    PREVIEW(
        "skip_preview_enabled",
        "Previews",
        false,
        "Ex.: Pular previa, Skip Preview. Desligado por padrao.",
    ),
    NEXT_EPISODE(
        "skip_next_episode_enabled",
        "Proximo episodio",
        true,
        "Ex.: Proximo, Proximo episodio, Next Episode.",
    ),
}
