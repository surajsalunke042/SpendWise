package com.surajdev.spendwise.feature

import com.surajdev.spendwise.core.designsystem.component.changeText
import kotlin.test.Test
import kotlin.test.assertEquals

class ChangeTextTest {
    @Test
    fun smallChangesUsePercent() {
        assertEquals("12% more than yesterday", changeText(0.12, 12, "yesterday"))
        assertEquals("40% less than yesterday", changeText(-0.4, 40, "yesterday"))
        assertEquals("Same as yesterday", changeText(0.001, 0, "yesterday"))
        assertEquals("150% more than yesterday", changeText(1.5, 150, "yesterday"))
    }

    @Test
    fun largeJumpsUseMultiples() {
        assertEquals("3× your average", changeText(2.0, 200, "your average"))
        assertEquals("3.4× your average", changeText(2.4, 240, "your average"))
        assertEquals("18× your average", changeText(16.48, 1648, "your average"))
    }
}
