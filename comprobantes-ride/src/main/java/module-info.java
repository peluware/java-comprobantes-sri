import org.jspecify.annotations.NullMarked;

/**
 * Modelo de la representación impresa de comprobantes electrónicos del SRI.
 *
 * <p>Depende únicamente del JDK y del módulo core. Ningún parámetro, retorno ni campo admite
 * {@code null} salvo los marcados con {@code @Nullable}.
 */
@NullMarked
module com.peluware.sri.comprobantes.ride {
    requires com.peluware.sri.comprobantes;
    requires static org.jspecify;

    exports com.peluware.sri.comprobantes.ride;
}
