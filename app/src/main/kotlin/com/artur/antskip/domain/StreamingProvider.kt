package com.artur.antskip.domain

enum class StreamingProvider(
    val preferenceKey: String,
    val label: String,
    val packageName: String,
    val enabledByDefault: Boolean,
    val description: String,
    val brandColor: Int,
    val brandAccentColor: Int,
    val packageAliases: Set<String> = emptySet(),
) {
    CRUNCHYROLL(
        preferenceKey = "provider_crunchyroll_enabled",
        label = "Crunchyroll",
        packageName = "com.crunchyroll.crunchyroid",
        enabledByDefault = true,
        description = "Detecta aberturas, encerramentos, recaps e próximo episódio com suporte a toques nativos e por gesto.",
        brandColor = 0xFFFF640A.toInt(),
        brandAccentColor = 0x33FF640A.toInt(),
        packageAliases = setOf(
            "com.crunchyroll.crunchyrollapp",
        ),
    ),
    NETFLIX(
        preferenceKey = "provider_netflix_enabled",
        label = "Netflix",
        packageName = "com.netflix.mediaclient",
        enabledByDefault = true,
        description = "Detecta 'Pular abertura', 'Pular resumo', 'Pular créditos' e o botão final 'Próximo episódio'.",
        brandColor = 0xFFE50914.toInt(),
        brandAccentColor = 0x33E50914.toInt(),
        packageAliases = setOf(
            "com.netflix.ninja",
        ),
    ),
    PRIME_VIDEO(
        preferenceKey = "provider_prime_video_enabled",
        label = "Prime Video",
        packageName = "com.amazon.avod.thirdpartyclient",
        enabledByDefault = true,
        description = "Detecta intro, recap e créditos. Suporte refinado para não confundir com o menu X-Ray.",
        brandColor = 0xFF00A8E1.toInt(),
        brandAccentColor = 0x3300A8E1.toInt(),
        packageAliases = setOf(
            "com.amazon.avod",
            "com.amazon.amazonvideo.livingroom",
        ),
    ),
    DISNEY_PLUS(
        preferenceKey = "provider_disney_plus_enabled",
        label = "Disney+",
        packageName = "com.disney.disneyplus",
        enabledByDefault = false,
        description = "Detecta 'Pular introdução', 'Pular resumo' e 'Próximo episódio' no player do Disney+.",
        brandColor = 0xFF113CCF.toInt(),
        brandAccentColor = 0x33113CCF.toInt(),
        packageAliases = setOf(
            "com.disney.disneyplus.tv",
        ),
    ),
    MAX(
        preferenceKey = "provider_max_enabled",
        label = "Max",
        packageName = "com.wbd.stream",
        enabledByDefault = false,
        description = "Detecta botões de pular introdução, recap e créditos na plataforma Max.",
        brandColor = 0xFF002BE7.toInt(),
        brandAccentColor = 0x33002BE7.toInt(),
        packageAliases = setOf(
            "com.hbo.hbonow",
            "com.hbo.go",
        ),
    ),
    PARAMOUNT_PLUS(
        preferenceKey = "provider_paramount_plus_enabled",
        label = "Paramount+",
        packageName = "com.cbs.app",
        enabledByDefault = false,
        description = "Detecta botões de introdução, resumo e avançar episódio no Paramount+.",
        brandColor = 0xFF0064FF.toInt(),
        brandAccentColor = 0x330064FF.toInt(),
        packageAliases = setOf(
            "com.paramountplus",
            "com.cbs.ott",
        ),
    );

    fun isActionEnabledByDefault(action: SkipAction): Boolean =
        when (this) {
            NETFLIX -> action in setOf(
                SkipAction.INTRO,
                SkipAction.RECAP,
                SkipAction.CREDITS,
                SkipAction.NEXT_EPISODE,
            )
            PRIME_VIDEO -> action in setOf(
                SkipAction.INTRO,
                SkipAction.RECAP,
                SkipAction.CREDITS,
            )
            else -> action.enabledByDefault
        }

    val allPackages: Set<String>
        get() = setOf(packageName) + packageAliases

    companion object {
        fun fromPackageName(packageName: String): StreamingProvider? =
            entries.firstOrNull { provider ->
                provider.packageName == packageName || packageName in provider.packageAliases
            }

        fun allSupportedPackages(): Set<String> =
            entries.flatMapTo(linkedSetOf()) { it.allPackages }
    }
}
