package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.Linea;
import com.peluware.sri.comprobantes.interno.Textos;
import org.jspecify.annotations.Nullable;

/**
 * Línea de una guía de remisión: qué se mueve y cuánto.
 *
 * <p>Su esquema no declara precio unitario, descuento, precio total sin impuesto ni impuestos.
 */
public final class DetalleGuiaRemision extends Linea {

    private final @Nullable String codigoInterno;
    private final @Nullable String codigoAdicional;

    private DetalleGuiaRemision(Builder builder) {
        super(builder);
        Textos.validarOpcional(builder.codigoInterno, CODIGO_LONGITUD_MAXIMA, "codigoInterno").lanzarSiInvalida();
        Textos.validarOpcional(builder.codigoAdicional, CODIGO_LONGITUD_MAXIMA, "codigoAdicional").lanzarSiInvalida();
        this.codigoInterno = builder.codigoInterno;
        this.codigoAdicional = builder.codigoAdicional;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Código del ítem; {@code null} si no aplica.
     */
    public @Nullable String codigoInterno() {
        return codigoInterno;
    }

    /**
     * Código alterno del ítem; {@code null} si no aplica.
     */
    public @Nullable String codigoAdicional() {
        return codigoAdicional;
    }

    @Override
    public String toString() {
        return "DetalleGuiaRemision[" + descripcion() + ", " + cantidad() + "]";
    }

    /**
     * Construye una línea de guía de remisión.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends Linea.Builder<Builder, DetalleGuiaRemision> {

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

        @Override
        public DetalleGuiaRemision build() {
            return new DetalleGuiaRemision(this);
        }
    }
}
