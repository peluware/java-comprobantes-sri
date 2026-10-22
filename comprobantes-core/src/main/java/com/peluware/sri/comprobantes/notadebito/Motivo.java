package com.peluware.sri.comprobantes.notadebito;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.util.Objects;

/**
 * Motivo de una nota de débito, correspondiente al bloque {@code <motivo>}.
 *
 * @param razon razón por la que se emite
 * @param valor importe que corresponde a esta razón
 */
public record Motivo(String razon, Dinero valor) {

    public static final int RAZON_LONGITUD_MAXIMA = 300;

    public Motivo {
        Objects.requireNonNull(valor, "valor");
        Textos.validar(razon, RAZON_LONGITUD_MAXIMA, "razon").lanzarSiInvalida();
        if (razon.indexOf('\n') >= 0) {
            throw new ValidacionException(new Validacion.CampoInvalido("razon", "El esquema no admite saltos de línea en razon"));
        }
    }

    @Override
    public String toString() {
        return "Motivo[" + razon + ": " + valor + "]";
    }
}
