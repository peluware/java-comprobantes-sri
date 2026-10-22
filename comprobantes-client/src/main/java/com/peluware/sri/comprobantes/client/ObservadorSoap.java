package com.peluware.sri.comprobantes.client;

import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * Observa los sobres SOAP que {@link ClienteSri} intercambia con el SRI.
 *
 * <p>No transforma la respuesta ni aborta la llamada. Se invoca una sola vez por llamada y
 * antes de interpretar la respuesta, también para los sobres ilegibles y los que acompañan a un
 * código HTTP distinto de 200.
 *
 * <p>Recibe el sobre completo. El comprobante autorizado va dentro, escapado, en el elemento
 * {@code <comprobante>}; {@link RespuestaAutorizacion.Autorizado#comprobante()} lo entrega ya
 * desescapado.
 *
 * <p>No debe lanzar: una excepción del observador sustituye a la de la llamada.
 *
 * <p>Se invoca en el hilo que hizo la llamada.
 */
public interface ObservadorSoap {

    /**
     * Observador que no hace nada. Es el de un cliente construido sin uno.
     */
    ObservadorSoap NINGUNO = (operacion, endpoint, peticion, respuesta) -> {
    };

    /**
     * Recibe los dos sobres de una llamada al SRI.
     *
     * @param operacion nombre de la operación en el WSDL del servicio
     * @param endpoint  dirección a la que se envió el sobre
     * @param peticion  sobre que se envió
     * @param respuesta sobre que devolvió el servicio, o {@code null} si no respondió
     */
    void intercambio(String operacion, URI endpoint, byte[] peticion, byte @Nullable [] respuesta);
}
