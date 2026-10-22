package com.peluware.sri.comprobantes.notadebito;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteNotaWriter;
import com.peluware.sri.comprobantes.interno.Bloques;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static com.peluware.sri.comprobantes.interno.Bloques.elemento;

/**
 * Escribe una {@link NotaDebito} como XML del esquema del SRI.
 *
 * <p>Produce el comprobante sin firmar, con el atributo {@code id="comprobante"} que la firma
 * necesita para referenciarlo. La firma la añade después un {@code Firmador}.
 *
 * <p>El XML sale sin indentar.
 */
public final class NotaDebitoWriter extends ComprobanteNotaWriter<NotaDebito> {

    /**
     * Única instancia.
     */
    public static final NotaDebitoWriter INSTANCIA = new NotaDebitoWriter();

    private NotaDebitoWriter() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.NOTA_DEBITO;
    }

    @Override
    protected void tras(XMLStreamWriter writer, NotaDebito notaDebito) throws XMLStreamException {
        var info = notaDebito.info();
        var totales = info.totales();

        elemento(writer, "totalSinImpuestos", totales.totalSinImpuestos().toString());
        Bloques.impuestos(writer, totales.impuestos());
        elemento(writer, "valorTotal", totales.valorTotal().toString());
        Bloques.pagos(writer, info.pagos());
    }

    @Override
    protected void cuerpo(XMLStreamWriter writer, NotaDebito notaDebito) throws XMLStreamException {
        writer.writeStartElement("motivos");
        for (var motivo : notaDebito.motivos()) {
            writer.writeStartElement("motivo");
            elemento(writer, "razon", motivo.razon());
            elemento(writer, "valor", motivo.valor().toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
