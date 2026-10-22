package com.peluware.sri.comprobantes.notacredito;

import com.peluware.sri.comprobantes.Detalle;
import com.peluware.sri.comprobantes.TotalImpuesto;
import com.peluware.sri.comprobantes.Totalizacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.util.List;

/**
 * Campos que totalizan una nota de crédito.
 *
 * <p>Su esquema no declara propina, descuento total ni importe total: totaliza con
 * {@code totalSinImpuestos}, los impuestos agrupados y {@code valorModificacion}, que es lo que
 * la nota de crédito modifica del documento sustentado.
 *
 * <p>Solo se obtiene con {@link #de(List)}, que lo calcula a partir de los detalles.
 */
public final class TotalesNotaCredito {

    private final Dinero totalSinImpuestos;
    private final List<TotalImpuesto> totalConImpuestos;
    private final Dinero valorModificacion;

    private TotalesNotaCredito(Dinero totalSinImpuestos, List<TotalImpuesto> totalConImpuestos, Dinero valorModificacion) {
        this.totalSinImpuestos = totalSinImpuestos;
        this.totalConImpuestos = List.copyOf(totalConImpuestos);
        this.valorModificacion = valorModificacion;
    }

    /**
     * Totales tal como los trae el XML, sin recalcularlos desde los detalles.
     */
    static TotalesNotaCredito dados(Dinero totalSinImpuestos, List<TotalImpuesto> totalConImpuestos, Dinero valorModificacion) {
        return new TotalesNotaCredito(totalSinImpuestos, List.copyOf(totalConImpuestos), valorModificacion);
    }

    public static TotalesNotaCredito de(List<? extends Detalle> detalles) {
        var totalizacion = Totalizacion.de(detalles, "La nota de crédito");

        return new TotalesNotaCredito(totalizacion.totalSinImpuestos(), totalizacion.totalConImpuestos(), totalizacion.conImpuestos());
    }

    /**
     * Suma de los precios totales sin impuesto de los detalles.
     */
    public Dinero totalSinImpuestos() {
        return totalSinImpuestos;
    }

    /**
     * Impuestos agrupados por impuesto y código de porcentaje.
     */
    public List<TotalImpuesto> totalConImpuestos() {
        return totalConImpuestos;
    }

    /**
     * Valor que la nota de crédito modifica del documento sustentado: el total sin impuestos
     * más los impuestos.
     */
    public Dinero valorModificacion() {
        return valorModificacion;
    }

    @Override
    public String toString() {
        return "TotalesNotaCredito[valorModificacion=" + valorModificacion + "]";
    }
}
