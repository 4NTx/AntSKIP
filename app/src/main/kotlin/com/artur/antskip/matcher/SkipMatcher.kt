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
        val matchedText: String,
        val visitedNodes: Int,
        val gestureBounds: Rect,
    )

    fun findTarget(root: AccessibilityNodeInfo, provider: StreamingProvider): MatchResult? {
        var visited = 0
        var firstMatchWithoutTargets: MatchResult? = null
        val rootBounds = Rect().also(root::getBoundsInScreen)
        val rootContextText = if (provider == StreamingProvider.NETFLIX) {
            collectRootContextText(root)
        } else {
            ""
        }
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++

            val nodeText = if (provider == StreamingProvider.CRUNCHYROLL) {
                NodeText.visibleFrom(node)
            } else {
                NodeText.from(node)
            }
            val matchedText = nodeText
            val action = matchAction(nodeText, node.viewIdResourceName, provider, rootContextText)
            if (action != null && isActionAllowed(action, provider)) {
                val targets = node.clickCandidates(provider, rootBounds)
                val gestureBounds = Rect().also(node::getBoundsInScreen)
                val result = MatchResult(targets, action, matchedText, visited, gestureBounds)
                if (targets.isNotEmpty()) {
                    return result
                }
                if (firstMatchWithoutTargets == null) {
                    firstMatchWithoutTargets = result
                }
            }

            addChildren(node, pending)
        }

        return firstMatchWithoutTargets
    }

    private fun matchAction(
        text: String,
        viewId: String?,
        provider: StreamingProvider,
        rootContextText: String,
    ): SkipAction? {
        if (isBlocked(text)) return null

        // Quick match by known resource ID if present
        if (!viewId.isNullOrBlank()) {
            val normalizedId = viewId.normalizeForMatch()
            matchByKnownViewId(normalizedId)?.let { return it }
        }

        return when (provider) {
            StreamingProvider.CRUNCHYROLL -> matchCrunchyrollAction(text)
            StreamingProvider.NETFLIX -> matchNetflixAction(text, rootContextText) ?: matchNetflixCustomAction(text, rootContextText)
            StreamingProvider.PRIME_VIDEO -> matchPrimeVideoAction(text) ?: matchCustomPhrase(text)
            else -> phraseBank.match(text) ?: matchCustomPhrase(text)
        }
    }

    private fun matchByKnownViewId(viewId: String): SkipAction? =
        when {
            viewId.contains("skip_intro") || viewId.contains("skip_opening") || viewId.contains("pular_abertura") -> SkipAction.INTRO
            viewId.contains("skip_recap") || viewId.contains("pular_resumo") || viewId.contains("pular_recap") -> SkipAction.RECAP
            viewId.contains("skip_credits") || viewId.contains("pular_creditos") || viewId.contains("skip_ending") -> SkipAction.CREDITS
            viewId.contains("next_episode") || viewId.contains("play_next") || viewId.contains("proximo_episodio") -> SkipAction.NEXT_EPISODE
            viewId.contains("skip_preview") || viewId.contains("pular_previa") -> SkipAction.PREVIEW
            else -> null
        }

    private fun isActionAllowed(action: SkipAction, provider: StreamingProvider): Boolean =
        when {
            action == SkipAction.NEXT_EPISODE && preferences.isNextEpisodeBlocked(provider) -> false
            action == SkipAction.CREDITS &&
                preferences.isSleepProtectionActive(provider) &&
                preferences.blocksCreditsDuringSleep(provider) -> false
            else -> preferences.isActionEnabledForProvider(provider, action)
        }

    private fun matchCrunchyrollAction(text: String): SkipAction? =
        if (text in CRUNCHYROLL_GENERIC_INTRO_PHRASES) {
            SkipAction.INTRO
        } else {
            CRUNCHYROLL_PHRASES.entries.firstOrNull { (_, phrases) ->
                phrases.any { phrase -> text == phrase || text.contains(phrase) }
            }?.key ?: phraseBank.match(text) ?: matchCustomPhrase(text)
        }

    private fun matchNetflixAction(text: String, rootContextText: String): SkipAction? {
        if (text == NETFLIX_FINAL_NEXT_BUTTON_PHRASE_PT_BR) {
            return SkipAction.NEXT_EPISODE.takeIf { hasNetflixEndPromptContext(rootContextText) }
        }
        val action = matchNetflixOrPrimeAction(text) ?: return null
        if (action != SkipAction.NEXT_EPISODE) return action
        if (!hasNetflixEndPromptContext(rootContextText)) return null
        return if (text in NETFLIX_FINAL_NEXT_BUTTON_PHRASES ||
            NETFLIX_FINAL_NEXT_EPISODE_PHRASES.any { phrase -> text == phrase || text.contains(phrase) }
        ) {
            SkipAction.NEXT_EPISODE
        } else {
            null
        }
    }

    private fun matchNetflixOrPrimeAction(text: String): SkipAction? =
        NETFLIX_PRIME_EXACT_PHRASES.entries.firstOrNull { (_, phrases) ->
            text in phrases
        }?.key ?: NETFLIX_PRIME_PHRASES.entries.firstOrNull { (_, phrases) ->
            phrases.any { phrase -> text == phrase || text.contains(phrase) }
        }?.key ?: phraseBank.match(text)

    private fun matchNetflixCustomAction(text: String, rootContextText: String): SkipAction? {
        val action = matchCustomPhrase(text) ?: return null
        if (action != SkipAction.NEXT_EPISODE) return action
        return action.takeIf { hasNetflixEndPromptContext(rootContextText) }
    }

    private fun hasNetflixEndPromptContext(rootContextText: String): Boolean =
        NETFLIX_END_PROMPT_CONTEXT_PHRASES.any { phrase ->
            rootContextText == phrase || rootContextText.contains(phrase)
        }

    private fun matchPrimeVideoAction(text: String): SkipAction? {
        // Exclude Prime Video X-Ray controls
        if (PRIME_VIDEO_EXCLUDE_PHRASES.any { text.contains(it) }) {
            return null
        }
        return PRIME_VIDEO_EXACT_PHRASES.entries.firstOrNull { (_, phrases) ->
            text in phrases
        }?.key ?: PRIME_VIDEO_PHRASES.entries.firstOrNull { (_, phrases) ->
            phrases.any { phrase -> text == phrase || text.contains(phrase) }
        }?.key ?: phraseBank.match(text)
    }

    fun matchCustomPhrase(text: String): SkipAction? =
        SkipAction.entries.firstOrNull { action ->
            preferences.customPhrases(action)
                .map { it.normalizeForMatch() }
                .filter { it.isNotBlank() }
                .any { phrase -> text == phrase || text.contains(phrase) }
        }

    fun isBlocked(text: String): Boolean =
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
        var current: AccessibilityNodeInfo? = this
        var depth = 0

        while (current != null && depth < MAX_PARENT_CLIMB_DEPTH) {
            if (current.isEnabled && current.isVisibleToUser && current.isValidClickTarget(provider, rootBounds)) {
                when {
                    current.isClickable -> clickableTargets.add(current)
                    current.hasClickAction() -> actionTargets.add(current)
                }
            }
            depth++
            current = current.parent
        }
        return clickableTargets + actionTargets
    }

    private fun AccessibilityNodeInfo.isValidClickTarget(provider: StreamingProvider, rootBounds: Rect): Boolean {
        val bounds = Rect()
        getBoundsInScreen(bounds)
        if (bounds.isEmpty || rootBounds.isEmpty) return false

        // Safeguard: buttons must have reasonable dimensions (reject tiny 0x0 or screen-covering containers)
        if (bounds.width() < MIN_BUTTON_DIMENSION_PX || bounds.height() < MIN_BUTTON_DIMENSION_PX) {
            return false
        }

        val maxWidthRatio = when (provider) {
            StreamingProvider.PRIME_VIDEO -> MAX_PRIME_VIDEO_BUTTON_WIDTH_RATIO
            StreamingProvider.CRUNCHYROLL -> MAX_CRUNCHYROLL_BUTTON_WIDTH_RATIO
            else -> MAX_GENERIC_BUTTON_WIDTH_RATIO
        }
        val maxHeightRatio = when (provider) {
            StreamingProvider.PRIME_VIDEO -> MAX_PRIME_VIDEO_BUTTON_HEIGHT_RATIO
            StreamingProvider.CRUNCHYROLL -> MAX_CRUNCHYROLL_BUTTON_HEIGHT_RATIO
            else -> MAX_GENERIC_BUTTON_HEIGHT_RATIO
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

    private fun collectRootContextText(root: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()
        var visited = 0
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++
            NodeText.visibleFrom(node).takeIf { it.isNotBlank() }?.let(parts::add)
            addChildren(node, pending)
        }
        return parts.joinToString(" ").normalizeForMatch()
    }

    private companion object {
        const val MAX_VISITED_NODES = 350
        const val MAX_PARENT_CLIMB_DEPTH = 6
        const val MIN_BUTTON_DIMENSION_PX = 16

        const val MAX_CRUNCHYROLL_BUTTON_WIDTH_RATIO = 0.85f
        const val MAX_CRUNCHYROLL_BUTTON_HEIGHT_RATIO = 0.25f
        const val MAX_PRIME_VIDEO_BUTTON_WIDTH_RATIO = 0.85f
        const val MAX_PRIME_VIDEO_BUTTON_HEIGHT_RATIO = 0.28f
        const val MAX_GENERIC_BUTTON_WIDTH_RATIO = 0.85f
        const val MAX_GENERIC_BUTTON_HEIGHT_RATIO = 0.28f

        val PRIME_VIDEO_EXCLUDE_PHRASES = setOf(
            "x ray",
            "xray",
            "elenco",
            "musica",
            "cenas",
            "detalhes",
            "bonus",
        )

        val CRUNCHYROLL_GENERIC_INTRO_PHRASES = setOf(
            "pular",
            "skip",
        )

        val CRUNCHYROLL_NEXT_EPISODE_PHRASES = setOf(
            "proximo episodio",
            "proximo episodio em",
            "proximo ep",
            "assistir proximo",
            "reproduzir proximo",
            "next episode",
            "next episode in",
            "play next",
            "play next episode",
            "watch next episode",
            "up next",
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
            SkipAction.NEXT_EPISODE to CRUNCHYROLL_NEXT_EPISODE_PHRASES,
        )

        val NETFLIX_PRIME_EXACT_PHRASES = linkedMapOf(
            SkipAction.INTRO to setOf(
                "pular",
                "skip",
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
                "proximo episodio em",
                "next episode",
                "next episode in",
                "play next episode",
                "watch next episode",
            ),
        )

        val NETFLIX_END_PROMPT_CONTEXT_PHRASES = setOf(
            "proximo episodio em",
            "proximo em",
            "next episode in",
            "next in",
        )

        val NETFLIX_FINAL_NEXT_BUTTON_PHRASES = setOf(
            "next",
            "proximo",
        )

        const val NETFLIX_FINAL_NEXT_BUTTON_PHRASE_PT_BR = "proximo"

        val NETFLIX_FINAL_NEXT_EPISODE_PHRASES = setOf(
            "play next episode",
            "watch next episode",
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
