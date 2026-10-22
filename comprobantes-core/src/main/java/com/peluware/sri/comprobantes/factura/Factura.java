package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.ComprobanteVenta;
import com.peluware.sri.comprobantes.ConfiguracionException;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Factura electrónica.
 *
 * <p>El comprobante de venta que emite quien vende; su contraparte es un {@link Comprador}.
 * Añade a {@link ComprobanteVenta} la propina y la referencia a una guía de remisión.
 *
 * <p>Solo se construye con {@link #builder()}, que calcula la clave de acceso y los totales a
 * partir de los demás campos.
 */
public final class Factura extends ComprobanteVenta {

    private final InfoFactura infoFactura;

    private Factura(Builder builder, InfoFactura infoFactura) {
        super(builder);
        this.infoFactura = infoFactura;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Tipo de comprobante, siempre {@link TipoComprobante#FACTURA}. */
    @Override
    public TipoComprobante tipoComprobante() {
        return TipoComprobante.FACTURA;
    }

    @Override
    public InfoFactura info() {
        return infoFactura;
    }

    /**
     * Construye una factura calculando la clave de acceso y los totales.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends ComprobanteVenta.Builder<Builder> {

        private @Nullable Comprador comprador;
        private @Nullable String guiaRemision;
        private Dinero propina = Dinero.CERO;
        private @Nullable TotalesFactura totales;

        private Builder() {
        }

        @Override
        protected TipoComprobante tipoComprobante() {
            return TipoComprobante.FACTURA;
        }

        public Builder comprador(Comprador comprador) {
            this.comprador = comprador;
            return this;
        }

        public Builder guiaRemision(@Nullable String guiaRemision) {
            this.guiaRemision = guiaRemision;
            return this;
        }

        /**
         * Totales tal como los trae el XML, en vez de derivarlos de los detalles.
         */
        Builder totales(TotalesFactura totales) {
            this.totales = totales;
            return this;
        }

        /**
         * Por omisión, cero.
         */
        public Builder propina(Dinero propina) {
            this.propina = Objects.requireNonNull(propina, "propina");
            return this;
        }

        /**
         * Construye la factura, calculando la clave de acceso y los totales.
         *
         * @throws ValidacionException    si falta algún dato obligatorio o los detalles están vacíos
         * @throws ConfiguracionException si falta el código numérico o se indicaron dos a la vez
         */
        public Factura build() {
            validarComun();
            var detalles = detallesExigidos("La factura");

            var infoFactura = new InfoFactura(
                fechaEmisionExigida(),
                Objects.requireNonNull(comprador, "comprador"),
                guiaRemision,
                totales == null ? TotalesFactura.de(detalles, propina) : totales,
                monedaIndicada(),
                pagosIndicados()
            );

            return new Factura(this, infoFactura);
        }
    }
}
