package com.peluware.sri.comprobantes.notacredito;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteNotaWriter;
import com.peluware.sri.comprobantes.interno.Bloques;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static com.peluware.sri.comprobantes.interno.Bloques.elemento;
import static com.peluware.sri.comprobantes.interno.Bloques.elementoOpcional;

/**
 * Escribe una {@link NotaCredito} como XML del esquema del SRI.
 *
 * <p>Produce el comprobante sin firmar, con el atributo {@code id="comprobante"} que la firma
 * necesita para referenciarlo. La firma la añade después un {@code Firmador}.
 *
 * <p>Los dos códigos de cada línea se escriben como {@code codigoInterno} y
 * {@code codigoAdicional}.
 *
 * <p>El XML sale sin indentar.
 */
public final class NotaCreditoWriter extends ComprobanteNotaWriter<NotaCredito> {

    /**
     * Única instancia.
     */
    public static final NotaCreditoWriter INSTANCIA = new NotaCreditoWriter();

    private NotaCreditoWriter() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.NOTA_CREDITO;
    }

    @Override
    protected void tras(XMLStreamWriter writer, NotaCredito notaCredito) throws XMLStreamException {
        var info = notaCredito.info();
        var totales = info.totales();

        elemento(writer, "totalSinImpuestos", totales.totalSinImpuestos().toString());
        elemento(writer, "valorModificacion", totales.valorModificacion().toString());
        elemento(writer, "moneda", info.moneda());
        totalConImpuestos(writer, totales);
        elemento(writer, "motivo", info.motivo());
    }

    @Override
    protected void cuerpo(XMLStreamWriter writer, NotaCredito notaCredito) throws XMLStreamException {
        Bloques.detalles(writer, notaCredito.detalles(), (w, detalle) -> {
            elementoOpcional(w, "codigoInterno", detalle.codigoInterno());
            elementoOpcional(w, "codigoAdicional", detalle.codigoAdicional());
            elemento(w, "descripcion", detalle.descripcion());
        });
    }

    /**
     * Escribe {@code <totalConImpuestos>} de la nota de crédito, cuyo total no declara
     * {@code tarifa}.
     */
    private static void totalConImpuestos(XMLStreamWriter writer, TotalesNotaCredito totales) throws XMLStreamException {
        writer.writeStartElement("totalConImpuestos");
        for (var total : totales.totalConImpuestos()) {
            writer.writeStartElement("totalImpuesto");
            elemento(writer, "codigo", total.codigo().codigo());
            elemento(writer, "codigoPorcentaje", total.codigoPorcentaje());
            elemento(writer, "baseImponible", total.baseImponible().toString());
            elemento(writer, "valor", total.valor().toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
