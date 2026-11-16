package com.orthodoxprayers.privateapp.ui;

/** Removes redundant role labels only at the start; prayer wording is untouched. */
public final class SpeakerLabelPolicy {
    private SpeakerLabelPolicy() {}

    public static String stripPrefix(String text, String speaker) {
        String value = text == null ? "" : text.trim();
        if (speaker == null || speaker.trim().isEmpty()) return value;
        String label = speaker.trim();
        while (value.startsWith(label)) {
            String remainder = value.substring(label.length());
            String trimmed = remainder.trim();
            if (trimmed.startsWith(":") || trimmed.startsWith("：")) {
                value = trimmed.substring(1).trim();
            } else if (remainder.startsWith("\n") || remainder.startsWith("\r")) {
                value = trimmed;
            } else {
                break;
            }
        }
        return value;
    }
}
