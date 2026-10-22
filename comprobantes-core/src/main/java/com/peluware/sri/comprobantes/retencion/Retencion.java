package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.interno.Decimales;
import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una retención sobre un documento sustento.
 *
 * <p>El valor retenido se calcula de la base imponible y el porcentaje con {@link #de}.
 *
 * <p>El {@code codigoRetencion} es texto de hasta cinco caracteres: sus valores salen de las
 * Tablas 19 y 20, que el SRI revisa varias veces al año y valida en la autorización.
 *
 * @param codigo           impuesto sobre el que se retiene
 * @param codigoRetencion  código del concepto, de las Tablas 19 y 20
 * @param baseImponible    base sobre la que se retiene
 * @param porcentaje       porcentaje de retención
 * @param valorRetenido    importe retenido, derivado de los dos anteriores
 */
public record Retencion(
    CodigoImpuestoRetencion codigo,
    String codigoRetencion,
    Dinero baseImponible,
    BigDecimal porcentaje,
    Dinero valorRetenido
) {

    public static final int CODIGO_RETENCION_LONGITUD_MAXIMA = 5;

    /**
     * Decimales del porcentaje, como {@code porcentajeRetener} en el esquema.
     */
    public static final int PORCENTAJE_DECIMALES = 2;

    public Retencion {
        Objects.requireNonNull(codigo, "codigo");
        Textos.validar(codigoRetencion, CODIGO_RETENCION_LONGITUD_MAXIMA, "codigoRetencion").lanzarSiInvalida();
        Objects.requireNonNull(baseImponible, "baseImponible");
        Objects.requireNonNull(porcentaje, "porcentaje");
        Objects.requireNonNull(valorRetenido, "valorRetenido");
    }

    /**
     * Calcula la retención a partir de su base y su porcentaje.
     */
    public static Retencion de(CodigoImpuestoRetencion codigo, String codigoRetencion, Dinero baseImponible, BigDecimal porcentaje) {
        return new Retencion(codigo, codigoRetencion, baseImponible, porcentaje, baseImponible.porcentaje(porcentaje));
    }

    /**
     * Calcula la retención a partir de su base y su porcentaje.
     */
    public static Retencion de(CodigoImpuestoRetencion codigo, String codigoRetencion, Dinero baseImponible, String porcentaje) {
        return de(codigo, codigoRetencion, baseImponible, new BigDecimal(porcentaje));
    }

    /**
     * El porcentaje con los decimales que exige el esquema.
     */
    public String porcentajeEscrito() {
        return Decimales.redondear(porcentaje, PORCENTAJE_DECIMALES).toPlainString();
    }
}
