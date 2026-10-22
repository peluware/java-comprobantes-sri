package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.InfoComprobanteVenta;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Bloque {@code <infoFactura>}.
 *
 * <p>Añade a {@link InfoComprobanteVenta} el adquirente y la guía de remisión asociada.
 *
 * <p>Se obtiene de {@link Factura#info()}. No es un {@code record} para que el esquema
 * pueda ganar campos sin cambiar ninguna firma pública: su constructor no sale del paquete.
 */
public final class InfoFactura extends InfoComprobanteVenta {

    private static final Pattern GUIA_REMISION = Pattern.compile("[0-9]{3}-[0-9]{3}-[0-9]{9}");

    private final Comprador comprador;
    private final @Nullable String guiaRemision;
    private final TotalesFactura totales;

    InfoFactura(LocalDate fechaEmision, Comprador comprador, @Nullable String guiaRemision, TotalesFactura totales, String moneda, List<Pago> pagos) {
        super(fechaEmision, totales, moneda, pagos);
        Objects.requireNonNull(comprador, "comprador");
        if (guiaRemision != null && !GUIA_REMISION.matcher(guiaRemision).matches()) {
            throw new ValidacionException(new Validacion.CampoInvalido("guiaRemision", "guiaRemision debe tener el formato 000-000-000000000: " + guiaRemision));
        }

        this.comprador = comprador;
        this.guiaRemision = guiaRemision;
        this.totales = totales;
    }

    /**
     * Datos del adquirente.
     */
    public Comprador comprador() {
        return comprador;
    }

    /**
     * Guía de remisión asociada, con el patrón {@code [0-9]{3}-[0-9]{3}-[0-9]{9}};
     * {@code null} si no aplica.
     */
    public @Nullable String guiaRemision() {
        return guiaRemision;
    }

    /**
     * Campos que totalizan la factura, que añaden la propina.
     */
    @Override
    public TotalesFactura totales() {
        return totales;
    }
}
