package com.peluware.sri.comprobantes.ride;

import com.peluware.sri.comprobantes.Comprobante;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.time.OffsetDateTime;

/**
 * Dibuja la representación impresa de un comprobante.
 *
 * <p>Recibe el {@link Comprobante} y no un modelo intermedio: cada uno de los seis tipos lleva
 * su propio formato en el Anexo 2 y comparten poco más que la cabecera. Como la jerarquía es
 * sellada, un {@code switch} sobre ella es exhaustivo y un tipo nuevo deja de compilar hasta
 * que se dibuja.
 *
 * <p>El estilo —logo, fuentes, medidas— es configuración de cada implementación y se recibe en
 * su constructor, no aquí: una implementación sobre plantillas se estiliza con su plantilla y
 * no con el modelo de estilo de otra.
 */
public interface GeneradorRide {

    /**
     * Escribe la representación en {@code destino}, que no se cierra.
     *
     * @param comprobante       comprobante a representar
     * @param fechaAutorizacion fecha y hora en que el SRI lo autorizó; {@code null} si todavía
     *                          no consta, que el Anexo 2 admite en los RIDE del emisor
     * @param destino           dónde escribir el documento
     * @throws RideException si falla la escritura
     */
    void escribir(Comprobante comprobante, @Nullable OffsetDateTime fechaAutorizacion, OutputStream destino);

    /**
     * Escribe la representación de un comprobante que todavía no consta autorizado.
     */
    default void escribir(Comprobante comprobante, OutputStream destino) {
        escribir(comprobante, null, destino);
    }
}
