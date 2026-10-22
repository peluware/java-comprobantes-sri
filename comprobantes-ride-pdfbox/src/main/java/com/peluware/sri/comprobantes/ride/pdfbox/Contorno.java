package com.peluware.sri.comprobantes.ride.pdfbox;

/**
 * Cómo se delimita cada bloque del RIDE: la cabecera, el recuadro del receptor, la tabla de
 * detalles, los totales, la información adicional y las formas de pago.
 *
 * <p>Ficha Técnica v2.34, numeral 9.19: los modelos del Anexo 2 fijan qué campos van y en qué
 * posición, no con qué se enmarcan.
 */
public enum Contorno {

    /**
     * Un recuadro alrededor de cada bloque, como en los modelos del Anexo 2. Es el único valor
     * al que {@link EstiloRide#radioBordes()} aplica.
     */
    CAJA,

    /**
     * Sin recuadros: una regla bajo cada bloque, que lo separa del siguiente.
     */
    REGLA,

    /**
     * Sin recuadros ni reglas; los bloques se separan solo por el aire entre ellos.
     */
    NINGUNO
}
