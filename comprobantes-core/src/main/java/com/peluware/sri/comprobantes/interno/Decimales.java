package com.peluware.sri.comprobantes.interno;

import com.peluware.sri.comprobantes.Validacion;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Operaciones sobre campos decimales.
 */
public final class Decimales {

    /**
     * Modo de redondeo aplicado a todo cálculo de la librería.
     *
     * <p>Redondeo comercial: la mitad se aleja del cero.
     */
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private Decimales() {
    }

    /**
     * Comprueba que {@code valor} no sea negativo, no exceda {@code decimales} decimales y quepa en
     * {@code digitosTotales} dígitos contando los decimales.
     */
    public static Validacion validar(@Nullable BigDecimal valor, int decimales, int digitosTotales, String campo) {
        if (valor == null) {
            return new Validacion.CampoInvalido(campo, campo + " no puede ser nulo");
        }
        if (valor.signum() < 0) {
            return new Validacion.CampoInvalido(campo, campo + " no puede ser negativo: " + valor.toPlainString());
        }
        if (valor.scale() > decimales) {
            return new Validacion.CampoInvalido(campo, campo + " admite hasta " + decimales + " decimales, tiene " + valor.scale() + ": " + valor.toPlainString());
        }
        int enteros = valor.precision() - valor.scale();
        int maxEnteros = digitosTotales - decimales;
        if (enteros > maxEnteros) {
            return new Validacion.CampoInvalido(campo, campo + " admite hasta " + maxEnteros + " dígitos enteros, tiene " + enteros + ": " + valor.toPlainString());
        }
        return Validacion.valida();
    }

    /**
     * Lleva {@code valor} a {@code decimales} decimales sin redondear.
     *
     * <p>Rellena con ceros los decimales que falten. Dos importes de igual valor quedan con la
     * misma representación y son iguales según {@code equals}.
     *
     * @throws java.lang.ArithmeticException si {@code valor} tiene más decimales que
     *                                       {@code decimales}
     */
    public static BigDecimal normalizar(BigDecimal valor, int decimales) {
        return valor.setScale(decimales, RoundingMode.UNNECESSARY);
    }

    /**
     * Lleva {@code valor} a {@code decimales} decimales redondeando según {@link #REDONDEO}.
     */
    public static BigDecimal redondear(BigDecimal valor, int decimales) {
        return valor.setScale(decimales, REDONDEO);
    }

    /**
     * Aplica un porcentaje a una base y redondea el resultado a {@code decimales} decimales.
     */
    public static BigDecimal porcentaje(BigDecimal base, BigDecimal tarifa, int decimales) {
        return redondear(base.multiply(tarifa).movePointLeft(2), decimales);
    }
}
