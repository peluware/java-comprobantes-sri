package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;

/**
 * Crea la imagen del logo dentro del documento que la va a llevar.
 *
 * <p>Es una fábrica y no una imagen ya hecha porque un {@link PDImageXObject} queda atado a su
 * {@link PDDocument}: su contenido vive en el almacén de ese documento y desaparece al
 * cerrarlo. Reutilizar uno en otro documento no lanza ninguna excepción, escribe el diccionario
 * de la imagen con el stream vacío y produce un RIDE que abre sin logo.
 *
 * <p>Como el documento lo crea el generador, el llamador no lo tiene hasta este momento. A
 * cambio decide cómo se embebe:
 *
 * <pre>{@code
 * doc -> PDImageXObject.createFromByteArray(doc, bytes, "logo")   // PDFBox husmea el formato
 * doc -> JPEGFactory.createFromImage(doc, imagen, 0.92f)          // calidad JPEG a elección
 * doc -> LosslessFactory.createFromImage(doc, imagen)             // sin pérdida
 * doc -> PDImageXObject.createFromFile(ruta, doc)                 // desde disco
 * }</pre>
 *
 * <p>Se invoca una vez por documento generado.
 */
@FunctionalInterface
public interface LogoRide {

    /**
     * Crea la imagen dentro de {@code documento}.
     *
     * @param documento documento que llevará la imagen
     * @throws IOException si la imagen no se puede leer o embeber
     */
    PDImageXObject crear(PDDocument documento) throws IOException;
}
