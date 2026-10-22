package com.peluware.sri.comprobantes.client;

import java.util.List;
import java.util.Objects;

/**
 * Comprobante que el servicio de recepción rechazó, con los motivos.
 *
 * <p>La clave de acceso se conserva como texto crudo; puede no ser una clave válida.
 *
 * @param claveAcceso clave de acceso tal como la devolvió el servicio
 * @param mensajes    motivos del rechazo
 */
public record ComprobanteDevuelto(String claveAcceso, List<Mensaje> mensajes) {

    public ComprobanteDevuelto {
        Objects.requireNonNull(claveAcceso, "claveAcceso");
        mensajes = List.copyOf(mensajes);
    }

    /**
     * Mensajes con severidad de error.
     */
    public List<Mensaje> errores() {
        return mensajes.stream().filter(Mensaje::esError).toList();
    }
}
