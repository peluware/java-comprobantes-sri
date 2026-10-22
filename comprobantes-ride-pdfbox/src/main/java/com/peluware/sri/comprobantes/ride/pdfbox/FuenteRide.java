package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;

/**
 * Carga una fuente dentro del documento que la va a usar.
 *
 * <p>Es una fábrica por la misma razón que {@link LogoRide}: una fuente incrustada queda atada
 * a su {@link PDDocument}, y el documento lo crea el generador.
 *
 * <pre>{@code
 * doc -> PDType0Font.load(doc, new ByteArrayInputStream(ttf))
 * doc -> PDType0Font.load(doc, archivo)
 * }</pre>
 */
@FunctionalInterface
public interface FuenteRide {

    /**
     * Carga la fuente en {@code documento}.
     *
     * @throws IOException si la fuente no se puede leer o incrustar
     */
    PDFont cargar(PDDocument documento) throws IOException;
}
