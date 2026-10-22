package com.peluware.sri.comprobantes.ride.pdfbox;

import java.io.IOException;
import java.util.List;

/**
 * Cómo se reparte en páginas y se dibuja el cuerpo de un RIDE.
 *
 * <p>Es una pieza aparte, y no un nivel de la jerarquía de {@link Cuerpo}, a propósito: cómo se
 * pagina un cuerpo y qué comprobante es son dos cosas distintas que no se agrupan igual. Cinco de
 * los seis modelos se maquetan con {@link ReceptorConTabla} aunque pertenezcan a tres familias
 * fiscales, y la guía de remisión necesita la suya porque repite la estructura entera una vez por
 * destinatario.
 *
 * <p>Al sacarla de la herencia, la jerarquía de {@link Cuerpo} queda libre para decir lo que el
 * comprobante <em>es</em>.
 */
sealed interface Maquetacion permits ReceptorConTabla, PorDestinatario {

    /**
     * Alto de cada fila, en el orden en que se dibujan. Vacío si el cuerpo no se reparte.
     */
    List<Float> altos();

    /**
     * Alto de lo que se repite en cada página, como la fila de rótulos de una tabla. Cero si no
     * se repite nada.
     */
    float altoPorPagina();

    /**
     * Alto del bloque que encabeza el cuerpo, sin el aire que lo separa de lo que sigue.
     */
    float altoPropio();

    /**
     * Dibuja el encabezado en la primera página.
     *
     * @return la altura a la que queda su pie
     */
    float encabezado(Pluma pluma, float arriba) throws IOException;

    /**
     * Dibuja las filas de {@code desde} a {@code hasta}, sin incluir esta última.
     *
     * @return la altura a la que queda el pie de lo dibujado
     */
    float filas(Pluma pluma, float arriba, int desde, int hasta) throws IOException;
}
