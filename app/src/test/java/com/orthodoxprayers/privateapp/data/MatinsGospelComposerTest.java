package com.orthodoxprayers.privateapp.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class MatinsGospelComposerTest {
    private JSONObject office() throws Exception {
        return new JSONObject().put("id", "orthros").put("segments", new JSONArray()
                .put(new JSONObject().put("type", "section").put("title", new JSONObject().put("ar", "المزمور الخمسون")))
                .put(new JSONObject().put("type", "text").put("text", new JSONObject().put("ar", "ارحمني يا الله"))));
    }
    private JSONArray reading() throws Exception {
        return new JSONArray().put(new JSONObject().put("kind", "gospel")
                .put("body", new JSONObject().put("ar", "إنجيل القداس")))
                .put(new JSONObject().put("kind", "matins_gospel")
                .put("body", new JSONObject().put("ar", "نص إنجيل السحر الموثّق"))
                .put("reference", new JSONObject().put("ar", "مرقس")));
    }
    @Test public void insertsMatinsOnlyBeforePsalmFiftyAndNeverDuplicatesIt() throws Exception {
        JSONObject input = office();
        JSONObject output = MatinsGospelComposer.compose(input, reading(), "ar");
        assertTrue(output.getBoolean("matins_gospel_available"));
        assertEquals(2, input.getJSONArray("segments").length());
        JSONArray segments = output.getJSONArray("segments");
        assertEquals(3, segments.length());
        assertEquals("matins_gospel", segments.getJSONObject(0).getString("resolved_dynamic_slot"));
        assertFalse(segments.getJSONObject(0).getJSONObject("text").getString("ar").contains("إنجيل القداس"));
        assertEquals(output.toString(), MatinsGospelComposer.compose(output, reading(), "ar").toString());
    }
    @Test public void missingLanguageOrReadingDoesNotInventAGospel() throws Exception {
        assertFalse(MatinsGospelComposer.compose(office(), null, "ar").getBoolean("matins_gospel_available"));
        assertFalse(MatinsGospelComposer.compose(office(), reading(), "en").getBoolean("matins_gospel_available"));
    }
    @Test public void greekSourceExampleDoesNotMislabelAnotherEvangelist() throws Exception {
        JSONObject service = new JSONObject().put("segments", new JSONArray()
                .put(new JSONObject().put("text", new JSONObject().put("el", "Ιερεύς Εκ τού κατά Ιωάννου αγίου Ευαγγελίου...")))
                .put(new JSONObject().put("text", new JSONObject().put("el", "ΕΥΑΓΓΕΛΙΟΝ"))));
        JSONArray readings = new JSONArray().put(new JSONObject().put("kind", "matins_gospel")
                .put("reference", new JSONObject().put("el", "Κατὰ Μᾶρκον 16:9-20"))
                .put("body", new JSONObject().put("el", "Native source text")));
        JSONObject output = MatinsGospelComposer.compose(service, readings, "el");
        assertTrue(output.getBoolean("matins_gospel_available"));
        assertFalse(output.toString().contains("Ιωάννου"));
        assertTrue(output.toString().contains("Κατὰ Μᾶρκον"));
        assertTrue(service.toString().contains("Ιωάννου"));
    }
}
