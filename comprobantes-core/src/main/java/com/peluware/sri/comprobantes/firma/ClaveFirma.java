package com.peluware.sri.comprobantes.firma;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

/**
 * Clave privada y cadena de certificados con la que se firma un comprobante.
 *
 * @param clavePrivada clave privada
 * @param cadena       cadena de certificados; el primero es el del firmante
 */
public record ClaveFirma(PrivateKey clavePrivada, List<X509Certificate> cadena) {

    public ClaveFirma {
        Objects.requireNonNull(clavePrivada, "clavePrivada");
        Objects.requireNonNull(cadena, "cadena");
        if (cadena.isEmpty()) {
            throw new FirmaException.SinCadenaDeCertificados(null);
        }
        cadena = List.copyOf(cadena);
    }

    /**
     * Certificado del firmante.
     */
    public X509Certificate certificado() {
        return cadena.getFirst();
    }
}
