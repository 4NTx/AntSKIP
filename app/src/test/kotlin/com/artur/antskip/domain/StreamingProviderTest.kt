package com.artur.antskip.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingProviderTest {

    @Test
    fun testProviderPackageDetection() {
        assertEquals(StreamingProvider.NETFLIX, StreamingProvider.fromPackageName("com.netflix.mediaclient"))
        assertEquals(StreamingProvider.NETFLIX, StreamingProvider.fromPackageName("com.netflix.ninja"))

        assertEquals(StreamingProvider.CRUNCHYROLL, StreamingProvider.fromPackageName("com.crunchyroll.crunchyroid"))
        assertEquals(StreamingProvider.CRUNCHYROLL, StreamingProvider.fromPackageName("com.crunchyroll.crunchyrollapp"))

        assertEquals(StreamingProvider.PRIME_VIDEO, StreamingProvider.fromPackageName("com.amazon.avod.thirdpartyclient"))
        assertEquals(StreamingProvider.PRIME_VIDEO, StreamingProvider.fromPackageName("com.amazon.avod"))
        assertEquals(StreamingProvider.PRIME_VIDEO, StreamingProvider.fromPackageName("com.amazon.amazonvideo.livingroom"))

        assertEquals(StreamingProvider.DISNEY_PLUS, StreamingProvider.fromPackageName("com.disney.disneyplus"))
        assertEquals(StreamingProvider.MAX, StreamingProvider.fromPackageName("com.wbd.stream"))
        assertEquals(StreamingProvider.PARAMOUNT_PLUS, StreamingProvider.fromPackageName("com.cbs.app"))
    }

    @Test
    fun testAllSupportedPackagesNotEmpty() {
        val allPkgs = StreamingProvider.allSupportedPackages()
        assertTrue(allPkgs.contains("com.netflix.mediaclient"))
        assertTrue(allPkgs.contains("com.netflix.ninja"))
        assertTrue(allPkgs.contains("com.crunchyroll.crunchyroid"))
        assertTrue(allPkgs.contains("com.amazon.avod.thirdpartyclient"))
    }
}
