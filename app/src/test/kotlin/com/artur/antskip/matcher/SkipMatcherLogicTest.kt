package com.artur.antskip.matcher

import com.artur.antskip.data.SkipSettings
import com.artur.antskip.domain.SkipAction
import com.artur.antskip.domain.StreamingProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkipMatcherLogicTest {

    private val allEnabledSettings = object : SkipSettings {
        override val isAutomationEnabled: Boolean = true
        override fun isActionEnabled(action: SkipAction): Boolean = true
        override fun isProviderEnabled(provider: StreamingProvider): Boolean = true
    }

    private val matcher = SkipMatcher(allEnabledSettings)

    @Test
    fun crunchyrollRejectsTitlesAndCloseButtons() {
        val playerContext = matcher.analyzeScreenContext(
            normalizedTexts = listOf("boruto naruto next generations", "fechar proximo popup"),
        )
        assertNull(
            matcher.matchAction(
                text = "boruto naruto next generations",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = playerContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "fechar proximo popup",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = playerContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "proximo",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = playerContext,
            ),
        )
    }

    @Test
    fun crunchyrollBlocksNextEpisodeDuringNavigationAndBrowsing() {
        val browseContext = matcher.analyzeScreenContext(
            normalizedTexts = listOf(
                "inicio",
                "minha lista",
                "navegar",
                "simulcasts",
                "conta",
                "boruto naruto next generations",
                "proximo episodio",
            ),
        )
        assertTrue(browseContext.isBrowsingScreen)
        assertNull(
            matcher.matchAction(
                text = "proximo episodio",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = browseContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "assistir proximo",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = browseContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "pular",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = browseContext,
            ),
        )
    }

    @Test
    fun crunchyrollBlocksNextEpisodeInsidePlayerTransportBar() {
        val transportContext = matcher.analyzeScreenContext(
            normalizedTexts = listOf(
                "episodio anterior",
                "pausar",
                "proximo episodio",
                "qualidade",
                "legendas",
            ),
            rawTexts = listOf(
                "Episódio anterior",
                "Pausar",
                "Próximo episódio",
                "04:21 / 23:40",
            ),
        )
        assertFalse(transportContext.isBrowsingScreen)
        assertTrue(transportContext.hasPlayerTransportControls)
        assertNull(
            matcher.matchAction(
                text = "proximo episodio",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = transportContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "assistir proximo",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = transportContext,
            ),
        )
    }

    @Test
    fun crunchyrollAllowsRealPlayerSkipsAndCountdownNextEpisode() {
        val introContext = matcher.analyzeScreenContext(
            normalizedTexts = listOf("pular introducao"),
        )
        assertFalse(introContext.isBrowsingScreen)
        assertEquals(
            SkipAction.INTRO,
            matcher.matchAction(
                text = "pular introducao",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = introContext,
            ),
        )
        assertEquals(
            SkipAction.CREDITS,
            matcher.matchAction(
                text = "pular creditos",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = introContext,
            ),
        )

        val endPopupContext = matcher.analyzeScreenContext(
            normalizedTexts = listOf("proximo episodio em 5s", "proximo episodio", "fechar proximo popup"),
        )
        assertTrue(endPopupContext.hasEndPromptContext)
        assertEquals(
            SkipAction.NEXT_EPISODE,
            matcher.matchAction(
                text = "proximo episodio em 5s",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = endPopupContext,
            ),
        )
        assertEquals(
            SkipAction.NEXT_EPISODE,
            matcher.matchAction(
                text = "proximo episodio",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = endPopupContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "fechar proximo popup",
                viewId = null,
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = endPopupContext,
            ),
        )
    }

    @Test
    fun viewIdMatchingPreservesUnderscoresAndRespectsBlocklist() {
        val playerContext = matcher.analyzeScreenContext(emptyList())
        assertEquals(
            SkipAction.INTRO,
            matcher.matchAction(
                text = "",
                viewId = "com.crunchyroll.crunchyroid:id/skip_intro_button",
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = playerContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "proximo episodio",
                viewId = "com.crunchyroll.crunchyroid:id/exo_next",
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = playerContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "fechar proximo popup",
                viewId = "com.crunchyroll.crunchyroid:id/close_popup_btn",
                provider = StreamingProvider.CRUNCHYROLL,
                screenContext = playerContext,
            ),
        )
    }

    @Test
    fun primeVideoAndYouTubeRespectContext() {
        val browseContext = matcher.analyzeScreenContext(
            normalizedTexts = listOf("inicio", "pesquisar", "minha lista", "proximo episodio"),
        )
        assertNull(
            matcher.matchAction(
                text = "proximo episodio 2",
                viewId = null,
                provider = StreamingProvider.PRIME_VIDEO,
                screenContext = browseContext,
            ),
        )

        val primePlayerContext = matcher.analyzeScreenContext(emptyList())
        assertEquals(
            SkipAction.NEXT_EPISODE,
            matcher.matchAction(
                text = "proximo episodio 2",
                viewId = null,
                provider = StreamingProvider.PRIME_VIDEO,
                screenContext = primePlayerContext,
            ),
        )

        assertEquals(
            SkipAction.INTRO,
            matcher.matchAction(
                text = "pular anuncio",
                viewId = null,
                provider = StreamingProvider.YOUTUBE,
                screenContext = browseContext,
            ),
        )
        assertNull(
            matcher.matchAction(
                text = "proximo episodio",
                viewId = null,
                provider = StreamingProvider.YOUTUBE,
                screenContext = primePlayerContext,
            ),
        )
    }
}
