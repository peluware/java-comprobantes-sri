package com.peluware.sri.comprobantes.notacredito;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteNotaReader;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.valores.Cantidad;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.PrecioUnitario;
import org.w3c.dom.Element;


import static com.peluware.sri.comprobantes.interno.xml.Dom.childrenIn;
import static com.peluware.sri.comprobantes.interno.xml.Dom.text;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee una {@link NotaCredito} de su XML, firmado o sin firmar.
 *
 * <p>Es el espejo de {@link NotaCreditoWriter}. Toma los importes tal como vienen, sin derivarlos
 * de la cantidad y el precio. La firma, si la lleva, se ignora.
 */
public final class NotaCreditoReader extends ComprobanteNotaReader<NotaCredito, NotaCredito.Builder> {

    /**
     * Única instancia.
     */
    public static final NotaCreditoReader INSTANCIA = new NotaCreditoReader();

    private NotaCreditoReader() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.NOTA_CREDITO;
    }


    /**
     * Arma la nota de crédito a partir del elemento raíz ya leído.
     */
    @Override
    protected NotaCredito.Builder builder() {
        return NotaCredito.builder();
    }

    @Override
    protected NotaCredito propio(NotaCredito.Builder builder, Element raiz, Element info) {
        builder
            .motivo(textoExigido(info, "motivo"))
            .moneda(textoExigido(info, "moneda"))
            .totales(totales(info));

        for (var detalle : childrenIn(raiz, "detalles", "detalle")) {
            builder.detalle(detalle(detalle));
        }
        return builder.build();
    }

    private static TotalesNotaCredito totales(Element info) {
        return TotalesNotaCredito.dados(
            Dinero.of(textoExigido(info, "totalSinImpuestos")),
            Lecturas.totalConImpuestos(info),
            Dinero.of(textoExigido(info, "valorModificacion"))
        );
    }

    private static DetalleNotaCredito detalle(Element detalle) {
        var builder = DetalleNotaCredito.builder()
            .codigoInterno(text(detalle, "codigoInterno"))
            .codigoAdicional(text(detalle, "codigoAdicional"))
            .descripcion(textoExigido(detalle, "descripcion"))
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
