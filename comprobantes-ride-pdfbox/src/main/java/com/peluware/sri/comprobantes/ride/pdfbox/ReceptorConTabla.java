package com.peluware.sri.comprobantes.ride.pdfbox;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.MARGEN;

/**
 * La maquetación de cinco de los seis modelos: un recuadro de receptor arriba y una tabla que se
 * reparte en páginas, repitiendo su fila de rótulos.
 *
 * <p>Anexo 2, páginas 60 a 63 y 65. La usan la factura, la liquidación de compra, las dos notas y
 * el comprobante de retención, que son tres familias fiscales distintas: por eso esto es una
 * pieza y no un nivel de herencia.
 */
final class ReceptorConTabla implements Maquetacion {

    private final Receptor receptor;
    private final Pares pares;
    private final Tabla tabla;

    ReceptorConTabla(EstiloRide estilo, Pares pares, Tabla tabla) {
        this.receptor = new Receptor(Objects.requireNonNull(estilo, "estilo"));
        this.pares = Objects.requireNonNull(pares, "pares");
        this.tabla = Objects.requireNonNull(tabla, "tabla");
    }

    @Override
    public List<Float> altos() {
        return tabla.altos();
    }

    @Override
    public float altoPorPagina() {
        return tabla.altoCabecera();
    }

    @Override
    public float altoPropio() {
        return receptor.alto(pares);
    }

    @Override
    public float encabezado(Pluma pluma, float arriba) throws IOException {
        return receptor.dibujar(pluma, pares, arriba);
    }

    @Override
    public float filas(Pluma pluma, float arriba, int desde, int hasta) throws IOException {
        return tabla.dibujar(pluma, MARGEN, arriba, desde, hasta);
    }
}
