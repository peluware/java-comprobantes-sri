package com.peluware.sri.comprobantes.notacredito;

import java.util.List;
import com.peluware.sri.comprobantes.Detalle;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.jspecify.annotations.Nullable;

/**
 * Línea de una nota de crédito, correspondiente al bloque {@code <detalle>} de su esquema.
 *
 * <p>Su esquema no declara unidad de medida y nombra los códigos del ítem
 * {@code codigoInterno} y {@code codigoAdicional}.
 */
public final class DetalleNotaCredito extends Detalle {

    private final @Nullable String codigoInterno;
    private final @Nullable String codigoAdicional;

    private DetalleNotaCredito(Builder builder, Calculado calculado) {
        super(builder, calculado.precioTotalSinImpuesto(), calculado.impuestos());
        this.codigoInterno = builder.codigoInterno;
        this.codigoAdicional = builder.codigoAdicional;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Código del producto o servicio, tag {@code <codigoInterno>}; {@code null} si no aplica.
     */
    public @Nullable String codigoInterno() {
        return codigoInterno;
    }

    /**
     * Código alterno, tag {@code <codigoAdicional>}; {@code null} si no aplica.
     */
    public @Nullable String codigoAdicional() {
        return codigoAdicional;
    }

    /**
     * Construye una línea de nota de crédito.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends Detalle.Builder<Builder, DetalleNotaCredito> {

        private @Nullable String codigoInterno;
        private @Nullable String codigoAdicional;

        private Builder() {
        }

        public Builder codigoInterno(@Nullable String codigoInterno) {
            this.codigoInterno = codigoInterno;
            return this;
        }

        public Builder codigoAdicional(@Nullable String codigoAdicional) {
            this.codigoAdicional = codigoAdicional;
            return this;
        }

        /**
         * Importes tal como los trae el XML.
         */
        Builder valores(Dinero precioTotalSinImpuesto, List<Impuesto> impuestos) {
            return calculado(precioTotalSinImpuesto, impuestos);
        }

        @Override
        public DetalleNotaCredito build() {
            Textos.validarOpcional(codigoInterno, Detalle.CODIGO_LONGITUD_MAXIMA, "codigoInterno").lanzarSiInvalida();
            Textos.validarOpcional(codigoAdicional, Detalle.CODIGO_LONGITUD_MAXIMA, "codigoAdicional").lanzarSiInvalida();
            return new DetalleNotaCredito(this, calcular());
        }
    }
}
