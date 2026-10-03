package com.orthodoxprayers.privateapp.data;

import org.json.JSONObject;

/**
 * Resolves the concrete bundled service id for a calendar-appointed Liturgy.
 *
 * The immutable 2026-2050 calendar intentionally stores service_id as JSON null
 * and uses service_type as the authoritative selector. Android's JSONObject can
 * expose JSON null through optString as the literal text "null", so callers must
 * never treat optString(service_id) alone as a valid service id.
 */
public final class AppointedLiturgyResolver {
    private AppointedLiturgyResolver() {}

    public static String serviceId(JSONObject selection) {
        if (selection == null) return "divine_liturgy";

        String explicit = nullableString(selection, "service_id");
        if (!explicit.isEmpty()) return explicit;

        String type = nullableString(selection, "service_type");
        if ("basil".equals(type)) return "divine_liturgy_basil";
        if ("presanctified".equals(type)) return "presanctified_liturgy";
        if ("no_divine_liturgy".equals(type) || "typikon_override_required".equals(type)) {
            return "";
        }
        // Missing/ordinary selection defaults to the normal Chrysostom Liturgy.
        return "divine_liturgy";
    }

    private static String nullableString(JSONObject object, String key) {
        if (object == null || key == null || object.isNull(key)) return "";
        String value = object.optString(key, "").trim();
        return "null".equalsIgnoreCase(value) ? "" : value;
    }
}
