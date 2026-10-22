import org.jspecify.annotations.NullMarked;

/**
 * Firma XAdES_BES de comprobantes electrónicos del SRI.
 *
 * <p>Depende únicamente del JDK y del módulo core. Ningún parámetro, retorno ni campo admite
 * {@code null} salvo los marcados con {@code @Nullable}.
 */
@NullMarked
module com.peluware.sri.comprobantes.xades {
    requires static org.jspecify;
    requires com.peluware.sri.comprobantes;
    requires java.xml;
    requires java.xml.crypto;

    exports com.peluware.sri.comprobantes.xades;
}
