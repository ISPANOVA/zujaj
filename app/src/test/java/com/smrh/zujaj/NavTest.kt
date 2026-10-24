package com.smrh.zujaj

import com.smrh.zujaj.catalog.Shape
import com.smrh.zujaj.ui.Nav
import com.smrh.zujaj.ui.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The back-stack contract: Back pops exactly one page, in order, and history survives process death. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class NavTest {
    private fun push(n: Nav, s: Screen) { Thread.sleep(320); n.go(s) } // real taps are never 300ms apart within one gesture

    @Test fun backReturnsToActualPreviousScreen() {
        val n = Nav(Screen.Home)
        push(n, Screen.Search); push(n, Screen.Category("CLOCK")); push(n, Screen.Editor("clock_dots")); push(n, Screen.Paywall)
        assertTrue(n.back()); assertEquals(Screen.Editor("clock_dots"), n.top)
        assertTrue(n.back()); assertEquals(Screen.Category("CLOCK"), n.top)
        assertTrue(n.back()); assertEquals(Screen.Search, n.top)
        assertTrue(n.back()); assertEquals(Screen.Home, n.top)
        assertFalse(n.back())
    }

    @Test fun duplicateAndDoubleTapPushesAreIgnored() {
        val n = Nav(Screen.Home)
        push(n, Screen.Editor("a"))
        n.go(Screen.Editor("a"))           // same page again
        n.go(Screen.Editor("b"))           // second card hit within the same tap
        assertEquals(2, n.stack.size)
    }

    @Test fun historySurvivesSaveAndRestore() {
        val n = Nav(Screen.Home)
        push(n, Screen.Category("PRAYER")); push(n, Screen.Pick(Shape.WIDE, 7)); push(n, Screen.Editor("prayer_grid", 7, true, "x"))
        val saved = n.save()
        val ids = n.entries.map { it.id }
        val m = Nav(Screen.Home); m.restore(saved)
        assertEquals(n.stack, m.stack)
        assertEquals(ids, m.entries.map { it.id })   // same ids → each page gets its saved UI state back
    }
}
