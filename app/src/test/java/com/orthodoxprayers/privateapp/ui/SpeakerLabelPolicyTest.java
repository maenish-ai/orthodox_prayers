package com.orthodoxprayers.privateapp.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SpeakerLabelPolicyTest {
    @Test public void removesRepeatedRoleLabelsWithoutRewritingPrayer() {
        assertEquals("لنصغِ.", SpeakerLabelPolicy.stripPrefix("الكاهن: الكاهن： لنصغِ.", "الكاهن"));
        assertEquals("آمين.", SpeakerLabelPolicy.stripPrefix("الشعب\nآمين.", "الشعب"));
    }
    @Test public void preservesRoleWordsInsidePrayerAndDifferentSpeakers() {
        assertEquals("الكاهن يصلي", SpeakerLabelPolicy.stripPrefix("الكاهن يصلي", "الكاهن"));
        assertEquals("الشعب: آمين", SpeakerLabelPolicy.stripPrefix("الشعب: آمين", "الكاهن"));
        assertEquals("يا رب احفظ الشعب", SpeakerLabelPolicy.stripPrefix("يا رب احفظ الشعب", "الشعب"));
    }
}
