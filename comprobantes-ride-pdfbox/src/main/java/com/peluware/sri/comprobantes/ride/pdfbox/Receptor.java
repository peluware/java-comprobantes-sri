package com.peluware.sri.comprobantes.ride.pdfbox;

import java.io.IOException;
import java.util.Objects;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;
import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.MARGEN;

/**
 * El recuadro de quien recibe el comprobante.
 *
 * <p>Sus dos primeras líneas —razón social e identificación arriba, fecha de emisión debajo— son
 * iguales en cinco de los seis modelos del Anexo 2. Lo que cada comprobante añade después lo pone
 * su propio cuerpo. La guía de remisión no usa este recuadro, porque en vez de un comprador tiene
 * un transportista.
 */
final class Receptor {

    /** Donde empieza la columna derecha, como fracción del ancho útil. */
    private static final float MITAD = 0.62f;

    /** Aire del recuadro, repartido entre arriba y abajo. */
    private static final float AIRE = 12;

    private final EstiloRide estilo;

    Receptor(EstiloRide estilo) {
        this.estilo = Objects.requireNonNull(estilo, "estilo");
    }

    /**
     * Alto del recuadro que contiene estos pares.
     */
    float alto(Pares pares) {
        return pares.alto() + estilo.espacio(AIRE);
    }

    /**
     * Dibuja el recuadro y los pares que lleva dentro.
     *
     * @return la altura a la que queda el pie del recuadro
     */
    float dibujar(Pluma pluma, Pares pares, float arriba) throws IOException {
        float alto = alto(pares);
        pluma.bloque(MARGEN, arriba - alto, ANCHO_UTIL, alto);
        pares.dibujar(pluma, arriba);
        return arriba - alto;
    }

    /** Donde empieza la columna izquierda. */
    static float izquierda() {
        return MARGEN + 6;
    }

    /** Donde empieza la columna derecha. */
    static float mitad() {
        return MARGEN + ANCHO_UTIL * MITAD;
    }

    /** Ancho de un par en la columna izquierda. */
    static float anchoIzquierda() {
        return mitad() - MARGEN - 18;
    }

    /** Ancho de un par en la columna derecha. */
    static float anchoDerecha() {
        return MARGEN + ANCHO_UTIL - mitad() - 6;
    }

    /** Ancho de un par que ocupa el recuadro entero. */
    static float anchoCompleto() {
        return ANCHO_UTIL - 12;
    }
}
