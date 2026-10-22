package com.peluware.sri.comprobantes.ride.pdfbox;

/**
 * Filete que rodea la hoja entera, por fuera de los bloques del comprobante.
 *
 * <p>Ficha Técnica v2.34, numeral 9.19: no es un requisito, cae en los datos adicionales que el
 * contribuyente puede imprimir. Cierra la composición, de modo que el papel que sobra al pie de
 * un comprobante de pocas líneas no se lea como un descuido.
 */
public enum MarcoPagina {

    /**
     * Sin marco.
     */
    NINGUNO,

    /**
     * Un filete.
     */
    FILETE,

    /**
     * Dos filetes, el de fuera más grueso, como en los certificados.
     */
    DOBLE
}
