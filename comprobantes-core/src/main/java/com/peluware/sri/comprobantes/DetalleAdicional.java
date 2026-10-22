package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Textos;

/**
 * Dato adicional de un detalle, correspondiente al elemento {@code <detAdicional>}.
 *
 * <p>El esquema admite hasta tres por detalle.
 *
 * @param nombre nombre del dato, atributo {@code nombre}
 * @param valor  contenido, atributo {@code valor}
 */
public record DetalleAdicional(String nombre, String valor) {

    public static final int NOMBRE_LONGITUD_MAXIMA = 300;
    public static final int VALOR_LONGITUD_MAXIMA = 300;

    /** Máximo de datos adicionales por detalle. */
    public static final int MAX_POR_DETALLE = 3;

    public DetalleAdicional {
        Textos.validar(nombre, NOMBRE_LONGITUD_MAXIMA, "detAdicional.nombre").lanzarSiInvalida();
        Textos.validar(valor, VALOR_LONGITUD_MAXIMA, "detAdicional.valor").lanzarSiInvalida();
    }
}
