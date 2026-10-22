import org.jspecify.annotations.NullMarked;

/**
 * Generación del RIDE en PDF sobre Apache PDFBox.
 *
 * <p>Ningún parámetro, retorno ni campo admite {@code null} salvo los marcados con
 * {@code @Nullable}.
 */
@NullMarked
module com.peluware.sri.comprobantes.ride.pdfbox {
    requires com.peluware.sri.comprobantes;
    requires com.peluware.sri.comprobantes.ride;
    requires org.apache.pdfbox;
    requires static org.jspecify;

    exports com.peluware.sri.comprobantes.ride.pdfbox;
}
