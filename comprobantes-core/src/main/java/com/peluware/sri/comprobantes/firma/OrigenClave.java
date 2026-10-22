package com.peluware.sri.comprobantes.firma;

/**
 * Origen de una clave de firma.
 *
 * <p>Las implementaciones reciben su configuración en el constructor: un archivo PKCS#12
 * desde un {@code InputStream}, un token PKCS#11 desde su configuración de dispositivo.
 */
@FunctionalInterface
public interface OrigenClave {

    /**
     * Abre la clave privada y su cadena de certificados.
     *
     * @throws FirmaException si la clave no puede abrirse
     */
    ClaveFirma abrir();
}
