package com.peluware.sri.comprobantes.catalogo;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Tarifa de IVA: el par de campos {@code <codigoPorcentaje>} y {@code <tarifa>} de un impuesto
 * con {@link CodigoImpuesto#IVA}.
 *
 * <p>Ficha Técnica v2.34, Tabla 17. Las tarifas de la tabla están como constantes, incluidas
 * las derogadas, que aparecen en comprobantes autorizados con anterioridad.
 *
 * <p>El esquema no acota los códigos: {@code codigoPorcentaje} admite de uno a cuatro dígitos
 * y {@code tarifa} cuatro dígitos con dos decimales. Una tarifa que no esté en la tabla, como
 * el IVA diferenciado del código 8 o una que el SRI añada, se construye con el constructor.
 */
public record TarifaIva(String codigo, BigDecimal tarifa) {

    public static final int CODIGO_LONGITUD_MAXIMA = 4;
    public static final int TARIFA_DECIMALES = 2;
    public static final int TARIFA_DIGITOS_TOTALES = 4;

    public static final TarifaIva CERO = de("0", "0.00");
    public static final TarifaIva CINCO = de("5", "5.00");
    public static final TarifaIva DOCE = de("2", "12.00");
    public static final TarifaIva TRECE = de("10", "13.00");
    public static final TarifaIva CATORCE = de("3", "14.00");
    public static final TarifaIva QUINCE = de("4", "15.00");

    /** Bienes y servicios no gravados con IVA. */
    public static final TarifaIva NO_OBJETO = de("6", "0.00");

    /** Bienes y servicios gravados con tarifa cero por exención expresa. */
    public static final TarifaIva EXENTO = de("7", "0.00");

    /** Código del IVA diferenciado, cuya tarifa la determina la normativa aplicable al bien. */
    public static final String CODIGO_DIFERENCIADO = "8";

    private static final List<TarifaIva> VALORES = List.of(
        CERO,
        CINCO,
        DOCE,
        TRECE,
        CATORCE,
        QUINCE,
        NO_OBJETO,
        EXENTO
    );

    public TarifaIva {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(tarifa, "tarifa");
        if (codigo.isEmpty() || codigo.length() > CODIGO_LONGITUD_MAXIMA || !codigo.chars().allMatch(Character::isDigit)) {
            throw new ValidacionException(new Validacion.CampoInvalido("codigoPorcentaje", "codigoPorcentaje admite de 1 a " + CODIGO_LONGITUD_MAXIMA + " dígitos: " + codigo));
        }
        if (tarifa.signum() < 0 || tarifa.scale() > TARIFA_DECIMALES || tarifa.precision() - tarifa.scale() > TARIFA_DIGITOS_TOTALES - TARIFA_DECIMALES) {
            throw new ValidacionException(new Validacion.CampoInvalido("tarifa", "tarifa admite hasta " + (TARIFA_DIGITOS_TOTALES - TARIFA_DECIMALES) + " dígitos enteros y " + TARIFA_DECIMALES + " decimales, sin signo: " + tarifa.toPlainString()));
        }
        tarifa = tarifa.setScale(TARIFA_DECIMALES, RoundingMode.UNNECESSARY);
    }

    /**
     * Construye una tarifa a partir de su porcentaje en texto.
     */
    public static TarifaIva de(String codigo, String tarifa) {
        return new TarifaIva(codigo, new BigDecimal(tarifa));
    }

    /**
     * Busca una tarifa de la Tabla 17 por su código.
     *
     * @throws CatalogoException si el código no pertenece a la Tabla 17, incluido
     *                           {@link #CODIGO_DIFERENCIADO}, que no tiene porcentaje fijo
     */
    public static TarifaIva of(String codigo) {
        Objects.requireNonNull(codigo, "codigo");
        for (var conocida : VALORES) {
            if (conocida.codigo.equals(codigo)) {
                return conocida;
            }
        }
        throw new CatalogoException("Código de tarifa de IVA desconocido: " + codigo);
    }

    /**
     * Tarifas que enumera la Tabla 17.
     */
    public static List<TarifaIva> valores() {
        return VALORES;
    }

    /**
     * Indica si la tarifa es una de las que enumera la Tabla 17.
     */
    public boolean esConocida() {
        return VALORES.contains(this);
    }
}
