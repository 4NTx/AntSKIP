package com.artur.antskip.domain

enum class StreamingProvider(
    val preferenceKey: String,
    val label: String,
    val packageName: String,
    val enabledByDefault: Boolean,
) {
    CRUNCHYROLL("provider_crunchyroll_enabled", "Crunchyroll", "com.crunchyroll.crunchyroid", true),
    NETFLIX("provider_netflix_enabled", "Netflix", "com.netflix.mediaclient", false),
    PRIME_VIDEO("provider_prime_video_enabled", "Prime Video", "com.amazon.avod.thirdpartyclient", false);

    companion object {
        fun fromPackageName(packageName: String): StreamingProvider? =
            entries.firstOrNull { it.packageName == packageName }
    }
}
