package com.abhi.musicapp

import com.abhi.musicapp.library.LibraryRepository.ScanStatus.*
import com.abhi.musicapp.library.LibraryRepository.scanSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanSummaryTest {
    @Test fun noPermission() = assertEquals("Music permission is off, can't scan", scanSummary(NO_PERMISSION, emptySet(), emptySet()))
    @Test fun nothingNew() = assertEquals("No new songs found", scanSummary(COMPLETE, setOf(1, 2), setOf(1, 2)))
    @Test fun oneAdded() = assertEquals("1 song added", scanSummary(COMPLETE, setOf(1), setOf(1, 2)))
    @Test fun manyAdded() = assertEquals("3 songs added", scanSummary(COMPLETE, setOf(1), setOf(1, 2, 3, 4)))
    @Test fun removedOnly() = assertEquals("1 song no longer found", scanSummary(COMPLETE, setOf(1, 2), setOf(1)))
    @Test fun both() = assertEquals("2 songs added, 1 no longer found", scanSummary(COMPLETE, setOf(1, 2), setOf(1, 3, 4)))
    @Test fun emptyLibraryStaysEmpty() = assertEquals("No new songs found", scanSummary(COMPLETE, emptySet(), emptySet()))
    @Test fun failuresAreNeverReportedAsNone() {
        for (st in listOf(FOLDERS_UNREADABLE, SCANNER_FAILED, TIMED_OUT)) {
            val m = scanSummary(st, setOf(1), setOf(1))
            org.junit.Assert.assertFalse(m, m.contains("No new songs"))
        }
        assertEquals("Scan didn't finish. Showing what Android has indexed so far", scanSummary(TIMED_OUT, setOf(1), setOf(1)))
    }
}
