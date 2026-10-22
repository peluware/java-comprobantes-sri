import org.jspecify.annotations.NullMarked;

/**
 * Modelo, catálogos, clave de acceso y writer StAX de comprobantes electrónicos del SRI.
 *
 * <p>Depende únicamente del JDK. Ningún parámetro, retorno ni campo admite {@code null}
 * salvo los marcados con {@code @Nullable}.
 */
@NullMarked
module com.peluware.sri.comprobantes {
    requires java.xml;
    requires static org.jspecify;

    exports com.peluware.sri.comprobantes;
    exports com.peluware.sri.comprobantes.valores;
    exports com.peluware.sri.comprobantes.factura;
    exports com.peluware.sri.comprobantes.guiaremision;
    exports com.peluware.sri.comprobantes.liquidacioncompra;
    exports com.peluware.sri.comprobantes.notacredito;
    exports com.peluware.sri.comprobantes.notadebito;
    exports com.peluware.sri.comprobantes.retencion;
    exports com.peluware.sri.comprobantes.catalogo;
    exports com.peluware.sri.comprobantes.firma;

    // Utilidad de XML sin nada del SRI, compartida con los módulos que también leen XML.
    exports com.peluware.sri.comprobantes.interno.xml to com.peluware.sri.comprobantes.client, com.peluware.sri.comprobantes.xades;
}
