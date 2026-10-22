package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteWriter;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.interno.Bloques;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.List;

import static com.peluware.sri.comprobantes.interno.Bloques.FECHA;
import static com.peluware.sri.comprobantes.interno.Bloques.NO;
import static com.peluware.sri.comprobantes.interno.Bloques.SI;
import static com.peluware.sri.comprobantes.interno.Bloques.elemento;
import static com.peluware.sri.comprobantes.interno.Bloques.elementoOpcional;

/**
 * Escribe un {@link ComprobanteRetencion} como XML del esquema del SRI.
 *
 * <p>Este esquema nombra los impuestos del documento sustento {@code codImpuestoDocSustento} y
 * {@code valorImpuesto}, donde la factura escribe {@code codigo} y {@code valor}. Sus formas de
 * pago son {@code formaPago} y {@code total}, sin plazo.
 *
 * <p>El XML sale sin indentar.
 */
public final class ComprobanteRetencionWriter extends ComprobanteWriter<ComprobanteRetencion> {

    /**
     * Única instancia.
     */
    public static final ComprobanteRetencionWriter INSTANCIA = new ComprobanteRetencionWriter();

    private ComprobanteRetencionWriter() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.COMPROBANTE_RETENCION;
    }


    @Override
    protected void info(XMLStreamWriter writer, ComprobanteRetencion retencion) throws XMLStreamException {
        var info = retencion.infoCompRetencion();
        var emisor = retencion.emisor();
        var sujeto = info.sujetoRetenido();

        elemento(writer, "fechaEmision", FECHA.format(info.fechaEmision()));
        elementoOpcional(writer, Establecimiento.CAMPO_DIRECCION, retencion.establecimiento().direccion());
        Bloques.contribuyenteEspecial(writer, emisor.contribuyenteEspecial());
        elemento(writer, "obligadoContabilidad", emisor.obligadoContabilidad() ? SI : NO);
        elemento(writer, "tipoIdentificacionSujetoRetenido", sujeto.tipoIdentificacion().codigo());
        var tipoSujeto = sujeto.tipo();
        if (tipoSujeto != null) {
            elemento(writer, "tipoSujetoRetenido", tipoSujeto.codigo());
        }
        elemento(writer, "parteRel", sujeto.parteRelacionada() ? SI : NO);
        elemento(writer, "razonSocialSujetoRetenido", sujeto.razonSocial());
        elemento(writer, "identificacionSujetoRetenido", sujeto.identificacion());
        elemento(writer, "periodoFiscal", info.periodoFiscal().valor());
    }

    @Override
    protected void cuerpo(XMLStreamWriter writer, ComprobanteRetencion retencion) throws XMLStreamException {
        writer.writeStartElement("docsSustento");
        for (var documento : retencion.documentosSustento()) {
            writer.writeStartElement("docSustento");
            elemento(writer, "codSustento", documento.codigoSustento());
            elemento(writer, "codDocSustento", documento.tipoComprobante().codigo());
            elemento(writer, "numDocSustento", documento.numero());
            elemento(writer, "fechaEmisionDocSustento", FECHA.format(documento.fechaEmision()));
            var registro = documento.fechaRegistroContable();
            if (registro != null) {
                elemento(writer, "fechaRegistroContable", FECHA.format(registro));
            }
            elementoOpcional(writer, "numAutDocSustento", documento.numeroAutorizacion());
            elemento(writer, "pagoLocExt", documento.pagoLocalExterior().codigo());
            elementoOpcional(writer, "tipoRegi", documento.tipoRegimenFiscal());
            elementoOpcional(writer, "paisEfecPago", documento.paisEfectuaPago());
            afirmacion(writer, "aplicConvDobTrib", documento.aplicaConvenioDobleTributacion());
            afirmacion(writer, "pagExtSujRetNorLeg", documento.pagoExteriorSujetoRetencion());
            afirmacion(writer, "pagoRegFis", documento.pagoRegimenFiscalPreferente());
            elemento(writer, "totalSinImpuestos", documento.totalSinImpuestos().toString());
            elemento(writer, "importeTotal", documento.importeTotal().toString());
            impuestosDocSustento(writer, documento);
            retenciones(writer, documento);
            pagos(writer, documento.pagos());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Escribe una afirmación solo cuando es cierta: los tres tags son opcionales y solo aplican
     * a pagos al exterior.
     */
    private static void afirmacion(XMLStreamWriter writer, String nombre, boolean valor) throws XMLStreamException {
        if (valor) {
            elemento(writer, nombre, SI);
        }
    }

    private static void impuestosDocSustento(XMLStreamWriter writer, DocumentoSustento documento) throws XMLStreamException {
        writer.writeStartElement("impuestosDocSustento");
        for (var impuesto : documento.impuestos()) {
            writer.writeStartElement("impuestoDocSustento");
            elemento(writer, "codImpuestoDocSustento", impuesto.codigo().codigo());
            elemento(writer, "codigoPorcentaje", impuesto.codigoPorcentaje());
            elemento(writer, "baseImponible", impuesto.baseImponible().toString());
            elemento(writer, "tarifa", impuesto.tarifa().toPlainString());
            elemento(writer, "valorImpuesto", impuesto.valor().toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private static void retenciones(XMLStreamWriter writer, DocumentoSustento documento) throws XMLStreamException {
        writer.writeStartElement("retenciones");
        for (var retencion : documento.retenciones()) {
            writer.writeStartElement("retencion");
            elemento(writer, "codigo", retencion.codigo().codigo());
            elemento(writer, "codigoRetencion", retencion.codigoRetencion());
            elemento(writer, "baseImponible", retencion.baseImponible().toString());
            elemento(writer, "porcentajeRetener", retencion.porcentajeEscrito());
            elemento(writer, "valorRetenido", retencion.valorRetenido().toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Las formas de pago de este esquema son {@code formaPago} y {@code total}, sin plazo.
     */
    private static void pagos(XMLStreamWriter writer, List<Pago> pagos) throws XMLStreamException {
        writer.writeStartElement("pagos");
        for (var pago : pagos) {
            writer.writeStartElement("pago");
            elemento(writer, "formaPago", pago.formaPago().codigo());
            elemento(writer, "total", pago.total().toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
