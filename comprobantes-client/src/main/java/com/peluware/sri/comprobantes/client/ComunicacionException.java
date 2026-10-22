package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.ComprobanteException;
import org.jspecify.annotations.Nullable;

/**
 * Fallo al comunicarse con un servicio del SRI.
 *
 * <p>Cubre los fallos de red, los códigos HTTP distintos de 200, los {@code soap:Fault} y las
 * respuestas que no se pueden interpretar. El rechazo de un comprobante no es un fallo de
 * comunicación: llega como {@link RespuestaRecepcion.Devuelta} o
 * {@link RespuestaAutorizacion.NoAutorizado}.
 */
public final class ComunicacionException extends ComprobanteException {

    public ComunicacionException(String detalle) {
        super(detalle);
    }

    public ComunicacionException(String detalle, @Nullable Throwable causa) {
        super(detalle, causa);
    }

    @Override
    public String mensajeUsuario() {
        return "No se pudo contactar con el servicio del SRI. Intente nuevamente en unos minutos.";
    }
}
