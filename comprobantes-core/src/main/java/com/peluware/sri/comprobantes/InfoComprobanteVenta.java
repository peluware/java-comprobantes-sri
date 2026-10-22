package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.factura.InfoFactura;
import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.liquidacioncompra.InfoLiquidacionCompra;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Lo que los bloques {@code <infoFactura>} e {@code <infoLiquidacionCompra>} declaran igual.
 *
 * <p>La fecha de emisión, la moneda y las formas de pago, más las dos reglas que los dos
 * esquemas comparten: la longitud de la moneda y que las formas de pago, si se declaran, sumen
 * el importe total.
 *
 * <p>Los totales no se guardan aquí: el constructor los recibe para validar los pagos y cada
 * subtipo guarda el suyo, de modo que {@link InfoFactura#totales()} devuelva su tipo sin
 * necesidad de un cast.
 */
public abstract sealed class InfoComprobanteVenta permits InfoFactura, InfoLiquidacionCompra {

    private final LocalDate fechaEmision;
    private final String moneda;
    private final List<Pago> pagos;

    /**
     * @throws ValidacionException si la moneda no cabe o las formas de pago no suman el importe
     */
    protected InfoComprobanteVenta(LocalDate fechaEmision, TotalesComprobanteVenta totales, String moneda, List<Pago> pagos) {
        Objects.requireNonNull(fechaEmision, "fechaEmision");
        Objects.requireNonNull(totales, "totales");
        Textos.validar(moneda, Sri.MONEDA_LONGITUD_MAXIMA, "moneda").lanzarSiInvalida();

        var copia = List.copyOf(pagos);
        if (!copia.isEmpty()) {
            var pagado = Dinero.CERO;
            for (var pago : copia) {
                pagado = pagado.mas(pago.total());
            }
            if (!pagado.equals(totales.importeTotal())) {
                throw new ValidacionException(new Validacion.ReglaInvalida("Las formas de pago suman " + pagado + " y el importe total es " + totales.importeTotal()));
            }
        }

        this.fechaEmision = fechaEmision;
        this.moneda = moneda;
        this.pagos = copia;
    }

    /**
     * Fecha de emisión, tag {@code <fechaEmision>}.
     */
    public final LocalDate fechaEmision() {
        return fechaEmision;
    }

    /**
     * Moneda en que se expresan los valores, tag {@code <moneda>}.
     */
    public final String moneda() {
        return moneda;
    }

    /**
     * Formas de pago; vacío si el comprobante no las detalla.
     */
    public final List<Pago> pagos() {
        return pagos;
    }

    /**
     * Campos que totalizan el comprobante. Cada subtipo lo estrecha a su propio tipo.
     */
    public abstract TotalesComprobanteVenta totales();
}
