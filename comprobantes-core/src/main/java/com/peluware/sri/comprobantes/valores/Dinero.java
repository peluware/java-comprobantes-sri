package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.interno.Decimales;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Importe monetario de dos decimales.
 *
 * <p>Ficha Técnica v2.34, numeral 9.17: los campos de valores admiten como máximo dos
 * decimales. El esquema los restringe además a catorce dígitos en total.
 *
 * <p>El constructor rechaza importes negativos y los que traen más de dos decimales; no
 * redondea. Para llevar a dos decimales el resultado de un cálculo, {@link #redondear(BigDecimal)}.
 */
public record Dinero(BigDecimal valor) implements Comparable<Dinero> {

    public static final int DECIMALES = 2;
    public static final int DIGITOS_TOTALES = 14;

    public static final Dinero CERO = new Dinero(BigDecimal.ZERO);

    public Dinero {
        validar(valor).lanzarSiInvalida();
        valor = Decimales.normalizar(valor, DECIMALES);
    }

    public static Dinero of(String valor) {
        return new Dinero(new BigDecimal(valor));
    }

    public static Dinero of(long valor) {
        return new Dinero(BigDecimal.valueOf(valor));
    }

    /**
     * Construye el importe redondeando a dos decimales.
     */
    public static Dinero redondear(BigDecimal valor) {
        return new Dinero(Decimales.redondear(valor, DECIMALES));
    }

    /**
     * Valida un decimal como importe monetario sin construirlo.
     */
    public static Validacion validar(@Nullable BigDecimal valor) {
        return Decimales.validar(valor, DECIMALES, DIGITOS_TOTALES, "importe");
    }

    public Dinero mas(Dinero otro) {
        return new Dinero(valor.add(otro.valor));
    }

    /**
     * Resta otro importe.
     *
     * @throws ValidacionException si el resultado es negativo
     */
    public Dinero menos(Dinero otro) {
        return new Dinero(valor.subtract(otro.valor));
    }

    /**
     * Aplica un porcentaje y redondea a dos decimales.
     */
    public Dinero porcentaje(BigDecimal tarifa) {
        return new Dinero(Decimales.porcentaje(valor, tarifa, DECIMALES));
    }

    public boolean esCero() {
        return valor.signum() == 0;
    }

    @Override
    public int compareTo(Dinero otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor.toPlainString();
    }
}
