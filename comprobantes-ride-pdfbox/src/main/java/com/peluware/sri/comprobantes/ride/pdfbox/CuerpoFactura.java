package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Contraparte;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.jspecify.annotations.Nullable;

/**
 * El cuerpo del RIDE de una factura.
 *
 * <p>Anexo 2, página 60. Añade a {@link CuerpoVenta} lo único que la separa de la liquidación de
 * compra: la guía de remisión junto a la fecha y la propina en los totales.
 */
final class CuerpoFactura extends CuerpoVenta<Factura> {

    CuerpoFactura(Factura factura, PDFont normal, PDFont negrita, EstiloRide estilo) {
        super(factura, normal, negrita, estilo);
    }

    @Override
    protected Contraparte contraparte() {
        return comprobante.info().comprador();
    }

    @Override
    protected @Nullable String guiaRemision() {
        return comprobante.info().guiaRemision();
    }

    @Override
    protected Dinero propina() {
        return comprobante.info().totales().propina();
    }
}
