package com.example.summitread.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA guard for the design-system palette: body text 4.5:1, large/secondary text noted. */
class ThemeContrastTest {

    private fun assertAA(name: String, ratio: Double, min: Double = 4.5) =
        assertTrue("$name contrast %.2f < $min".format(ratio), ratio >= min)

    @Test
    fun accentTextOnTintMeetsAA() {
        AccentColor.entries.forEach { a ->
            assertAA("${a.name} light onPrimary", contrastRatio(a.lightOnPrimary, a.lightPrimary))
            assertAA("${a.name} dark onPrimary", contrastRatio(a.darkOnPrimary, a.darkPrimary))
        }
    }

    @Test
    fun tintAsTextOnSurfacesMeetsAA() {
        AccentColor.entries.forEach { a ->
            listOf(false, true).forEach { dark ->
                val c = appColors(dark, a)
                assertAA("${a.name} dark=$dark tint on surface", contrastRatio(c.tint, c.surface))
                assertAA("${a.name} dark=$dark tint on grouped bg", contrastRatio(c.tint, c.groupedBackground))
            }
        }
    }

    @Test
    fun labelsMeetAA() {
        AccentColor.entries.forEach { a ->
            listOf(false, true).forEach { dark ->
                val c = appColors(dark, a)
                listOf(c.surface, c.groupedBackground, c.elevatedSurface).forEach { bg ->
                    assertAA("label dark=$dark", contrastRatio(c.label, bg))
                    assertAA("secondaryLabel dark=$dark", contrastRatio(c.secondaryLabel, bg))
                    assertAA("destructive dark=$dark", contrastRatio(c.destructive, bg))
                }
            }
        }
    }

    @Test
    fun materialSchemePairsMeetAA() {
        AccentColor.entries.forEach { a ->
            listOf(false, true).forEach { dark ->
                val s = summitColorScheme(dark, a)
                val pairs = mapOf(
                    "primary" to (s.onPrimary to s.primary),
                    "secondary" to (s.onSecondary to s.secondary),
                    "tertiary" to (s.onTertiary to s.tertiary),
                    "primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
                    "secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
                    "surface" to (s.onSurface to s.surface),
                    "surfaceVariant" to (s.onSurfaceVariant to s.surfaceVariant),
                    "background" to (s.onBackground to s.background),
                    "error" to (s.onError to s.error),
                    "errorContainer" to (s.onErrorContainer to s.errorContainer)
                )
                pairs.forEach { (name, p) ->
                    assertAA("${a.name} dark=$dark $name", contrastRatio(p.first, p.second))
                }
            }
        }
    }
}
