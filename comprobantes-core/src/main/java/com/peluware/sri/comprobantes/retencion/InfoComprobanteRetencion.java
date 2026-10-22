package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.valores.PeriodoFiscal;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bloque {@code <infoCompRetencion>}.
 *
 * <p>Se obtiene de {@link ComprobanteRetencion#infoCompRetencion()}. No es un {@code record}
 * para que el esquema pueda ganar campos sin cambiar ninguna firma pública: su constructor no
 * sale del paquete.
 */
public final class InfoComprobanteRetencion {

    private final LocalDate fechaEmision;
    private final SujetoRetenido sujetoRetenido;
    private final PeriodoFiscal periodoFiscal;

    InfoComprobanteRetencion(LocalDate fechaEmision, SujetoRetenido sujetoRetenido, PeriodoFiscal periodoFiscal) {
        this.fechaEmision = Objects.requireNonNull(fechaEmision, "fechaEmision");
        this.sujetoRetenido = Objects.requireNonNull(sujetoRetenido, "sujetoRetenido");
        this.periodoFiscal = Objects.requireNonNull(periodoFiscal, "periodoFiscal");
    }

    /**
     * Fecha de emisión del comprobante de retención, tag {@code <fechaEmision>}.
     */
    public LocalDate fechaEmision() {
        return fechaEmision;
    }

    /**
     * A quién se le retiene.
     */
    public SujetoRetenido sujetoRetenido() {
        return sujetoRetenido;
    }

    /**
     * Periodo al que corresponde la retención, tag {@code <periodoFiscal>}.
     */
    public PeriodoFiscal periodoFiscal() {
        return periodoFiscal;
    }
}
