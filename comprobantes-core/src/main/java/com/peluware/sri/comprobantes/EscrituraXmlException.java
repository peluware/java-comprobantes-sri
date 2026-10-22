package com.peluware.sri.comprobantes;

import org.jspecify.annotations.Nullable;

/**
 * Fallo al escribir el XML de un comprobante.
 *
 * <p>Corresponde a un fallo del destino de escritura, no al contenido del comprobante.
 */
public final class EscrituraXmlException extends ComprobanteException {

    public EscrituraXmlException(String detalle, @Nullable Throwable causa) {
        super(detalle, causa);
    }

    @Override
    public String mensajeUsuario() {
        return "No se pudo generar el archivo del comprobante. Intente nuevamente.";
    }
}
