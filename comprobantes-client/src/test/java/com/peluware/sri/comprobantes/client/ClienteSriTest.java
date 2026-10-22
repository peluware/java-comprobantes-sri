package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteSriTest {

    private static final String CLAVE = "0510202601000000000000110010010000000010000000114";
    private static final byte[] COMPROBANTE = "<factura id=\"comprobante\"/>".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("el envío va al endpoint de recepción con las cabeceras de SOAP 1.1")
    void cabecerasDeSoap11() {
        var http = new HttpFalso(200, recurso("recepcion-recibida.xml"));

        new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5)).enviar(COMPROBANTE);

        var peticion = noNulo(http.peticion);
        assertAll(
            () -> assertEquals(SriEndpoints.PRUEBAS.recepcion(), peticion.uri()),
            () -> assertEquals("POST", peticion.method()),
            () -> assertEquals("text/xml; charset=utf-8", peticion.headers().firstValue("Content-Type").orElse("")),
            () -> assertEquals("\"\"", peticion.headers().firstValue("SOAPAction").orElse(""))
        );
    }

    @Test
    @DisplayName("el comprobante viaja en base64 dentro del sobre")
    void elComprobanteVaEnBase64() {
        var http = new HttpFalso(200, recurso("recepcion-recibida.xml"));

        new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5)).enviar(COMPROBANTE);

        assertTrue(http.cuerpo().contains(Base64.getEncoder().encodeToString(COMPROBANTE)), http.cuerpo());
    }

    @Test
    @DisplayName("la consulta de autorización envía la clave de acceso")
    void laConsultaEnviaLaClave() {
        var http = new HttpFalso(200, recurso("autorizacion-autorizado.xml"));

        var respuesta = new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5)).consultarAutorizacion(CLAVE);

        var sobre = http.cuerpo();
        assertAll(
            () -> assertTrue(respuesta.fueAutorizado()),
            () -> assertEquals(SriEndpoints.PRUEBAS.autorizacion(), noNulo(http.peticion).uri()),
            () -> assertTrue(sobre.contains("<ec:autorizacionComprobante"), sobre),
            () -> assertTrue(sobre.contains("<claveAccesoComprobante>" + CLAVE + "</claveAccesoComprobante>"), sobre)
        );
    }

    @Test
    @DisplayName("rechaza una clave de acceso inválida antes de salir a la red")
    void rechazaUnaClaveInvalidaSinRed() {
        var http = new HttpFalso(200, recurso("autorizacion-autorizado.xml"));
        var client = new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5));

        assertThrows(ValidacionException.class, () -> client.consultarAutorizacion("123"));
        assertNull(http.peticion, "no debe haber salido ninguna petición");
    }

    @Test
    @DisplayName("no impone el tamaño máximo del numeral 7.5: lo valida el SRI")
    void noImponeElTamanoMaximo() {
        var http = new HttpFalso(200, recurso("recepcion-recibida.xml"));
        var client = new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5));
        byte[] sobreElLimite = new byte[ClienteSri.TAMANO_MAXIMO_REFERENCIA_BYTES + 1];

        var respuesta = client.enviar(sobreElLimite);

        assertAll(
            () -> assertTrue(respuesta.fueRecibida()),
            () -> assertNotNull(http.peticion, "la petición debe salir; la cifra es de prosa normativa")
        );
    }

    @Test
    @DisplayName("un HTTP distinto de 200 lleva el código y el cuerpo, para poder diagnosticarlo")
    void httpNoExitoso() {
        byte[] paginaDeMantenimiento = "<html><body><h1>503 Service Unavailable</h1></body></html>".getBytes(StandardCharsets.UTF_8);
        var http = new HttpFalso(503, paginaDeMantenimiento);
        var client = new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5));

        var e = assertThrows(ComunicacionException.class, () -> client.enviar(COMPROBANTE));

        assertAll(
            () -> assertTrue(e.getMessage().contains("503"), e.getMessage()),
            () -> assertTrue(e.getMessage().contains("Service Unavailable"), e.getMessage())
        );
    }

    @Test
    @DisplayName("una respuesta que no es XML se diagnostica con su contenido")
    void respuestaQueNoEsXml() {
        byte[] html = "<html><head><title>Portal del SRI en mantenimiento</title></head></html>".getBytes(StandardCharsets.UTF_8);
        var http = new HttpFalso(200, html);
        var client = new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5));

        var e = assertThrows(ComunicacionException.class, () -> client.enviar(COMPROBANTE));

        assertTrue(e.getMessage().contains("Portal del SRI en mantenimiento"), e.getMessage());
    }

    @Test
    @DisplayName("el fragmento de diagnóstico se recorta y dice el tamaño total")
    void elFragmentoSeRecorta() {
        byte[] enorme = ("<html>" + "x".repeat(4000)).getBytes(StandardCharsets.UTF_8);
        var http = new HttpFalso(200, enorme);
        var client = new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5));

        var e = assertThrows(ComunicacionException.class, () -> client.enviar(COMPROBANTE));

        assertAll(
            () -> assertTrue(e.getMessage().contains(String.valueOf(enorme.length)), e.getMessage()),
            () -> assertTrue(e.getMessage().length() < 1000, "el mensaje no debe arrastrar la respuesta entera: " + e.getMessage().length())
        );
    }

    @Test
    @DisplayName("un fallo de red se convierte en ComunicacionException")
    void falloDeRed() {
        var http = new HttpFalso(new IOException("connection reset"));
        var client = new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5));

        var e = assertThrows(ComunicacionException.class, () -> client.enviar(COMPROBANTE));

        assertEquals("connection reset", noNulo(e.getCause()).getMessage());
    }

    @Test
    @DisplayName("los endpoints de cada ambiente apuntan a hosts distintos")
    void endpointsPorAmbiente() {
        assertAll(
            () -> assertEquals(SriEndpoints.PRUEBAS, SriEndpoints.of(Ambiente.PRUEBAS)),
            () -> assertEquals(SriEndpoints.PRODUCCION, SriEndpoints.of(Ambiente.PRODUCCION)),
            () -> assertEquals("celcer.sri.gob.ec", SriEndpoints.PRUEBAS.recepcion().getHost()),
            () -> assertEquals("cel.sri.gob.ec", SriEndpoints.PRODUCCION.recepcion().getHost()),
            () -> assertFalse(SriEndpoints.PRUEBAS.recepcion().toString().endsWith("?wsdl"), "el endpoint no lleva el sufijo wsdl")
        );
    }


    @Test
    @DisplayName("el observador recibe el sobre que salió y el que llegó")
    void elObservadorVeLosDosSobres() {
        var sobres = recurso("autorizacion-autorizado.xml");
        var http = new HttpFalso(200, sobres);
        var visto = new ObservadorContado();

        new ClienteSri(SriEndpoints.PRUEBAS, http, Duration.ofSeconds(5), visto).consultarAutorizacion(CLAVE);

        assertAll(
            () -> assertEquals(1, visto.llamadas),
            () -> assertEquals("autorizacionComprobante", visto.operacion),
            () -> assertEquals(SriEndpoints.PRUEBAS.autorizacion(), visto.endpoint),
            () -> assertTrue(new String(noNulo(visto.peticion), StandardCharsets.UTF_8).contains(CLAVE)),
            () -> assertArrayEquals(sobres, visto.respuesta)
        );
    }

    @Test
    @DisplayName("el observador ve el sobre que el cliente no logra interpretar")
    void elObservadorVeLoIlegible() {
        byte[] html = "<html><head><title>Portal del SRI en mantenimiento</title></head></html>".getBytes(StandardCharsets.UTF_8);
        var visto = new ObservadorContado();
        var client = new ClienteSri(SriEndpoints.PRUEBAS, new HttpFalso(200, html), Duration.ofSeconds(5), visto);

        assertThrows(ComunicacionException.class, () -> client.enviar(COMPROBANTE));

        assertAll(
            () -> assertEquals(1, visto.llamadas),
            () -> assertEquals("validarComprobante", visto.operacion),
            () -> assertArrayEquals(html, visto.respuesta)
        );
    }

    @Test
    @DisplayName("en un fallo de red el observador recibe null como respuesta")
    void elObservadorVeElFalloDeRed() {
        var visto = new ObservadorContado();
        var client = new ClienteSri(SriEndpoints.PRUEBAS, new HttpFalso(new IOException("connection reset")), Duration.ofSeconds(5), visto);

        assertThrows(ComunicacionException.class, () -> client.enviar(COMPROBANTE));

        assertAll(
            () -> assertEquals(1, visto.llamadas),
            () -> assertNotNull(visto.peticion),
            () -> assertNull(visto.respuesta)
        );
    }

    /**
     * Observador que guarda el último intercambio y cuenta cuántos vio.
     */
    private static final class ObservadorContado implements ObservadorSoap {

        private int llamadas;
        private @Nullable String operacion;
        private @Nullable URI endpoint;
        private byte @Nullable [] peticion;
        private byte @Nullable [] respuesta;

        @Override
        public void intercambio(String operacion, URI endpoint, byte[] peticion, byte @Nullable [] respuesta) {
            llamadas++;
            this.operacion = operacion;
            this.endpoint = endpoint;
            this.peticion = peticion;
            this.respuesta = respuesta;
        }
    }
    private static <T> T noNulo(@Nullable T valor) {
        assertNotNull(valor);
        return Objects.requireNonNull(valor);
    }

    private static byte[] recurso(String nombre) {
        try (InputStream entrada = ClienteSriTest.class.getResourceAsStream("/" + nombre)) {
            assertNotNull(entrada, "falta el recurso " + nombre);
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
