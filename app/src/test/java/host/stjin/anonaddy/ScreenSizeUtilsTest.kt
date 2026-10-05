package host.stjin.anonaddy

import host.stjin.anonaddy.utils.ScreenSizeUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenSizeUtilsTest {

    @Test
    fun testStandardPhoneSingleColumn() {
        // Standard phone (e.g. Pixel 7/8 portrait, width 412dp)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 412, isTablet = false)
        assertEquals(380, availableWidth)
        assertEquals(1, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = false))
    }

    @Test
    fun testSmallPhoneSingleColumn() {
        // Compact phone (e.g. 360dp)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 360, isTablet = false)
        assertEquals(328, availableWidth)
        assertEquals(1, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = false))
    }

    @Test
    fun testPhoneLandscapeTwoColumns() {
        // Phone in landscape (e.g. 800dp, height < 600dp so isTablet = false)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 800, isTablet = false)
        assertEquals(768, availableWidth)
        assertEquals(2, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = false))
    }

    @Test
    fun testFoldableFoldedSingleColumn() {
        // Foldable cover screen (e.g. Galaxy Z Fold cover screen, width 380dp)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 380, isTablet = false)
        assertEquals(348, availableWidth)
        assertEquals(1, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = false))
    }

    @Test
    fun testSurfaceDuoFoldedSingleColumn() {
        // Surface Duo single screen (width 537dp, isTablet = false)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 537, isTablet = false)
        assertEquals(505, availableWidth)
        assertEquals(1, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = false))
    }

    @Test
    fun testSurfaceDuoUnfoldedTwoColumns() {
        // Surface Duo spanned dual screen (width 1101dp, isTablet = true)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 1101, isTablet = true)
        assertEquals(957, availableWidth)
        // Dual screen fits 2 columns (1 column per screen)
        assertEquals(2, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = true))
    }

    @Test
    fun testFoldableUnfoldedPortraitTwoColumns() {
        // Foldable inner screen in portrait (e.g. Galaxy Z Fold 4/5/6, width ~700dp)
        // In tablet mode, navigation rail + end padding + fragment padding = 144dp insets
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 700, isTablet = true)
        assertEquals(556, availableWidth)
        // 556dp available width fits 2 columns (each ~278dp)
        assertEquals(2, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = true))
    }

    @Test
    fun testFoldableUnfoldedLandscapeTwoColumns() {
        // Foldable inner screen in landscape (e.g. width ~840dp)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 840, isTablet = true)
        assertEquals(696, availableWidth)
        // 696dp fits 2 columns (each ~348dp)
        assertEquals(2, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = true))
    }

    @Test
    fun testTabletPortraitTwoColumns() {
        // 10-inch tablet in portrait (e.g. 800dp)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 800, isTablet = true)
        assertEquals(656, availableWidth)
        // 656dp fits 2 columns (each 328dp)
        assertEquals(2, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = true))
    }

    @Test
    fun testTabletLandscapeThreeColumns() {
        // 10-inch tablet in landscape (e.g. 1200dp)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 1200, isTablet = true)
        assertEquals(1056, availableWidth)
        // 1056dp fits 3 columns (each ~352dp)
        assertEquals(3, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = true))
    }

    @Test
    fun testLargeTabletLandscapeFourColumns() {
        // 12.4"+ tablet in landscape (e.g. 1600dp)
        val availableWidth = ScreenSizeUtils.getAvailableContentWidthDp(screenWidthDp = 1600, isTablet = true)
        assertEquals(1456, availableWidth)
        // 1456dp fits 4 columns (each ~364dp)
        assertEquals(4, ScreenSizeUtils.calculateNoOfColumns(availableWidth, isTablet = true))
    }

    @Test
    fun testNarrowSplitScreenSingleColumn() {
        // Split screen window resized to narrow width (< 420dp)
        assertEquals(1, ScreenSizeUtils.calculateNoOfColumns(350, isTablet = true))
    }
}
