package com.peluware.sri.comprobantes.firma;

import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.ComprobanteFirmado;

import java.util.List;
import java.util.Objects;

/**
 * Firma comprobantes electrónicos.
 *
 * <p>Ficha Técnica v2.34, Tabla 7: XAdES_BES, esquema 1.3.2, codificación UTF-8, firma
 * ENVELOPED. Numeral 6.8: RSA-SHA1 con clave de 2048 bits.
 *
 * <p>El contrato va de {@link Comprobante} a {@link ComprobanteFirmado}. Un firmador que
 * trabaje sobre el XML se escribe extendiendo {@link FirmadorXml}.
 *
 * <p>Las sobrecargas que reciben un {@link OrigenClave} abren la clave una sola vez, incluida la
 * que firma varios comprobantes.
 *
 * <p>Las implementaciones no retienen la clave. Un mismo firmador sirve para varios emisores.
 */
public interface Firmador {

    /**
     * Firma un comprobante.
     *
     * @param clave clave con la que firmar, ya abierta
     * @throws FirmaException                                  si el comprobante no puede
     *                                                         firmarse
     * @throws com.peluware.sri.comprobantes.EscrituraXmlException si no puede escribirse
     */
    ComprobanteFirmado firmar(ClaveFirma clave, Comprobante comprobante);

    /**
     * Firma un comprobante, abriendo la clave.
     *
     * @throws FirmaException si la clave no puede abrirse o el comprobante no puede firmarse
     */
    default ComprobanteFirmado firmar(OrigenClave clave, Comprobante comprobante) {
        return firmar(abrir(clave), comprobante);
    }

    /**
     * Firma varios comprobantes, abriendo la clave una sola vez.
     *
     * @return los comprobantes firmados, en el mismo orden
     * @throws FirmaException si la clave no puede abrirse o alguno no puede firmarse
     */
    default List<ComprobanteFirmado> firmar(OrigenClave clave, List<? extends Comprobante> comprobantes) {
        Objects.requireNonNull(comprobantes, "comprobantes");

        var abierta = abrir(clave);
        return comprobantes.stream().map(comprobante -> firmar(abierta, comprobante)).toList();
    }

    private static ClaveFirma abrir(OrigenClave clave) {
        return Objects.requireNonNull(clave, "clave").abrir();
    }
}
