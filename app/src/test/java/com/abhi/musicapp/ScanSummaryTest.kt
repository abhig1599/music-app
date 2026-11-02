package com.abhi.musicapp

import com.abhi.musicapp.library.LibraryRepository.scanSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanSummaryTest {
    @Test fun noPermission() = assertEquals("Music permission is off, can't scan", scanSummary(false, emptySet(), emptySet()))
    @Test fun nothingNew() = assertEquals("No new songs found", scanSummary(true, setOf(1, 2), setOf(1, 2)))
    @Test fun oneAdded() = assertEquals("1 song added", scanSummary(true, setOf(1), setOf(1, 2)))
    @Test fun manyAdded() = assertEquals("3 songs added", scanSummary(true, setOf(1), setOf(1, 2, 3, 4)))
    @Test fun removedOnly() = assertEquals("1 song no longer found", scanSummary(true, setOf(1, 2), setOf(1)))
    @Test fun both() = assertEquals("2 songs added, 1 no longer found", scanSummary(true, setOf(1, 2), setOf(1, 3, 4)))
    @Test fun emptyLibraryStaysEmpty() = assertEquals("No new songs found", scanSummary(true, emptySet(), emptySet()))
}
