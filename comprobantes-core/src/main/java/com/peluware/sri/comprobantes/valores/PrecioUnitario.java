package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.interno.Decimales;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Precio unitario de un detalle: hasta seis decimales.
 *
 * <p>Ficha Técnica v2.34, numeral 9.17 y Anexo 3. El esquema lo restringe además a dieciocho
 * dígitos en total.
 */
public record PrecioUnitario(BigDecimal valor) implements Comparable<PrecioUnitario> {

    public static final int DECIMALES = 6;
    public static final int DIGITOS_TOTALES = 18;

    public PrecioUnitario {
        validar(valor).lanzarSiInvalida();
        valor = Decimales.normalizar(valor, DECIMALES);
    }

    public static PrecioUnitario of(String valor) {
        return new PrecioUnitario(new BigDecimal(valor));
    }

    public static PrecioUnitario of(long valor) {
        return new PrecioUnitario(BigDecimal.valueOf(valor));
    }

    /**
     * Valida un decimal como precio unitario sin construirlo.
     */
    public static Validacion validar(@Nullable BigDecimal valor) {
        return Decimales.validar(valor, DECIMALES, DIGITOS_TOTALES, "precioUnitario");
    }

    @Override
    public int compareTo(PrecioUnitario otro) {
        return valor.compareTo(otro.valor);
    }

    @Override
    public String toString() {
        return valor.toPlainString();
    }
}
