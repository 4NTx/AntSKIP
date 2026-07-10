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
        "Liga cliques em botoes como Pular abertura, Skip Intro e Skip Opening.",
    ),
    RECAP(
        "skip_recap_enabled",
        "Recaps e resumos",
        true,
        "Liga cliques em botoes como Pular resumo, Pular recapitulacao e Skip Recap.",
    ),
    CREDITS(
        "skip_credits_enabled",
        "Creditos e encerramentos",
        true,
        "Liga cliques em botoes de creditos ou encerramento. Pode adiantar o fim em alguns apps.",
    ),
    PREVIEW(
        "skip_preview_enabled",
        "Previews",
        false,
        "Liga cliques em previews, trailers e pre-visualizacoes. Desligado por padrao.",
    ),
    NEXT_EPISODE(
        "skip_next_episode_enabled",
        "Proximo episodio",
        true,
        "Liga o avanco automatico para outro episodio. Use apenas se voce quer continuar sem confirmar.",
    ),
}
