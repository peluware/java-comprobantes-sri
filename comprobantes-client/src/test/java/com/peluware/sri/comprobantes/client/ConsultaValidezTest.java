package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consulta de validez sobre las respuestas de ejemplo del numeral 8.2 de la Ficha Técnica.
 */
class ConsultaValidezTest {

    private static final String CLAVE = "0510202601000000000000110010010000000010000000114";

    @Test
    @DisplayName("la consulta va al endpoint de ConsultaComprobante con la clave sin cualificar")
    void peticion() {
        var http = new HttpFalso(200, recurso("validez-autorizado.xml"));

        new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5)).consultarValidez(CLAVE);

        var peticion = http.peticion;
        assertNotNull(peticion);
        var cuerpo = http.cuerpo();
        assertAll(
            () -> assertEquals(SriEndpoints.PRUEBAS.consultaValidez(), peticion.uri()),
            () -> assertTrue(SriEndpoints.PRUEBAS.consultaValidez().toString().endsWith("/ConsultaComprobante"), SriEndpoints.PRUEBAS.consultaValidez().toString()),
            () -> assertTrue(cuerpo.contains("http://ec.gob.sri.ws.consultas"), cuerpo),
            () -> assertTrue(cuerpo.contains("consultarEstadoAutorizacionComprobante"), cuerpo),
            () -> assertTrue(cuerpo.contains("<claveAcceso>" + CLAVE + "</claveAcceso>"), cuerpo)
        );
    }

    @Test
    @DisplayName("un comprobante autorizado tiene validez y trae su tipo y su emisor")
    void autorizado() {
        var validez = consultar("validez-autorizado.xml");

        var autorizado = assertInstanceOf(RespuestaValidez.Autorizado.class, validez);
        assertAll(
            () -> assertTrue(validez.tieneValidez()),
            () -> assertEquals("2111202405176001321000110010010000001241234567810", autorizado.comprobante().claveAcceso()),
            () -> assertEquals("Nota de Débito", autorizado.comprobante().tipoComprobante()),
            () -> assertEquals("1760013210001", autorizado.comprobante().rucEmisor()),
            () -> assertNotNull(autorizado.comprobante().fechaAutorizacion())
        );
    }

    @Test
    @DisplayName("un comprobante anulado ya no tiene validez")
    void anulado() {
        var validez = consultar("validez-anulado.xml");

        assertInstanceOf(RespuestaValidez.Anulado.class, validez);
        assertFalse(validez.tieneValidez());
    }

    @Test
    @DisplayName("un comprobante pendiente de anular conserva su validez, artículo 4 de la resolución")
    void pendienteDeAnular() {
        var validez = consultar("validez-pendiente-de-anular.xml");

        assertInstanceOf(RespuestaValidez.PendienteDeAnular.class, validez);
        assertTrue(validez.tieneValidez(), "mientras el receptor no acepte, el comprobante mantiene su validez");
    }

    @Test
    @DisplayName("una consulta rechazada no es un comprobante sin validez: es una consulta que no se resolvió")
    void rechazada() {
        var validez = consultar("validez-rechazada.xml");

        var noConsultable = assertInstanceOf(RespuestaValidez.NoConsultable.class, validez);
        assertAll(
            () -> assertFalse(validez.tieneValidez()),
            () -> assertEquals("1510202407099313057500110010010000103591234567816", noConsultable.claveAcceso()),
            () -> assertEquals("99", noConsultable.mensajes().getFirst().identificador()),
            () -> assertTrue(noConsultable.mensajes().getFirst().esError())
        );
    }

    @Test
    @DisplayName("el RECHAZADA que la ficha describe en estadoAutorizacion se lee igual que el de estadoConsulta")
    void rechazadaEnLaOtraEtiqueta() {
        var sobre = new String(recurso("validez-rechazada.xml"), StandardCharsets.UTF_8)
            .replace("estadoConsulta", "estadoAutorizacion");

        var validez = consultarSobre(sobre.getBytes(StandardCharsets.UTF_8));

        assertInstanceOf(RespuestaValidez.NoConsultable.class, validez);
    }

    @Test
    @DisplayName("un estado que la librería no conoce se devuelve tal cual")
    void desconocido() {
        var sobre = new String(recurso("validez-autorizado.xml"), StandardCharsets.UTF_8)
            .replace("AUTORIZADO", "EN REVISION MANUAL");

        var desconocida = assertInstanceOf(RespuestaValidez.Desconocida.class, consultarSobre(sobre.getBytes(StandardCharsets.UTF_8)));
        assertEquals("EN REVISION MANUAL", desconocida.estado());
    }

    @Test
    @DisplayName("una clave con el dígito verificador mal no sale a la red")
    void claveInvalida() {
        var http = new HttpFalso(200, recurso("validez-autorizado.xml"));
        var cliente = new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5));

        assertThrows(ValidacionException.class, () -> cliente.consultarValidez(CLAVE.substring(0, 48) + "0"));
        assertNull(http.peticion, "no debe haberse hecho ninguna petición");
    }

    private static RespuestaValidez consultar(String recurso) {
        return consultarSobre(recurso(recurso));
    }

    private static RespuestaValidez consultarSobre(byte[] sobre) {
        var http = new HttpFalso(200, sobre);
        return new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5)).consultarValidez(CLAVE);
    }

    private static byte[] recurso(String nombre) {
        try (InputStream entrada = ConsultaValidezTest.class.getResourceAsStream("/" + nombre)) {
            assertNotNull(entrada, "falta el recurso " + nombre);
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
