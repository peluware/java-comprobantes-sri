package com.peluware.sri.comprobantes;

/**
 * El XML no se pudo leer: está mal formado, le falta un elemento obligatorio o uno de sus valores
 * no es del tipo que el esquema declara.
 */
public class LecturaXmlException extends RuntimeException {

    public LecturaXmlException(String mensaje) {
        super(mensaje);
    }

    public LecturaXmlException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
