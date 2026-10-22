package com.peluware.sri.comprobantes.interno;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;

import java.util.Objects;

/**
 * Dígito verificador de la clave de acceso por el método Módulo 11 con factor de chequeo
 * ponderado 2.
 *
 * <p>Ficha Técnica v2.34, numeral 5.2. Los pesos 2 a 7 se aplican cíclicamente de derecha a
 * izquierda. Un resultado de 11 devuelve {@code 0} y un resultado de 10 devuelve {@code 1}.
 */
public final class DigitoVerificador {

    private static final int[] PESOS = {2, 3, 4, 5, 6, 7};

    private DigitoVerificador() {
    }

    /**
     * Calcula el dígito verificador de una cadena de dígitos.
     *
     * @param digitos los 48 dígitos que preceden al dígito verificador
     * @return el dígito verificador, entre {@code '0'} y {@code '9'}
     * @throws ValidacionException si la cadena está vacía o contiene caracteres no numéricos
     */
    public static char modulo11(String digitos) {
        Objects.requireNonNull(digitos, "digitos");
        if (digitos.isEmpty()) {
            throw new ValidacionException(new Validacion.CampoInvalido("digitos", "La cadena de dígitos no puede estar vacía"));
        }
        int suma = 0;
        for (int i = digitos.length() - 1, peso = 0; i >= 0; i--, peso++) {
            char c = digitos.charAt(i);
            if (c < '0' || c > '9') {
                throw new ValidacionException(new Validacion.CampoInvalido("digitos", "La cadena solo admite dígitos, se encontró " + c + " en la posición " + i));
            }
            suma += (c - '0') * PESOS[peso % PESOS.length];
        }
        int dv = 11 - (suma % 11);
        return switch (dv) {
            case 11 -> '0';
            case 10 -> '1';
            default -> (char) ('0' + dv);
        };
    }
}
