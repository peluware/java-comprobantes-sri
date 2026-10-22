package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.interno.Decimales;
import com.peluware.sri.comprobantes.interno.Digitos;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Impuesto aplicado a un detalle, correspondiente al bloque {@code <impuesto>}.
 *
 * @param codigo           impuesto de la Tabla 16
 * @param codigoPorcentaje código de tarifa dentro de ese impuesto
 * @param tarifa           porcentaje o tarifa específica, según el impuesto
 * @param baseImponible    base sobre la que se calcula
 * @param valor            importe resultante
 */
public record Impuesto(CodigoImpuesto codigo, String codigoPorcentaje, BigDecimal tarifa, Dinero baseImponible, Dinero valor) {

    public static final int TARIFA_DECIMALES = 2;
    public static final int TARIFA_DIGITOS_TOTALES = 4;
    public static final int CODIGO_PORCENTAJE_LONGITUD_MAXIMA = 4;

    public Impuesto {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(baseImponible, "baseImponible");
        Objects.requireNonNull(valor, "valor");
        Digitos.validar(codigoPorcentaje, 1, CODIGO_PORCENTAJE_LONGITUD_MAXIMA, "codigoPorcentaje").lanzarSiInvalida();
        Decimales.validar(tarifa, TARIFA_DECIMALES, TARIFA_DIGITOS_TOTALES, "tarifa").lanzarSiInvalida();
        tarifa = Decimales.normalizar(tarifa, TARIFA_DECIMALES);
    }

    /**
     * Construye el impuesto calculando el valor como porcentaje de la base.
     *
     * <p>Aplicable a los impuestos cuya tarifa es un porcentaje: IVA e ICE ad valorem. El ICE
     * específico y el IRBPNR se calculan por unidad y requieren el constructor.
     */
    public static Impuesto of(CodigoImpuesto codigo, String codigoPorcentaje, BigDecimal tarifa, Dinero baseImponible) {
        return new Impuesto(codigo, codigoPorcentaje, tarifa, baseImponible, baseImponible.porcentaje(tarifa));
    }

    /**
     * Construye el IVA de una base.
     */
    public static Impuesto iva(TarifaIva tarifa, Dinero baseImponible) {
        return of(CodigoImpuesto.IVA, tarifa.codigo(), tarifa.tarifa(), baseImponible);
    }

    /**
     * Construye un ICE ad valorem de una base.
     */
    public static Impuesto ice(String codigoPorcentaje, BigDecimal tarifa, Dinero baseImponible) {
        return of(CodigoImpuesto.ICE, codigoPorcentaje, tarifa, baseImponible);
    }
}
