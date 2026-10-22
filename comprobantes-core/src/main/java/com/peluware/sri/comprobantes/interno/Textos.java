package com.peluware.sri.comprobantes.interno;

import com.peluware.sri.comprobantes.Validacion;
import org.jspecify.annotations.Nullable;

/**
 * Operaciones sobre campos de texto.
 */
public final class Textos {

    private Textos() {
    }

    /**
     * Comprueba que {@code valor} no esté vacío, no exceda {@code longitudMaxima} caracteres y no
     * contenga saltos de línea.
     */
    public static Validacion validar(@Nullable String valor, int longitudMaxima, String campo) {
        if (valor == null) {
            return new Validacion.CampoInvalido(campo, campo + " no puede ser nulo");
        }
        if (valor.isEmpty()) {
            return new Validacion.CampoInvalido(campo, campo + " no puede estar vacío");
        }
        if (valor.length() > longitudMaxima) {
            return new Validacion.CampoInvalido(campo, campo + " no puede exceder " + longitudMaxima + " caracteres, tiene " + valor.length());
        }
        int salto = valor.indexOf('\n');
        if (salto >= 0) {
            return new Validacion.CampoInvalido(campo, campo + " no admite saltos de línea, se encontró uno en la posición " + salto);
        }
        return Validacion.valida();
    }

    /**
     * Igual que {@link #validar(String, int, String)}, admitiendo {@code null}.
     */
    public static Validacion validarOpcional(@Nullable String valor, int longitudMaxima, String campo) {
        return valor == null ? Validacion.valida() : validar(valor, longitudMaxima, campo);
    }
}
