package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.factura.FacturaWriter;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemision;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemisionWriter;
import com.peluware.sri.comprobantes.interno.Bloques;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompra;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompraWriter;
import com.peluware.sri.comprobantes.notacredito.NotaCredito;
import com.peluware.sri.comprobantes.notacredito.NotaCreditoWriter;
import com.peluware.sri.comprobantes.notadebito.NotaDebito;
import com.peluware.sri.comprobantes.notadebito.NotaDebitoWriter;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencion;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencionWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Escribe el XML de un comprobante.
 *
 * <p>Los seis esquemas comparten el esqueleto del documento —el elemento raíz con sus atributos
 * {@code id} y {@code version}, luego {@code infoTributaria}, el bloque propio del comprobante,
 * su colección, {@code maquinaFiscal} e {@code infoAdicional}— y se diferencian en el contenido
 * de los dos bloques del medio. Esta clase escribe el esqueleto y deja esos dos a {@link #info}
 * y {@link #cuerpo}.
 *
 * <p>Cada subtipo expone su única instancia. Para escribir un comprobante cuyo tipo no se conoce
 * en tiempo de compilación está {@link #xml(Comprobante)}, que despacha sobre él.
 *
 * @param <C> comprobante que escribe
 */
public abstract sealed class ComprobanteWriter<C extends Comprobante> permits ComprobanteRetencionWriter, ComprobanteVentaWriter, GuiaRemisionWriter, ComprobanteNotaWriter {

    protected ComprobanteWriter() {
    }

    /**
     * El XML del comprobante, sin firmar, despachando sobre su tipo.
     *
     * <p>La firma la añade después un {@code Firmador}.
     *
     * @throws EscrituraXmlException si falla la escritura
     */
    public static byte[] xml(Comprobante comprobante) {
        var destino = new ByteArrayOutputStream();
        xml(comprobante, destino);
        return destino.toByteArray();
    }

    /**
     * Escribe el XML del comprobante en {@code destino}, que no se cierra, despachando sobre su
     * tipo.
     *
     * @throws EscrituraXmlException si falla la escritura en el destino
     */
    public static void xml(Comprobante comprobante, OutputStream destino) {
        switch (comprobante) {
            case Factura c -> FacturaWriter.INSTANCIA.write(c, destino);
            case LiquidacionCompra c -> LiquidacionCompraWriter.INSTANCIA.write(c, destino);
            case NotaCredito c -> NotaCreditoWriter.INSTANCIA.write(c, destino);
            case NotaDebito c -> NotaDebitoWriter.INSTANCIA.write(c, destino);
            case GuiaRemision c -> GuiaRemisionWriter.INSTANCIA.write(c, destino);
            case ComprobanteRetencion c -> ComprobanteRetencionWriter.INSTANCIA.write(c, destino);
        }
    }

    /**
     * Escribe el comprobante en {@code destino}, que no se cierra.
     *
     * @throws EscrituraXmlException si falla la escritura en el destino
     */
    public final void write(C comprobante, OutputStream destino) {
        var codificacion = Sri.CODIFICACION.name();
        try {
            var writer = XMLOutputFactory.newFactory().createXMLStreamWriter(destino, codificacion);
            writer.writeStartDocument(codificacion, "1.0");
            documento(writer, comprobante);
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new EscrituraXmlException("No se pudo escribir el XML de " + esquema().nombre() + " " + comprobante.claveAcceso(), e);
        }
    }

    /**
     * Escribe el comprobante en memoria.
     *
     * @throws EscrituraXmlException si falla la escritura
     */
    public final byte[] toByteArray(C comprobante) {
        var destino = new ByteArrayOutputStream();
        write(comprobante, destino);
        return destino.toByteArray();
    }

    /**
     * Los nombres fijos del esquema que escribe este writer.
     */
    protected abstract Esquema esquema();

    /**
     * Escribe el contenido del bloque propio del comprobante. El elemento lo abre y lo cierra
     * esta clase con el nombre que da {@link Esquema#tagInfo()}.
     */
    protected abstract void info(XMLStreamWriter writer, C comprobante) throws XMLStreamException;

    /**
     * Escribe la colección que sigue al bloque propio: los detalles, los destinatarios, los
     * motivos o los documentos de sustento, según el comprobante.
     */
    protected abstract void cuerpo(XMLStreamWriter writer, C comprobante) throws XMLStreamException;

    private void documento(XMLStreamWriter writer, C comprobante) throws XMLStreamException {
        var esquema = esquema();

        writer.writeStartElement(esquema.raiz());
        writer.writeAttribute("id", Sri.ID_COMPROBANTE);
        writer.writeAttribute("version", esquema.version());

        Bloques.infoTributaria(writer, comprobante);

        writer.writeStartElement(esquema.tagInfo());
        info(writer, comprobante);
        writer.writeEndElement();

        cuerpo(writer, comprobante);
        Bloques.maquinaFiscal(writer, comprobante.maquinaFiscal());
        Bloques.infoAdicional(writer, comprobante);

        writer.writeEndElement();
    }
}
