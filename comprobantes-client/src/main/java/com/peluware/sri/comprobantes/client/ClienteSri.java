package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.interno.xml.Dom;
import com.peluware.sri.comprobantes.ClaveAcceso;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.Ambiente;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

/**
 * Cliente de los servicios de recepción y autorización de comprobantes del SRI.
 *
 * <p>El flujo es asíncrono (Ficha Técnica v2.34, numeral 7.4): primero se envía el comprobante
 * firmado con {@link #enviar(byte[])} y, si la respuesta es
 * {@link RespuestaRecepcion.Recibida}, se consulta más tarde con
 * {@link #consultarAutorizacion(String)}. El SRI dispone de hasta 24 horas para resolver
 * (numeral 7.5); una consulta inmediata devuelve normalmente
 * {@link RespuestaAutorizacion.EnProceso}.
 *
 * <p>No reintenta ni espera. La política de reintentos queda en la aplicación.
 *
 * <p>Para archivar o registrar los sobres tal como viajan, se le puede dar un
 * {@link ObservadorSoap}.
 *
 * <p>Es inmutable y se puede compartir entre hilos.
 */
public final class ClienteSri {

    /**
     * Tamaño máximo de un comprobante en el envío individual.
     *
     * <p>Ficha Técnica v2.34, numeral 7.5: 320 Kb. Esta librería no lo valida.
     */
    public static final int TAMANO_MAXIMO_REFERENCIA_BYTES = 320 * 1024;

    private static final Duration TIMEOUT_POR_OMISION = Duration.ofSeconds(30);

    /** Tipo de contenido de SOAP 1.1. */
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";

    /**
     * Valor de la cabecera {@code SOAPAction}: cadena vacía entrecomillada.
     *
     * <p>SOAP 1.1 exige la cabecera con valor entrecomillado. Los dos WSDL declaran
     * {@code soapAction=""}.
     */
    private static final String SOAP_ACTION = "\"\"";

    private static final int HTTP_OK = 200;

    private final SriEndpoints endpoints;
    private final HttpClient http;
    private final Duration timeout;
    private final ObservadorSoap observador;

    public ClienteSri(SriEndpoints endpoints) {
        this(endpoints, ObservadorSoap.NINGUNO);
    }

    /**
     * Cliente que entrega a un observador los sobres que intercambia.
     */
    public ClienteSri(SriEndpoints endpoints, ObservadorSoap observador) {
        this(endpoints, HttpClient.newBuilder().connectTimeout(TIMEOUT_POR_OMISION).build(), TIMEOUT_POR_OMISION, observador);
    }

    /**
     * Cliente con transporte y tiempo de espera propios.
     */
    public ClienteSri(SriEndpoints endpoints, HttpClient http, Duration timeout) {
        this(endpoints, http, timeout, ObservadorSoap.NINGUNO);
    }

    /**
     * Cliente con transporte, tiempo de espera y observador propios.
     */
    public ClienteSri(SriEndpoints endpoints, HttpClient http, Duration timeout, ObservadorSoap observador) {
        this.endpoints = Objects.requireNonNull(endpoints, "endpoints");
        this.http = Objects.requireNonNull(http, "http");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.observador = Objects.requireNonNull(observador, "observador");
    }

    /**
     * Cliente apuntando al ambiente indicado.
     */
    public static ClienteSri of(Ambiente ambiente) {
        return new ClienteSri(SriEndpoints.of(ambiente));
    }

    /**
     * Envía un comprobante firmado al servicio de recepción.
     *
     * <p>Una respuesta {@link RespuestaRecepcion.Recibida} solo significa que el comprobante
     * entró en la cola; no es la autorización.
     *
     * @param comprobanteFirmado XML firmado del comprobante
     * @throws ComunicacionException si el servicio no responde, responde algo ilegible o
     *                               devuelve un {@code soap:Fault}
     */
    public RespuestaRecepcion enviar(byte[] comprobanteFirmado) {
        Objects.requireNonNull(comprobanteFirmado, "comprobanteFirmado");

        byte[] sobre = SoapSri.envolver(
            OperacionSoap.RECEPCION,
            Base64.getEncoder().encodeToString(comprobanteFirmado)
        );

        return Respuestas.recepcion(SoapSri.contenido(invocar(OperacionSoap.RECEPCION, endpoints.recepcion(), sobre)));
    }

    /**
     * Consulta el resultado de la autorización de un comprobante.
     *
     * @param claveAcceso clave de acceso del comprobante enviado
     * @throws ValidacionException    si la clave no tiene 49 dígitos o el dígito verificador no cuadra
     * @throws ComunicacionException si el servicio no responde, responde algo ilegible o
     *                               devuelve un {@code soap:Fault}
     */
    public RespuestaAutorizacion consultarAutorizacion(String claveAcceso) {
        ClaveAcceso.exigir(claveAcceso);

        byte[] sobre = SoapSri.envolver(
            OperacionSoap.AUTORIZACION,
            claveAcceso
        );

        return Respuestas.autorizacion(SoapSri.contenido(invocar(OperacionSoap.AUTORIZACION, endpoints.autorizacion(), sobre)), claveAcceso);
    }

    /**
     * Consulta el estado en que está un comprobante.
     *
     * <p>Ficha Técnica v2.34, numerales 8.2 y 8.4. Es el único servicio que distingue
     * {@link RespuestaValidez.PendienteDeAnular} y {@link RespuestaValidez.Anulado}, estados a
     * los que un comprobante autorizado puede llegar después, sin que el emisor intervenga.
     *
     * <p>El servicio no devuelve el XML del comprobante.
     *
     * @param claveAcceso clave de acceso del comprobante
     * @throws ValidacionException   si la clave no tiene 49 dígitos o el dígito verificador no cuadra
     * @throws ComunicacionException si el servicio no responde, responde algo ilegible o
     *                               devuelve un {@code soap:Fault}
     */
    public RespuestaValidez consultarValidez(String claveAcceso) {
        ClaveAcceso.exigir(claveAcceso);

        byte[] sobre = SoapSri.envolver(
            OperacionSoap.CONSULTA_VALIDEZ,
            claveAcceso
        );

        return Respuestas.validez(SoapSri.contenido(invocar(OperacionSoap.CONSULTA_VALIDEZ, endpoints.consultaValidez(), sobre)), claveAcceso);
    }

    private byte[] invocar(OperacionSoap operacion, URI endpoint, byte[] sobre) {
        var peticion = HttpRequest.newBuilder(endpoint)
            .header("Content-Type", CONTENT_TYPE)
            .header("SOAPAction", SOAP_ACTION)
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofByteArray(sobre))
            .build();

        byte[] cuerpo;
        int estado;
        try {
            var respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
            cuerpo = respuesta.body();
            estado = respuesta.statusCode();
        } catch (IOException e) {
            observador.intercambio(operacion.nombre(), endpoint, sobre, null);
            throw new ComunicacionException("Falló la conexión con " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            observador.intercambio(operacion.nombre(), endpoint, sobre, null);
            throw new ComunicacionException("Se interrumpió la espera de " + endpoint, e);
        }

        observador.intercambio(operacion.nombre(), endpoint, sobre, cuerpo);

        if (estado != HTTP_OK) {
            throw new ComunicacionException("El servicio " + endpoint + " respondió HTTP " + estado + ": " + Dom.preview(cuerpo));
        }
        return cuerpo;
    }
}
