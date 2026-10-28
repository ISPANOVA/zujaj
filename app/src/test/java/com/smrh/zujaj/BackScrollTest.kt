package com.smrh.zujaj

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.ui.Nav
import com.smrh.zujaj.ui.Root
import com.smrh.zujaj.ui.Screen
import com.smrh.zujaj.ui.ZujajTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Scroll the Discover list down, open a widget, press Back: the list must still be where it was. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h860dp-xhdpi")
class BackScrollTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun firstIndex(): Int {
        val node = rule.onNodeWithTag("discover_list").fetchSemanticsNode()
        val pos = node.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value?.invoke() ?: 0f
        return pos.toInt()
    }

    @Test fun backKeepsScroll() {
        Prefs.init(rule.activity)
        val nav = Nav(Screen.Home).also { var t = 0L; it.clock = { t += 1000; t } }
        rule.setContent { ZujajTheme { Root(nav) } }
        rule.waitForIdle()
        rule.onNodeWithTag("discover_list").performScrollToIndex(12)
        rule.waitForIdle()
        val before = firstIndex()
        assertTrue("list should have scrolled", before > 0)
        rule.runOnUiThread { nav.go(Screen.Editor("clock_dots")) }
        rule.waitForIdle()
        rule.runOnUiThread { nav.back() }
        rule.waitForIdle()
        val after = firstIndex()
        assertTrue("scroll was $before before, $after after Back", kotlin.math.abs(after - before) < 4)
    }
}
