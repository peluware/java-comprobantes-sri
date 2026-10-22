package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.guiaremision.DetalleGuiaRemision;
import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.valores.Cantidad;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Línea de un comprobante: qué es y cuánto.
 *
 * <p>Base del detalle de la factura, la nota de crédito y la liquidación de compra, que añaden
 * importes e impuestos, y del detalle de la guía de remisión, que no los lleva.
 *
 * <p>Los tres campos que declara —descripción, cantidad y datos adicionales— los definen con el
 * mismo tipo los cuatro esquemas que llevan línea.
 */
public abstract sealed class Linea permits Detalle, DetalleGuiaRemision {

    /**
     * Longitud máxima de los códigos del ítem, que cada subtipo declara con su propio nombre.
     */
    public static final int CODIGO_LONGITUD_MAXIMA = 25;

    public static final int DESCRIPCION_LONGITUD_MAXIMA = 300;

    private final String descripcion;
    private final Cantidad cantidad;
    private final List<DetalleAdicional> detallesAdicionales;

    /**
     * Toma del builder los campos comunes y los valida.
     *
     * @throws ValidacionException si falta la descripción o la cantidad, la descripción excede
     *                             {@link #DESCRIPCION_LONGITUD_MAXIMA} o hay más datos adicionales
     *                             de los que admite {@link DetalleAdicional#MAX_POR_DETALLE}
     */
    protected Linea(Builder<?, ?> builder) {
        var descripcion = Objects.requireNonNull(builder.descripcion, "descripcion");
        Textos.validar(descripcion, DESCRIPCION_LONGITUD_MAXIMA, "descripcion").lanzarSiInvalida();

        var adicionales = List.copyOf(builder.detallesAdicionales);
        if (adicionales.size() > DetalleAdicional.MAX_POR_DETALLE) {
            throw new ValidacionException(new Validacion.CampoInvalido("detallesAdicionales", "El esquema admite hasta " + DetalleAdicional.MAX_POR_DETALLE + " datos adicionales por detalle, se indicaron " + adicionales.size()));
        }

        this.descripcion = descripcion;
        this.cantidad = Objects.requireNonNull(builder.cantidad, "cantidad");
        this.detallesAdicionales = adicionales;
    }

    public String descripcion() {
        return descripcion;
    }

    public Cantidad cantidad() {
        return cantidad;
    }

    /**
     * Datos adicionales de la línea, hasta {@link DetalleAdicional#MAX_POR_DETALLE}.
     */
    public List<DetalleAdicional> detallesAdicionales() {
        return detallesAdicionales;
    }

    /**
     * Recoge los campos que toda línea lleva.
     *
     * <p>No es thread-safe.
     *
     * @param <B> builder del subtipo
     * @param <L> línea que construye
     */
    public abstract static sealed class Builder<B extends Builder<B, L>, L extends Linea> permits Detalle.Builder, DetalleGuiaRemision.Builder {

        private @Nullable String descripcion;
        private @Nullable Cantidad cantidad;
        private final List<DetalleAdicional> detallesAdicionales = new ArrayList<>();

        protected Builder() {
        }

        /**
         * El propio builder, con el tipo del subtipo.
         */
        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
        }

        public B descripcion(String descripcion) {
            this.descripcion = descripcion;
            return self();
        }

        public B cantidad(Cantidad cantidad) {
            this.cantidad = cantidad;
            return self();
        }

        public B cantidad(long cantidad) {
            return cantidad(Cantidad.of(cantidad));
        }

        /**
         * Añade un dato adicional al bloque {@code <detallesAdicionales>}.
         */
        public B detalleAdicional(String nombre, String valor) {
            detallesAdicionales.add(new DetalleAdicional(nombre, valor));
            return self();
        }

        /**
         * Cantidad indicada, o {@code null} si todavía no se indicó.
         */
        protected final @Nullable Cantidad cantidadIndicada() {
            return cantidad;
        }

        /**
         * Construye la línea del subtipo.
         */
        public abstract L build();
    }
}
