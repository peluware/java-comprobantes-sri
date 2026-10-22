package com.peluware.sri.comprobantes.ride.pdfbox;

/**
 * Qué líneas separan las celdas dentro de las tablas del RIDE: la de detalles, la de totales y
 * la de formas de pago.
 *
 * <p>No afecta al contorno de la tabla, que lo decide {@link Contorno}.
 */
public enum Divisiones {

    /**
     * Horizontales y verticales, como en los modelos del Anexo 2.
     */
    REJILLA,

    /**
     * Solo entre filas.
     */
    HORIZONTALES,

    /**
     * Solo entre columnas.
     */
    VERTICALES,

    /**
     * Solo la raya que separa los rótulos de los datos.
     */
    CABECERA,

    /**
     * Ninguna; las columnas se distinguen por su posición.
     */
    NINGUNA;

    /**
     * Si se traza la raya que separa los rótulos de los datos.
     */
    boolean bajoCabecera() {
        return this != NINGUNA;
    }

    boolean entreFilas() {
        return this == REJILLA || this == HORIZONTALES;
    }

    boolean entreColumnas() {
        return this == REJILLA || this == VERTICALES;
    }
}
