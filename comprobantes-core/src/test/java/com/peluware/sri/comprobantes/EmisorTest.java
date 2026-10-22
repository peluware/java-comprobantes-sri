package com.peluware.sri.comprobantes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmisorTest {

    @Test
    @DisplayName("of deja ausentes los campos opcionales")
    void ofDejaOpcionalesAusentes() {
        var emisor = DatosDePrueba.emisor();

        assertEquals(DatosDePrueba.RUC, emisor.ruc());
        assertNull(emisor.nombreComercial());
        assertNull(emisor.agenteRetencion());
        assertFalse(emisor.contribuyenteRimpe());
    }

    @ParameterizedTest(name = "[{index}]")
    @DisplayName("rechaza razones sociales que el XSD no admite")
    @ValueSource(strings = {"", "DISTRIBUIDORA\nFICTICIA S.A."})
    void rechazaRazonSocialInvalida(String razonSocial) {
        assertEquals("razonSocial", campoInvalido(() -> Emisor.of(DatosDePrueba.RUC, razonSocial, DatosDePrueba.DIRECCION_MATRIZ)));
    }

    @Test
    @DisplayName("rechaza una razón social de más de 300 caracteres")
    void rechazaRazonSocialDemasiadoLarga() {
        var larga = "X".repeat(Emisor.RAZON_SOCIAL_LONGITUD_MAXIMA + 1);

        assertEquals("razonSocial", campoInvalido(() -> Emisor.of(DatosDePrueba.RUC, larga, DatosDePrueba.DIRECCION_MATRIZ)));
    }

    @Test
    @DisplayName("la dirección de la matriz es obligatoria")
    void dirMatrizObligatoria() {
        assertEquals(Emisor.CAMPO_DIRECCION_MATRIZ, campoInvalido(() -> Emisor.of(DatosDePrueba.RUC, DatosDePrueba.RAZON_SOCIAL, null)));
    }

    @Test
    @DisplayName("el agente de retención admite hasta ocho dígitos")
    void agenteRetencionHastaOchoDigitos() {
        assertEquals("12345678", conAgenteRetencion("12345678").agenteRetencion());

        assertEquals("agenteRetencion", campoInvalido(() -> conAgenteRetencion("123456789")));
    }

    private static Emisor conAgenteRetencion(String resolucion) {
        return Emisor.builder(DatosDePrueba.RUC, DatosDePrueba.RAZON_SOCIAL, DatosDePrueba.DIRECCION_MATRIZ).agenteRetencion(resolucion).build();
    }

    private static String campoInvalido(Executable accion) {
        var e = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo();
    }
}
