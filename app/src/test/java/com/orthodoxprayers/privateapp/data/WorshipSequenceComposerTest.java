package com.orthodoxprayers.privateapp.data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class WorshipSequenceComposerTest {
    private List<JSONObject> offices() throws Exception {
        List<JSONObject> result = new ArrayList<>();
        for (String id : WorshipSequenceComposer.OFFICE_IDS)
            result.add(new JSONObject().put("id", id).put("source_language", "ar")
                    .put("title", new JSONObject().put("ar", id))
                    .put("segments", new JSONArray().put(new JSONObject().put("type", "text")
                    .put("text", new JSONObject().put("ar", "source:" + id)))));
        return result;
    }
    @Test public void allUsesTheExactTabSequenceWithoutChangingSource() throws Exception {
        List<JSONObject> input = offices(); String first = input.get(0).toString();
        JSONObject result = WorshipSequenceComposer.compose(input, "ar", "الكل");
        assertEquals(8, result.getJSONArray("included_offices").length());
        assertEquals(16, result.getJSONArray("segments").length());
        assertTrue(result.getBoolean("full_service_complete"));
        for (int i = 0; i < 8; i++)
            assertEquals(WorshipSequenceComposer.OFFICE_IDS[i], result.getJSONArray("segments").getJSONObject(i * 2).getString("worship_office_id"));
        assertEquals(first, input.get(0).toString());
    }
    @Test public void missingOrOtherLanguageIsExplicitlyPartial() throws Exception {
        List<JSONObject> input = offices(); input.set(0, null); input.get(1).put("source_language", "en");
        JSONObject result = WorshipSequenceComposer.compose(input, "ar", "الكل");
        assertEquals(2, result.getJSONArray("unavailable_offices").length());
        assertFalse(result.getBoolean("full_service_complete"));
    }
    @Test public void blockedCoreIsNotUpgradedToComplete() throws Exception {
        List<JSONObject> input = offices(); input.get(6).put("full_service_complete", false);
        assertFalse(WorshipSequenceComposer.compose(input, "ar", "الكل").getBoolean("full_service_complete"));
    }
}
