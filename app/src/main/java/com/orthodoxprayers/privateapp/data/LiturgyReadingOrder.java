package com.orthodoxprayers.privateapp.data;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Repairs known import-slot placement errors without composing new prayer text. */
public final class LiturgyReadingOrder {
    private LiturgyReadingOrder() {}

    public static JSONArray prepare(JSONArray input, String serviceId, String language) throws Exception {
        JSONArray output = new JSONArray();
        if (input == null) return output;
        if (!serviceId.contains("liturgy")) return new JSONArray(input.toString());
        for (int i = 0; i < input.length(); i++) {
            JSONObject segment = input.optJSONObject(i);
            if (segment == null) continue;
            JSONObject copy = new JSONObject(segment.toString());
            String slot = copy.optString("dynamic_slot", "");
            String phase = copy.optString("follow_along_phase", "");
            // Matins has its own tab; its Gospel is never part of the Liturgy core.
            if ("matins_gospel".equals(slot) || "matins_gospel".equals(phase)) continue;
            String title = local(copy, "title", language);
            if ("section".equals(copy.optString("type")) &&
                    (title.contains("إنجيل السَحَر") || title.contains("إنجيل السحر")
                    || title.toUpperCase(Locale.ROOT).contains("MATINS GOSPEL") || title.contains("Ἑωθινὸν Εὐαγγέλιον"))) continue;
            output.put(copy);
        }
        if (!"ar".equals(language) || !"divine_liturgy".equals(serviceId)) return output;

        moveSlotAfter(output, "prokeimenon", "القارئ: يقول بروكيمنن الرسالة.");
        moveSlotBefore(output, "epistle", "أثناء قراءة الرسالة");
        moveSlotAfter(output, "gospel", "… ثم يتلو فصل الإنجيل");
        // The importer injected another Trisagion at the Epistle heading, after
        // the source has already ended that hymn. Remove only that exact block.
        for (int i = 0; i + 3 < output.length(); i++) {
            if ("القراءات: الرسالة".equals(local(output.getJSONObject(i), "title", "ar"))
                    && "قدوس الله، قدوس القوي، قدوس الذي لا يموت، ارحمنا.".equals(local(output.getJSONObject(i + 1), "text", "ar"))
                    && "المجد للآب والابن والروح القدس.".equals(local(output.getJSONObject(i + 2), "text", "ar"))
                    && "قدوس الذي لا يموت، ارحمنا.".equals(local(output.getJSONObject(i + 3), "text", "ar"))) {
                output.remove(i + 1);
                output.remove(i + 1);
                output.remove(i + 1);
                break;
            }
        }
        // The source already contains the post-Epistle Alleluia. Bind that response
        // to the proper instead of singing an extra Alleluia before the Epistle.
        int alleluia = slotIndex(output, "alleluia_verses");
        int response = textIndex(output, "الشعب: هلليلويا");
        if (alleluia >= 0 && response > alleluia) {
            JSONObject original = output.getJSONObject(response);
            original.put("dynamic_slot", "alleluia_verses");
            original.put("dynamic_slot_mode", "replace_if_present");
            output.remove(alleluia);
        }
        // Keep the variable evangelist announcement, remove its imported duplicate.
        boolean hasAnnouncement = false;
        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.getJSONObject(i);
            if ("gospel_evangelist_name".equals(item.optString("dynamic_inline_slot"))) hasAnnouncement = true;
        }
        if (hasAnnouncement) {
            for (int i = output.length() - 1; i >= 0; i--) {
                if (local(output.getJSONObject(i), "text", "ar").startsWith("الكاهن: فصلٌ شريفٌ من بشارة القديس ( فلان")) output.remove(i);
            }
        }
        return output;
    }

    private static void moveSlotBefore(JSONArray array, String slot, String prefix) throws Exception {
        int from = slotIndex(array, slot);
        int anchor = textIndex(array, prefix);
        if (from < 0 || anchor < 0 || from == anchor - 1) return;
        JSONObject item = array.getJSONObject(from);
        array.remove(from);
        anchor = textIndex(array, prefix);
        // Move rather than duplicate: preserve all surrounding source paragraphs.
        array.put(item);
        for (int i = array.length() - 1; i > anchor; i--) array.put(i, array.get(i - 1));
        array.put(anchor, item);
    }

    private static void moveSlotAfter(JSONArray array, String slot, String prefix) throws Exception {
        int from = slotIndex(array, slot);
        int anchor = textIndex(array, prefix);
        if (from < 0 || anchor < 0 || from == anchor + 1) return;
        JSONObject item = array.getJSONObject(from);
        array.remove(from);
        int target = textIndex(array, prefix) + 1;
        array.put(item);
        for (int i = array.length() - 1; i > target; i--) array.put(i, array.get(i - 1));
        array.put(target, item);
    }

    private static int slotIndex(JSONArray array, String slot) {
        for (int i = 0; i < array.length(); i++)
            if (slot.equals(array.optJSONObject(i).optString("dynamic_slot", ""))) return i;
        return -1;
    }
    private static int textIndex(JSONArray array, String prefix) {
        for (int i = 0; i < array.length(); i++)
            if (local(array.optJSONObject(i), "text", "ar").startsWith(prefix)) return i;
        return -1;
    }
    private static String local(JSONObject object, String key, String language) {
        JSONObject value = object == null ? null : object.optJSONObject(key);
        return value == null ? "" : value.optString(language, "").trim();
    }
}
