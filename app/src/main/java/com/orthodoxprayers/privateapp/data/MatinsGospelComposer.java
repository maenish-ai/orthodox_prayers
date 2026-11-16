package com.orthodoxprayers.privateapp.data;

import org.json.JSONArray;
import org.json.JSONObject;

/** Inserts only a calendar-appointed native Gospel at a known Matins source anchor. */
public final class MatinsGospelComposer {
    private MatinsGospelComposer() {}

    public static JSONObject compose(JSONObject service, JSONArray readings, String language) {
        if (service == null) return null;
        try {
            JSONObject result = new JSONObject(service.toString());
            JSONArray segments = result.optJSONArray("segments");
            if (segments == null) return result;
            JSONObject reading = null;
            if (readings != null) for (int i = 0; i < readings.length(); i++) {
                JSONObject item = readings.optJSONObject(i);
                if (item != null && "matins_gospel".equals(item.optString("kind", ""))) {
                    reading = item;
                    break;
                }
            }
            String body = local(reading, "body", language);
            int anchor = -1;
            for (int i = 0; i < segments.length(); i++) {
                JSONObject item = segments.optJSONObject(i);
                if (item == null) continue;
                if ("matins_gospel".equals(item.optString("resolved_dynamic_slot", ""))) return result;
                String title = local(item, "title", language);
                String text = local(item, "text", language);
                if ("ar".equals(language) && "المزمور الخمسون".equals(title)) { anchor = i; break; }
                if ("en".equals(language) && text.startsWith("And the priest reads the appointed Gospel.")) { anchor = i; break; }
                if ("el".equals(language) && "ΕΥΑΓΓΕΛΙΟΝ".equals(text)) { anchor = i + 1; break; }
            }
            result.put("matins_gospel_available", !body.isEmpty() && anchor >= 0);
            if (body.isEmpty() || anchor < 0) return result;
            JSONObject text = new JSONObject().put(language,
                    local(reading, "reference", language) + "\n\n" + body);
            JSONObject inserted = new JSONObject().put("type", "text")
                    .put("resolved_dynamic_slot", "matins_gospel")
                    .put("speaker", new JSONObject().put(language,
                            "ar".equals(language) ? "الكاهن" : "el".equals(language) ? "Ἱερεύς" : "Priest"))
                    .put("text", text);
            JSONArray output = new JSONArray();
            for (int i = 0; i <= segments.length(); i++) {
                if (i == anchor) output.put(inserted);
                if (i < segments.length()) {
                    String original = local(segments.optJSONObject(i), "text", language);
                    // These are source-edition example announcements, not the
                    // appointed reading. Its verified native reference is printed
                    // with the inserted Gospel; never leave a conflicting author.
                    if ("en".equals(language) && original.equals("The reading from the holy Gospel according to _____.")) continue;
                    if ("el".equals(language) && original.equals("Ιερεύς Εκ τού κατά Ιωάννου αγίου Ευαγγελίου...")) continue;
                    output.put(segments.get(i));
                }
            }
            result.put("segments", output);
            return result;
        } catch (Exception error) {
            return service;
        }
    }

    private static String local(JSONObject object, String key, String language) {
        JSONObject value = object == null ? null : object.optJSONObject(key);
        return value == null ? "" : value.optString(language, "").trim();
    }
}
