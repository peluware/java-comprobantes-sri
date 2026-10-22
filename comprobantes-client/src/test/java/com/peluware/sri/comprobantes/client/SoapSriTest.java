package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.Sri;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoapSriTest {

    private static final List<OperacionSoap> OPERACIONES = List.of(OperacionSoap.RECEPCION, OperacionSoap.AUTORIZACION);

    @Test
    @DisplayName("la operación decide el namespace, el nombre y el parámetro del sobre")
    void laOperacionDecideElSobre() {
        var recepcion = sobre(SoapSri.envolver(OperacionSoap.RECEPCION, "PD94bWw="));
        var autorizacion = sobre(SoapSri.envolver(OperacionSoap.AUTORIZACION, "0123456789"));

        assertAll(
            () -> assertTrue(recepcion.contains("xmlns:ec=\"http://ec.gob.sri.ws.recepcion\""), recepcion),
            () -> assertTrue(recepcion.contains("<ec:validarComprobante"), recepcion),
            () -> assertTrue(recepcion.contains("<xml>PD94bWw=</xml>"), recepcion),
            () -> assertTrue(autorizacion.contains("xmlns:ec=\"http://ec.gob.sri.ws.autorizacion\""), autorizacion),
            () -> assertTrue(autorizacion.contains("<ec:autorizacionComprobante"), autorizacion),
            () -> assertTrue(autorizacion.contains("<claveAccesoComprobante>0123456789</claveAccesoComprobante>"), autorizacion));
    }

    @Test
    @DisplayName("el sobre lleva el namespace de SOAP 1.1 y el cuerpo")
    void elSobreEsSoap11() {
        var sobre = sobre(SoapSri.envolver(OperacionSoap.AUTORIZACION, "0123456789"));

        assertAll(
            () -> assertTrue(sobre.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), sobre),
            () -> assertTrue(sobre.contains("xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""), sobre),
            () -> assertTrue(sobre.contains("<soapenv:Body>"), sobre));
    }

    @Test
    @DisplayName("escapa el contenido del parámetro")
    void escapaElContenido() {
        var sobre = sobre(SoapSri.envolver(OperacionSoap.AUTORIZACION, "a&b<c"));

        assertTrue(sobre.contains("<claveAccesoComprobante>a&amp;b&lt;c</claveAccesoComprobante>"), sobre);
    }

    private static String sobre(byte[] sobre) {
        return new String(sobre, Sri.CODIFICACION);
    }

    @Test
    @DisplayName("el sobre de cada operación cabe en el margen que se le reserva")
    void elSobreCabeEnElMargen() {
        assertAll(OPERACIONES.stream().map(operacion -> () -> {
            int sobre = SoapSri.envolver(operacion, "").length;
            assertTrue(sobre < SoapSri.MARGEN_SOBRE, () -> "el sobre de " + operacion.nombre() + " ocupa " + sobre + " bytes y el margen es " + SoapSri.MARGEN_SOBRE + "; el destino volvería a crecer");
        }));
    }

    @Test
    @DisplayName("cada carácter del payload cuesta un byte, que es lo que supone el margen")
    void cadaCaracterCuestaUnByte() {
        assertAll(OPERACIONES.stream().map(operacion -> () -> {
            int vacio = SoapSri.envolver(operacion, "").length;
            int con100 = SoapSri.envolver(operacion, "A".repeat(100)).length;
            assertEquals(100, con100 - vacio, operacion.nombre());
        }));
    }
}
