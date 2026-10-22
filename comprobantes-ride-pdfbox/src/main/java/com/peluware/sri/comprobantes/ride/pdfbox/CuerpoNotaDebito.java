package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.TotalImpuesto;
import com.peluware.sri.comprobantes.notadebito.Motivo;
import com.peluware.sri.comprobantes.notadebito.NotaDebito;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.ArrayList;
import java.util.List;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;

/**
 * El cuerpo del RIDE de una nota de débito.
 *
 * <p>Anexo 2, página 62. En un recargo no se entregó nada que detallar, así que justifica la
 * corrección con sus {@link Motivo motivos} —una razón y su valor— en vez de líneas de producto,
 * y sí declara formas de pago, porque el importe que lleva se puede pagar.
 */
final class CuerpoNotaDebito extends CuerpoNota<NotaDebito> {

    CuerpoNotaDebito(NotaDebito nota, PDFont normal, PDFont negrita, EstiloRide estilo) {
        super(nota, normal, negrita, estilo);
    }

    @Override
    protected Tabla tabla() {
        return Tabla.de(columnas(), comprobante.motivos(), normal, negrita, estilo, ANCHO_UTIL);
    }

    @Override
    protected Cifras cifras() {
        var totales = comprobante.info().totales();
        var impuestos = new ArrayList<TotalImpuesto>();
        for (var impuesto : totales.impuestos()) {
            impuestos.add(new TotalImpuesto(impuesto.codigo(), impuesto.codigoPorcentaje(), impuesto.tarifa(), impuesto.baseImponible(), impuesto.valor()));
        }
        return new Cifras(List.copyOf(impuestos), totales.totalSinImpuestos(), totales.valorTotal());
    }

    @Override
    protected List<Pago> pagos() {
        return comprobante.info().pagos();
    }

    /**
     * Las columnas de la tabla de motivos.
     */
    private static List<Columna<Motivo>> columnas() {
        return List.of(
            Columna.elastica("Razón de la Modificación", Motivo::razon),
            Columna.derecha("Valor de la Modificación", 150, motivo -> motivo.valor().toString())
        );
    }
}
