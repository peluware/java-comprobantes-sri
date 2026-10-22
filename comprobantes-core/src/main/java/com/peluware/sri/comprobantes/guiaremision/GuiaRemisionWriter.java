package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteWriter;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.interno.Bloques;
import org.jspecify.annotations.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.List;

import static com.peluware.sri.comprobantes.interno.Bloques.FECHA;
import static com.peluware.sri.comprobantes.interno.Bloques.NO;
import static com.peluware.sri.comprobantes.interno.Bloques.SI;
import static com.peluware.sri.comprobantes.interno.Bloques.elemento;
import static com.peluware.sri.comprobantes.interno.Bloques.elementoOpcional;

/**
 * Escribe una {@link GuiaRemision} como XML del esquema del SRI.
 *
 * <p>No escribe {@code fechaEmision}: su esquema no declara ese elemento.
 *
 * <p>El XML sale sin indentar.
 */
public final class GuiaRemisionWriter extends ComprobanteWriter<GuiaRemision> {

    /**
     * Única instancia.
     */
    public static final GuiaRemisionWriter INSTANCIA = new GuiaRemisionWriter();

    private GuiaRemisionWriter() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.GUIA_REMISION;
    }


    @Override
    protected void info(XMLStreamWriter writer, GuiaRemision guia) throws XMLStreamException {
        var info = guia.infoGuiaRemision();
        var emisor = guia.emisor();
        var transportista = info.transportista();

        elementoOpcional(writer, Establecimiento.CAMPO_DIRECCION, guia.establecimiento().direccion());
        elemento(writer, "dirPartida", info.direccionPartida());
        elemento(writer, "razonSocialTransportista", transportista.razonSocial());
        elemento(writer, "tipoIdentificacionTransportista", transportista.tipoIdentificacion().codigo());
        elemento(writer, "rucTransportista", transportista.identificacion());
        elementoOpcional(writer, "rise", info.rise());
        elemento(writer, "obligadoContabilidad", emisor.obligadoContabilidad() ? SI : NO);
        Bloques.contribuyenteEspecial(writer, emisor.contribuyenteEspecial());
        elemento(writer, "fechaIniTransporte", FECHA.format(info.fechaInicio()));
        elemento(writer, "fechaFinTransporte", FECHA.format(info.fechaFin()));
        elemento(writer, "placa", info.placa());
    }

    @Override
    protected void cuerpo(XMLStreamWriter writer, GuiaRemision guia) throws XMLStreamException {
        writer.writeStartElement("destinatarios");
        for (var destinatario : guia.destinatarios()) {
            writer.writeStartElement("destinatario");
            elemento(writer, "identificacionDestinatario", destinatario.identificacion());
            elemento(writer, "razonSocialDestinatario", destinatario.razonSocial());
            elemento(writer, "dirDestinatario", destinatario.direccion());
            elemento(writer, "motivoTraslado", destinatario.motivoTraslado());
            elementoOpcional(writer, "docAduaneroUnico", destinatario.documentoAduanero());
            elementoOpcional(writer, "codEstabDestino", destinatario.codigoEstablecimientoDestino());
            elementoOpcional(writer, "ruta", destinatario.ruta());
            sustentoTraslado(writer, destinatario.sustentoTraslado());
            detalles(writer, destinatario.detalles());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void sustentoTraslado(XMLStreamWriter writer, @Nullable SustentoTraslado sustento) throws XMLStreamException {
        if (sustento == null) {
            return;
        }
        elemento(writer, "codDocSustento", sustento.tipoComprobante().codigo());
        elemento(writer, "numDocSustento", sustento.numero());
        elementoOpcional(writer, "numAutDocSustento", sustento.numeroAutorizacion());
        var fecha = sustento.fechaEmision();
        if (fecha != null) {
            elemento(writer, "fechaEmisionDocSustento", FECHA.format(fecha));
        }
    }

    private static void detalles(XMLStreamWriter writer, List<DetalleGuiaRemision> detalles) throws XMLStreamException {
        Bloques.lineas(writer, detalles, (w, detalle) -> {
            elementoOpcional(w, "codigoInterno", detalle.codigoInterno());
            elementoOpcional(w, "codigoAdicional", detalle.codigoAdicional());
            elemento(w, "descripcion", detalle.descripcion());
            elemento(w, "cantidad", detalle.cantidad().toString());
            Bloques.detallesAdicionales(w, detalle.detallesAdicionales());
        });
    }

}
