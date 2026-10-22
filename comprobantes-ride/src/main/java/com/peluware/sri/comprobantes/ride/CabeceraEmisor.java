package com.peluware.sri.comprobantes.ride;

import com.peluware.sri.comprobantes.Comprobante;
import org.jspecify.annotations.Nullable;

/**
 * Bloque del emisor, arriba a la izquierda del Anexo 2.
 *
 * <p>Ficha Técnica v2.34, Anexo 2 y Tabla 11. El logo es el campo 10 de esa tabla; no viaja en
 * el XML y lo aporta quien genera la representación.
 *
 * @param razonSocial             razón social o nombres y apellidos del emisor
 * @param nombreComercial         nombre comercial; {@code null} si el emisor no lo declara
 * @param direccionMatriz         dirección del establecimiento matriz
 * @param direccionSucursal       dirección del establecimiento emisor; {@code null} si no la
 *                                declara
 * @param contribuyenteEspecial   número de resolución; {@code null} si no lo es
 * @param obligadoContabilidad    si está obligado a llevar contabilidad
 * @param agenteRetencion         número de resolución de agente de retención; {@code null} si
 *                                no lo es
 * @param contribuyenteRimpe      si pertenece al régimen RIMPE
 */
public record CabeceraEmisor(
    String razonSocial,
    @Nullable String nombreComercial,
    String direccionMatriz,
    @Nullable String direccionSucursal,
    @Nullable String contribuyenteEspecial,
    boolean obligadoContabilidad,
    @Nullable String agenteRetencion,
    boolean contribuyenteRimpe
) {

    /**
     * Toma del comprobante los datos del emisor y la dirección del establecimiento.
     */
    public static CabeceraEmisor de(Comprobante comprobante) {
        var emisor = comprobante.emisor();
        var especial = emisor.contribuyenteEspecial();
        return new CabeceraEmisor(
            emisor.razonSocial(),
            emisor.nombreComercial(),
            emisor.direccionMatriz(),
            comprobante.establecimiento().direccion(),
            especial == null ? null : especial.valor(),
            emisor.obligadoContabilidad(),
            emisor.agenteRetencion(),
            emisor.contribuyenteRimpe()
        );
    }
}
