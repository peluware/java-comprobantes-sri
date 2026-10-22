package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.catalogo.Ambiente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespuestasTest {

    private static final String CLAVE = "0510202601000000000000110010010000000010000000114";


    @Test
    @DisplayName("lee una recepción exitosa del ejemplo de la ficha")
    void recepcionRecibida() {
        var respuesta = recepcion("recepcion-recibida.xml");

        assertAll(
            () -> assertInstanceOf(RespuestaRecepcion.Recibida.class, respuesta),
            () -> assertTrue(respuesta.fueRecibida()));
    }

    @Test
    @DisplayName("lee una recepción fallida del ejemplo de la ficha, con sus mensajes")
    void recepcionDevuelta() {
        RespuestaRecepcion.Devuelta devuelta = assertInstanceOf(RespuestaRecepcion.Devuelta.class, recepcion("recepcion-devuelta.xml"));

        assertEquals(1, devuelta.comprobantes().size());
        var comprobante = devuelta.comprobantes().getFirst();

        assertAll(
            () -> assertEquals("1702201205176001321000110010030001000011234567816", comprobante.claveAcceso()),
            () -> assertEquals(1, comprobante.mensajes().size()),
            () -> assertEquals("35", comprobante.mensajes().getFirst().identificador()),
            () -> assertEquals(TipoMensaje.ERROR, comprobante.mensajes().getFirst().tipo()),
            () -> assertEquals(1, comprobante.errores().size()),
            () -> assertFalse(devuelta.fueRecibida()));
    }

    @Test
    @DisplayName("el texto del mensaje no se confunde con el elemento mensaje que lo contiene")
    void noConfundeMensajeConSuContenedor() {
        RespuestaRecepcion.Devuelta devuelta = assertInstanceOf(RespuestaRecepcion.Devuelta.class, recepcion("recepcion-devuelta.xml"));
        var mensaje = devuelta.comprobantes().getFirst().mensajes().getFirst();

        assertAll(
            () -> assertEquals("DOCUMENTO INVÁLIDO", mensaje.mensaje()),
            () -> assertNotNull(mensaje.informacionAdicional()),
            () -> assertTrue(mensaje.informacionAdicional().contains("totalSinImpuestos"), mensaje.informacionAdicional()));
    }

    @Test
    @DisplayName("lee un comprobante autorizado del ejemplo de la ficha")
    void autorizado() {
        RespuestaAutorizacion.Autorizado autorizado = assertInstanceOf(RespuestaAutorizacion.Autorizado.class, autorizacion("autorizacion-autorizado.xml"));

        assertAll(
            () -> assertEquals(CLAVE, autorizado.numeroAutorizacion()),
            () -> assertEquals(OffsetDateTime.parse("2026-10-05T16:57:34.997-05:00"), autorizado.fechaAutorizacion()),
            () -> assertEquals(Ambiente.PRUEBAS, autorizado.ambiente()),
            () -> assertTrue(autorizado.comprobante().contains("<factura id=\"comprobante\""), autorizado.comprobante()),
            () -> assertEquals(1, autorizado.mensajes().size()),
            () -> assertEquals(TipoMensaje.ADVERTENCIA, autorizado.mensajes().getFirst().tipo()));
    }

    @Test
    @DisplayName("una advertencia no impide que el comprobante quede autorizado")
    void laAdvertenciaNoImpideLaAutorizacion() {
        var respuesta = autorizacion("autorizacion-autorizado.xml");
        RespuestaAutorizacion.Autorizado autorizado = assertInstanceOf(RespuestaAutorizacion.Autorizado.class, respuesta);

        assertAll(
            () -> assertTrue(respuesta.fueAutorizado()),
            () -> assertTrue(autorizado.mensajes().stream().noneMatch(Mensaje::esError))
        );
    }

    @Test
    @DisplayName("lee un comprobante rechazado del ejemplo de la ficha")
    void rechazado() {
        RespuestaAutorizacion.NoAutorizado rechazado = assertInstanceOf(RespuestaAutorizacion.NoAutorizado.class, autorizacion("autorizacion-rechazado.xml"));

        assertAll(
            () -> assertEquals(1, rechazado.errores().size()),
            () -> assertEquals("46", rechazado.errores().getFirst().identificador()),
            () -> assertEquals(Ambiente.PRUEBAS, rechazado.ambiente()),
            () -> assertNotNull(rechazado.fechaAutorizacion())
        );
    }

    @Test
    @DisplayName("una respuesta sin autorizaciones significa que el SRI no ha resuelto")
    void enProceso() {
        var respuesta = autorizacion("autorizacion-en-proceso.xml");

        assertAll(
            () -> assertInstanceOf(RespuestaAutorizacion.EnProceso.class, respuesta),
            () -> assertTrue(respuesta.convieneReintentar()),
            () -> assertEquals(CLAVE, ((RespuestaAutorizacion.EnProceso) respuesta).claveAccesoConsultada())
        );
    }

    @ParameterizedTest
    @DisplayName("acepta los tres vocabularios de estado que usa la ficha para autorizado")
    @ValueSource(strings = {"AUTORIZADO", "AUT", "autorizado", " Autorizado "})
    void vocabulariosDeAutorizado(String estado) {
        assertInstanceOf(RespuestaAutorizacion.Autorizado.class, autorizacionConEstado(estado));
    }

    @ParameterizedTest
    @DisplayName("acepta los vocabularios de estado para no autorizado")
    @ValueSource(strings = {"RECHAZADO", "NO AUTORIZADO", "NOAUTORIZADO", "NAT"})
    void vocabulariosDeNoAutorizado(String estado) {
        assertInstanceOf(RespuestaAutorizacion.NoAutorizado.class, autorizacionConEstado(estado));
    }

    @ParameterizedTest
    @DisplayName("acepta los vocabularios de estado para en proceso")
    @ValueSource(strings = {"EN PROCESAMIENTO", "EN PROCESO", "PPR", "PROCESAMIENTO"})
    void vocabulariosDeEnProceso(String estado) {
        assertInstanceOf(RespuestaAutorizacion.EnProceso.class, autorizacionConEstado(estado));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("una severidad que el WSDL no acota se resuelve como OTRO")
    @ValueSource(strings = {"CRITICO", "", "   "})
    void severidadDesconocida(String tipo) {
        assertEquals(TipoMensaje.OTRO, TipoMensaje.of(tipo));
    }

    @Test
    @DisplayName("una severidad conocida se reconoce sin importar tildes ni mayúsculas")
    void severidadConocida() {
        assertAll(
            () -> assertEquals(TipoMensaje.ERROR, TipoMensaje.of("ERROR")),
            () -> assertEquals(TipoMensaje.ADVERTENCIA, TipoMensaje.of("advertencia")),
            () -> assertEquals(TipoMensaje.INFORMATIVO, TipoMensaje.of("INFORMACIÓN")),
            () -> assertEquals(TipoMensaje.OTRO, TipoMensaje.of(null)));
    }

    @Test
    @DisplayName("un estado desconocido se conserva en lugar de fallar")
    void estadoDesconocido() {
        RespuestaAutorizacion.Desconocida desconocida = assertInstanceOf(RespuestaAutorizacion.Desconocida.class, autorizacionConEstado("EN REVISIÓN MANUAL"));

        assertEquals("EN REVISION MANUAL", desconocida.estado());
    }

    @Test
    @DisplayName("un autorizado sin el XML autorizado se trata como desconocido")
    void autorizadoSinComprobante() {
        var sinComprobante = plantillaAutorizacion("AUTORIZADO").replaceFirst("<comprobante>.*</comprobante>", "");

        assertInstanceOf(RespuestaAutorizacion.Desconocida.class, Respuestas.autorizacion(SoapSri.contenido(sinComprobante.getBytes(StandardCharsets.UTF_8)), CLAVE));
    }

    @Test
    @DisplayName("un soap:Fault se convierte en ComunicacionException con su faultstring")
    void soapFault() {
        byte[] fault = recurso("soap-fault.xml");

        var e = assertThrows(ComunicacionException.class, () -> SoapSri.contenido(fault));

        assertTrue(e.getMessage().contains("Servicio no disponible temporalmente"), e.getMessage());
    }

    @Test
    @DisplayName("rechaza una respuesta con DOCTYPE en lugar de expandir la entidad")
    void rechazaDoctype() {
        byte[] conDoctype = recurso("con-doctype.xml");

        assertThrows(ComunicacionException.class, () -> SoapSri.contenido(conDoctype));
    }

    private static RespuestaAutorizacion autorizacionConEstado(String estado) {
        return Respuestas.autorizacion(SoapSri.contenido(plantillaAutorizacion(estado).getBytes(StandardCharsets.UTF_8)), CLAVE);
    }

    private static String plantillaAutorizacion(String estado) {
        return new String(recurso("autorizacion-autorizado.xml"), StandardCharsets.UTF_8)
            .replace("<estado>AUTORIZADO</estado>", "<estado>" + estado + "</estado>");
    }

    private static RespuestaRecepcion recepcion(String recurso) {
        return Respuestas.recepcion(SoapSri.contenido(recurso(recurso)));
    }

    private static RespuestaAutorizacion autorizacion(String recurso) {
        return Respuestas.autorizacion(SoapSri.contenido(recurso(recurso)), CLAVE);
    }

    private static byte[] recurso(String nombre) {
        try (InputStream entrada = RespuestasTest.class.getResourceAsStream("/" + nombre)) {
            assertNotNull(entrada, "falta el recurso " + nombre);
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
