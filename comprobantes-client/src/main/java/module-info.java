import org.jspecify.annotations.NullMarked;

/**
 * Clientes de los servicios web de recepción y autorización de comprobantes del SRI.
 *
 * <p>Depende únicamente del JDK y del módulo core. Ningún parámetro, retorno ni campo admite
 * {@code null} salvo los marcados con {@code @Nullable}.
 */
@NullMarked
module com.peluware.sri.comprobantes.client {
    requires com.peluware.sri.comprobantes;
    requires java.net.http;
    requires java.xml;
    requires static org.jspecify;

    exports com.peluware.sri.comprobantes.client;
}
