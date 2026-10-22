package com.peluware.sri.comprobantes.client;

import org.jspecify.annotations.Nullable;

/**
 * Severidad de un mensaje devuelto por los servicios del SRI.
 *
 * <p>El WSDL declara el campo {@code tipo} como texto libre. Un valor no reconocido se
 * resuelve como {@link #OTRO}.
 */
public enum TipoMensaje {

    /** Impide la recepción o la autorización del comprobante. */
    ERROR,

    /** No impide la autorización. El identificador 60 avisa de que el ambiente es de pruebas. */
    ADVERTENCIA,

    INFORMATIVO,

    /** Severidad que el SRI devolvió y esta librería no reconoce. */
    OTRO;

    /**
     * Resuelve la severidad a partir del texto del campo {@code tipo}.
     *
     * @return {@link #OTRO} si el texto es nulo o no corresponde a ninguna severidad conocida
     */
    public static TipoMensaje of(@Nullable String tipo) {
        var normalizado = Estados.normalizar(tipo);
        return switch (normalizado) {
            case "ERROR" -> ERROR;
            case "ADVERTENCIA" -> ADVERTENCIA;
            case "INFORMATIVO", "INFORMACION" -> INFORMATIVO;
            default -> OTRO;
        };
    }

    /**
     * Indica si el mensaje impide que el comprobante avance.
     */
    public boolean esError() {
        return this == ERROR;
    }
}
