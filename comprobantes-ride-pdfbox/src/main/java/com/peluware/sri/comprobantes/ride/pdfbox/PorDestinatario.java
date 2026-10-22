package com.peluware.sri.comprobantes.ride.pdfbox;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;
import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.MARGEN;

/**
 * La maquetación de la guía de remisión: un recuadro de transportista arriba y debajo un bloque
 * por destinatario, cada uno con sus datos de traslado y su propia tabla.
 *
 * <p>Anexo 2, página 64. Es la única que no es un {@link ReceptorConTabla} porque repite la
 * estructura entera una vez por destinatario, y cada bloque es una fila que no se parte entre
 * páginas.
 */
final class PorDestinatario implements Maquetacion {

    /** Sangría de la tabla de detalle dentro del bloque del destinatario. */
    static final float SANGRIA = 12;

    /** Aire del recuadro, repartido entre arriba y abajo. */
    private static final float AIRE = 12;

    private final EstiloRide estilo;
    private final Pares transportista;
    private final List<Destino> destinos;
    private final List<Float> altos;

    PorDestinatario(EstiloRide estilo, Pares transportista, List<Destino> destinos) {
        this.estilo = Objects.requireNonNull(estilo, "estilo");
        this.transportista = Objects.requireNonNull(transportista, "transportista");
        this.destinos = List.copyOf(destinos);
        this.altos = this.destinos.stream().map(destino -> destino.alto(estilo)).toList();
    }

    /**
     * Un destinatario con sus datos de traslado y su tabla de detalle, ya medidos.
     */
    record Destino(Pares pares, Tabla tabla) {

        /**
         * Alto del recuadro: sus pares, la tabla y el mismo aire bajo ella que la sangría deja a
         * los lados.
         */
        float altoCaja(EstiloRide estilo) {
            float alto = pares.alto() + estilo.espacio(11) + tabla.altoCabecera() + estilo.espacio(SANGRIA);
            for (var fila : tabla.altos()) {
                alto += fila;
            }
            return alto;
        }

        /**
         * Alto de la fila: el recuadro más el aire que lo separa del siguiente.
         */
        float alto(EstiloRide estilo) {
            return altoCaja(estilo) + estilo.espacio(10);
        }
    }

    @Override
    public List<Float> altos() {
        return altos;
    }

    @Override
    public float altoPorPagina() {
        return 0;
    }

    @Override
    public float altoPropio() {
        return transportista.alto() + estilo.espacio(AIRE);
    }

    @Override
    public float encabezado(Pluma pluma, float arriba) throws IOException {
        float alto = altoPropio();
        pluma.bloque(MARGEN, arriba - alto, ANCHO_UTIL, alto);
        transportista.dibujar(pluma, arriba);
        return arriba - alto;
    }

    @Override
    public float filas(Pluma pluma, float arriba, int desde, int hasta) throws IOException {
        float y = arriba;
        for (int i = desde; i < hasta; i++) {
            y = destino(pluma, destinos.get(i), y);
        }
        return y;
    }

    /**
     * Dibuja el bloque de un destinatario: sus datos de traslado y debajo su tabla de detalle.
     *
     * @return la altura a la que queda su pie
     */
    private float destino(Pluma pluma, Destino destino, float arriba) throws IOException {
        float alto = destino.altoCaja(estilo);
        pluma.bloque(MARGEN, arriba - alto, ANCHO_UTIL, alto);

        float y = destino.pares().dibujar(pluma, arriba) - estilo.espacio(11);
        destino.tabla().dibujar(pluma, MARGEN + SANGRIA, y, 0, destino.tabla().altos().size());
        return arriba - destino.alto(estilo);
    }
}
