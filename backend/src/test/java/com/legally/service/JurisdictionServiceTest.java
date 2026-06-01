package com.legally.service;

import com.legally.model.JurisdictionContext;
import com.legally.model.dto.ConsultRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JurisdictionServiceTest {

    private final JurisdictionService service = new JurisdictionService();

    @Test
    void extractFromUserMessage_parsesAbiaStatePhrase() {
        var ctx = service.extractFromUserMessage(
                        "I want to buy a plot of land in abia state. What documents do I need?")
                .orElseThrow();

        assertEquals("NG", ctx.getCountryCode());
        assertEquals("ABIA", ctx.getRegionCode());
        assertEquals("Abia State", ctx.getRegionName());
    }

    @Test
    void resolve_prefersMessageOverDeviceLocation() {
        ConsultRequest request = new ConsultRequest();
        request.setMessage("I want to buy land in Abia State.");
        request.setCountryCode("NG");
        request.setCountryName("Nigeria");
        request.setRegionCode("KWARA");
        request.setRegionName("Kwara State");
        request.setLocationSource("device");

        JurisdictionContext ctx = service.resolve(request);

        assertEquals(JurisdictionContext.LocationSource.input_override, ctx.getLocationSource());
        assertEquals("ABIA", ctx.getRegionCode());
    }

    @Test
    void extractFromUserMessage_parsesAccraGhana() {
        var ctx = service.extractFromUserMessage(
                        "I want to buy a plot of land in accra, ghana, what documents should I obtain?")
                .orElseThrow();

        assertEquals("GH", ctx.getCountryCode());
        assertEquals("Ghana", ctx.getCountryName());
        assertEquals("Accra", ctx.getRegionName());
        assertEquals("Ghana, Accra", ctx.displayLabel());
    }

    @Test
    void displayableRegionName_omitsGeneral() {
        JurisdictionContext ctx = new JurisdictionContext();
        ctx.setCountryCode("GH");
        ctx.setCountryName("Ghana");
        ctx.setRegionCode("GENERAL");
        ctx.setRegionName("General");

        assertEquals("Ghana", ctx.displayLabel());
        assertEquals(null, ctx.displayableRegionName());
    }

    @Test
    void extractFromUserMessage_parsesTexas() {
        var ctx = service.extractFromUserMessage(
                        "in texas, A parent were living separately without divorce legalized...")
                .orElseThrow();

        assertEquals("US", ctx.getCountryCode());
        assertEquals("TX", ctx.getRegionCode());
        assertEquals("Texas", ctx.getRegionName());
    }

    @Test
    void resolve_texasInMessageOverridesNigeriaDevice() {
        ConsultRequest request = new ConsultRequest();
        request.setMessage(
                "in texas, A parent were living separately without their divorce legalized but they agreed to divorce");
        request.setCountryCode("NG");
        request.setCountryName("Nigeria");
        request.setRegionCode("KWARA");
        request.setRegionName("Kwara State");
        request.setLocationSource("device");

        JurisdictionContext ctx = service.resolve(request);

        assertEquals(JurisdictionContext.LocationSource.input_override, ctx.getLocationSource());
        assertEquals("US", ctx.getCountryCode());
        assertEquals("TX", ctx.getRegionCode());
    }

    @Test
    void isExplicitUserJurisdiction_detectsCrossStateOverride() {
        JurisdictionContext device = new JurisdictionContext();
        device.setCountryCode("NG");
        device.setRegionCode("KWARA");
        device.setLocationSource(JurisdictionContext.LocationSource.device);

        JurisdictionContext abia = new JurisdictionContext();
        abia.setCountryCode("NG");
        abia.setRegionCode("ABIA");

        assertTrue(service.isExplicitUserJurisdiction(abia, device));
    }
}
