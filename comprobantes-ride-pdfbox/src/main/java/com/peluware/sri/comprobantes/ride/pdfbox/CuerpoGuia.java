package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.guiaremision.Destinatario;
import com.peluware.sri.comprobantes.guiaremision.DetalleGuiaRemision;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemision;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.ArrayList;
import java.util.List;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;
import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.MARGEN;
import static com.peluware.sri.comprobantes.ride.pdfbox.Receptor.anchoCompleto;
import static com.peluware.sri.comprobantes.ride.pdfbox.Receptor.izquierda;

/**
 * El cuerpo del RIDE de una guía de remisión: el recuadro del transportista, un bloque por
 * destinatario y la información adicional.
 *
 * <p>Anexo 2, página 64. Cada destinatario es una fila del cuerpo, con sus datos de traslado y su
 * propia tabla de detalle, y no se parte entre páginas.
 */
final class CuerpoGuia extends Cuerpo<GuiaRemision> {

    CuerpoGuia(GuiaRemision guia, PDFont normal, PDFont negrita, EstiloRide estilo) {
        super(guia, normal, negrita, estilo);
    }

    @Override
    protected Maquetacion armarMaquetacion() {
        return new PorDestinatario(estilo, transportista(comprobante, normal, negrita, estilo), destinos(comprobante, normal, negrita, estilo));
    }

    @Override
    protected Cierre.Contenido armarContenido() {
        return Cierre.Contenido.soloAdicionales(Cierre.adicionales(comprobante.infoAdicional(), normal, negrita, estilo));
    }

    /**
     * Un bloque por destinatario, cada uno con sus datos de traslado y su propia tabla.
     */
    private static List<PorDestinatario.Destino> destinos(GuiaRemision guia, PDFont normal, PDFont negrita, EstiloRide estilo) {
        var destinos = new ArrayList<PorDestinatario.Destino>();
        for (var destinatario : guia.destinatarios()) {
            var tabla = Tabla.de(columnas(), destinatario.detalles(), normal, negrita, estilo, ANCHO_UTIL - 2 * PorDestinatario.SANGRIA);
            destinos.add(new PorDestinatario.Destino(pares(destinatario, normal, negrita, estilo), tabla));
        }
        return List.copyOf(destinos);
    }

    private static Pares transportista(GuiaRemision guia, PDFont normal, PDFont negrita, EstiloRide estilo) {
        var info = guia.infoGuiaRemision();
        var transportista = info.transportista();

        return Pares.constructor(normal, negrita, estilo)
            .fila(izquierda(), anchoCompleto(), "Identificación (Transportista):", transportista.identificacion())
            .fila(izquierda(), anchoCompleto(), "Razón Social / Nombres y Apellidos:", transportista.razonSocial())
            .fila(izquierda(), anchoCompleto(), "Placa:", info.placa())
            .fila(izquierda(), anchoCompleto(), "Punto de Partida:", info.direccionPartida())
            .fila(izquierda(), ANCHO_UTIL / 2 - 12, "Fecha Inicio Transporte:", Formatos.FECHA.format(info.fechaInicio()), MARGEN + ANCHO_UTIL / 2, ANCHO_UTIL / 2 - 6, "Fecha Fin Transporte:", Formatos.FECHA.format(info.fechaFin()))
            .fila(izquierda(), anchoCompleto(), "RISE:", info.rise())
            .build();
    }

    /**
     * Los datos de traslado de un destinatario; las filas que el esquema admite vacías se omiten
     * cuando no vienen.
     */
    private static Pares pares(Destinatario destinatario, PDFont normal, PDFont negrita, EstiloRide estilo) {
        var constructor = Pares.constructor(normal, negrita, estilo);
        float x = izquierda();
        float ancho = anchoCompleto();

        var sustento = destinatario.sustentoTraslado();
        if (sustento != null) {
            constructor.fila(x, ancho, "Comprobante de Venta:", Formatos.titulo(sustento.tipoComprobante()) + "  " + sustento.numero());
            if (sustento.fechaEmision() != null) {
                constructor.fila(x, ancho, "Fecha de Emisión:", Formatos.FECHA.format(sustento.fechaEmision()));
            }
            constructor.fila(x, ancho, "Número de Autorización:", sustento.numeroAutorizacion());
        }
        return constructor
            .fila(x, ancho, "Motivo Traslado:", destinatario.motivoTraslado())
            .fila(x, ancho, "Destino (Punto de Llegada):", destinatario.direccion())
            .fila(x, ancho, "Identificación (Destinatario):", destinatario.identificacion())
            .fila(x, ancho, "Razón Social / Nombres y Apellidos:", destinatario.razonSocial())
            .fila(x, ancho, "Documento Aduanero:", destinatario.documentoAduanero())
            .fila(x, ancho, "Código Establecimiento Destino:", destinatario.codigoEstablecimientoDestino())
            .fila(x, ancho, "Ruta:", destinatario.ruta())
            .build();
    }

    /**
     * Las columnas de la tabla de detalle de cada destinatario. Anexo 2, página 64.
     */
    private static List<Columna<DetalleGuiaRemision>> columnas() {
        return List.of(
            Columna.derecha("Cantidad", 60, linea -> Formatos.cantidad(linea.cantidad().valor())),
            Columna.elastica("Descripción", DetalleGuiaRemision::descripcion),
            Columna.opcional("Código Principal", 100, linea -> Formatos.texto(linea.codigoInterno())),
            Columna.opcional("Código Auxiliar", 100, linea -> Formatos.texto(linea.codigoAdicional()))
        );
    }
}
