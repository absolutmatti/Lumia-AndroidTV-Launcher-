package com.example

import com.example.data.model.FavoriteApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Collator
import java.util.Locale

class LumiaFavoritesLogicTest {

    @Test
    fun testRenumberingGapless() {
        val original = listOf(
            FavoriteApp("com.app.a", 0),
            FavoriteApp("com.app.b", 1),
            FavoriteApp("com.app.c", 2),
            FavoriteApp("com.app.d", 3)
        )

        // Remove item at index 1 ("com.app.b")
        val filtered = original.filter { it.packageName != "com.app.b" }
        val reindexed = filtered.mapIndexed { index, app ->
            FavoriteApp(app.packageName, index)
        }

        assertEquals(3, reindexed.size)
        assertEquals(0, reindexed[0].position)
        assertEquals("com.app.a", reindexed[0].packageName)
        assertEquals(1, reindexed[1].position)
        assertEquals("com.app.c", reindexed[1].packageName)
        assertEquals(2, reindexed[2].position)
        assertEquals("com.app.d", reindexed[2].packageName)
    }

    @Test
    fun testMaxSixFavoritesLimit() {
        val favorites = mutableListOf<FavoriteApp>()
        for (i in 0 until 10) {
            if (favorites.size < 6) {
                favorites.add(FavoriteApp("com.app.$i", favorites.size))
            }
        }
        assertEquals(6, favorites.size)
        assertEquals(5, favorites.last().position)
    }

    @Test
    fun testAlphabeticalSortingWithCollator() {
        val collator = Collator.getInstance(Locale.GERMAN).apply {
            strength = Collator.PRIMARY
        }
        val appLabels = listOf("ZDF", "ARD", "Ärzte App", "Netflix", "YouTube", "Öffentlicher Dienst")
        val sorted = appLabels.sortedWith { a, b -> collator.compare(a, b) }

        assertEquals("ARD", sorted[0])
        assertTrue(sorted.indexOf("ARD") < sorted.indexOf("Netflix"))
        assertTrue(sorted.indexOf("Netflix") < sorted.indexOf("YouTube"))
        assertTrue(sorted.indexOf("YouTube") < sorted.indexOf("ZDF"))
    }
}
