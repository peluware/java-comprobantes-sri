package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Línea de un comprobante de venta, correspondiente al bloque {@code <detalle>} de los esquemas
 * de factura y de liquidación de compra.
 *
 * <p>Añade a {@link Detalle} la unidad de medida y los dos códigos del ítem, que los dos
 * esquemas nombran igual: {@code codigoPrincipal} y {@code codigoAuxiliar}.
 */
public final class DetalleComprobanteVenta extends Detalle {

    public static final int UNIDAD_MEDIDA_LONGITUD_MAXIMA = 50;

    private final @Nullable String codigoPrincipal;
    private final @Nullable String codigoAuxiliar;
    private final @Nullable String unidadMedida;

    private DetalleComprobanteVenta(Builder builder, Calculado calculado) {
        super(builder, calculado.precioTotalSinImpuesto(), calculado.impuestos());
        this.codigoPrincipal = builder.codigoPrincipal;
        this.codigoAuxiliar = builder.codigoAuxiliar;
        this.unidadMedida = builder.unidadMedida;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Código del producto o servicio, tag {@code <codigoPrincipal>}; {@code null} si no aplica.
     */
    public @Nullable String codigoPrincipal() {
        return codigoPrincipal;
    }

    /**
     * Código alterno, tag {@code <codigoAuxiliar>}; {@code null} si no aplica.
     */
    public @Nullable String codigoAuxiliar() {
        return codigoAuxiliar;
    }

    /**
     * Unidad de medida; {@code null} si no aplica.
     */
    public @Nullable String unidadMedida() {
        return unidadMedida;
    }

    /**
     * Construye una línea de comprobante de venta.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends Detalle.Builder<Builder, DetalleComprobanteVenta> {

        private @Nullable String codigoPrincipal;
        private @Nullable String codigoAuxiliar;
        private @Nullable String unidadMedida;

        private Builder() {
        }

        public Builder codigoPrincipal(@Nullable String codigoPrincipal) {
            this.codigoPrincipal = codigoPrincipal;
            return this;
        }

        public Builder codigoAuxiliar(@Nullable String codigoAuxiliar) {
            this.codigoAuxiliar = codigoAuxiliar;
            return this;
        }

        public Builder unidadMedida(@Nullable String unidadMedida) {
            this.unidadMedida = unidadMedida;
            return this;
        }

        /**
         * Importes tal como los trae el XML, en vez de derivarlos de la cantidad y el precio.
         *
         * <p>La usan los readers, que viven en los paquetes de cada comprobante y no pueden
         * alcanzar un miembro de paquete de este.
         */
        @Interno
        public Builder valores(Dinero precioTotalSinImpuesto, List<Impuesto> impuestos) {
            return calculado(precioTotalSinImpuesto, impuestos);
        }

        @Override
        public DetalleComprobanteVenta build() {
            Textos.validarOpcional(codigoPrincipal, Detalle.CODIGO_LONGITUD_MAXIMA, "codigoPrincipal").lanzarSiInvalida();
            Textos.validarOpcional(codigoAuxiliar, Detalle.CODIGO_LONGITUD_MAXIMA, "codigoAuxiliar").lanzarSiInvalida();
            Textos.validarOpcional(unidadMedida, UNIDAD_MEDIDA_LONGITUD_MAXIMA, "unidadMedida").lanzarSiInvalida();
            return new DetalleComprobanteVenta(this, calcular());
        }
    }
}
