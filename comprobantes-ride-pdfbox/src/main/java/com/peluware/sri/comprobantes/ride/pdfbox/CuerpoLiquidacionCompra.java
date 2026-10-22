package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Contraparte;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompra;
import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * El cuerpo del RIDE de una liquidación de compra de bienes y prestación de servicios.
 *
 * <p>Anexo 2, página 65. Solo declara su contraparte: su esquema no lleva ni guía de remisión ni
 * propina, y por eso no sobreescribe esos dos huecos.
 */
final class CuerpoLiquidacionCompra extends CuerpoVenta<LiquidacionCompra> {

    CuerpoLiquidacionCompra(LiquidacionCompra liquidacion, PDFont normal, PDFont negrita, EstiloRide estilo) {
        super(liquidacion, normal, negrita, estilo);
    }

    @Override
    protected Contraparte contraparte() {
        return comprobante.info().proveedor();
    }
}
