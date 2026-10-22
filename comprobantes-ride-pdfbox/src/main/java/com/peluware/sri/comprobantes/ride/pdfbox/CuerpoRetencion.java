package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.retencion.ComprobanteRetencion;
import com.peluware.sri.comprobantes.retencion.DocumentoSustento;
import com.peluware.sri.comprobantes.retencion.Retencion;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.ArrayList;
import java.util.List;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;
import static com.peluware.sri.comprobantes.ride.pdfbox.Receptor.*;

/**
 * El cuerpo del RIDE de un comprobante de retención: el recuadro del sujeto retenido, la tabla de
 * retenciones y la información adicional.
 *
 * <p>Anexo 2, página 63. Cada fila de la tabla es una retención con los datos del documento que la
 * sustenta, de modo que un documento con varias retenciones ocupa varias filas. El modelo no lleva
 * totales ni formas de pago.
 */
final class CuerpoRetencion extends Cuerpo<ComprobanteRetencion> {

    CuerpoRetencion(ComprobanteRetencion retencion, PDFont normal, PDFont negrita, EstiloRide estilo) {
        super(retencion, normal, negrita, estilo);
    }

    @Override
    protected Maquetacion armarMaquetacion() {
        var ejercicio = comprobante.infoCompRetencion().periodoFiscal().valor();
        return new ReceptorConTabla(estilo, pares(comprobante, normal, negrita, estilo), Tabla.de(columnas(ejercicio), lineasDe(comprobante), normal, negrita, estilo, ANCHO_UTIL));
    }

    @Override
    protected Cierre.Contenido armarContenido() {
        return Cierre.Contenido.soloAdicionales(Cierre.adicionales(comprobante.infoAdicional(), normal, negrita, estilo));
    }

    /**
     * Una retención junto al documento que la sustenta.
     */
    private record Linea(DocumentoSustento documento, Retencion retencion) {
    }

    /**
     * Las retenciones de todos los documentos de sustento, en el orden en que vienen.
     */
    private static List<Linea> lineasDe(ComprobanteRetencion retencion) {
        var lineas = new ArrayList<Linea>();
        for (var documento : retencion.documentosSustento()) {
            for (var una : documento.retenciones()) {
                lineas.add(new Linea(documento, una));
            }
        }
        return List.copyOf(lineas);
    }

    /**
     * Las columnas de la tabla de retenciones. Anexo 2, página 63.
     */
    private static List<Columna<Linea>> columnas(String ejercicioFiscal) {
        return List.of(
            Columna.de("Comprobante", 70, linea -> Formatos.titulo(linea.documento().tipoComprobante())),
            Columna.elastica("Número", linea -> linea.documento().numero()),
            Columna.de("Fecha Emisión", 62, linea -> Formatos.FECHA.format(linea.documento().fechaEmision())),
            Columna.de("Ejercicio Fiscal", 52, linea -> ejercicioFiscal),
            Columna.derecha("Base Imponible para la Retención", 74, linea -> linea.retencion().baseImponible().toString()),
            Columna.de("Impuesto", 54, linea -> linea.retencion().codigo().name()),
            Columna.derecha("Porcentaje Retención", 56, linea -> linea.retencion().porcentajeEscrito()),
            Columna.derecha("Valor Retenido", 56, linea -> linea.retencion().valorRetenido().toString())
        );
    }

    /**
     * El recuadro del sujeto retenido, que no lleva dirección.
     */
    private static Pares pares(ComprobanteRetencion retencion, PDFont normal, PDFont negrita, EstiloRide estilo) {
        var info = retencion.infoCompRetencion();
        var sujeto = info.sujetoRetenido();

        return Pares.constructor(normal, negrita, estilo)
            .fila(izquierda(), anchoIzquierda(), "Razón Social / Nombres y Apellidos:", sujeto.razonSocial(), mitad(), anchoDerecha(), "Identificación:", sujeto.identificacion())
            .fila(izquierda(), anchoCompleto(), "Fecha de Emisión:", Formatos.FECHA.format(info.fechaEmision()))
            .build();
    }


}
