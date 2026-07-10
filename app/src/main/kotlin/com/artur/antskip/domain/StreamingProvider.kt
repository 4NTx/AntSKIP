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
        "Quando ligado, o AntSKIP observa o player do Crunchyroll e toca apenas nos tipos de botao permitidos.",
    ),
    NETFLIX(
        "provider_netflix_enabled",
        "Netflix",
        "com.netflix.mediaclient",
        true,
        "Quando ligado, o AntSKIP observa o player da Netflix. Proximo episodio e creditos ficam mais restritos por seguranca.",
        setOf("com.netflix.ninja"),
    ),
    PRIME_VIDEO(
        "provider_prime_video_enabled",
        "Prime Video",
        "com.amazon.avod.thirdpartyclient",
        false,
        "Quando ligado, observa o Prime Video. Proximo episodio fica desligado por seguranca.",
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
        "Experimental. So funciona quando o app mostra textos acessiveis nos botoes.",
    ),
    MAX(
        "provider_max_enabled",
        "Max",
        "com.wbd.stream",
        false,
        "Experimental. So toca nos tipos de botao que voce deixar ligados.",
    ),
    PARAMOUNT_PLUS(
        "provider_paramount_plus_enabled",
        "Paramount+",
        "com.cbs.app",
        false,
        "Experimental. Incluido para testar botoes de intro, recap e creditos.",
    );

    fun isActionEnabledByDefault(action: SkipAction): Boolean =
        when (this) {
            NETFLIX -> action in setOf(
                SkipAction.INTRO,
                SkipAction.RECAP,
            )
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
