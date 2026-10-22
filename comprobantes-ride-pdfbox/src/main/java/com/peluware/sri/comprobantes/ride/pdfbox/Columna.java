package com.peluware.sri.comprobantes.ride.pdfbox;

import java.util.function.Function;

/**
 * Una columna de una tabla del RIDE: cómo se rotula, cuánto ocupa y de dónde sale su contenido.
 *
 * @param <R>              fila de la que lee
 * @param rotulo           rótulo que encabeza la columna
 * @param ancho            ancho en puntos; cero para la columna que recibe el sobrante
 * @param alineadaDerecha  si el contenido de las celdas se alinea a la derecha
 * @param opcional         si la columna se omite cuando ninguna fila la llena
 * @param valor            contenido de la celda; cadena vacía cuando la fila no trae ese dato
 */
record Columna<R>(String rotulo, float ancho, boolean alineadaDerecha, boolean opcional, Function<R, String> valor) {

    /**
     * Columna que recibe el ancho que sobre tras repartir las demás.
     */
    static <R> Columna<R> elastica(String rotulo, Function<R, String> valor) {
        return new Columna<>(rotulo, 0, false, false, valor);
    }

    /**
     * Columna de ancho fijo, alineada a la izquierda.
     */
    static <R> Columna<R> de(String rotulo, float ancho, Function<R, String> valor) {
        return new Columna<>(rotulo, ancho, false, false, valor);
    }

    /**
     * Columna de ancho fijo, alineada a la derecha.
     */
    static <R> Columna<R> derecha(String rotulo, float ancho, Function<R, String> valor) {
        return new Columna<>(rotulo, ancho, true, false, valor);
    }

    /**
     * Columna de ancho fijo que se omite cuando ninguna fila la llena.
     */
    static <R> Columna<R> opcional(String rotulo, float ancho, Function<R, String> valor) {
        return new Columna<>(rotulo, ancho, false, true, valor);
    }
}
