package com.peluware.sri.comprobantes.firma;

import com.peluware.sri.comprobantes.ComprobanteWriter;
import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.ComprobanteFirmado;

import java.util.Objects;

/**
 * Firmador que trabaja sobre el XML del comprobante.
 *
 * <p>Base de los firmadores cuya firma es una operación sobre XML, como XAdES. Escribe el
 * {@link Comprobante}, delega en {@link #firmarXml(ClaveFirma, byte[])} y devuelve el
 * {@link ComprobanteFirmado}.
 */
public abstract class FirmadorXml implements Firmador {

    /**
     * Firma el XML de un comprobante.
     *
     * @param clave clave con la que firmar, ya abierta
     * @param xml XML del comprobante, con {@code id="comprobante"} en el elemento raíz
     * @return XML firmado
     * @throws FirmaException si el comprobante no puede firmarse
     */
    protected abstract byte[] firmarXml(ClaveFirma clave, byte[] xml);

    @Override
    public final ComprobanteFirmado firmar(ClaveFirma clave, Comprobante comprobante) {
        Objects.requireNonNull(comprobante, "comprobante");
        return new ComprobanteFirmado(comprobante, firmarXml(clave, ComprobanteWriter.xml(comprobante)));
    }
}
