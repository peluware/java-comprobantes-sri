package com.peluware.sri.comprobantes.liquidacioncompra;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteVentaReader;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.w3c.dom.Element;


import static com.peluware.sri.comprobantes.interno.xml.Dom.text;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee una {@link LiquidacionCompra} de su XML, firmado o sin firmar.
 *
 * <p>Es el espejo de {@link LiquidacionCompraWriter}. Toma los importes tal como vienen, sin
 * derivarlos de la cantidad y el precio. La firma, si la lleva, se ignora.
 */
public final class LiquidacionCompraReader extends ComprobanteVentaReader<LiquidacionCompra, LiquidacionCompra.Builder> {

    /**
     * Única instancia.
     */
    public static final LiquidacionCompraReader INSTANCIA = new LiquidacionCompraReader();

    private LiquidacionCompraReader() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.LIQUIDACION_COMPRA;
    }


    /**
     * Arma la liquidación de compra a partir del elemento raíz ya leído.
     */
    @Override
    protected LiquidacionCompra.Builder builder() {
        return LiquidacionCompra.builder();
    }

    @Override
    protected LiquidacionCompra propio(LiquidacionCompra.Builder builder, Element raiz, Element info) {
        return builder
            .proveedor(proveedor(info))
            .totales(totales(info))
            .build();
    }

    private static Proveedor proveedor(Element info) {
        return new Proveedor(
            TipoIdentificacion.of(textoExigido(info, "tipoIdentificacionProveedor")),
            textoExigido(info, "identificacionProveedor"),
            textoExigido(info, "razonSocialProveedor"),
            text(info, "direccionProveedor")
        );
    }

    private static TotalesLiquidacionCompra totales(Element info) {
        return TotalesLiquidacionCompra.dados(
            Dinero.of(textoExigido(info, "totalSinImpuestos")),
            Dinero.of(textoExigido(info, "totalDescuento")),
            Lecturas.totalConImpuestos(info),
            Dinero.of(textoExigido(info, "importeTotal"))
        );
    }
}
