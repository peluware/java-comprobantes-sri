package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;

import java.util.Objects;

/**
 * Identidad de negocio de un comprobante: RUC, tipo de comprobante, serie y secuencial.
 *
 * <p>Es la entrada de la que deriva una {@link EstrategiaCodigoNumerico}.
 */
public record ComprobanteId(Ruc ruc, TipoComprobante tipoComprobante, Serie serie, Secuencial secuencial) {

    public ComprobanteId {
        Objects.requireNonNull(ruc, "ruc");
        Objects.requireNonNull(tipoComprobante, "tipoComprobante");
        Objects.requireNonNull(serie, "serie");
        Objects.requireNonNull(secuencial, "secuencial");
    }

    /** Representación textual estable usada como entrada de derivación. */
    String derivationInput() {
        return ruc.valor() + '|' + tipoComprobante.codigo() + '|' + serie.valor() + '|' + secuencial.valor();
    }
}
