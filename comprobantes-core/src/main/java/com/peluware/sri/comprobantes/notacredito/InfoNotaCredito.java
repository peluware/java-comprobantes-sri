package com.peluware.sri.comprobantes.notacredito;

import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.InfoNota;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.interno.Textos;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bloque {@code <infoNotaCredito>}.
 *
 * <p>Añade a {@link InfoNota} el motivo de la corrección, en texto, la moneda y los totales. No
 * lleva formas de pago: una nota de crédito no se paga, se aplica contra lo que se debe.
 *
 * <p>Se obtiene de {@link NotaCredito#info()}. No es un {@code record} para que el esquema
 * pueda ganar campos sin cambiar ninguna firma pública: su constructor no sale del paquete.
 */
public final class InfoNotaCredito extends InfoNota {

    public static final int MOTIVO_LONGITUD_MAXIMA = 300;

    private final TotalesNotaCredito totales;
    private final String moneda;
    private final String motivo;

    InfoNotaCredito(LocalDate fechaEmision, Comprador comprador, @Nullable String rise, DocumentoModificado documentoModificado, TotalesNotaCredito totales, String moneda, String motivo) {
        super(fechaEmision, comprador, rise, documentoModificado);
        Objects.requireNonNull(totales, "totales");
        Textos.validar(moneda, Sri.MONEDA_LONGITUD_MAXIMA, "moneda").lanzarSiInvalida();
        Textos.validar(motivo, MOTIVO_LONGITUD_MAXIMA, "motivo").lanzarSiInvalida();
        if (motivo.indexOf('\n') >= 0) {
            throw new ValidacionException(new Validacion.CampoInvalido("motivo", "El esquema no admite saltos de línea en motivo"));
        }

        this.totales = totales;
        this.moneda = moneda;
        this.motivo = motivo;
    }

    /**
     * Campos que totalizan la nota de crédito.
     */
    public TotalesNotaCredito totales() {
        return totales;
    }

    /**
     * Moneda en que se expresan los valores, tag {@code <moneda>}.
     */
    public String moneda() {
        return moneda;
    }

    /**
     * Razón por la que se emite, tag {@code <motivo>}.
     */
    public String motivo() {
        return motivo;
    }
}
