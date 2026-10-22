package com.peluware.sri.comprobantes.liquidacioncompra;

import com.peluware.sri.comprobantes.InfoComprobanteVenta;
import com.peluware.sri.comprobantes.Pago;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Bloque {@code <infoLiquidacionCompra>}.
 *
 * <p>Añade a {@link InfoComprobanteVenta} el proveedor por cuenta del cual se emite, y nada más:
 * su esquema no declara propina ni guía de remisión.
 *
 * <p>Se obtiene de {@link LiquidacionCompra#info()}. No es un {@code record} para que el esquema
 * pueda ganar campos sin cambiar ninguna firma pública: su constructor no sale del paquete.
 */
public final class InfoLiquidacionCompra extends InfoComprobanteVenta {

    private final Proveedor proveedor;
    private final TotalesLiquidacionCompra totales;

    InfoLiquidacionCompra(LocalDate fechaEmision, Proveedor proveedor, TotalesLiquidacionCompra totales, String moneda, List<Pago> pagos) {
        super(fechaEmision, totales, moneda, pagos);
        this.proveedor = Objects.requireNonNull(proveedor, "proveedor");
        this.totales = totales;
    }

    /**
     * Proveedor por cuenta del cual se emite.
     */
    public Proveedor proveedor() {
        return proveedor;
    }

    /**
     * Campos que totalizan la liquidación.
     */
    @Override
    public TotalesLiquidacionCompra totales() {
        return totales;
    }
}
