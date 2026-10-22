package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Una tabla del RIDE, medida al construirse y dibujable por tramos de filas.
 *
 * <p>Queda medida entera antes de abrir ninguna página: {@link #altos()} da el alto de cada fila y
 * {@link #altoCabecera()} el de la fila de rótulos, que se repite en cada página.
 */
final class Tabla {

    /** Renglones que admite el rótulo de una columna. */
    private static final int RENGLONES_CABECERA = 2;

    /** Renglones que admite una celda antes de recortarse. */
    private static final int RENGLONES_CELDA = 4;

    private final EstiloRide estilo;
    private final List<Boolean> alineadas;
    private final List<Float> anchos;
    private final List<List<String>> cabecera;
    private final List<List<List<String>>> filas;
    private final List<Float> altos;
    private final float altoCabecera;

    private Tabla(
        EstiloRide estilo,
        List<Boolean> alineadas,
        List<Float> anchos,
        List<List<String>> cabecera,
        List<List<List<String>>> filas,
        List<Float> altos
    ) {
        this.estilo = estilo;
        this.alineadas = alineadas;
        this.anchos = anchos;
        this.cabecera = cabecera;
        this.filas = filas;
        this.altos = altos;
        this.altoCabecera = alto(cabecera, estilo);
    }

    /**
     * Mide una tabla: descarta las columnas opcionales que ninguna fila llena, reparte el ancho y
     * parte cada celda en renglones.
     *
     * @param candidatas columnas en el orden en que se dibujan
     * @param ancho      ancho total de que dispone la tabla, en puntos
     */
    static <R> Tabla de(List<Columna<R>> candidatas, List<R> lineas, PDFont fuente, PDFont fuenteRotulos, EstiloRide estilo, float ancho) {
        var columnas = columnasDe(candidatas, lineas);
        var anchos = anchosDe(columnas, ancho);

        var alineadas = new ArrayList<Boolean>();
        var cabecera = new ArrayList<List<String>>();
        for (int i = 0; i < columnas.size(); i++) {
            alineadas.add(columnas.get(i).alineadaDerecha());
            cabecera.add(Medidas.partir(fuenteRotulos, estilo.tamChico(), columnas.get(i).rotulo(), anchos.get(i) - 6, RENGLONES_CABECERA));
        }

        var filas = new ArrayList<List<List<String>>>();
        var altos = new ArrayList<Float>();
        for (var linea : lineas) {
            var celdas = celdas(fuente, linea, columnas, anchos, estilo);
            filas.add(celdas);
            altos.add(alto(celdas, estilo));
        }
        return new Tabla(estilo, List.copyOf(alineadas), List.copyOf(anchos), List.copyOf(cabecera), List.copyOf(filas), List.copyOf(altos));
    }

    /**
     * Alto de cada fila, en el orden en que se dibujan.
     */
    List<Float> altos() {
        return altos;
    }

    /**
     * Alto de la fila de rótulos.
     */
    float altoCabecera() {
        return altoCabecera;
    }

    /**
     * Ancho que ocupa la tabla dibujada.
     */
    float anchoTotal() {
        float total = 0;
        for (var ancho : anchos) {
            total += ancho;
        }
        return total;
    }

    /**
     * Dibuja las filas de {@code desde} a {@code hasta}, sin incluir esta última, con su cabecera.
     *
     * @return la altura a la que queda el pie de la tabla
     */
    float dibujar(Pluma pluma, float x, float arriba, int desde, int hasta) throws IOException {
        var tramo = filas.subList(desde, hasta);
        float ancho = anchoTotal();

        float alto = altoCabecera;
        for (var fila : tramo) {
            alto += alto(fila, estilo);
        }
        float abajo = arriba - alto;

        pluma.lienzo().recortarA(x, abajo, ancho, alto, estilo.radio());
        pluma.lienzo().relleno(x, arriba - altoCabecera, ancho, altoCabecera, estilo.paleta().fondoCabecera(), estilo.paleta().textoCabecera());
        pluma.rayaDeCabecera(x, arriba - altoCabecera, x + ancho);

        float columna = x;
        for (int i = 0; i < anchos.size(); i++) {
            if (i > 0) {
                pluma.divisionVertical(columna, abajo, arriba);
            }
            pluma.renglones(pluma.fuenteRotulo(), columna + 3, arriba, cabecera.get(i));
            columna += anchos.get(i);
        }

        float raya = arriba - altoCabecera;
        for (var fila : tramo.subList(0, Math.max(0, tramo.size() - 1))) {
            raya -= alto(fila, estilo);
            pluma.divisionHorizontal(x, raya, x + ancho);
        }
        pluma.lienzo().finRecorte();
        pluma.bloque(x, abajo, ancho, alto);
        pluma.lienzo().colorRelleno(estilo.paleta().texto());

        float y = arriba - altoCabecera;
        for (var fila : tramo) {
            float celda = x;
            for (int i = 0; i < anchos.size(); i++) {
                float anchoCelda = anchos.get(i);
                if (alineadas.get(i)) {
                    pluma.lienzo().textoDerecha(pluma.fuenteValor(), estilo.tamChico(), celda + anchoCelda - 3, y - estilo.espacio(9), fila.get(i).getFirst());
                } else {
                    pluma.renglones(pluma.fuenteValor(), celda + 3, y, fila.get(i));
                }
                celda += anchoCelda;
            }
            y -= alto(fila, estilo);
        }
        return abajo;
    }

    /**
     * Columnas que se dibujan: las fijas más las opcionales que alguna fila llena.
     */
    private static <R> List<Columna<R>> columnasDe(List<Columna<R>> candidatas, List<R> lineas) {
        var columnas = new ArrayList<Columna<R>>();
        for (var columna : candidatas) {
            if (!columna.opcional() || lineas.stream().anyMatch(linea -> !columna.valor().apply(linea).isEmpty())) {
                columnas.add(columna);
            }
        }
        return columnas;
    }

    /**
     * Ancho de cada columna, en el orden de {@code columnas}. La columna de ancho cero recibe lo
     * que sobre.
     */
    private static List<Float> anchosDe(List<? extends Columna<?>> columnas, float ancho) {
        float ocupado = 0;
        for (var columna : columnas) {
            ocupado += columna.ancho();
        }
        var anchos = new ArrayList<Float>();
        for (var columna : columnas) {
            anchos.add(columna.ancho() == 0 ? ancho - ocupado : columna.ancho());
        }
        return anchos;
    }

    /**
     * Reparte una fila en sus columnas, cada una ya partida en renglones.
     */
    private static <R> List<List<String>> celdas(PDFont fuente, R linea, List<Columna<R>> columnas, List<Float> anchos, EstiloRide estilo) {
        var celdas = new ArrayList<List<String>>();
        for (int i = 0; i < columnas.size(); i++) {
            var contenido = columnas.get(i).valor().apply(linea);
            celdas.add(columnas.get(i).alineadaDerecha()
                ? List.of(contenido)
                : Medidas.partir(fuente, estilo.tamChico(), contenido, anchos.get(i) - 6, RENGLONES_CELDA));
        }
        return List.copyOf(celdas);
    }

    /**
     * Alto de una fila: el de su celda más alta, más el aire de arriba y abajo.
     */
    private static float alto(List<List<String>> fila, EstiloRide estilo) {
        int renglones = 1;
        for (var celda : fila) {
            renglones = Math.max(renglones, celda.size());
        }
        return estilo.espacio(4) + renglones * estilo.espacio(9);
    }
}
