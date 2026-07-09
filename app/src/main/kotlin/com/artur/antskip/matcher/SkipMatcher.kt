package com.artur.antskip.matcher

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.artur.antskip.data.PreferenceStore
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider

class SkipMatcher(
    private val preferences: PreferenceStore,
    private val phraseBank: SkipPhraseBank = SkipPhraseBank,
) {
    data class MatchResult(
        val targets: List<AccessibilityNodeInfo>,
        val action: SkipAction,
    )

    fun findTarget(root: AccessibilityNodeInfo, provider: StreamingProvider): MatchResult? {
        var visited = 0
        val rootBounds = Rect().also(root::getBoundsInScreen)
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(AccessibilityNodeInfo.obtain(root))

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++

            val nodeText = if (provider == StreamingProvider.CRUNCHYROLL) {
                NodeText.visibleFrom(node)
            } else {
                NodeText.from(node)
            }
            val action = matchAction(nodeText, provider)
            if (action != null && isActionAllowed(action, provider)) {
                val targets = node.clickCandidates(provider, rootBounds)
                node.recycle()
                pending.recycleAll()
                return targets.takeIf { it.isNotEmpty() }?.let { MatchResult(it, action) }
            }

            addChildren(node, pending)
            node.recycle()
        }

        pending.recycleAll()
        return null
    }

    private fun matchAction(text: String, provider: StreamingProvider): SkipAction? =
        when {
            isBlocked(text) -> null
            provider == StreamingProvider.CRUNCHYROLL -> matchCrunchyrollAction(text)
            provider == StreamingProvider.NETFLIX -> matchNetflixOrPrimeAction(text) ?: matchDefaultAction(text)
            provider == StreamingProvider.PRIME_VIDEO -> matchPrimeVideoAction(text) ?: matchCustomPhrase(text)
            else -> phraseBank.match(text) ?: matchCustomPhrase(text)
        }

    private fun isActionAllowed(action: SkipAction, provider: StreamingProvider): Boolean =
        when {
            action == SkipAction.NEXT_EPISODE && preferences.isNextEpisodeBlocked(provider) -> false
            provider == StreamingProvider.CRUNCHYROLL && action != SkipAction.NEXT_EPISODE -> {
                preferences.isActionEnabled(action)
            }
            else -> preferences.isActionEnabledForProvider(provider, action)
        }

    private fun matchCrunchyrollAction(text: String): SkipAction? =
        if (text in CRUNCHYROLL_GENERIC_INTRO_PHRASES) {
            SkipAction.INTRO
        } else {
            CRUNCHYROLL_PHRASES.entries.firstOrNull { (_, phrases) ->
                phrases.any { phrase -> text == phrase || text.contains(phrase) }
            }?.key
        }

    private fun matchNetflixOrPrimeAction(text: String): SkipAction? =
        NETFLIX_PRIME_EXACT_PHRASES.entries.firstOrNull { (_, phrases) ->
            text in phrases
        }?.key ?: NETFLIX_PRIME_PHRASES.entries.firstOrNull { (_, phrases) ->
            phrases.any { phrase -> text == phrase || text.contains(phrase) }
        }?.key

    private fun matchPrimeVideoAction(text: String): SkipAction? =
        PRIME_VIDEO_EXACT_PHRASES.entries.firstOrNull { (_, phrases) ->
            text in phrases
        }?.key ?: PRIME_VIDEO_PHRASES.entries.firstOrNull { (_, phrases) ->
            phrases.any { phrase -> text == phrase || text.contains(phrase) }
        }?.key

    private fun matchDefaultAction(text: String): SkipAction? =
        phraseBank.match(text) ?: matchCustomPhrase(text)

    private fun matchCustomPhrase(text: String): SkipAction? =
        SkipAction.entries.firstOrNull { action ->
            preferences.customPhrases(action)
                .map { it.normalizeForMatch() }
                .filter { it.isNotBlank() }
                .any { phrase -> text == phrase || text.contains(phrase) }
        }

    private fun isBlocked(text: String): Boolean =
        preferences.blockedPhrases()
            .map { it.normalizeForMatch() }
            .filter { it.isNotBlank() }
            .any { phrase -> text == phrase || text.contains(phrase) }

    private fun AccessibilityNodeInfo.clickCandidates(
        provider: StreamingProvider,
        rootBounds: Rect,
    ): List<AccessibilityNodeInfo> {
        val clickableTargets = mutableListOf<AccessibilityNodeInfo>()
        val actionTargets = mutableListOf<AccessibilityNodeInfo>()
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(this)
        while (current != null) {
            if (current.isEnabled && current.isVisibleToUser && current.isValidClickTarget(provider, rootBounds)) {
                when {
                    current.isClickable -> clickableTargets.add(AccessibilityNodeInfo.obtain(current))
                    provider != StreamingProvider.CRUNCHYROLL && current.hasClickAction() -> {
                        actionTargets.add(AccessibilityNodeInfo.obtain(current))
                    }
                }
            }
            val parent = current.parent
            current.recycle()
            current = parent
        }
        return clickableTargets + actionTargets
    }

    private fun AccessibilityNodeInfo.isValidClickTarget(provider: StreamingProvider, rootBounds: Rect): Boolean {
        if (provider != StreamingProvider.CRUNCHYROLL && provider != StreamingProvider.PRIME_VIDEO) return true
        val bounds = Rect()
        getBoundsInScreen(bounds)
        if (bounds.isEmpty || rootBounds.isEmpty) return false
        val maxWidthRatio = when (provider) {
            StreamingProvider.PRIME_VIDEO -> MAX_PRIME_VIDEO_BUTTON_WIDTH_RATIO
            else -> MAX_CRUNCHYROLL_BUTTON_WIDTH_RATIO
        }
        val maxHeightRatio = when (provider) {
            StreamingProvider.PRIME_VIDEO -> MAX_PRIME_VIDEO_BUTTON_HEIGHT_RATIO
            else -> MAX_CRUNCHYROLL_BUTTON_HEIGHT_RATIO
        }
        return bounds.width() <= rootBounds.width() * maxWidthRatio &&
            bounds.height() <= rootBounds.height() * maxHeightRatio
    }

    private fun AccessibilityNodeInfo.hasClickAction(): Boolean =
        actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }

    private fun addChildren(node: AccessibilityNodeInfo, pending: ArrayDeque<AccessibilityNodeInfo>) {
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { pending.add(it) }
        }
    }

    private fun ArrayDeque<AccessibilityNodeInfo>.recycleAll() {
        while (isNotEmpty()) {
            removeFirst().recycle()
        }
    }

    private companion object {
        const val MAX_VISITED_NODES = 300
        const val MAX_CRUNCHYROLL_BUTTON_WIDTH_RATIO = 0.9f
        const val MAX_CRUNCHYROLL_BUTTON_HEIGHT_RATIO = 0.25f
        const val MAX_PRIME_VIDEO_BUTTON_WIDTH_RATIO = 0.95f
        const val MAX_PRIME_VIDEO_BUTTON_HEIGHT_RATIO = 0.35f

        val CRUNCHYROLL_GENERIC_INTRO_PHRASES = setOf(
            "pular",
            "skip",
        )

        val CRUNCHYROLL_PHRASES = linkedMapOf(
            SkipAction.INTRO to setOf(
                "pular abertura",
                "pular introducao",
                "pular a introducao",
                "skip intro",
                "skip opening",
            ),
            SkipAction.RECAP to setOf(
                "pular resumo",
                "pular recapitulacao",
                "skip recap",
                "skip recapitulation",
            ),
            SkipAction.CREDITS to setOf(
                "pular creditos",
                "pular encerramento",
                "skip credits",
                "skip ending",
                "skip end credits",
            ),
            SkipAction.NEXT_EPISODE to setOf(
                "proximo episodio",
                "proximo episodio em",
                "proximo ep",
                "assistir proximo",
                "reproduzir proximo",
                "next episode",
                "next episode in",
                "play next episode",
                "watch next episode",
            ),
        )

        val NETFLIX_PRIME_EXACT_PHRASES = linkedMapOf(
            SkipAction.INTRO to setOf(
                "pular",
                "skip",
            ),
            SkipAction.NEXT_EPISODE to setOf(
                "proximo",
                "proxima",
                "next",
            ),
        )

        val NETFLIX_PRIME_PHRASES = linkedMapOf(
            SkipAction.INTRO to setOf(
                "pular abertura",
                "pular a abertura",
                "pular intro",
                "pular introducao",
                "pular a introducao",
                "skip intro",
                "skip opening",
                "skip introduction",
            ),
            SkipAction.RECAP to setOf(
                "pular resumo",
                "pular o resumo",
                "pular recap",
                "pular recapitulacao",
                "skip recap",
                "skip recapitulation",
                "skip previously on",
            ),
            SkipAction.CREDITS to setOf(
                "pular creditos",
                "pular encerramento",
                "pular final",
                "skip credits",
                "skip ending",
                "skip end credits",
            ),
            SkipAction.PREVIEW to setOf(
                "pular previa",
                "pular pre visualizacao",
                "pular preview",
                "pular trailer",
                "skip preview",
                "skip next preview",
                "skip trailer",
            ),
            SkipAction.NEXT_EPISODE to setOf(
                "proximo episodio",
                "proximo episodio em",
                "proxima parte",
                "reproduzir proximo",
                "assistir proximo",
                "next episode",
                "next episode in",
                "play next",
                "play next episode",
                "watch next episode",
                "next up",
            ),
        )

        val PRIME_VIDEO_EXACT_PHRASES = linkedMapOf(
            SkipAction.INTRO to setOf(
                "pular",
                "skip",
            ),
        )

        val PRIME_VIDEO_PHRASES = linkedMapOf(
            SkipAction.INTRO to setOf(
                "pular abertura",
                "pular a abertura",
                "pular intro",
                "pular introducao",
                "pular a introducao",
                "skip intro",
                "skip opening",
                "skip introduction",
            ),
            SkipAction.RECAP to setOf(
                "pular resumo",
                "pular o resumo",
                "pular recap",
                "pular recapitulacao",
                "skip recap",
                "skip recapitulation",
                "skip previously on",
            ),
            SkipAction.CREDITS to setOf(
                "pular creditos",
                "pular encerramento",
                "pular final",
                "skip credits",
                "skip ending",
                "skip end credits",
            ),
            SkipAction.PREVIEW to setOf(
                "pular previa",
                "pular pre visualizacao",
                "pular preview",
                "skip preview",
                "skip next preview",
            ),
            SkipAction.NEXT_EPISODE to setOf(
                "proximo episodio",
                "proximo episodio em",
                "reproduzir proximo",
                "assistir proximo",
                "next episode",
                "next episode in",
                "play next episode",
                "watch next episode",
                "next up",
            ),
        )
    }
}
