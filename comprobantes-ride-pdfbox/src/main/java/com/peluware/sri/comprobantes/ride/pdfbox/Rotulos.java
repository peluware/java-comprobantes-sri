package com.peluware.sri.comprobantes.ride.pdfbox;

/**
 * Cómo se escriben los rótulos que acompañan a cada dato.
 *
 * <p>No afecta a los valores, ni al nombre del comprobante, ni a los rótulos de las columnas de
 * la tabla, que el Anexo 2 ya escribe en caja mixta.
 */
public enum Rotulos {

    /**
     * Tal como los escribe el Anexo 2, con buena parte en mayúsculas.
     */
    ANEXO_2,

    /**
     * Con mayúscula solo en la inicial. Las siglas se respetan, de modo que «IVA 15%» no pasa a
     * «Iva 15%».
     */
    FRASE
}
