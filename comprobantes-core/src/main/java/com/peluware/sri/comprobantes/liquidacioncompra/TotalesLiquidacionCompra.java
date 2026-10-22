package com.peluware.sri.comprobantes.liquidacioncompra;

import com.peluware.sri.comprobantes.Detalle;
import com.peluware.sri.comprobantes.TotalImpuesto;
import com.peluware.sri.comprobantes.TotalesComprobanteVenta;
import com.peluware.sri.comprobantes.Totalizacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.util.List;

/**
 * Campos que totalizan la liquidación de compra.
 *
 * <p>Son los cuatro de {@link TotalesComprobanteVenta} sin añadir ninguno: el importe total suma
 * el subtotal y los impuestos, porque su esquema no declara propina.
 *
 * <p>Solo se obtiene con {@link #de(List)}, que lo calcula a partir de los detalles.
 */
public final class TotalesLiquidacionCompra extends TotalesComprobanteVenta {

    private TotalesLiquidacionCompra(Dinero totalSinImpuestos, Dinero totalDescuento, List<TotalImpuesto> totalConImpuestos, Dinero importeTotal) {
        super(totalSinImpuestos, totalDescuento, totalConImpuestos, importeTotal);
    }

    /**
     * Totales tal como los trae el XML, sin recalcularlos desde los detalles.
     */
    static TotalesLiquidacionCompra dados(Dinero totalSinImpuestos, Dinero totalDescuento, List<TotalImpuesto> totalConImpuestos, Dinero importeTotal) {
        return new TotalesLiquidacionCompra(totalSinImpuestos, totalDescuento, totalConImpuestos, importeTotal);
    }

    /**
     * Calcula los totales de un conjunto de detalles.
     *
     * @throws ValidacionException si los detalles están vacíos o no totalizan ningún impuesto
     */
    public static TotalesLiquidacionCompra de(List<? extends Detalle> detalles) {
        var totalizacion = Totalizacion.de(detalles, "La liquidación de compra");

        return new TotalesLiquidacionCompra(totalizacion.totalSinImpuestos(), totalizacion.totalDescuento(), totalizacion.totalConImpuestos(), totalizacion.conImpuestos());
    }
}
