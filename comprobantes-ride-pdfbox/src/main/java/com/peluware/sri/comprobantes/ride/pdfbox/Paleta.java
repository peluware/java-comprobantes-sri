package com.peluware.sri.comprobantes.ride.pdfbox;

import org.jspecify.annotations.Nullable;

/**
 * Colores del RIDE, en RGB de 24 bits como {@code 0x1A3D7C}.
 *
 * <p>El color no es un requisito del Anexo 2: lo que la ficha fija son los campos y sus
 * posiciones, y el numeral 9.19 admite añadir lo que el contribuyente requiera.
 *
 * <p>Se usa {@code int} y no {@code java.awt.Color} para no arrastrar {@code java.desktop} al
 * módulo.
 *
 * <p>Se construye con {@link #builder()}, de modo que un color nuevo no cambie ninguna firma.
 */
public final class Paleta {

    private static final int NEGRO = 0x000000;
    private static final int GRIS = 0x808080;
    private static final int GRIS_CLARO = 0xEEEEEE;
    private static final int BLANCO = 0xFFFFFF;

    private final int lineas;
    private final int titulo;
    private final int texto;
    private final int rotulo;
    private final @Nullable Integer fondoTotal;
    private final int textoTotal;
    private final int fondoCabecera;
    private final int textoCabecera;

    private Paleta(Builder builder) {
        this.lineas = builder.lineas;
        this.titulo = builder.titulo;
        this.texto = builder.texto;
        this.rotulo = builder.rotulo;
        this.fondoTotal = builder.fondoTotal;
        this.textoTotal = builder.textoTotal;
        this.fondoCabecera = builder.fondoCabecera;
        this.textoCabecera = builder.textoCabecera;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * En negro sobre blanco, con los recuadros en gris y la cabecera de las tablas en gris
     * claro.
     */
    public static Paleta porDefecto() {
        return builder().build();
    }

    /**
     * La paleta por defecto con otro color para el título, los recuadros y la fila del valor
     * total.
     *
     * @param color color de marca, en RGB de 24 bits
     */
    public static Paleta deMarca(int color) {
        return builder().lineas(color).titulo(color).fondoTotal(color).textoTotal(BLANCO).build();
    }

    /**
     * Color de los recuadros y los separadores de las tablas.
     */
    public int lineas() {
        return lineas;
    }

    /**
     * Color del nombre del comprobante, por ejemplo {@code FACTURA}.
     */
    public int titulo() {
        return titulo;
    }

    /**
     * Color del texto corriente.
     */
    public int texto() {
        return texto;
    }

    /**
     * Color de los rótulos que acompañan a cada valor, como «Dirección Matriz:» o «SUBTOTAL 15%».
     */
    public int rotulo() {
        return rotulo;
    }

    /**
     * Color de fondo de la fila del valor total; {@code null} la deja sin relleno.
     */
    public @Nullable Integer fondoTotal() {
        return fondoTotal;
    }

    /**
     * Color del texto de esa fila.
     */
    public int textoTotal() {
        return textoTotal;
    }

    /**
     * Color de fondo de la fila de rótulos de las tablas.
     */
    public int fondoCabecera() {
        return fondoCabecera;
    }

    /**
     * Color del texto de esa fila.
     */
    public int textoCabecera() {
        return textoCabecera;
    }

    /**
     * Construye una paleta. Lo que no se indique queda en su valor por omisión.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder {

        private int lineas = GRIS;
        private int titulo = NEGRO;
        private int texto = NEGRO;
        private int rotulo = NEGRO;
        private @Nullable Integer fondoTotal;
        private int textoTotal = NEGRO;
        private int fondoCabecera = GRIS_CLARO;
        private int textoCabecera = NEGRO;

        private Builder() {
        }

        public Builder lineas(int color) {
            this.lineas = color;
            return this;
        }

        public Builder titulo(int color) {
            this.titulo = color;
            return this;
        }

        public Builder texto(int color) {
            this.texto = color;
            return this;
        }

        /**
         * Cambia el color de los rótulos.
         *
         * <p>Los rótulos se escriben en el cuerpo de letra más pequeño del formato, así que un gris
         * demasiado claro deja de leerse impreso. Sobre blanco, el límite de la recomendación
         * WCAG AA para texto pequeño, 4,5:1, lo marca {@code 0x767676}; por encima de ese valor
         * el rótulo se difumina.
         */
        public Builder rotulo(int color) {
            this.rotulo = color;
            return this;
        }

        /**
         * Rellena la fila del valor total, que es el dato que primero se busca.
         */
        public Builder fondoTotal(int color) {
            this.fondoTotal = color;
            return this;
        }

        public Builder textoTotal(int color) {
            this.textoTotal = color;
            return this;
        }

        public Builder fondoCabecera(int color) {
            this.fondoCabecera = color;
            return this;
        }

        public Builder textoCabecera(int color) {
            this.textoCabecera = color;
            return this;
        }

        public Paleta build() {
            return new Paleta(this);
        }
    }
}
