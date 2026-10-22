package com.peluware.sri.comprobantes.client;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Mensaje devuelto por un servicio del SRI.
 *
 * <p>Ficha Técnica v2.34, Tabla 29: el identificador corresponde al catálogo de errores y
 * advertencias.
 *
 * @param identificador        código del catálogo de errores
 * @param mensaje              texto del mensaje
 * @param informacionAdicional detalle del error; {@code null} si el servicio no lo envió
 * @param tipo                 severidad
 */
public record Mensaje(String identificador, String mensaje, @Nullable String informacionAdicional, TipoMensaje tipo) {

    public Mensaje {
        Objects.requireNonNull(identificador, "identificador");
        Objects.requireNonNull(mensaje, "mensaje");
        Objects.requireNonNull(tipo, "tipo");
    }

    /**
     * Indica si el mensaje impide que el comprobante avance.
     */
    public boolean esError() {
        return tipo.esError();
    }

    @Override
    public String toString() {
        var base = identificador + " " + mensaje;
        return informacionAdicional == null ? base : base + ": " + informacionAdicional;
    }
}
