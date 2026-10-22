package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.interno.Decimales;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * Cantidad de un detalle: hasta seis decimales.
 *
 * <p>Ficha Técnica v2.34, numeral 9.17 y Anexo 3: los campos {@code cantidad} y
 * {@code precioUnitario} admiten hasta seis decimales desde la versión 1.1.0 del comprobante.
 * El esquema los restringe además a dieciocho dígitos en total.
 */
public record Cantidad(BigDecimal valor) implements Comparable<Cantidad> {

    public static final int DECIMALES = 6;
    public static final int DIGITOS_TOTALES = 18;

    public Cantidad {
        validar(valor).lanzarSiInvalida();
        valor = Decimales.normalizar(valor, DECIMALES);
    }

    public static Cantidad of(String valor) {
        return new Cantidad(new BigDecimal(valor));
    }

    public static Cantidad of(long valor) {
        return new Cantidad(BigDecimal.valueOf(valor));
    }

    /**
     * Valida un decimal como cantidad sin construirla.
     */
    public static Validacion validar(@Nullable BigDecimal valor) {
        return Decimales.validar(valor, DECIMALES, DIGITOS_TOTALES, "cantidad");
    }

    @Override
    public int compareTo(Cantidad otra) {
        return valor.compareTo(otra.valor);
    }

    @Override
    public String toString() {
        return valor.toPlainString();
    }
}
