package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Un bloque de pares rótulo-valor, medido al construirse y dibujable después.
 *
 * <p>Cada fila lleva una celda o dos, y su alto es el de la celda que más renglones ocupa: un
 * valor que no cabe a lo ancho pasa al renglón siguiente en vez de recortarse. Una fila cuyo valor
 * es {@code null} no se añade.
 */
final class Pares {

    /** Renglones que admite un valor antes de recortarse. */
    private static final int RENGLONES_MAXIMOS = 4;

    /** Alto de un renglón, antes de escalar. */
    private static final float RENGLON = 11;

    private final EstiloRide estilo;
    private final List<Fila> filas;
    private final float tamano;
    private final float renglon;

    private Pares(EstiloRide estilo, List<Fila> filas, float tamano, float renglon) {
        this.estilo = estilo;
        this.filas = filas;
        this.tamano = tamano;
        this.renglon = renglon;
    }

    private record Celda(String rotulo, float x, float desde, List<String> renglones) {
    }

    private record Fila(List<Celda> celdas) {

        int renglones() {
            int renglones = 1;
            for (var celda : celdas) {
                renglones = Math.max(renglones, celda.renglones().size());
            }
            return renglones;
        }
    }

    static Builder constructor(PDFont normal, PDFont negrita, EstiloRide estilo) {
        return new Builder(normal, negrita, estilo, estilo.tamTexto(), RENGLON);
    }

    /**
     * Pares de otro tamaño, con su propio alto de renglón.
     */
    static Builder constructor(PDFont normal, PDFont negrita, EstiloRide estilo, float tamano, float renglon) {
        return new Builder(normal, negrita, estilo, tamano, renglon);
    }

    /**
     * Renglones que ocupan todas las filas juntas.
     */
    int renglones() {
        int renglones = 0;
        for (var fila : filas) {
            renglones += fila.renglones();
        }
        return renglones;
    }

    /**
     * Alto de las filas, sin el aire del recuadro que las contiene.
     */
    float alto() {
        return estilo.espacio(renglon) * renglones();
    }

    /**
     * Dibuja las filas bajo {@code arriba}.
     *
     * @return la altura a la que queda el pie de la última
     */
    float dibujar(Pluma pluma, float arriba) throws IOException {
        float techo = arriba;
        for (var fila : filas) {
            for (var celda : fila.celdas()) {
                float y = techo - estilo.espacio(renglon);
                pluma.rotulo(tamano, celda.x(), y, celda.rotulo());
                for (var texto : celda.renglones()) {
                    pluma.lienzo().texto(pluma.fuenteValor(), tamano, celda.x() + celda.desde(), y, texto);
                    y -= estilo.espacio(this.renglon);
                }
            }
            techo -= estilo.espacio(renglon) * fila.renglones();
        }
        return techo;
    }

    /**
     * Recoge las filas y mide cada valor contra el ancho de que dispone.
     *
     * <p>No es thread-safe.
     */
    static final class Builder {

        private final PDFont rotulos;
        private final PDFont valores;
        private final EstiloRide estilo;
        private final float tamano;
        private final float renglon;
        private final List<Fila> filas = new ArrayList<>();

        private Builder(PDFont normal, PDFont negrita, EstiloRide estilo, float tamano, float renglon) {
            this.rotulos = estilo.enfasis() == Enfasis.ROTULO ? negrita : normal;
            this.valores = estilo.enfasis() == Enfasis.VALOR ? negrita : normal;
            this.estilo = estilo;
            this.tamano = tamano;
            this.renglon = renglon;
        }

        /**
         * Una fila de una celda.
         */
        Builder fila(float x, float ancho, String rotulo, @Nullable String valor) {
            if (valor != null) {
                filas.add(new Fila(List.of(celda(x, ancho, rotulo, valor))));
            }
            return this;
        }

        /**
         * Una fila de dos celdas. La segunda se omite si su valor es {@code null}.
         */
        Builder fila(float x, float ancho, String rotulo, String valor, float x2, float ancho2, String rotulo2, @Nullable String valor2) {
            var celdas = new ArrayList<Celda>();
            celdas.add(celda(x, ancho, rotulo, valor));
            if (valor2 != null) {
                celdas.add(celda(x2, ancho2, rotulo2, valor2));
            }
            filas.add(new Fila(List.copyOf(celdas)));
            return this;
        }

        /**
         * Una fila cuyo valor empieza a una distancia fija del rótulo, para que los valores de
         * varias filas queden alineados.
         */
        Builder filaAlineada(float x, float sangria, float ancho, String rotulo, @Nullable String valor) {
            if (valor != null) {
                filas.add(new Fila(List.of(new Celda(rotulo, x, sangria, Medidas.partir(valores, tamano, valor, ancho - sangria, RENGLONES_MAXIMOS)))));
            }
            return this;
        }

        private Celda celda(float x, float ancho, String rotulo, String valor) {
            float desde = Medidas.ancho(rotulos, tamano, rotulo) + estilo.v(5);
            return new Celda(rotulo, x, desde, Medidas.partir(valores, tamano, valor, ancho - desde, RENGLONES_MAXIMOS));
        }

        Pares build() {
            return new Pares(estilo, List.copyOf(filas), tamano, renglon);
        }
    }
}
