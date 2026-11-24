package com.artur.antskip.matcher

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.artur.antskip.data.SkipSettings
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import java.util.Locale

class SkipMatcher(
    private val preferences: SkipSettings,
    private val phraseBank: SkipPhraseBank = SkipPhraseBank,
) {
    data class MatchResult(
        val targets: List<AccessibilityNodeInfo>,
        val action: SkipAction,
        val matchedText: String,
        val visitedNodes: Int,
        val gestureBounds: Rect,
    )

    data class ScreenContext(
        val fullText: String,
        val isBrowsingScreen: Boolean,
        val hasPlayerTransportControls: Boolean,
        val hasEndPromptContext: Boolean,
    )

    fun findTarget(root: AccessibilityNodeInfo, provider: StreamingProvider): MatchResult? {
        var visited = 0
        var firstMatchWithoutTargets: MatchResult? = null
        val rootBounds = Rect().also(root::getBoundsInScreen)
        val screenContext = collectScreenContext(root)
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++

            if (node.isVisibleToUser) {
                val nodeText = if (provider == StreamingProvider.CRUNCHYROLL) {
                    NodeText.visibleFrom(node)
                } else {
                    NodeText.from(node)
                }
                val action = matchAction(nodeText, node.viewIdResourceName, provider, screenContext)
                if (action != null && isActionAllowed(action, provider)) {
                    val targets = node.clickCandidates(provider, rootBounds)
                    val gestureBounds = Rect().also(node::getBoundsInScreen)
                    if (gestureBounds.isEmpty) {
                        node.parent?.getBoundsInScreen(gestureBounds)
                    }
                    val validGestureBounds = isValidButtonBounds(gestureBounds, rootBounds)
                    if (targets.isNotEmpty()) {
                        return MatchResult(targets, action, nodeText, visited, gestureBounds)
                    }
                    if (validGestureBounds && firstMatchWithoutTargets == null) {
                        firstMatchWithoutTargets = MatchResult(targets, action, nodeText, visited, gestureBounds)
                    }
                }
            }

            addChildren(node, pending)
        }

        return firstMatchWithoutTargets
    }

    fun simulateText(
        rawText: String,
        provider: StreamingProvider,
        isBrowsingScreen: Boolean = false,
        hasPlayerTransportControls: Boolean = false,
        hasEndPromptContext: Boolean = false,
    ): SkipAction? {
        val normalized = rawText.normalizeForMatch()
        val context = ScreenContext(
            fullText = normalized,
            isBrowsingScreen = isBrowsingScreen,
            hasPlayerTransportControls = hasPlayerTransportControls,
            hasEndPromptContext = hasEndPromptContext || hasEndPromptText(normalized),
        )
        return matchAction(normalized, null, provider, context)
    }

    internal fun matchAction(
        text: String,
        viewId: String?,
        provider: StreamingProvider,
        screenContext: ScreenContext,
    ): SkipAction? {
        if (isBlocked(text, viewId)) return null

        // Quick match by known resource ID if present (preserving underscores)
        if (!viewId.isNullOrBlank()) {
            val lowerId = viewId.lowercase(Locale.ROOT)
            matchByKnownViewId(lowerId)?.let { idAction ->
                if (isActionAllowedInScreenContext(idAction, text, provider, screenContext, fromExplicitViewId = true)) {
                    return idAction
                }
                return null
            }
        }

        return when (provider) {
            StreamingProvider.CRUNCHYROLL -> matchCrunchyrollAction(text, screenContext)
            StreamingProvider.NETFLIX -> matchNetflixAction(text, screenContext)
            StreamingProvider.PRIME_VIDEO -> matchPrimeVideoAction(text, screenContext)
            StreamingProvider.MAX,
            StreamingProvider.DISNEY_PLUS,
            StreamingProvider.GLOBOPLAY,
            -> matchGenericProviderAction(text, provider, screenContext)
            StreamingProvider.YOUTUBE -> matchYouTubeAction(text)
        }
    }

    private fun isActionAllowedInScreenContext(
        action: SkipAction,
        text: String,
        provider: StreamingProvider,
        screenContext: ScreenContext,
        fromExplicitViewId: Boolean = false,
    ): Boolean {
        if (action == SkipAction.NEXT_EPISODE) {
            if (provider == StreamingProvider.YOUTUBE) return false
            if (screenContext.isBrowsingScreen) return false
            if (screenContext.hasPlayerTransportControls && !screenContext.hasEndPromptContext) return false
        }
        if (!fromExplicitViewId && screenContext.isBrowsingScreen && provider != StreamingProvider.YOUTUBE) {
            if (text in GENERIC_SINGLE_WORD_SKIP_PHRASES) return false
        }
        return true
    }

    private fun matchYouTubeAction(text: String): SkipAction? =
        when {
            text == "pular" || text == "skip" -> SkipAction.INTRO
            SkipPhraseBank.containsWholePhrase(text, "pular anuncio") ||
                SkipPhraseBank.containsWholePhrase(text, "skip ad") ||
                SkipPhraseBank.containsWholePhrase(text, "skip ads") ||
                SkipPhraseBank.containsWholePhrase(text, "skip advertisement") -> SkipAction.INTRO
            else -> phraseBank.match(text)?.takeIf { it != SkipAction.NEXT_EPISODE }
        }

    private fun matchByKnownViewId(viewId: String): SkipAction? =
        when {
            viewId.contains("skip_intro") || viewId.contains("skip_opening") || viewId.contains("pular_abertura") || viewId.contains("skip_ad") || viewId.contains("ad_skip") -> SkipAction.INTRO
            viewId.contains("skip_recap") || viewId.contains("pular_resumo") || viewId.contains("pular_recap") -> SkipAction.RECAP
            viewId.contains("skip_credits") || viewId.contains("pular_creditos") || viewId.contains("skip_ending") -> SkipAction.CREDITS
            viewId.contains("play_next_episode") || viewId.contains("proximo_episodio_btn") -> SkipAction.NEXT_EPISODE
            else -> null
        }

    private fun isActionAllowed(action: SkipAction, provider: StreamingProvider): Boolean =
        preferences.isActionEnabled(action) && preferences.isProviderEnabled(provider)

    private fun matchCrunchyrollAction(text: String, screenContext: ScreenContext): SkipAction? {
        if (text.isBlank() || text.length > MAX_BUTTON_TEXT_LENGTH) return null

        if (text in CRUNCHYROLL_GENERIC_INTRO_PHRASES) {
            return SkipAction.INTRO.takeIf { !screenContext.isBrowsingScreen }
        }

        // Unambiguous player skip buttons (Intro / Recap / Credits)
        CRUNCHYROLL_SKIP_PHRASES.entries.firstOrNull { (_, phrases) ->
            phrases.any { phrase -> SkipPhraseBank.containsWholePhrase(text, phrase) }
        }?.key?.let { return it }

        // Next Episode in Crunchyroll: strictly guard against browse screens & player transport controls
        if (screenContext.isBrowsingScreen) {
            return phraseBank.match(text)?.takeIf {
                it != SkipAction.NEXT_EPISODE && text !in GENERIC_SINGLE_WORD_SKIP_PHRASES
            }
        }

        if (CRUNCHYROLL_COUNTDOWN_NEXT_PHRASES.any { SkipPhraseBank.containsWholePhrase(text, it) }) {
            return SkipAction.NEXT_EPISODE
        }

        if (CRUNCHYROLL_ACTION_NEXT_PHRASES.any { SkipPhraseBank.containsWholePhrase(text, it) }) {
            return SkipAction.NEXT_EPISODE.takeIf {
                !screenContext.hasPlayerTransportControls || screenContext.hasEndPromptContext
            }
        }

        val fallbackAction = phraseBank.match(text) ?: return null
        if (fallbackAction != SkipAction.NEXT_EPISODE) return fallbackAction

        // Generic "Próximo episódio" / "Next episode" only allowed during real end-of-episode prompt without transport bar
        return SkipAction.NEXT_EPISODE.takeIf {
            screenContext.hasEndPromptContext && !screenContext.hasPlayerTransportControls
        }
    }

    private fun matchNetflixAction(text: String, screenContext: ScreenContext): SkipAction? {
        if (text == NETFLIX_FINAL_NEXT_BUTTON_PHRASE_PT_BR) {
            return SkipAction.NEXT_EPISODE.takeIf {
                !screenContext.isBrowsingScreen &&
                    !screenContext.hasPlayerTransportControls &&
                    hasNetflixEndPromptContext(screenContext.fullText)
            }
        }
        val action = matchNetflixOrPrimeAction(text) ?: return null
        if (!isActionAllowedInScreenContext(action, text, StreamingProvider.NETFLIX, screenContext)) return null
        if (action != SkipAction.NEXT_EPISODE) return action
        if (!hasNetflixEndPromptContext(screenContext.fullText)) return null
        return if (text in NETFLIX_FINAL_NEXT_BUTTON_PHRASES ||
            NETFLIX_FINAL_NEXT_EPISODE_PHRASES.any { phrase -> SkipPhraseBank.containsWholePhrase(text, phrase) }
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
            phrases.any { phrase -> SkipPhraseBank.containsWholePhrase(text, phrase) }
        }?.key ?: phraseBank.match(text)

    private fun hasNetflixEndPromptContext(rootContextText: String): Boolean =
        NETFLIX_END_PROMPT_CONTEXT_PHRASES.any { phrase ->
            SkipPhraseBank.containsWholePhrase(rootContextText, phrase)
        }

    private fun matchPrimeVideoAction(text: String, screenContext: ScreenContext): SkipAction? {
        if (text.isBlank() || text.length > MAX_BUTTON_TEXT_LENGTH) return null
        if (PRIME_VIDEO_EXCLUDE_PHRASES.any { SkipPhraseBank.containsWholePhrase(text, it) }) {
            return null
        }

        // Prime Video next episode bottom card: "Próximo: Nome do Episódio", "Next: Episode Name", "Up next: ...", "Siguiente: ..."
        val isNextPrefixCard = text.startsWith("proximo ") ||
            text.startsWith("next ") ||
            text.startsWith("up next") ||
            text.startsWith("siguiente ")
        if (isNextPrefixCard) {
            return SkipAction.NEXT_EPISODE.takeIf {
                !screenContext.isBrowsingScreen && !screenContext.hasPlayerTransportControls
            }
        }

        val action = PRIME_VIDEO_EXACT_PHRASES.entries.firstOrNull { (_, phrases) ->
            text in phrases
        }?.key ?: PRIME_VIDEO_PHRASES.entries.firstOrNull { (_, phrases) ->
            phrases.any { phrase -> SkipPhraseBank.containsWholePhrase(text, phrase) }
        }?.key ?: phraseBank.match(text) ?: return null

        return action.takeIf {
            isActionAllowedInScreenContext(it, text, StreamingProvider.PRIME_VIDEO, screenContext)
        }
    }

    private fun matchGenericProviderAction(
        text: String,
        provider: StreamingProvider,
        screenContext: ScreenContext,
    ): SkipAction? {
        val action = phraseBank.match(text) ?: return null
        return action.takeIf {
            isActionAllowedInScreenContext(it, text, provider, screenContext)
        }
    }

    fun isBlocked(text: String, viewId: String? = null): Boolean {
        if (!viewId.isNullOrBlank()) {
            val lowerId = viewId.lowercase(Locale.ROOT)
            if (BLOCKED_VIEW_IDS.any { lowerId.contains(it) }) {
                return true
            }
        }

        if (text.isBlank()) return false

        if (SCRUB_TIME_REGEX.containsMatchIn(text) && !hasEndPromptText(text)) {
            return true
        }

        if (SkipPhraseBank.containsAnyWord(text, BLOCKED_DISMISS_WORDS)) {
            return true
        }

        return BLOCKED_TRANSPORT_WORDS.any { SkipPhraseBank.containsWholePhrase(text, it) }
    }

    private fun AccessibilityNodeInfo.clickCandidates(
        provider: StreamingProvider,
        rootBounds: Rect,
    ): List<AccessibilityNodeInfo> {
        val clickableTargets = mutableListOf<AccessibilityNodeInfo>()
        val actionTargets = mutableListOf<AccessibilityNodeInfo>()
        var current: AccessibilityNodeInfo? = this
        var depth = 0

        while (current != null && depth < MAX_PARENT_CLIMB_DEPTH) {
            if (current.isEnabled && current.isValidClickTarget(provider, rootBounds)) {
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
        return isValidButtonBounds(bounds, rootBounds)
    }

    private fun isValidButtonBounds(bounds: Rect, rootBounds: Rect): Boolean {
        if (bounds.isEmpty || rootBounds.isEmpty) return false

        // Safeguard: buttons must have reasonable dimensions (reject 0x0 or entire screen)
        if (bounds.width() < MIN_BUTTON_DIMENSION_PX || bounds.height() < MIN_BUTTON_DIMENSION_PX) {
            return false
        }

        return bounds.width() <= rootBounds.width() * MAX_BUTTON_WIDTH_RATIO &&
            bounds.height() <= rootBounds.height() * MAX_BUTTON_HEIGHT_RATIO
    }

    private fun AccessibilityNodeInfo.hasClickAction(): Boolean =
        actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }

    private fun addChildren(node: AccessibilityNodeInfo, pending: ArrayDeque<AccessibilityNodeInfo>) {
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { pending.add(it) }
        }
    }

    private fun collectScreenContext(root: AccessibilityNodeInfo): ScreenContext {
        val normalizedTexts = mutableListOf<String>()
        val rawTexts = mutableListOf<String>()
        val viewIds = mutableListOf<String>()
        var visited = 0
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)

        while (pending.isNotEmpty() && visited < MAX_VISITED_NODES) {
            val node = pending.removeFirst()
            visited++
            if (node.isVisibleToUser) {
                node.viewIdResourceName?.takeIf { it.isNotBlank() }?.let(viewIds::add)
                val raw = buildString {
                    node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(::append)
                    node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let { desc ->
                        if (isNotEmpty()) append(' ')
                        append(desc)
                    }
                }
                if (raw.isNotBlank()) {
                    rawTexts.add(raw)
                    val normalized = raw.normalizeForMatch()
                    if (normalized.isNotBlank()) {
                        normalizedTexts.add(normalized)
                    }
                }
            }
            addChildren(node, pending)
        }

        return analyzeScreenContext(normalizedTexts, rawTexts, viewIds)
    }

    internal fun analyzeScreenContext(
        normalizedTexts: List<String>,
        rawTexts: List<String> = normalizedTexts,
        viewIds: List<String> = emptyList(),
    ): ScreenContext {
        val fullText = normalizedTexts.filter { it.isNotBlank() }.joinToString(" ").normalizeForMatch()

        val hasBrowseViewId = viewIds.any { id ->
            val lower = id.lowercase(Locale.ROOT)
            BROWSE_VIEW_ID_HINTS.any { lower.contains(it) }
        }
        val browseSignalsFound = mutableSetOf<String>()
        for (text in normalizedTexts) {
            if (text.length <= 40) {
                for (signal in BROWSE_SCREEN_SIGNALS) {
                    if (text == signal || SkipPhraseBank.containsWholePhrase(text, signal)) {
                        browseSignalsFound.add(signal)
                    }
                }
            }
        }
        val isBrowsingScreen = hasBrowseViewId || browseSignalsFound.size >= 2

        val hasTransportViewId = viewIds.any { id ->
            val lower = id.lowercase(Locale.ROOT)
            TRANSPORT_VIEW_ID_HINTS.any { lower.contains(it) }
        }
        val hasPreviousEpisodeButton = normalizedTexts.any { text ->
            SkipPhraseBank.containsWholePhrase(text, "episodio anterior") ||
                SkipPhraseBank.containsWholePhrase(text, "previous episode")
        }
        val transportSignalsFound = mutableSetOf<String>()
        for (text in normalizedTexts) {
            if (text.length <= 40) {
                for (signal in TRANSPORT_TEXT_SIGNALS) {
                    if (text == signal || SkipPhraseBank.containsWholePhrase(text, signal)) {
                        transportSignalsFound.add(signal)
                    }
                }
            }
        }
        val hasPlaybackTimestamp = rawTexts.any { TIMESTAMP_REGEX.containsMatchIn(it) }
        val hasPlayerTransportControls = hasTransportViewId ||
            hasPreviousEpisodeButton ||
            transportSignalsFound.size >= 2 ||
            (transportSignalsFound.isNotEmpty() && hasPlaybackTimestamp)

        val hasEndPromptContext = hasEndPromptText(fullText)

        return ScreenContext(
            fullText = fullText,
            isBrowsingScreen = isBrowsingScreen,
            hasPlayerTransportControls = hasPlayerTransportControls,
            hasEndPromptContext = hasEndPromptContext,
        )
    }

    private fun hasEndPromptText(fullText: String): Boolean =
        END_PROMPT_CONTEXT_PHRASES.any { phrase ->
            SkipPhraseBank.containsWholePhrase(fullText, phrase)
        }

    private companion object {
        const val MAX_VISITED_NODES = 350
        const val MAX_PARENT_CLIMB_DEPTH = 6
        const val MIN_BUTTON_DIMENSION_PX = 16
        const val MAX_BUTTON_WIDTH_RATIO = 0.85f
        const val MAX_BUTTON_HEIGHT_RATIO = 0.28f
        const val MAX_BUTTON_TEXT_LENGTH = 72

        val SCRUB_TIME_REGEX = Regex("""\b\d+\s*(s|seg|segundos?|seconds?|sec)\b""")
        val TIMESTAMP_REGEX = Regex("""\b\d{1,2}:\d{2}(:\d{2})?\b""")

        val BLOCKED_VIEW_IDS = setOf(
            "exo_ffwd",
            "exo_rew",
            "exo_next",
            "exo_prev",
            "exo_play",
            "exo_pause",
            "exo_progress",
            "exo_position",
            "exo_duration",
            "fast_forward",
            "rewind",
            "seek_forward",
            "seek_backward",
            "skip_forward",
            "skip_backward",
            "btn_ffwd",
            "btn_rew",
            "btn_next",
            "btn_prev",
            "transport_controls",
            "media_control",
            "player_control",
            "scrub",
            "seek_bar",
            "progress_bar",
            "previous_episode",
            "prev_episode",
            "episodio_anterior",
            "close",
            "dismiss",
            "cancel",
            "bottom_nav",
            "bottom_navigation",
            "navigation_bar",
            "tab_layout",
        )

        val BLOCKED_DISMISS_WORDS = setOf(
            "fechar",
            "close",
            "cancelar",
            "cancel",
            "dispensar",
            "dismiss",
            "ocultar",
            "hide",
            "recusar",
            "decline",
            "anterior",
            "previous",
            "nao",
        )

        val BLOCKED_TRANSPORT_WORDS = setOf(
            "segundo",
            "segundos",
            "second",
            "seconds",
            "avancar",
            "avançar",
            "retroceder",
            "voltar",
            "forward",
            "rewind",
            "backward",
            "back",
            "ahead",
            "seek",
            "scrub",
            "play",
            "pause",
            "reproduzir",
            "pausar",
            "velocidade",
            "speed",
            "playback speed",
            "qualidade",
            "quality",
            "legenda",
            "legendas",
            "subtitle",
            "subtitles",
            "audio",
            "idioma",
            "language",
            "bloqueio",
            "bloquear",
            "lock",
            "episodios",
            "episódios",
            "episodes",
            "temporadas",
            "seasons",
            "minha lista",
            "watchlist",
            "compartilhar",
            "share",
            "trailer",
            "mais informacoes",
            "more info",
            "detalhes",
            "details",
            "assistir do inicio",
            "watch from beginning",
            "restart",
            "reiniciar",
        )

        val BROWSE_VIEW_ID_HINTS = setOf(
            "bottom_nav",
            "bottom_navigation",
            "navigation_bar",
            "nav_bar",
            "tab_layout",
            "search_bar",
            "search_src_text",
            "swipe_refresh",
        )

        val BROWSE_SCREEN_SIGNALS = setOf(
            "inicio",
            "home",
            "navegar",
            "browse",
            "explorar",
            "explore",
            "simulcasts",
            "novidades",
            "new hot",
            "pesquisar",
            "buscar",
            "search",
            "minha lista",
            "my list",
            "listas",
            "crunchylists",
            "watchlist",
            "conta",
            "account",
            "perfil",
            "profile",
            "downloads",
            "offline",
            "mais como este",
            "more like this",
            "recomendados",
            "recommended",
            "temporadas",
            "seasons",
            "avaliacoes",
            "reviews",
            "detalhes",
            "details",
            "sinopse",
            "synopsis",
            "continuar assistindo",
            "continue watching",
            "adicionar a lista",
            "add to list",
            "add to my list",
            "historico",
            "history",
            "manga",
            "noticias",
            "news",
        )

        val TRANSPORT_VIEW_ID_HINTS = setOf(
            "exo_progress",
            "exo_play",
            "exo_pause",
            "exo_rew",
            "exo_ffwd",
            "exo_prev",
            "exo_next",
            "seek_bar",
            "progress_bar",
            "transport_controls",
            "player_control",
            "media_control",
        )

        val TRANSPORT_TEXT_SIGNALS = setOf(
            "pausar",
            "pause",
            "reproduzir",
            "velocidade",
            "playback speed",
            "qualidade",
            "quality",
            "legenda",
            "legendas",
            "subtitles",
            "bloqueio",
            "bloquear tela",
            "lock screen",
            "episodio anterior",
            "previous episode",
        )

        val GENERIC_SINGLE_WORD_SKIP_PHRASES = setOf(
            "pular",
            "skip",
            "saltar",
            "omitir",
            "passer",
            "salta",
        )

        val END_PROMPT_CONTEXT_PHRASES = setOf(
            "proximo episodio em",
            "proximo em",
            "reproduzindo em",
            "comecando em",
            "iniciando em",
            "a seguir em",
            "next episode in",
            "next in",
            "up next in",
            "starting in",
            "playing in",
            "auto play in",
            "autoplay in",
            "siguiente episodio en",
            "episode suivant dans",
        )

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

        val CRUNCHYROLL_COUNTDOWN_NEXT_PHRASES = setOf(
            "proximo episodio em",
            "next episode in",
            "up next in",
        )

        val CRUNCHYROLL_ACTION_NEXT_PHRASES = setOf(
            "assistir proximo",
            "reproduzir proximo",
            "play next episode",
            "watch next episode",
        )

        val CRUNCHYROLL_SKIP_PHRASES = linkedMapOf(
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
            SkipAction.NEXT_EPISODE to setOf(
                "proximo ep",
                "proximo episodio",
                "proximo episodio em",
                "proximo capitulo",
                "proxima parte",
                "reproduzir proximo",
                "assistir proximo",
                "next episode",
                "next episode in",
                "play next episode",
                "watch next episode",
                "next up",
                "up next",
            ),
        )
    }
}
