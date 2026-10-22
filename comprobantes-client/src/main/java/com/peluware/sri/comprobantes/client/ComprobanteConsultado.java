package com.peluware.sri.comprobantes.client;

import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Lo que el servicio de consulta de validez informa de un comprobante.
 *
 * <p>Ficha Técnica v2.34, numeral 8.2. Son los campos del elemento
 * {@code EstadoAutorizacionComprobante} que acompañan al estado. El servicio no devuelve el XML
 * del comprobante; para archivarlo está {@link RespuestaAutorizacion.Autorizado#comprobante()}.
 *
 * @param claveAcceso       clave de acceso que el servicio devolvió
 * @param fechaAutorizacion fecha en que se autorizó; {@code null} si el servicio no la envió
 * @param tipoComprobante   tipo en texto legible, por ejemplo {@code Nota de Débito};
 *                          {@code null} si el servicio no lo envió
 * @param rucEmisor         RUC del emisor; {@code null} si el servicio no lo envió
 * @param mensajes          mensajes que acompañan a la respuesta
 */
public record ComprobanteConsultado(
    String claveAcceso,
    @Nullable OffsetDateTime fechaAutorizacion,
    @Nullable String tipoComprobante,
    @Nullable String rucEmisor,
    List<Mensaje> mensajes
) {

    public ComprobanteConsultado {
        Objects.requireNonNull(claveAcceso, "claveAcceso");
        mensajes = List.copyOf(mensajes);
    }

    @Override
    public String toString() {
        return claveAcceso + (tipoComprobante == null ? "" : " (" + tipoComprobante + ")");
    }
}
