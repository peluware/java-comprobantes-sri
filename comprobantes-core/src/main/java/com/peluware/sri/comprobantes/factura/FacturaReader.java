package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteVentaReader;
import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.w3c.dom.Element;


import static com.peluware.sri.comprobantes.interno.xml.Dom.text;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee una {@link Factura} de su XML, firmado o sin firmar.
 *
 * <p>Es el espejo de {@link FacturaWriter}. Toma los importes tal como vienen, sin derivarlos de
 * la cantidad y el precio: un comprobante autorizado por el SRI manda sobre la aritmética de esta
 * librería. La firma, si la lleva, se ignora.
 */
public final class FacturaReader extends ComprobanteVentaReader<Factura, Factura.Builder> {

    /**
     * Única instancia.
     */
    public static final FacturaReader INSTANCIA = new FacturaReader();

    private FacturaReader() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.FACTURA;
    }


    /**
     * Arma la factura a partir del elemento raíz ya leído.
     */
    @Override
    protected Factura.Builder builder() {
        return Factura.builder();
    }

    @Override
    protected Factura propio(Factura.Builder builder, Element raiz, Element info) {
        return builder
            .comprador(comprador(info))
            .guiaRemision(text(info, "guiaRemision"))
            .totales(totales(info))
            .build();
    }

    private static Comprador comprador(Element info) {
        return new Comprador(
            TipoIdentificacion.of(textoExigido(info, "tipoIdentificacionComprador")),
            textoExigido(info, "identificacionComprador"),
            textoExigido(info, "razonSocialComprador"),
            text(info, "direccionComprador")
        );
    }

    private static TotalesFactura totales(Element info) {
        return TotalesFactura.dados(
            Dinero.of(textoExigido(info, "totalSinImpuestos")),
            Dinero.of(textoExigido(info, "totalDescuento")),
            Lecturas.totalConImpuestos(info),
            Dinero.of(textoExigido(info, "propina")),
            Dinero.of(textoExigido(info, "importeTotal"))
        );
    }
}
