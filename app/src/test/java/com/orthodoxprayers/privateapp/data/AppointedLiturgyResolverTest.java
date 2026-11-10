package com.orthodoxprayers.privateapp.data;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;

public final class AppointedLiturgyResolverTest {
    @Test
    public void jsonNullServiceIdUsesChrysostomType() throws Exception {
        JSONObject selection = new JSONObject()
                .put("service_id", JSONObject.NULL)
                .put("service_type", "chrysostom");
        assertEquals("divine_liturgy", AppointedLiturgyResolver.serviceId(selection));
    }

    @Test
    public void jsonNullServiceIdUsesBasilType() throws Exception {
        JSONObject selection = new JSONObject()
                .put("service_id", JSONObject.NULL)
                .put("service_type", "basil");
        assertEquals("divine_liturgy_basil", AppointedLiturgyResolver.serviceId(selection));
    }

    @Test
    public void jsonNullServiceIdUsesPresanctifiedType() throws Exception {
        JSONObject selection = new JSONObject()
                .put("service_id", JSONObject.NULL)
                .put("service_type", "presanctified");
        assertEquals("presanctified_liturgy", AppointedLiturgyResolver.serviceId(selection));
    }

    @Test
    public void blockedDaysDoNotResolveToAService() throws Exception {
        assertEquals("", AppointedLiturgyResolver.serviceId(new JSONObject()
                .put("service_id", JSONObject.NULL)
                .put("service_type", "no_divine_liturgy")));
        assertEquals("", AppointedLiturgyResolver.serviceId(new JSONObject()
                .put("service_id", JSONObject.NULL)
                .put("service_type", "typikon_override_required")));
    }

    @Test
    public void explicitServiceIdStillWins() throws Exception {
        JSONObject selection = new JSONObject()
                .put("service_id", "custom_liturgy")
                .put("service_type", "chrysostom");
        assertEquals("custom_liturgy", AppointedLiturgyResolver.serviceId(selection));
    }
}
