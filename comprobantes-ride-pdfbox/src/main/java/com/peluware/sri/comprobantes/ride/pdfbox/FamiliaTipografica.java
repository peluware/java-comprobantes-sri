package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

/**
 * FamiliaTipografica tipográfica del RIDE.
 *
 * <p>Solo las base-14 del PDF, que no se incrustan y cuya codificación WinAnsi cubre el
 * español. Una fuente propia haría falta incrustarla, que es otra cosa y pesa en cada
 * documento.
 */
public enum FamiliaTipografica {

    /** Sin serifas. La de los modelos del Anexo 2. */
    HELVETICA(FontName.HELVETICA, FontName.HELVETICA_BOLD),

    /** Con serifas. */
    TIMES(FontName.TIMES_ROMAN, FontName.TIMES_BOLD),

    /** De ancho fijo. */
    COURIER(FontName.COURIER, FontName.COURIER_BOLD);

    private final FontName normal;
    private final FontName negrita;

    FamiliaTipografica(FontName normal, FontName negrita) {
        this.normal = normal;
        this.negrita = negrita;
    }

    FontName normal() {
        return normal;
    }

    FontName negrita() {
        return negrita;
    }
}
