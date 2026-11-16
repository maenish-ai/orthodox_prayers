package com.orthodoxprayers.privateapp.data;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

/** Concatenates the actual tab contents, preserving each office as a distinct phase. */
public final class WorshipSequenceComposer {
    public static final String[] OFFICE_IDS = {"orthros", "first_hour", "third_hour", "sixth_hour",
            "proskomide", "pre_communion_prayers", "appointed_liturgy", "thanksgiving_after_communion"};
    private WorshipSequenceComposer() {}

    public static JSONObject compose(List<JSONObject> offices, String language, String title) throws Exception {
        JSONArray segments = new JSONArray();
        JSONArray included = new JSONArray();
        JSONArray unavailable = new JSONArray();
        for (int index = 0; index < OFFICE_IDS.length; index++) {
            JSONObject office = index < offices.size() ? offices.get(index) : null;
            JSONArray body = office == null ? null : office.optJSONArray("segments");
            if (body == null || body.length() == 0) { unavailable.put(OFFICE_IDS[index]); continue; }
            String sourceLanguage = office.optString("source_language", language);
            if (!language.equals(sourceLanguage)) { unavailable.put(OFFICE_IDS[index]); continue; }
            boolean nativeText = false;
            for (int i = 0; i < body.length(); i++) {
                JSONObject item = body.optJSONObject(i);
                JSONObject text = item == null ? null : item.optJSONObject("text");
                if (text != null && !text.optString(language, "").trim().isEmpty()) { nativeText = true; break; }
            }
            if (!nativeText) { unavailable.put(OFFICE_IDS[index]); continue; }
            segments.put(new JSONObject().put("type", "section")
                    .put("title", office.optJSONObject("title"))
                    .put("worship_office_id", OFFICE_IDS[index]));
            for (int i = 0; i < body.length(); i++) segments.put(new JSONObject(body.getJSONObject(i).toString()));
            included.put(OFFICE_IDS[index]);
        }
        return new JSONObject().put("id", "worship_sequence").put("source_language", language)
                .put("title", new JSONObject().put(language, title)).put("segments", segments)
                .put("included_offices", included).put("unavailable_offices", unavailable)
                .put("adjacent_offices_separate", true).put("full_service_complete", unavailable.length() == 0
                        && offices.size() > 6 && offices.get(6) != null && offices.get(6).optBoolean("full_service_complete", true));
    }
}
