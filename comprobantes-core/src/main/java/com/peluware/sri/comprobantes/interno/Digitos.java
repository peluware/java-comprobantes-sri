package com.peluware.sri.comprobantes.interno;

import com.peluware.sri.comprobantes.Validacion;
import org.jspecify.annotations.Nullable;

/**
 * Operaciones sobre campos numéricos.
 */
public final class Digitos {

    private Digitos() {
    }

    /**
     * Comprueba que {@code valor} tenga exactamente {@code length} dígitos.
     */
    public static Validacion validar(@Nullable String valor, int length, String campo) {
        return validar(valor, length, length, campo);
    }

    /**
     * Comprueba que {@code valor} tenga entre {@code longitudMinima} y {@code longitudMaxima} dígitos.
     */
    public static Validacion validar(@Nullable String valor, int longitudMinima, int longitudMaxima, String campo) {
        if (valor == null) {
            return new Validacion.CampoInvalido(campo, campo + " no puede ser nulo");
        }
        if (valor.length() < longitudMinima || valor.length() > longitudMaxima) {
            return new Validacion.CampoInvalido(campo, campo + " debe tener " + longitudEsperada(longitudMinima, longitudMaxima) + ", tiene " + valor.length() + ": " + valor);
        }
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c < '0' || c > '9') {
                return new Validacion.CampoInvalido(campo, campo + " solo admite dígitos, se encontró " + c + " en la posición " + i);
            }
        }
        return Validacion.valida();
    }

    private static String longitudEsperada(int longitudMinima, int longitudMaxima) {
        return longitudMinima == longitudMaxima ? "exactamente " + longitudMinima + " dígitos" : "entre " + longitudMinima + " y " + longitudMaxima + " dígitos";
    }

    /**
     * Representación decimal de {@code valor} rellenada con ceros a la izquierda hasta
     * {@code length} dígitos.
     *
     * <p>Si el valor es negativo o no cabe en {@code length} dígitos devuelve su representación
     * sin rellenar, que la validación del campo rechaza.
     */
    public static String pad(long valor, int length) {
        var s = Long.toString(valor);
        if (valor < 0 || s.length() >= length) {
            return s;
        }
        return "0".repeat(length - s.length()) + s;
    }
}
