package com.peluware.sri.comprobantes.client;

import java.util.List;

/**
 * Respuesta del servicio {@code RecepcionComprobantesOffline}.
 *
 * <p>Ficha Técnica v2.34, numeral 7.2.3. El servicio acusa recibo: {@link Recibida} indica
 * que el comprobante pasó la primera validación y entró en la cola, no que esté autorizado.
 * La autorización se consulta contra {@code AutorizacionComprobantesOffline} (numeral 7.4).
 */
public sealed interface RespuestaRecepcion {

    /**
     * El comprobante entró en la cola de autorización.
     */
    record Recibida() implements RespuestaRecepcion {
    }

    /**
     * El comprobante no pasó la primera validación.
     *
     * @param comprobantes comprobantes rechazados con sus motivos
     */
    record Devuelta(List<ComprobanteDevuelto> comprobantes) implements RespuestaRecepcion {

        public Devuelta {
            comprobantes = List.copyOf(comprobantes);
        }

        /**
         * Todos los mensajes de todos los comprobantes rechazados.
         */
        public List<Mensaje> mensajes() {
            return comprobantes.stream().flatMap(comprobante -> comprobante.mensajes().stream()).toList();
        }
    }

    /**
     * Estado que el servicio devolvió y esta librería no reconoce.
     *
     * @param estado       texto del estado tal como llegó
     * @param comprobantes comprobantes que acompañaban a la respuesta, si los hubo
     */
    record Desconocida(String estado, List<ComprobanteDevuelto> comprobantes) implements RespuestaRecepcion {

        public Desconocida {
            comprobantes = List.copyOf(comprobantes);
        }
    }

    /**
     * Indica si el comprobante entró en la cola de autorización.
     */
    default boolean fueRecibida() {
        return this instanceof Recibida;
    }
}
