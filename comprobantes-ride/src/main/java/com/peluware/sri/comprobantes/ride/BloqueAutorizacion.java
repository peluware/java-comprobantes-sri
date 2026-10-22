package com.peluware.sri.comprobantes.ride;

import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoEmision;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

/**
 * Bloque de autorización, arriba a la derecha del Anexo 2.
 *
 * <p>La fecha y hora de autorización es opcional: el Anexo 2 indica que los RIDE descargados
 * del portal del SRI la contienen y que «dicha información no es obligatoria registrarla en el
 * RIDE generado por los emisores de comprobantes electrónicos». Por eso una representación se
 * puede emitir antes de que el SRI resuelva.
 *
 * <p>El Anexo 2 también señala que el número de autorización corresponde a la clave de acceso,
 * de ahí que ambos campos lleven el mismo valor.
 *
 * @param ruc                 RUC del emisor
 * @param tipoComprobante     tipo de comprobante, que da el título del documento
 * @param numeroDocumento     establecimiento, punto de emisión y secuencial, con guiones
 * @param claveAcceso         clave de acceso de 49 dígitos, que es el número de autorización
 * @param fechaAutorizacion   fecha y hora en que el SRI autorizó; {@code null} si todavía no
 *                            consta
 * @param ambiente            ambiente en que se emitió
 * @param tipoEmision         tipo de emisión
 */
public record BloqueAutorizacion(
    String ruc,
    TipoComprobante tipoComprobante,
    String numeroDocumento,
    String claveAcceso,
    @Nullable OffsetDateTime fechaAutorizacion,
    Ambiente ambiente,
    TipoEmision tipoEmision
) {

    /**
     * Toma del comprobante su identidad y su clave de acceso.
     *
     * @param fechaAutorizacion fecha y hora de autorización; {@code null} si todavía no consta
     */
    public static BloqueAutorizacion de(Comprobante comprobante, @Nullable OffsetDateTime fechaAutorizacion) {
        return new BloqueAutorizacion(
            comprobante.emisor().ruc().valor(),
            comprobante.tipoComprobante(),
            comprobante.serie().numeroDocumento(comprobante.secuencial()),
            comprobante.claveAcceso(),
            fechaAutorizacion,
            comprobante.ambiente(),
            comprobante.tipoEmision()
        );
    }
}
