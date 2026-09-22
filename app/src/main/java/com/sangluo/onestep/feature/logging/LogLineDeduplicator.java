package com.sangluo.onestep.feature.logging;


/**
 * Folds repetitive logcat lines so an oscillating loop cannot grow the session
 * log into hundreds of megabytes. Two alternating signatures are tracked: a
 * repeated signature is skipped (with a repeat note emitted for the previous
 * slot when it is evicted), while genuinely new lines always pass through.
 * Line format is logcat threadtime; anything unparseable passes through as is.
 */
public final class LogLineDeduplicator {
    private static final int SIGNATURE_FIELDS = 6;

    private final String[] slotSignatures = new String[2];
    private final long[] slotCounts = new long[2];

    /**
     * Feeds one raw logcat line.
     *
     * @return the text to write (the line itself, optionally prefixed with a
     *     repeat note for the evicted slot), or null when the line is a folded
     *     duplicate of a recently seen signature.
     */
    public synchronized String feed(String rawLine) {
        if (rawLine == null || rawLine.isEmpty()) {
            return rawLine;
        }
        String signature = signatureOf(rawLine);
        if (signature == null) {
            return rawLine;
        }
        if (signature.equals(slotSignatures[0])) {
            slotCounts[0]++;
            return null;
        }
        if (signature.equals(slotSignatures[1])) {
            slotCounts[1]++;
            return null;
        }
        StringBuilder output = null;
        if (slotSignatures[0] != null && slotCounts[0] > 1) {
            output = new StringBuilder();
            output.append("[repeated ").append(slotCounts[0]).append(" times: ")
                    .append(slotSignatures[0]).append("]\n");
        }
        slotSignatures[1] = slotSignatures[0];
        slotCounts[1] = slotCounts[0];
        slotSignatures[0] = signature;
        slotCounts[0] = 1;
        if (output == null) {
            return rawLine;
        }
        return output.append(rawLine).toString();
    }

    /** Repeat note for the still-tracked newest slot, used when the stream ends. */
    public synchronized String drainNote() {
        if (slotSignatures[0] != null && slotCounts[0] > 1) {
            return "[repeated " + slotCounts[0] + " times: " + slotSignatures[0] + "]";
        }
        return null;
    }

    /**
     * Extracts the repetition signature of a threadtime line
     * ("date time pid tid level tag: message" -> "level tag: message").
     * Returns null for blank or non-standard lines so they pass through untouched.
     */
    static String signatureOf(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        String[] fields = line.trim().split("\\s+", SIGNATURE_FIELDS);
        if (fields.length < SIGNATURE_FIELDS) {
            return null;
        }
        return fields[4] + " " + fields[5];
    }
}
