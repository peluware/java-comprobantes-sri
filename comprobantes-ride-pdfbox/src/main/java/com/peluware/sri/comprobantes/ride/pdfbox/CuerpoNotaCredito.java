package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.notacredito.DetalleNotaCredito;
import com.peluware.sri.comprobantes.notacredito.NotaCredito;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.List;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;

/**
 * El cuerpo del RIDE de una nota de crédito.
 *
 * <p>Anexo 2, página 61. Justifica la corrección con líneas de producto, así que su tabla es la
 * del detalle, y no sobreescribe las formas de pago porque una nota de crédito no se paga.
 */
final class CuerpoNotaCredito extends CuerpoNota<NotaCredito> {

    CuerpoNotaCredito(NotaCredito nota, PDFont normal, PDFont negrita, EstiloRide estilo) {
        super(nota, normal, negrita, estilo);
    }

    @Override
    protected Tabla tabla() {
        return Tabla.de(columnas(estilo.formatoAdicionales()), comprobante.detalles(), normal, negrita, estilo, ANCHO_UTIL);
    }

    @Override
    protected Cifras cifras() {
        var totales = comprobante.info().totales();
        return new Cifras(totales.totalConImpuestos(), totales.totalSinImpuestos(), totales.valorModificacion());
    }

    @Override
    protected String motivo() {
        return comprobante.info().motivo();
    }

    /**
     * Las columnas del detalle. Su esquema nombra los códigos del ítem {@code codigoInterno} y
     * {@code codigoAdicional}, y no declara unidad de medida.
     */
    private static List<Columna<DetalleNotaCredito>> columnas(FormatoAdicionales formato) {
        return ColumnasDetalle.de(formato,
            linea -> Formatos.texto(linea.codigoInterno()),
            linea -> Formatos.texto(linea.codigoAdicional()),
            linea -> Formatos.cantidad(linea.cantidad().valor())
        );
    }
}
