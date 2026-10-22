package com.peluware.sri.comprobantes.ride.pdfbox;

/**
 * Cuál de las dos partes de un dato va en negrita: el rótulo que lo nombra o el valor.
 *
 * <p>No afecta al nombre del comprobante ni al bloque de RUC y número, que van siempre en
 * negrita.
 */
public enum Enfasis {

    /**
     * El rótulo, como en los modelos del Anexo 2.
     */
    ROTULO,

    /**
     * El valor, dejando el rótulo en la letra corriente.
     */
    VALOR,

    /**
     * Ninguno; todo en la letra corriente y la jerarquía la marcan el color y el tamaño.
     */
    NINGUNO
}
