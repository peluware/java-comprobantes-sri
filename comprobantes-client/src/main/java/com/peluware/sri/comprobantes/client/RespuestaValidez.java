package com.peluware.sri.comprobantes.client;

import java.util.List;

/**
 * Respuesta del servicio {@code ConsultaComprobante}.
 *
 * <p>Ficha Técnica v2.34, numerales 8.2 y 8.4. Informa del estado en que está el comprobante en
 * el momento de la consulta, que puede cambiar con el tiempo: un comprobante autorizado pasa a
 * {@link Anulado} si su anulación se completa. {@link RespuestaAutorizacion} informa del
 * resultado de la autorización, que es un hecho y no cambia.
 *
 * <p>El servicio lleva dos niveles de estado, {@code estadoConsulta} y
 * {@code estadoAutorizacion}: {@link NoConsultable} corresponde al primero y significa que la
 * consulta no se pudo resolver, no que el comprobante carezca de validez.
 */
public sealed interface RespuestaValidez {

    /**
     * El comprobante está autorizado y tiene validez tributaria.
     */
    record Autorizado(ComprobanteConsultado comprobante) implements RespuestaValidez {
    }

    /**
     * El SRI rechazó el comprobante.
     */
    record NoAutorizado(ComprobanteConsultado comprobante) implements RespuestaValidez {
    }

    /**
     * Hay una solicitud de anulación esperando la respuesta del receptor.
     *
     * <p>Resolución NAC-DGERCGC25-00000014, artículo 4: el receptor dispone de cinco días
     * hábiles para aceptarla o rechazarla, y si no responde la solicitud queda sin efecto y el
     * comprobante mantiene su validez.
     */
    record PendienteDeAnular(ComprobanteConsultado comprobante) implements RespuestaValidez {
    }

    /**
     * El comprobante quedó anulado y ya no tiene validez tributaria.
     */
    record Anulado(ComprobanteConsultado comprobante) implements RespuestaValidez {
    }

    /**
     * La consulta no se pudo resolver.
     *
     * <p>Ocurre cuando la clave no consta en las bases del SRI o cuando su fecha de emisión está
     * fuera del rango que el servicio admite, los dos con el identificador {@code 99} y el
     * mensaje {@code ERROR AL CONSULTAR DATOS DEL SERVICIO WEB}. No dice nada sobre la validez
     * del comprobante.
     *
     * @param claveAcceso clave que se consultó
     * @param mensajes    motivos que devolvió el servicio
     */
    record NoConsultable(String claveAcceso, List<Mensaje> mensajes) implements RespuestaValidez {
    }

    /**
     * Estado que el servicio devolvió y esta librería no reconoce.
     *
     * @param estado   texto del estado tal como llegó
     * @param mensajes mensajes que acompañaban a la respuesta, si los hubo
     */
    record Desconocida(String estado, List<Mensaje> mensajes) implements RespuestaValidez {
    }

    /**
     * Indica si el comprobante tiene validez tributaria en el momento de la consulta.
     *
     * <p>Cierto para {@link Autorizado} y para {@link PendienteDeAnular}, cuyo comprobante
     * mantiene su validez mientras la solicitud de anulación no se complete.
     */
    default boolean tieneValidez() {
        return this instanceof Autorizado || this instanceof PendienteDeAnular;
    }
}
