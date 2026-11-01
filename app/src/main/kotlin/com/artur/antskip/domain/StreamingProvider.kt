package com.artur.antskip.domain

enum class StreamingProvider(
    val preferenceKey: String,
    val label: String,
    val packageName: String,
    val enabledByDefault: Boolean,
    val brandColor: Int,
    val packageAliases: Set<String> = emptySet(),
) {
    NETFLIX(
        preferenceKey = "provider_netflix_enabled",
        label = "Netflix",
        packageName = "com.netflix.mediaclient",
        enabledByDefault = true,
        brandColor = 0xFFE50914.toInt(),
        packageAliases = setOf(
            "com.netflix.ninja",
        ),
    ),
    CRUNCHYROLL(
        preferenceKey = "provider_crunchyroll_enabled",
        label = "Crunchyroll",
        packageName = "com.crunchyroll.crunchyroid",
        enabledByDefault = true,
        brandColor = 0xFFFF640A.toInt(),
        packageAliases = setOf(
            "com.crunchyroll.crunchyrollapp",
        ),
    ),
    PRIME_VIDEO(
        preferenceKey = "provider_prime_video_enabled",
        label = "Prime Video",
        packageName = "com.amazon.avod.thirdpartyclient",
        enabledByDefault = true,
        brandColor = 0xFF00A8E1.toInt(),
        packageAliases = setOf(
            "com.amazon.avod",
            "com.amazon.amazonvideo.livingroom",
        ),
    ),
    MAX(
        preferenceKey = "provider_max_enabled",
        label = "Max",
        packageName = "com.wbd.stream",
        enabledByDefault = true,
        brandColor = 0xFF002BE7.toInt(),
        packageAliases = setOf(
            "com.hbo.hbonow",
            "com.hbomax",
        ),
    ),
    DISNEY_PLUS(
        preferenceKey = "provider_disney_plus_enabled",
        label = "Disney+",
        packageName = "com.disney.disneyplus",
        enabledByDefault = true,
        brandColor = 0xFF113CCF.toInt(),
    ),
    YOUTUBE(
        preferenceKey = "provider_youtube_enabled",
        label = "YouTube",
        packageName = "com.google.android.youtube",
        enabledByDefault = true,
        brandColor = 0xFFFF0000.toInt(),
        packageAliases = setOf(
            "com.google.android.youtube.tv",
        ),
    );

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
