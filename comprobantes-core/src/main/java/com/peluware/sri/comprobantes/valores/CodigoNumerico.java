package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.interno.Digitos;
import org.jspecify.annotations.Nullable;

/**
 * Código numérico de la clave de acceso: ocho dígitos.
 *
 * <p>Ficha Técnica v2.34, Tabla 1, campo 7. El algoritmo que lo produce es potestad
 * del contribuyente emisor.
 */
public record CodigoNumerico(String valor) {

    public static final int LONGITUD = 8;

    public CodigoNumerico {
        validar(valor).lanzarSiInvalida();
    }

    /**
     * Construye el código numérico rellenando con ceros a la izquierda.
     */
    public static CodigoNumerico of(long valor) {
        return new CodigoNumerico(Digitos.pad(valor, LONGITUD));
    }

    /**
     * Valida una cadena como código numérico sin construirlo.
     */
    public static Validacion validar(@Nullable String valor) {
        return Digitos.validar(valor, LONGITUD, "codigoNumerico");
    }

    /**
     * Indica si {@code valor} es un código numérico válido.
     */
    public static boolean esValido(@Nullable String valor) {
        return validar(valor).esValida();
    }

    @Override
    public String toString() {
        return valor;
    }
}
