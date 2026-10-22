package com.peluware.sri.comprobantes.liquidacioncompra;

import com.peluware.sri.comprobantes.ComprobanteVenta;
import com.peluware.sri.comprobantes.ConfiguracionException;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Liquidación de compra de bienes y prestación de servicios.
 *
 * <p>El comprobante de venta que emite el comprador por cuenta de un vendedor que no puede
 * emitir comprobantes; su contraparte es un {@link Proveedor}. No añade nada a
 * {@link ComprobanteVenta}: su esquema no declara propina.
 *
 * <p>Solo se construye con {@link #builder()}, que calcula la clave de acceso y los totales a
 * partir de los demás campos.
 */
public final class LiquidacionCompra extends ComprobanteVenta {

    private final InfoLiquidacionCompra infoLiquidacionCompra;

    private LiquidacionCompra(Builder builder, InfoLiquidacionCompra infoLiquidacionCompra) {
        super(builder);
        this.infoLiquidacionCompra = infoLiquidacionCompra;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Tipo de comprobante, siempre {@link TipoComprobante#LIQUIDACION_COMPRA}. */
    @Override
    public TipoComprobante tipoComprobante() {
        return TipoComprobante.LIQUIDACION_COMPRA;
    }

    @Override
    public InfoLiquidacionCompra info() {
        return infoLiquidacionCompra;
    }

    /**
     * Construye una liquidación de compra calculando la clave de acceso y los totales.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends ComprobanteVenta.Builder<Builder> {

        private @Nullable Proveedor proveedor;
        private @Nullable TotalesLiquidacionCompra totales;

        private Builder() {
        }

        @Override
        protected TipoComprobante tipoComprobante() {
            return TipoComprobante.LIQUIDACION_COMPRA;
        }

        /**
         * Proveedor por cuenta del cual se emite.
         */
        public Builder proveedor(Proveedor proveedor) {
            this.proveedor = proveedor;
            return this;
        }

        /**
         * Totales tal como los trae el XML, en vez de derivarlos de los detalles.
         */
        Builder totales(TotalesLiquidacionCompra totales) {
            this.totales = totales;
            return this;
        }

        /**
         * Construye la liquidación de compra, calculando la clave de acceso y los totales.
         *
         * @throws ValidacionException    si falta algún dato obligatorio o los detalles están vacíos
         * @throws ConfiguracionException si falta el código numérico o se indicaron dos a la vez
         */
        public LiquidacionCompra build() {
            validarComun();
            var detalles = detallesExigidos("La liquidación de compra");

            var info = new InfoLiquidacionCompra(
                fechaEmisionExigida(),
                Objects.requireNonNull(proveedor, "proveedor"),
                totales == null ? TotalesLiquidacionCompra.de(detalles) : totales,
                monedaIndicada(),
                pagosIndicados()
            );

            return new LiquidacionCompra(this, info);
        }
    }
}
