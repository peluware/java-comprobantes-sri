package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.factura.FacturaReader;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompraReader;
import com.peluware.sri.comprobantes.valores.Cantidad;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.PrecioUnitario;
import org.w3c.dom.Element;

import static com.peluware.sri.comprobantes.interno.xml.Dom.childrenIn;
import static com.peluware.sri.comprobantes.interno.xml.Dom.text;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee un comprobante de venta de su XML.
 *
 * <p>Es el espejo de {@link ComprobanteVentaWriter}. La factura y la liquidación de compra
 * declaran su bloque propio igual salvo en la contraparte, lo que la factura añade y el tipo de
 * los totales, así que esta clase rellena todo lo demás y devuelve el builder para que cada
 * subtipo ponga lo suyo y lo construya.
 *
 * @param <C> comprobante de venta que lee
 */
public abstract sealed class ComprobanteVentaReader<C extends ComprobanteVenta, B extends ComprobanteVenta.Builder<B>> extends ComprobanteReader<C, B> permits FacturaReader, LiquidacionCompraReader {

    protected ComprobanteVentaReader() {
    }

    /**
     * Rellena lo que los dos esquemas declaran igual, incluidas las líneas y las formas de pago.
     *
     * <p>Es {@code final}: la base la invoca siempre y una hoja no puede saltársela ni cambiarla.
     */
    @Override
    protected final void familia(B builder, Element raiz, Element info) {
        builder
            .fechaEmision(Lecturas.fecha(textoExigido(info, "fechaEmision"), "fechaEmision"))
            .moneda(textoExigido(info, "moneda"));

        for (var pago : Lecturas.pagos(info)) {
            builder.pago(pago);
        }
        for (var detalle : childrenIn(raiz, "detalles", "detalle")) {
            builder.detalle(detalle(detalle));
        }
    }

    /**
     * Una línea, que los dos esquemas declaran con los mismos nombres.
     */
    private static DetalleComprobanteVenta detalle(Element detalle) {
        var builder = DetalleComprobanteVenta.builder()
            .codigoPrincipal(text(detalle, "codigoPrincipal"))
            .codigoAuxiliar(text(detalle, "codigoAuxiliar"))
            .descripcion(textoExigido(detalle, "descripcion"))
            .unidadMedida(text(detalle, "unidadMedida"))
            .cantidad(Cantidad.of(textoExigido(detalle, "cantidad")))
            .precioUnitario(PrecioUnitario.of(textoExigido(detalle, "precioUnitario")))
            .descuento(Dinero.of(textoExigido(detalle, "descuento")));

        for (var adicional : Lecturas.detallesAdicionales(detalle)) {
            builder.detalleAdicional(adicional.nombre(), adicional.valor());
        }
        return builder
            .valores(Dinero.of(textoExigido(detalle, "precioTotalSinImpuesto")), Lecturas.impuestos(detalle))
            .build();
    }
}
