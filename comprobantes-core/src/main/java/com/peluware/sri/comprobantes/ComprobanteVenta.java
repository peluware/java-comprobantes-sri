package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompra;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Comprobante que documenta el intercambio de bienes o servicios.
 *
 * <p>La familia que el Reglamento de Comprobantes de Venta, Retención y Documentos
 * Complementarios llama comprobantes de venta, de la que esta librería modela dos: la
 * {@link Factura}, que emite quien vende, y la {@link LiquidacionCompra}, que emite quien compra
 * cuando el vendedor no puede emitir comprobantes. Misma forma, extremos opuestos de la
 * transacción, y por eso su contraparte no se comparte: {@link Comprador} frente a
 * {@code Proveedor}.
 *
 * <p>Lo que comparten es la línea y, en su bloque propio, las formas de pago, la moneda y
 * los totales; eso último lo recoge {@link InfoComprobanteVenta}. La nota de
 * crédito queda fuera aunque también lleve líneas, porque no documenta un intercambio: modifica
 * uno anterior, y por eso no declara importe total ni formas de pago.
 */
public abstract sealed class ComprobanteVenta extends Comprobante permits Factura, LiquidacionCompra {

    private final List<DetalleComprobanteVenta> detalles;

    protected ComprobanteVenta(Builder<?> builder) {
        super(builder);
        this.detalles = List.copyOf(builder.detalles);
    }

    /**
     * Líneas del comprobante, al menos una.
     */
    public final List<DetalleComprobanteVenta> detalles() {
        return detalles;
    }

    /**
     * Bloque propio del comprobante, donde viven los totales, la moneda y las formas de pago.
     * Cada subtipo lo estrecha a su propio tipo.
     */
    public abstract InfoComprobanteVenta info();

    /**
     * Construye un comprobante de venta.
     *
     * <p>No es thread-safe.
     *
     * @param <B> builder del subtipo
     */
    public abstract static sealed class Builder<B extends Builder<B>> extends Comprobante.Builder<B> permits Factura.Builder, LiquidacionCompra.Builder {

        private final List<DetalleComprobanteVenta> detalles = new ArrayList<>();
        private final List<Pago> pagos = new ArrayList<>();
        private String moneda = Sri.MONEDA;

        protected Builder() {
        }

        public B detalle(DetalleComprobanteVenta detalle) {
            detalles.add(Objects.requireNonNull(detalle, "detalle"));
            return self();
        }

        public B detalles(List<DetalleComprobanteVenta> detalles) {
            this.detalles.addAll(detalles);
            return self();
        }

        /**
         * Añade una forma de pago. Si se indica alguna, deben sumar el importe total.
         */
        public B pago(Pago pago) {
            pagos.add(Objects.requireNonNull(pago, "pago"));
            return self();
        }

        /**
         * Por omisión, {@link Sri#MONEDA}.
         */
        public B moneda(String moneda) {
            this.moneda = Objects.requireNonNull(moneda, "moneda");
            return self();
        }

        /**
         * Las líneas indicadas, que los subtipos totalizan en su {@code build()}.
         *
         * @throws ValidacionException si no se indicó ninguna
         */
        protected final List<DetalleComprobanteVenta> detallesExigidos(String nombreDocumento) {
            if (detalles.isEmpty()) {
                throw new ValidacionException(new Validacion.CampoInvalido("detalles", nombreDocumento + " debe tener al menos un detalle"));
            }
            return detalles;
        }

        /**
         * Moneda indicada, para el bloque propio de cada subtipo.
         */
        protected final String monedaIndicada() {
            return moneda;
        }

        /**
         * Formas de pago indicadas, para el bloque propio de cada subtipo.
         */
        protected final List<Pago> pagosIndicados() {
            return pagos;
        }
    }
}
