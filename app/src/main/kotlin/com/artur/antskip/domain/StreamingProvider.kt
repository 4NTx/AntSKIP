package com.artur.antskip.domain

enum class StreamingProvider(
    val preferenceKey: String,
    val label: String,
    val packageName: String,
    val enabledByDefault: Boolean,
    val description: String,
    val packageAliases: Set<String> = emptySet(),
) {
    CRUNCHYROLL(
        "provider_crunchyroll_enabled",
        "Crunchyroll",
        "com.crunchyroll.crunchyroid",
        true,
        "Suporte principal. Tenta tocar em abertura, resumo e creditos.",
    ),
    NETFLIX(
        "provider_netflix_enabled",
        "Netflix",
        "com.netflix.mediaclient",
        true,
        "Reconhece botoes como Pular abertura, Pular recapitulacao e Proximo episodio.",
        setOf("com.netflix.ninja"),
    ),
    PRIME_VIDEO(
        "provider_prime_video_enabled",
        "Prime Video",
        "com.amazon.avod.thirdpartyclient",
        false,
        "Suporte restrito. Comeca com intro, resumo e creditos; proximo episodio fica desligado por seguranca.",
        setOf(
            "com.amazon.avod",
            "com.amazon.amazonvideo.livingroom",
        ),
    ),
    DISNEY_PLUS(
        "provider_disney_plus_enabled",
        "Disney+",
        "com.disney.disneyplus",
        false,
        "Experimental. Depende dos textos expostos pelo app para acessibilidade.",
    ),
    MAX(
        "provider_max_enabled",
        "Max",
        "com.wbd.stream",
        false,
        "Experimental. Usa os mesmos tipos de skip quando o botao aparece.",
    ),
    PARAMOUNT_PLUS(
        "provider_paramount_plus_enabled",
        "Paramount+",
        "com.cbs.app",
        false,
        "Experimental. Incluido para teste com botoes de intro e creditos.",
    );

    fun isActionEnabledByDefault(action: SkipAction): Boolean =
        when (this) {
            PRIME_VIDEO -> action in setOf(
                SkipAction.INTRO,
                SkipAction.RECAP,
                SkipAction.CREDITS,
            )
            else -> action.enabledByDefault
        }

    companion object {
        fun fromPackageName(packageName: String): StreamingProvider? =
            entries.firstOrNull { provider ->
                provider.packageName == packageName || packageName in provider.packageAliases
            }
    }
}
