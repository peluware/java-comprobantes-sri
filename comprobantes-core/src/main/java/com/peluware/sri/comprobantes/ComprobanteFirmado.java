package com.peluware.sri.comprobantes;

import java.util.Objects;

/**
 * Comprobante con su XML ya firmado, tal como se envía al SRI.
 *
 * <p>Lo produce un {@code Firmador} a partir de un {@link Comprobante}, que queda accesible
 * junto al XML.
 *
 * @param comprobante comprobante que se firmó
 * @param xml         XML firmado; se copia al construir y al leer
 */
public record ComprobanteFirmado(Comprobante comprobante, byte[] xml) {

    public ComprobanteFirmado {
        Objects.requireNonNull(comprobante, "comprobante");
        Objects.requireNonNull(xml, "xml");
        if (xml.length == 0) {
            throw new ValidacionException(new Validacion.CampoInvalido("xml", "El XML firmado no puede estar vacío"));
        }
        xml = xml.clone();
    }

    /**
     * XML firmado.
     */
    @Override
    public byte[] xml() {
        return xml.clone();
    }

    /**
     * Clave de acceso del comprobante.
     */
    public String claveAcceso() {
        return comprobante.claveAcceso();
    }

    /**
     * Emisor del comprobante.
     */
    public Emisor emisor() {
        return comprobante.emisor();
    }

    @Override
    public String toString() {
        return "ComprobanteFirmado[" + comprobante.tipoComprobante() + " " + claveAcceso() + "]";
    }
}
