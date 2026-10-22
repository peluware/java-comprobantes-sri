package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.interno.Digitos;
import org.jspecify.annotations.Nullable;

/**
 * Secuencial del comprobante: nueve dígitos.
 *
 * <p>Su unicidad por RUC, codigoDocumento, establecimiento y punto de emisión es responsabilidad
 * de quien llama. Esta librería no asigna secuenciales ni mantiene estado.
 */
public record Secuencial(String valor) {

    public static final int LONGITUD = 9;

    public Secuencial {
        validar(valor).lanzarSiInvalida();
    }

    /**
     * Construye el secuencial rellenando con ceros a la izquierda.
     */
    public static Secuencial of(long valor) {
        return new Secuencial(Digitos.pad(valor, LONGITUD));
    }

    /**
     * Valida una cadena como secuencial sin construirlo.
     */
    public static Validacion validar(@Nullable String valor) {
        return Digitos.validar(valor, LONGITUD, "secuencial");
    }

    /**
     * Indica si {@code valor} es un secuencial válido.
     */
    public static boolean esValido(@Nullable String valor) {
        return validar(valor).esValida();
    }

    @Override
    public String toString() {
        return valor;
    }
}
