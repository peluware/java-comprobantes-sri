package com.peluware.sri.comprobantes.ride;

import com.peluware.sri.comprobantes.ComprobanteException;
import org.jspecify.annotations.Nullable;

/**
 * Fallo al generar la representación impresa de un comprobante.
 */
public class RideException extends ComprobanteException {

    public RideException(String detalle, @Nullable Throwable causa) {
        super(detalle, causa);
    }

    @Override
    public String mensajeUsuario() {
        return "No se pudo generar la representación impresa del comprobante.";
    }
}
