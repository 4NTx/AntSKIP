package com.artur.antskip.matcher

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizationTest {

    @Test
    fun testAccentRemoval() {
        assertEquals("proximo episodio", "Próximo Episódio".normalizeForMatch())
        assertEquals("pular introducao", "Pular Introdução".normalizeForMatch())
        assertEquals("recapitulacao", "Recapitulação".normalizeForMatch())
    }

    @Test
    fun testWhitespaceAndPunctuation() {
        assertEquals("pular abertura", "  Pular   Abertura!  ".normalizeForMatch())
        assertEquals("skip intro", "Skip-Intro".normalizeForMatch())
        assertEquals("skip intro", "Skip_Intro".normalizeForMatch())
    }

    @Test
    fun testNullAndBlank() {
        val nullStr: String? = null
        assertEquals("", nullStr.normalizeForMatch())
        assertEquals("", "".normalizeForMatch())
        assertEquals("", "    ".normalizeForMatch())
    }
}
