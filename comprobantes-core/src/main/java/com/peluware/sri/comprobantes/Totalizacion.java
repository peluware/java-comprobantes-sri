package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.valores.Dinero;

import java.util.List;

/**
 * Los importes que salen de totalizar las líneas de un comprobante.
 *
 * <p>Los declaran igual la factura, la nota de crédito y la liquidación de compra, que son los
 * tres comprobantes cuyo detalle lleva importes.
 *
 * @param totalSinImpuestos suma de los precios totales sin impuesto de las líneas
 * @param totalDescuento    suma de los descuentos de las líneas
 * @param totalConImpuestos impuestos agrupados por código y porcentaje
 * @param conImpuestos      el total sin impuestos más el valor de cada impuesto
 */
public record Totalizacion(Dinero totalSinImpuestos, Dinero totalDescuento, List<TotalImpuesto> totalConImpuestos, Dinero conImpuestos) {

    public Totalizacion {
        totalConImpuestos = List.copyOf(totalConImpuestos);
    }

    /**
     * Totaliza las líneas.
     *
     * @param comprobante cómo nombrar al comprobante en el mensaje de error
     * @throws ValidacionException si no hay líneas, o si no totalizan ningún impuesto
     */
    public static Totalizacion de(List<? extends Detalle> detalles, String comprobante) {
        if (detalles.isEmpty()) {
            throw new ValidacionException(new Validacion.CampoInvalido("detalles", comprobante + " debe tener al menos un detalle"));
        }

        var totalSinImpuestos = Dinero.CERO;
        var totalDescuento = Dinero.CERO;
        for (var detalle : detalles) {
            totalSinImpuestos = totalSinImpuestos.mas(detalle.precioTotalSinImpuesto());
            totalDescuento = totalDescuento.mas(detalle.descuento());
        }

        var totalConImpuestos = TotalImpuesto.agrupar(detalles);
        if (totalConImpuestos.isEmpty()) {
            throw new ValidacionException(new Validacion.CampoInvalido("totalConImpuestos", "El comprobante debe totalizar al menos un impuesto"));
        }

        var conImpuestos = totalSinImpuestos;
        for (var total : totalConImpuestos) {
            conImpuestos = conImpuestos.mas(total.valor());
        }

        return new Totalizacion(totalSinImpuestos, totalDescuento, totalConImpuestos, conImpuestos);
    }
}
