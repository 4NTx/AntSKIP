package com.artur.antskip.matcher

import com.artur.antskip.domain.SkipAction

object SkipPhraseBank {
    private val genericIntroPhrases = normalizedSet(
        "pular",
        "skip",
        "saltar",
        "omitir",
        "passer",
        "salta",
    )

    private val exactPhrases: Map<SkipAction, Set<String>> = mapOf(
        SkipAction.INTRO to genericIntroPhrases,
    )

    private val phrases: Map<SkipAction, Set<String>> = mapOf(
        SkipAction.INTRO to normalizedSet(
            // Portuguese
            "pular abertura",
            "pular a abertura",
            "pular intro",
            "pular introducao",
            "pular introdução",
            "pular a introducao",
            "pular a introdução",
            "pular tema",
            "pular tema de abertura",
            // English
            "skip intro",
            "skip opening",
            "skip introduction",
            "skip op",
            "skip theme song",
            "skip title sequence",
            "skip intro icon",
            "skip opening icon",
            "skip intro button",
            "skip opening button",
            "skip-intro",
            "skip-opening",
            // Spanish
            "saltar intro",
            "saltar introduccion",
            "saltar introducción",
            "saltar cabecera",
            "omitir intro",
            "omitir introduccion",
            "omitir introducción",
            // French
            "ignorer l intro",
            "ignorer l'intro",
            "passer l intro",
            "passer l'intro",
            "passer l ouverture",
            "passer l'ouverture",
            "passer l introduction",
            "passer l'introduction",
            // German
            "opening uberspringen",
            "opening überspringen",
            "intro uberspringen",
            "intro überspringen",
            "vorspann uberspringen",
            "vorspann überspringen",
            // Dutch & Scandinavian
            "intro overslaan",
            "opening overslaan",
            "hopp over intro",
            "hoppa over intro",
            // Italian
            "salta intro",
            "salta apertura",
            "salta sigla",
            "salta introduzione",
            "salta la sigla",
            // Turkish
            "intro atla",
            "atla intro",
            "giris atla",
            "jenerigi atla",
            // Polish
            "pomiń intro",
            "pomin intro",
            "pominac intro",
            // Asian Languages
            "イントロをスキップ",
            "オープニングをスキップ",
            "오프닝 건너뛰기",
            "인트ロ 건너뛰기",
            "跳过片头",
            "跳過片頭",
            "跳过开头",
            "跳過開頭",
        ),
        SkipAction.RECAP to normalizedSet(
            // Portuguese
            "pular recap",
            "pular recapitular",
            "pular recapitulacao",
            "pular recapitulação",
            "pular o resumo",
            "pular resumo",
            "pular anteriores",
            "pular no episodio anterior",
            "pular nos episodios anteriores",
            // English
            "skip recap",
            "skip recap icon",
            "skip recap button",
            "skip-recap",
            "skip recapitulation",
            "skip previously on",
            "previously on",
            // Spanish
            "saltar resumen",
            "saltar recapitulacion",
            "saltar recapitulación",
            "omitir resumen",
            "omitir recapitulacion",
            "omitir recapitulación",
            // French
            "passer le recapitulatif",
            "passer le récapitulatif",
            "passer le resume",
            "passer le résumé",
            // German
            "recap uberspringen",
            "recap überspringen",
            "zusammenfassung uberspringen",
            "zusammenfassung überspringen",
            // Italian
            "salta riassunto",
            "salta recap",
            // Dutch & Polish
            "recap overslaan",
            "samenvatting overslaan",
            "pomin podsumowanie",
            "pomin recap",
            // Asian & Turkish
            "要約をスキップ",
            "前回のあらすじをスキップ",
            "건너뛰기 요약",
            "요약 건너뛰기",
            "跳过回顾",
            "跳過回顧",
            "ozeti atla",
        ),
        SkipAction.CREDITS to normalizedSet(
            // Portuguese
            "pular creditos",
            "pular créditos",
            "pular encerramento",
            "pular o encerramento",
            "pular ending",
            "pular ed",
            "pular final",
            "pular para o proximo",
            "pular para o próximo",
            // English
            "skip credits",
            "skip ending",
            "skip outro",
            "skip end credits",
            "skip credits icon",
            "skip ending icon",
            "skip credits button",
            "skip-credits",
            "skip-ending",
            // Spanish
            "saltar creditos",
            "saltar créditos",
            "saltar final",
            "omitir creditos",
            "omitir créditos",
            // French
            "generique de fin",
            "générique de fin",
            "passer le generique",
            "passer le générique",
            // German
            "abspann uberspringen",
            "abspann überspringen",
            "ending uberspringen",
            "ending überspringen",
            // Italian & Dutch
            "salta crediti",
            "salta i crediti",
            "credits overslaan",
            "aftiteling overslaan",
            // Polish
            "pomin napisy",
            "pomiń napisy",
            "napisy pomin",
            // Asian & Turkish
            "クレジットをスキップ",
            "エンディングをスキップ",
            "크레딧 건너뛰기",
            "엔딩 건너뛰기",
            "跳过片尾",
            "跳過片尾",
            "jenerigi atla",
        ),
        SkipAction.NEXT_EPISODE to normalizedSet(
            // Portuguese
            "proximo episodio",
            "próximo episódio",
            "proximo ep",
            "próximo ep",
            "proxima parte",
            "próxima parte",
            "proximo episodio em",
            "próximo episódio em",
            "reproduzir proximo",
            "reproduzir próximo",
            "assistir proximo",
            "assistir próximo",
            "ver proximo episodio",
            "ver próximo episódio",
            // English
            "next episode",
            "next episode in",
            "play next",
            "play next episode",
            "watch next episode",
            "watch next up",
            "next up",
            "watch next",
            "up next",
            // Spanish
            "episodio siguiente",
            "siguiente episodio",
            "ver siguiente",
            "reproducir siguiente",
            // French
            "episode suivant",
            "épisode suivant",
            "regarder l episode suivant",
            // German
            "nachste folge",
            "nächste folge",
            "naechste folge",
            // Dutch & Italian
            "volgende aflevering",
            "prossimo episodio",
            "episodio successivo",
            // Polish
            "następny odcinek",
            "nastepny odcinek",
            // Asian & Turkish
            "次のエピソード",
            "次の話",
            "다음 화",
            "다음 에피소드",
            "下一集",
            "下一個",
            "sonraki bolum",
        ),
    )

    fun match(text: String): SkipAction? {
        if (text.isBlank()) return null
        val relaxed = text.replace('-', ' ')
        exactPhrases.entries.firstOrNull { (_, patterns) -> relaxed in patterns }?.let { return it.key }
        return phrases.entries.firstOrNull { (_, patterns) ->
            patterns.any { pattern -> relaxed == pattern || relaxed.contains(pattern) }
        }?.key
    }

    private fun normalizedSet(vararg values: String): Set<String> =
        values.mapTo(linkedSetOf()) { it.normalizeForMatch() }
}
