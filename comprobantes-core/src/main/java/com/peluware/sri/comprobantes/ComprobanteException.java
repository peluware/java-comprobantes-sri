package com.peluware.sri.comprobantes;

import org.jspecify.annotations.Nullable;

/**
 * Raíz de la jerarquía de errores de la librería.
 *
 * <p>Expone dos mensajes: {@link #getMessage()} con el detalle técnico y
 * {@link #mensajeUsuario()} con un texto apto para mostrar en pantalla.
 */
public abstract class ComprobanteException extends RuntimeException {

    protected ComprobanteException(String detalle) {
        super(detalle);
    }

    protected ComprobanteException(String detalle, @Nullable Throwable causa) {
        super(detalle, causa);
    }

    /**
     * Mensaje apto para mostrar al usuario final, sin nombres de campo ni detalles del esquema.
     */
    public abstract String mensajeUsuario();
}
