package com.surajdev.spendwise.core.data

import com.surajdev.spendwise.core.data.sync.ConflictResolver
import com.surajdev.spendwise.core.data.sync.VersionStamp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConflictResolverTest {

    @Test
    fun newRecordIsAlwaysApplied() {
        assertTrue(ConflictResolver.shouldApplyRemote(null, VersionStamp(1, false, "a")))
    }

    @Test
    fun newerWriteWins() {
        assertTrue(ConflictResolver.shouldApplyRemote(VersionStamp(10, false, "a"), VersionStamp(11, false, "b")))
        assertFalse(ConflictResolver.shouldApplyRemote(VersionStamp(11, false, "a"), VersionStamp(10, false, "b")))
    }

    @Test
    fun deletionWinsATie() {
        assertTrue(ConflictResolver.shouldApplyRemote(VersionStamp(10, false, "z"), VersionStamp(10, true, "a")))
        assertFalse(ConflictResolver.shouldApplyRemote(VersionStamp(10, true, "a"), VersionStamp(10, false, "z")))
    }

    @Test
    fun deviceIdBreaksRemainingTies() {
        assertTrue(ConflictResolver.shouldApplyRemote(VersionStamp(10, false, "a"), VersionStamp(10, false, "b")))
        assertFalse(ConflictResolver.shouldApplyRemote(VersionStamp(10, false, "b"), VersionStamp(10, false, "a")))
    }

    @Test
    fun ownEchoIsIgnored() {
        val stamp = VersionStamp(10, false, "a")
        assertFalse(ConflictResolver.shouldApplyRemote(stamp, stamp.copy()))
    }

    @Test
    fun twoDevicesAlwaysPickTheSameWinner() {
        val versions = listOf(
            VersionStamp(5, false, "a"), VersionStamp(5, false, "b"), VersionStamp(5, true, "a"),
            VersionStamp(6, false, "a"), VersionStamp(4, true, "c"),
        )
        for (x in versions) for (y in versions) {
            if (x == y) continue
            // Device holding x receives y and vice versa: exactly one of them must switch.
            val xTakesY = ConflictResolver.shouldApplyRemote(x, y)
            val yTakesX = ConflictResolver.shouldApplyRemote(y, x)
            assertTrue(xTakesY != yTakesX, "Diverged on $x vs $y")
        }
    }
}
