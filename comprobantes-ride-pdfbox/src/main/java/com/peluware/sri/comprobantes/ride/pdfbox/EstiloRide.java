package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Cómo se ve el RIDE.
 *
 * <p>Ficha Técnica v2.34, numeral 9.19: los modelos del Anexo 2 fijan qué campos van y en qué
 * posición, y admiten imprimir «datos adicionales conforme lo requiera el contribuyente». El
 * color, la tipografía, la escala, el redondeo y el pie caen de ese lado, así que son
 * configurables; las posiciones de los requisitos y la caja del logo no, para que el formato
 * siga siendo el de la ficha y un logo de otra proporción no lo deforme.
 *
 * <p>Si necesitas cambiar el formato en sí, lo que toca es implementar
 * {@link com.peluware.sri.comprobantes.ride.GeneradorRide} y no estirar este estilo.
 *
 * <p>Se construye con {@link #builder()}, de modo que un ajuste nuevo no cambie ninguna firma.
 */
public final class EstiloRide {

    /** Escala mínima admitida; por debajo el texto deja de ser legible impreso. */
    public static final float ESCALA_MINIMA = 0.8f;

    /** Escala máxima admitida; por encima el formato no cabe en una A4. */
    public static final float ESCALA_MAXIMA = 1.4f;

    /** Redondeo máximo admitido, en puntos. */
    public static final float RADIO_MAXIMO = 8;

    /** Cuerpo del texto corriente, antes de escalar. */
    private static final float BASE_TEXTO = 7.5f;

    /** Cuerpo de los rótulos y las celdas, antes de escalar. */
    private static final float BASE_CHICO = 6.5f;

    /** Cuerpo del nombre del comprobante, antes de escalar. */
    private static final float BASE_TITULO = 13;

    /** Separación mínima admitida entre bloques. */
    public static final float AIRE_MINIMO = 0.5f;

    /** Separación máxima admitida entre bloques. */
    public static final float AIRE_MAXIMO = 3;

    /** Interlineado mínimo admitido. */
    public static final float INTERLINEADO_MINIMO = 0.8f;

    /** Interlineado máximo admitido; por encima el formato no cabe en una A4. */
    public static final float INTERLINEADO_MAXIMO = 2;

    /** Alto mínimo admitido para el logo, en puntos. */
    public static final float ALTO_LOGO_MINIMO = 20;

    /** Alto máximo admitido para el logo, en puntos. */
    public static final float ALTO_LOGO_MAXIMO = 120;

    /** Grosor de línea mínimo admitido, en puntos; por debajo la impresión lo pierde. */
    public static final float GROSOR_MINIMO = 0.25f;

    /** Grosor de línea máximo admitido, en puntos. */
    public static final float GROSOR_MAXIMO = 3;

    private final @Nullable LogoRide logo;
    private final boolean codigoBarras;
    private final Paleta paleta;
    private final Tipografia tipografia;
    private final float escala;
    private final float radioBordes;
    private final float interlineado;
    private final Enfasis enfasis;
    private final float altoLogo;
    private final Alineacion alineacionLogo;
    private final Rotulos rotulos;
    private final FormatoAdicionales formatoAdicionales;
    private final float aireEntreBloques;
    private final MarcoPagina marcoPagina;
    private final Contorno contorno;
    private final Divisiones divisiones;
    private final float grosorLineas;
    private final @Nullable PieRide pie;

    private EstiloRide(Builder builder) {
        this.logo = builder.logo;
        this.codigoBarras = builder.codigoBarras;
        this.paleta = builder.paleta;
        this.tipografia = builder.tipografia;
        this.escala = builder.escala;
        this.radioBordes = builder.radioBordes;
        this.interlineado = builder.interlineado;
        this.enfasis = builder.enfasis;
        this.altoLogo = builder.altoLogo;
        this.alineacionLogo = builder.alineacionLogo;
        this.rotulos = builder.rotulos;
        this.formatoAdicionales = builder.formatoAdicionales;
        this.aireEntreBloques = builder.aireEntreBloques;
        this.marcoPagina = builder.marcoPagina;
        this.contorno = builder.contorno;
        this.divisiones = builder.divisiones;
        this.grosorLineas = builder.grosorLineas;
        this.pie = builder.pie;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Sin logo, sin código de barras, sin pie, en Helvetica, negro sobre blanco, sin escalar y
     * con las esquinas rectas, cada bloque en su recuadro y las tablas en rejilla.
     */
    public static EstiloRide porDefecto() {
        return builder().build();
    }

    /**
     * Cómo crear la imagen del logo del emisor; {@code null} si no se imprime.
     */
    public @Nullable LogoRide logo() {
        return logo;
    }

    /**
     * Si se imprime el código de barras de la clave de acceso.
     */
    public boolean codigoBarras() {
        return codigoBarras;
    }

    /**
     * Colores.
     */
    public Paleta paleta() {
        return paleta;
    }

    /**
     * Las dos fuentes del documento.
     */
    public Tipografia tipografia() {
        return tipografia;
    }

    /**
     * Factor del tamaño de letra y de las medidas verticales.
     */
    public float escala() {
        return escala;
    }

    /**
     * Radio de las esquinas de los recuadros, en puntos; cero las deja rectas. Solo aplica con
     * {@link Contorno#CAJA}.
     */
    public float radioBordes() {
        return radioBordes;
    }

    /**
     * Factor del espaciado vertical, aparte de la escala.
     */
    public float interlineado() {
        return interlineado;
    }

    /**
     * Dónde va la negrita.
     */
    public Enfasis enfasis() {
        return enfasis;
    }

    /**
     * Alto de la banda reservada al logo, en puntos. El logo entra en ella sin deformarse.
     */
    public float altoLogo() {
        return altoLogo;
    }

    /**
     * Dónde se coloca el logo dentro de la mitad izquierda de la cabecera.
     */
    public Alineacion alineacionLogo() {
        return alineacionLogo;
    }

    /**
     * Cómo se escriben los rótulos.
     */
    public Rotulos rotulos() {
        return rotulos;
    }

    /**
     * Cómo se escriben los datos adicionales de una línea en su columna.
     */
    public FormatoAdicionales formatoAdicionales() {
        return formatoAdicionales;
    }

    /**
     * Factor del hueco que separa un bloque del siguiente.
     */
    public float aireEntreBloques() {
        return aireEntreBloques;
    }

    /**
     * Filete que rodea la hoja entera.
     */
    public MarcoPagina marcoPagina() {
        return marcoPagina;
    }

    /**
     * Cómo se delimita cada bloque.
     */
    public Contorno contorno() {
        return contorno;
    }

    /**
     * Qué líneas separan las celdas dentro de las tablas.
     */
    public Divisiones divisiones() {
        return divisiones;
    }

    /**
     * Grosor de los recuadros, las reglas y las divisiones, en puntos.
     */
    public float grosorLineas() {
        return grosorLineas;
    }

    /**
     * Pie de página; {@code null} si no lleva.
     */
    public @Nullable PieRide pie() {
        return pie;
    }

    /**
     * Construye un estilo. Lo que no se indique queda en su valor por omisión.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder {

        private @Nullable LogoRide logo;
        private boolean codigoBarras;
        private Paleta paleta = Paleta.porDefecto();
        private Tipografia tipografia = Tipografia.de(FamiliaTipografica.HELVETICA);
        private float escala = 1;
        private float radioBordes;
        private float interlineado = 1;
        private Enfasis enfasis = Enfasis.ROTULO;
        private float altoLogo = 56;
        private Alineacion alineacionLogo = Alineacion.CENTRO;
        private Rotulos rotulos = Rotulos.ANEXO_2;
        private FormatoAdicionales formatoAdicionales = FormatoAdicionales.EN_LINEA;
        private float aireEntreBloques = 1;
        private MarcoPagina marcoPagina = MarcoPagina.NINGUNO;
        private Contorno contorno = Contorno.CAJA;
        private Divisiones divisiones = Divisiones.REJILLA;
        private float grosorLineas = 1;
        private @Nullable PieRide pie;

        private Builder() {
        }

        /**
         * Logo del emisor, creado por esa fábrica en cada documento.
         */
        public Builder logo(LogoRide logo) {
            this.logo = Objects.requireNonNull(logo, "logo");
            return this;
        }

        /**
         * Logo del emisor leído de esos bytes, dejando que PDFBox reconozca el formato por sus
         * bytes mágicos: JPEG, PNG, TIFF, BMP o GIF.
         *
         * <p>Los bytes se copian y se reutilizan en cada documento. Para controlar cómo se
         * embebe —calidad JPEG, compresión sin pérdida— está {@link #logo(LogoRide)}.
         */
        public Builder logo(byte[] logo) {
            var copia = logo.clone();
            return logo(documento -> PDImageXObject.createFromByteArray(documento, copia, "logo"));
        }

        /**
         * Por omisión, falso. Dibuja la clave de acceso en Code 128 bajo la caja de la
         * autorización. El numeral 9.20 lo declara opcional.
         */
        public Builder codigoBarras(boolean codigoBarras) {
            this.codigoBarras = codigoBarras;
            return this;
        }

        public Builder paleta(Paleta paleta) {
            this.paleta = Objects.requireNonNull(paleta, "paleta");
            return this;
        }

        /**
         * Una de las familias base-14. Atajo de tipografia(Tipografia.de(familia)).
         */
        public Builder familia(FamiliaTipografica familia) {
            return tipografia(Tipografia.de(familia));
        }

        /**
         * Las dos fuentes del documento, que pueden ser propias.
         */
        public Builder tipografia(Tipografia tipografia) {
            this.tipografia = Objects.requireNonNull(tipografia, "tipografia");
            return this;
        }

        /**
         * Escala la letra y las medidas verticales.
         *
         * <p>Los anchos no cambian: la página sigue siendo A4 y las columnas se reparten su
         * ancho útil, así que un texto que deje de caber se recorta en vez de desbordar.
         *
         * @param escala factor entre {@link #ESCALA_MINIMA} y {@link #ESCALA_MAXIMA}
         * @throws IllegalArgumentException si queda fuera de ese rango
         */
        public Builder escala(float escala) {
            if (!(escala >= ESCALA_MINIMA && escala <= ESCALA_MAXIMA)) {
                throw new IllegalArgumentException("La escala debe estar entre " + ESCALA_MINIMA + " y " + ESCALA_MAXIMA + ", se indicó " + escala);
            }
            this.escala = escala;
            return this;
        }

        /**
         * Redondea las esquinas de los recuadros.
         *
         * @param radioBordes radio en puntos, entre cero y {@link #RADIO_MAXIMO}
         * @throws IllegalArgumentException si queda fuera de ese rango
         */
        public Builder radioBordes(float radioBordes) {
            if (!(radioBordes >= 0 && radioBordes <= RADIO_MAXIMO)) {
                throw new IllegalArgumentException("El radio debe estar entre 0 y " + RADIO_MAXIMO + ", se indicó " + radioBordes);
            }
            this.radioBordes = radioBordes;
            return this;
        }

        /**
         * Airea o aprieta el espaciado vertical sin tocar el tamaño de la letra.
         *
         * @param interlineado factor entre {@link #INTERLINEADO_MINIMO} y {@link #INTERLINEADO_MAXIMO}
         * @throws IllegalArgumentException si queda fuera de ese rango
         */
        public Builder interlineado(float interlineado) {
            if (!(interlineado >= INTERLINEADO_MINIMO && interlineado <= INTERLINEADO_MAXIMO)) {
                throw new IllegalArgumentException("El interlineado debe estar entre " + INTERLINEADO_MINIMO + " y " + INTERLINEADO_MAXIMO + ", se indicó " + interlineado);
            }
            this.interlineado = interlineado;
            return this;
        }

        /**
         * Cambia dónde va la negrita.
         */
        public Builder enfasis(Enfasis enfasis) {
            this.enfasis = Objects.requireNonNull(enfasis, "enfasis");
            return this;
        }

        /**
         * Cambia el alto de la banda del logo.
         *
         * @param altoLogo alto en puntos, entre {@link #ALTO_LOGO_MINIMO} y {@link #ALTO_LOGO_MAXIMO}
         * @throws IllegalArgumentException si queda fuera de ese rango
         */
        public Builder altoLogo(float altoLogo) {
            if (!(altoLogo >= ALTO_LOGO_MINIMO && altoLogo <= ALTO_LOGO_MAXIMO)) {
                throw new IllegalArgumentException("El alto del logo debe estar entre " + ALTO_LOGO_MINIMO + " y " + ALTO_LOGO_MAXIMO + ", se indicó " + altoLogo);
            }
            this.altoLogo = altoLogo;
            return this;
        }

        /**
         * Cambia dónde se coloca el logo.
         */
        public Builder alineacionLogo(Alineacion alineacionLogo) {
            this.alineacionLogo = Objects.requireNonNull(alineacionLogo, "alineacionLogo");
            return this;
        }

        /**
         * Cambia cómo se escriben los rótulos.
         */
        public Builder rotulos(Rotulos rotulos) {
            this.rotulos = Objects.requireNonNull(rotulos, "rotulos");
            return this;
        }

        /**
         * Cómo se escriben los datos adicionales de una línea. Por omisión, seguidos y separados
         * por barras.
         */
        public Builder formatoAdicionales(FormatoAdicionales formatoAdicionales) {
            this.formatoAdicionales = Objects.requireNonNull(formatoAdicionales, "formatoAdicionales");
            return this;
        }

        /**
         * Separa o junta los bloques entre sí, sin tocar el espaciado de dentro de cada uno.
         *
         * @param aireEntreBloques factor entre {@link #AIRE_MINIMO} y {@link #AIRE_MAXIMO}
         * @throws IllegalArgumentException si queda fuera de ese rango
         */
        public Builder aireEntreBloques(float aireEntreBloques) {
            if (!(aireEntreBloques >= AIRE_MINIMO && aireEntreBloques <= AIRE_MAXIMO)) {
                throw new IllegalArgumentException("El aire entre bloques debe estar entre " + AIRE_MINIMO + " y " + AIRE_MAXIMO + ", se indicó " + aireEntreBloques);
            }
            this.aireEntreBloques = aireEntreBloques;
            return this;
        }

        /**
         * Rodea la hoja con un filete.
         */
        public Builder marcoPagina(MarcoPagina marcoPagina) {
            this.marcoPagina = Objects.requireNonNull(marcoPagina, "marcoPagina");
            return this;
        }

        /**
         * Cambia la forma de delimitar cada bloque.
         */
        public Builder contorno(Contorno contorno) {
            this.contorno = Objects.requireNonNull(contorno, "contorno");
            return this;
        }

        /**
         * Cambia las líneas que separan las celdas dentro de las tablas.
         */
        public Builder divisiones(Divisiones divisiones) {
            this.divisiones = Objects.requireNonNull(divisiones, "divisiones");
            return this;
        }

        /**
         * Cambia el grosor de los recuadros, las reglas y las divisiones.
         *
         * @param grosorLineas grosor en puntos, entre {@link #GROSOR_MINIMO} y {@link #GROSOR_MAXIMO}
         * @throws IllegalArgumentException si queda fuera de ese rango
         */
        public Builder grosorLineas(float grosorLineas) {
            if (!(grosorLineas >= GROSOR_MINIMO && grosorLineas <= GROSOR_MAXIMO)) {
                throw new IllegalArgumentException("El grosor debe estar entre " + GROSOR_MINIMO + " y " + GROSOR_MAXIMO + ", se indicó " + grosorLineas);
            }
            this.grosorLineas = grosorLineas;
            return this;
        }

        public Builder pie(PieRide pie) {
            this.pie = Objects.requireNonNull(pie, "pie");
            return this;
        }

        public EstiloRide build() {
            return new EstiloRide(this);
        }
    }

    // ---- el estilo medido en puntos ----

    /**
     * Radio con el que se recortan las tablas: el del estilo cuando hay recuadro, cero cuando no,
     * para que las divisiones no queden cortadas por una curva que no se dibuja.
     */
    float radio() {
        return this.contorno() == Contorno.CAJA ? this.radioBordes() : 0;
    }


    /**
     * Hueco entre un bloque y el siguiente.
     */
    float hueco() {
        return espacio(10) * this.aireEntreBloques();
    }


    /**
     * Tamaño del texto corriente, ya escalado.
     */
    float tamTexto() {
        return BASE_TEXTO * this.escala();
    }


    /**
     * Tamaño del texto de las tablas, ya escalado.
     */
    float tamChico() {
        return BASE_CHICO * this.escala();
    }


    /**
     * Tamaño del nombre del comprobante, ya escalado.
     */
    float tamTitulo() {
        return BASE_TITULO * this.escala();
    }


    /**
     * Escala una medida con la letra, sin airearla: la caja del logo y los huecos entre una
     * etiqueta y su valor.
     *
     * <p>Los anchos no se escalan: la página sigue siendo A4 y las columnas se reparten su
     * ancho útil. Un texto que deje de caber se parte o se recorta, no desborda.
     */
    float v(float medida) {
        return medida * this.escala();
    }


    /**
     * Escala una medida de espaciado vertical: la letra y, encima, el interlineado.
     */
    float espacio(float medida) {
        return medida * this.escala() * this.interlineado();
    }

}
