package com.artur.antskip.matcher

import com.artur.antskip.domain.SkipAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SkipPhraseBankTest {

    @Test
    fun testPortugueseIntroPhrases() {
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("pular abertura"))
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("PULAR ABERTURA"))
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("Pular Introdução"))
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("Pular Intro"))
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("pular a introducao"))
    }

    @Test
    fun testEnglishIntroPhrases() {
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("Skip Intro"))
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("SKIP INTRO"))
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("skip opening"))
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("Skip Introduction"))
    }

    @Test
    fun testRecapPhrases() {
        assertEquals(SkipAction.RECAP, SkipPhraseBank.match("Pular resumo"))
        assertEquals(SkipAction.RECAP, SkipPhraseBank.match("Pular recapitulação"))
        assertEquals(SkipAction.RECAP, SkipPhraseBank.match("Skip Recap"))
        assertEquals(SkipAction.RECAP, SkipPhraseBank.match("saltar resumen"))
    }

    @Test
    fun testCreditsPhrases() {
        assertEquals(SkipAction.CREDITS, SkipPhraseBank.match("Pular créditos"))
        assertEquals(SkipAction.CREDITS, SkipPhraseBank.match("Pular encerramento"))
        assertEquals(SkipAction.CREDITS, SkipPhraseBank.match("Skip credits"))
        assertEquals(SkipAction.CREDITS, SkipPhraseBank.match("Skip ending"))
    }

    @Test
    fun testNextEpisodePhrases() {
        assertEquals(SkipAction.NEXT_EPISODE, SkipPhraseBank.match("Próximo episódio"))
        assertEquals(SkipAction.NEXT_EPISODE, SkipPhraseBank.match("proximo ep"))
        assertEquals(SkipAction.NEXT_EPISODE, SkipPhraseBank.match("Next Episode"))
        assertEquals(SkipAction.NEXT_EPISODE, SkipPhraseBank.match("Play Next Episode"))
        assertEquals(SkipAction.NEXT_EPISODE, SkipPhraseBank.match("Episodio siguiente"))
    }

    @Test
    fun testJapanesePhrases() {
        assertEquals(SkipAction.INTRO, SkipPhraseBank.match("オープニングをスキップ"))
        assertEquals(SkipAction.CREDITS, SkipPhraseBank.match("エンディングをスキップ"))
        assertEquals(SkipAction.NEXT_EPISODE, SkipPhraseBank.match("次のエピソード"))
    }

    @Test
    fun testBlankOrIrrelevantText() {
        assertNull(SkipPhraseBank.match(""))
        assertNull(SkipPhraseBank.match("   "))
        assertNull(SkipPhraseBank.match("Pausar vídeo"))
        assertNull(SkipPhraseBank.match("Volume 100%"))
        assertNull(SkipPhraseBank.match("Configurações"))
    }
}
