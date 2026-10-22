package com.peluware.sri.comprobantes.client;

import java.util.Objects;

/**
 * Operación de un servicio web del SRI, tal como la declara su WSDL.
 *
 * <p>Son los únicos campos en que difieren los sobres. Ver {@link SoapSri} para su forma.
 *
 * @param namespace  namespace del servicio, al que pertenece el elemento de la operación
 * @param nombre     nombre de la operación, que da nombre a ese elemento
 * @param parametro  nombre del único parámetro, que va sin cualificar dentro de él
 */
record OperacionSoap(String namespace, String nombre, String parametro) {

    /** {@code validarComprobante(byte[] xml)}: recibe el comprobante firmado. */
    static final OperacionSoap RECEPCION = new OperacionSoap("http://ec.gob.sri.ws.recepcion", "validarComprobante", "xml");

    /** {@code autorizacionComprobante(String claveAccesoComprobante)}: consulta el resultado. */
    static final OperacionSoap AUTORIZACION = new OperacionSoap("http://ec.gob.sri.ws.autorizacion", "autorizacionComprobante", "claveAccesoComprobante");

    /** {@code consultarEstadoAutorizacionComprobante(String claveAcceso)}: consulta la validez. */
    static final OperacionSoap CONSULTA_VALIDEZ = new OperacionSoap("http://ec.gob.sri.ws.consultas", "consultarEstadoAutorizacionComprobante", "claveAcceso");

    OperacionSoap {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(parametro, "parametro");
    }
}
