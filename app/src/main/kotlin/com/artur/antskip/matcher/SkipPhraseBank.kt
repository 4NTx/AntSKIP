package com.artur.antskip.matcher

import com.artur.antskip.domain.SkipAction

object SkipPhraseBank {
    private val phrases: Map<SkipAction, Set<String>> = mapOf(
        SkipAction.INTRO to normalizedSet(
            "pular abertura",
            "pular intro",
            "pular introducao",
            "saltar intro",
            "saltar abertura",
            "skip intro",
            "skip opening",
            "skip op",
            "skip intro icon",
            "skip opening icon",
            "skip intro button",
            "skip opening button",
            "skip-intro",
            "skip-opening",
            "opening uberspringen",
            "intro uberspringen",
            "ignorer l intro",
            "passer l intro",
            "saltar introduccion",
            "omitir intro",
            "salta intro",
            "salta apertura",
            "salta sigla",
            "intro overslaan",
            "opening overslaan",
        ),
        SkipAction.RECAP to normalizedSet(
            "pular recap",
            "pular recapitulacao",
            "pular recapitulos",
            "pular resumo",
            "skip recap",
            "skip recap icon",
            "skip recap button",
            "skip-recap",
            "skip recapitulation",
            "saltar resumen",
            "omitir resumen",
            "passer le recapitulatif",
            "recap uberspringen",
            "recap overslaan",
        ),
        SkipAction.CREDITS to normalizedSet(
            "pular creditos",
            "pular encerramento",
            "pular ending",
            "skip credits",
            "skip ending",
            "skip outro",
            "skip credits icon",
            "skip ending icon",
            "skip credits button",
            "skip-credits",
            "skip-ending",
            "saltar creditos",
            "omitir creditos",
            "salta crediti",
            "abspann uberspringen",
            "ending uberspringen",
            "generique de fin",
            "credits overslaan",
        ),
        SkipAction.PREVIEW to normalizedSet(
            "pular previa",
            "pular pre visualizacao",
            "pular preview",
            "skip preview",
            "skip next preview",
            "skip preview icon",
            "skip-preview",
            "saltar avance",
            "omitir avance",
            "vorschau uberspringen",
            "preview overslaan",
        ),
        SkipAction.NEXT_EPISODE to normalizedSet(
            "proximo episodio",
            "proximo ep",
            "next episode",
            "play next",
            "reproduzir proximo",
            "episodio siguiente",
            "siguiente episodio",
            "episode suivant",
            "nachste folge",
            "naechste folge",
            "volgende aflevering",
        ),
    )

    fun match(text: String): SkipAction? {
        if (text.isBlank()) return null
        val relaxed = text.replace('-', ' ')
        return phrases.entries.firstOrNull { (_, patterns) ->
            patterns.any { pattern -> relaxed == pattern || relaxed.contains(pattern) }
        }?.key
    }

    private fun normalizedSet(vararg values: String): Set<String> =
        values.mapTo(linkedSetOf()) { it.normalizeForMatch() }
}
