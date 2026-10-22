package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.Detalle;
import com.peluware.sri.comprobantes.TotalImpuesto;
import com.peluware.sri.comprobantes.TotalesComprobanteVenta;
import com.peluware.sri.comprobantes.Totalizacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.util.List;

/**
 * Campos que totalizan la factura.
 *
 * <p>Ficha Técnica v2.34, Tabla 21. Añade a {@link TotalesComprobanteVenta} la propina, que el
 * importe total suma al subtotal y los impuestos.
 *
 * <p>Solo se obtiene con {@link #de(List, Dinero)}, que lo calcula a partir de los detalles.
 */
public final class TotalesFactura extends TotalesComprobanteVenta {

    /**
     * Porcentaje del subtotal que la propina no debe exceder.
     *
     * <p>Ficha Técnica v2.34, Tabla 21, campo 9. Esta librería no lo valida; el esquema solo
     * exige que la propina no sea negativa.
     */
    public static final int PROPINA_MAX_PORCENTAJE_REFERENCIA = 10;

    private final Dinero propina;

    private TotalesFactura(Dinero totalSinImpuestos, Dinero totalDescuento, List<TotalImpuesto> totalConImpuestos, Dinero propina, Dinero importeTotal) {
        super(totalSinImpuestos, totalDescuento, totalConImpuestos, importeTotal);
        this.propina = propina;
    }

    /**
     * Totales tal como los trae el XML, sin recalcularlos desde los detalles.
     */
    static TotalesFactura dados(Dinero totalSinImpuestos, Dinero totalDescuento, List<TotalImpuesto> totalConImpuestos, Dinero propina, Dinero importeTotal) {
        return new TotalesFactura(totalSinImpuestos, totalDescuento, totalConImpuestos, propina, importeTotal);
    }

    /**
     * Calcula los totales de un conjunto de detalles.
     *
     * @throws ValidacionException si los detalles están vacíos o no totalizan ningún impuesto
     */
    public static TotalesFactura de(List<? extends Detalle> detalles, Dinero propina) {
        var totalizacion = Totalizacion.de(detalles, "La factura");

        return new TotalesFactura(totalizacion.totalSinImpuestos(), totalizacion.totalDescuento(), totalizacion.totalConImpuestos(), propina, totalizacion.conImpuestos().mas(propina));
    }

    /**
     * Calcula los totales de un conjunto de detalles, sin propina.
     */
    public static TotalesFactura de(List<? extends Detalle> detalles) {
        return de(detalles, Dinero.CERO);
    }

    /**
     * Propina incluida en el total.
     */
    public Dinero propina() {
        return propina;
    }
}
