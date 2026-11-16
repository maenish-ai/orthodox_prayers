package com.orthodoxprayers.privateapp.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class LiturgyReadingOrderTest {
    private JSONArray source() throws Exception {
        Path path = Path.of("src/main/assets/data/native/services/ar/divine_liturgy.json");
        if (!Files.exists(path)) path = Path.of("app").resolve(path);
        return new JSONObject(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)).getJSONArray("segments");
    }
    private int slot(JSONArray array, String slot) throws Exception {
        for (int i = 0; i < array.length(); i++)
            if (slot.equals(array.getJSONObject(i).optString("dynamic_slot"))) return i;
        return -1;
    }
    private String text(JSONArray array, int i) throws Exception {
        return array.getJSONObject(i).optJSONObject("text").optString("ar");
    }

    @Test public void appointedReadingsFollowTheirIntroductions() throws Exception {
        JSONArray output = LiturgyReadingOrder.prepare(source(), "divine_liturgy", "ar");
        int prokeimenon = slot(output, "prokeimenon");
        int epistle = slot(output, "epistle");
        int alleluia = slot(output, "alleluia_verses");
        int gospel = slot(output, "gospel");
        assertTrue(text(output, prokeimenon - 1).contains("يقول بروكيمنن الرسالة"));
        assertTrue(text(output, epistle - 1).contains("لنصغِ"));
        assertTrue(text(output, epistle + 1).startsWith("أثناء قراءة الرسالة"));
        assertTrue(prokeimenon < epistle && epistle < alleluia && alleluia < gospel);
        assertTrue(text(output, gospel - 2).contains("لنصغِ"));
        assertTrue(text(output, gospel - 1).contains("ثم يتلو فصل الإنجيل"));
        assertEquals(-1, slot(output, "matins_gospel"));
    }

    @Test public void preparationIsIdempotentAndLeavesSourceUntouched() throws Exception {
        JSONArray input = source();
        String before = input.toString();
        JSONArray output = LiturgyReadingOrder.prepare(input, "divine_liturgy", "ar");
        assertEquals(before, input.toString());
        assertEquals(output.toString(), LiturgyReadingOrder.prepare(output, "divine_liturgy", "ar").toString());
    }

    @Test public void unrelatedOfficesAndLanguagesKeepTheirText() throws Exception {
        JSONArray input = source();
        assertEquals(input.toString(), LiturgyReadingOrder.prepare(input, "orthros", "ar").toString());
        JSONArray en = new JSONArray().put(new JSONObject().put("type", "text")
                .put("text", new JSONObject().put("en", "Blessed is the Kingdom")));
        assertEquals(en.toString(), LiturgyReadingOrder.prepare(en, "divine_liturgy", "en").toString());
    }
}
