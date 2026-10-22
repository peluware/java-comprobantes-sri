package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.factura.TotalesFactura;
import com.peluware.sri.comprobantes.liquidacioncompra.TotalesLiquidacionCompra;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.util.List;

/**
 * Campos que totalizan un comprobante de venta.
 *
 * <p>Los cuatro que declaran igual la factura y la liquidación de compra. La factura añade la
 * propina, que su esquema declara y el de la liquidación no.
 *
 * <p>El subtotal viene neto de descuentos: cada detalle descuenta antes de totalizar.
 */
public abstract sealed class TotalesComprobanteVenta permits TotalesFactura, TotalesLiquidacionCompra {

    private final Dinero totalSinImpuestos;
    private final Dinero totalDescuento;
    private final List<TotalImpuesto> totalConImpuestos;
    private final Dinero importeTotal;

    protected TotalesComprobanteVenta(Dinero totalSinImpuestos, Dinero totalDescuento, List<TotalImpuesto> totalConImpuestos, Dinero importeTotal) {
        this.totalSinImpuestos = totalSinImpuestos;
        this.totalDescuento = totalDescuento;
        this.totalConImpuestos = List.copyOf(totalConImpuestos);
        this.importeTotal = importeTotal;
    }

    /**
     * Suma de los precios totales sin impuesto de los detalles.
     */
    public final Dinero totalSinImpuestos() {
        return totalSinImpuestos;
    }

    /**
     * Suma de los descuentos de los detalles.
     */
    public final Dinero totalDescuento() {
        return totalDescuento;
    }

    /**
     * Impuestos agrupados por impuesto y código de porcentaje.
     */
    public final List<TotalImpuesto> totalConImpuestos() {
        return totalConImpuestos;
    }

    /**
     * Total a pagar.
     */
    public final Dinero importeTotal() {
        return importeTotal;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[importeTotal=" + importeTotal + "]";
    }
}
