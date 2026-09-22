package com.sangluo.onestep.feature.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Validates the repetition folding that keeps session logs within a sane size. */
public class LogLineDeduplicatorTest {
    private static final String LINE_A =
            "09-22 09:13:30.001   4092  5301 E DisplayModeDirectorImpl: Invalid displayId: 8";
    private static final String LINE_A_LATER =
            "09-22 09:13:30.500   4092  5301 E DisplayModeDirectorImpl: Invalid displayId: 8";
    private static final String LINE_B =
            "09-22 09:13:30.002   4092  5302 W BpBinder: Large outgoing transaction of 21124 bytes";

    @Test
    public void firstLine_passesThrough() {
        LogLineDeduplicator deduplicator = new LogLineDeduplicator();
        assertEquals(LINE_A, deduplicator.feed(LINE_A));
    }

    @Test
    public void consecutiveDuplicates_folded() {
        LogLineDeduplicator deduplicator = new LogLineDeduplicator();
        assertEquals(LINE_A, deduplicator.feed(LINE_A));
        assertNull(deduplicator.feed(LINE_A_LATER));
        assertNull(deduplicator.feed(LINE_A_LATER));
    }

    @Test
    public void evictedSlot_emitsRepeatNote() {
        LogLineDeduplicator deduplicator = new LogLineDeduplicator();
        deduplicator.feed(LINE_A);
        deduplicator.feed(LINE_A_LATER);
        deduplicator.feed(LINE_A_LATER);
        deduplicator.feed(LINE_A_LATER);
        String output = deduplicator.feed(LINE_B);
        assertTrue(output, output.startsWith("[repeated 4 times:"));
        assertTrue(output, output.contains(LINE_B));
    }

    @Test
    public void alternatingLines_bothFolded() {
        LogLineDeduplicator deduplicator = new LogLineDeduplicator();
        assertEquals(LINE_A, deduplicator.feed(LINE_A));
        assertEquals(LINE_B, deduplicator.feed(LINE_B));
        assertNull(deduplicator.feed(LINE_A_LATER));
        assertNull(deduplicator.feed(LINE_B));
        assertNull(deduplicator.feed(LINE_A_LATER));
        assertNull(deduplicator.feed(LINE_B));
    }

    @Test
    public void nonStandardLine_passesThroughUnfolded() {
        LogLineDeduplicator deduplicator = new LogLineDeduplicator();
        String stackLine = "        at java.io.FileOutputStream.<init>(FileOutputStream.java:5)";
        assertEquals(stackLine, deduplicator.feed(stackLine));
        assertEquals(stackLine, deduplicator.feed(stackLine));
    }

    @Test
    public void nullAndEmpty_passThrough() {
        LogLineDeduplicator deduplicator = new LogLineDeduplicator();
        assertNull(deduplicator.feed(null));
        assertEquals("", deduplicator.feed(""));
    }
}
