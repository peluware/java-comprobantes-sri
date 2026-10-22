package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Decimales;
import com.peluware.sri.comprobantes.interno.Textos;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Plazo concedido en un pago, correspondiente a los tags {@code <plazo>} y
 * {@code <unidadTiempo>}.
 *
 * <p>El esquema declara los dos tags por separado y ambos son opcionales. Un {@link Pago} con
 * plazo escribe los dos; uno sin plazo, ninguno.
 *
 * @param valor        cantidad de unidades de tiempo
 * @param unidadTiempo unidad en que se expresa
 */
public record Plazo(BigDecimal valor, String unidadTiempo) {

    public static final int DECIMALES = 2;
    public static final int DIGITOS_TOTALES = 14;
    public static final int UNIDAD_TIEMPO_LONGITUD_MAXIMA = 10;

    /** Unidad de tiempo de los ejemplos de la Ficha Técnica v2.34. */
    public static final String DIAS = "dias";

    public Plazo {
        Objects.requireNonNull(valor, "valor");
        Decimales.validar(valor, DECIMALES, DIGITOS_TOTALES, "plazo").lanzarSiInvalida();
        Textos.validar(unidadTiempo, UNIDAD_TIEMPO_LONGITUD_MAXIMA, "unidadTiempo").lanzarSiInvalida();
        valor = Decimales.normalizar(valor, DECIMALES);
    }

    /**
     * Plazo expresado en días.
     */
    public static Plazo enDias(int dias) {
        return new Plazo(BigDecimal.valueOf(dias), DIAS);
    }

    @Override
    public String toString() {
        return valor.toPlainString() + " " + unidadTiempo;
    }
}
