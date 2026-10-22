package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Bloques;
import com.peluware.sri.comprobantes.notacredito.NotaCreditoWriter;
import com.peluware.sri.comprobantes.notadebito.NotaDebitoWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static com.peluware.sri.comprobantes.interno.Bloques.FECHA;
import static com.peluware.sri.comprobantes.interno.Bloques.NO;
import static com.peluware.sri.comprobantes.interno.Bloques.SI;
import static com.peluware.sri.comprobantes.interno.Bloques.elemento;
import static com.peluware.sri.comprobantes.interno.Bloques.elementoOpcional;

/**
 * Escribe el XML de una nota que corrige un comprobante.
 *
 * <p>Los dos esquemas declaran la misma cabecera en el mismo orden, hasta el subtotal: quién
 * emite, a quién, y qué comprobante se corrige. Esta clase la escribe y deja a {@link #tras} lo
 * que cada nota declara después, que es donde se separan.
 *
 * @param <C> nota que escribe
 */
public abstract sealed class ComprobanteNotaWriter<C extends ComprobanteNota> extends ComprobanteWriter<C> permits NotaCreditoWriter, NotaDebitoWriter {

    protected ComprobanteNotaWriter() {
    }

    /**
     * Escribe lo que la nota declara tras {@code fechaEmisionDocSustento}: los totales como cada
     * esquema los nombre, y lo que cada una añada.
     */
    protected abstract void tras(XMLStreamWriter writer, C nota) throws XMLStreamException;

    @Override
    protected final void info(XMLStreamWriter writer, C nota) throws XMLStreamException {
        var info = nota.info();
        var emisor = nota.emisor();
        var comprador = info.comprador();
        var modificado = info.documentoModificado();

        elemento(writer, "fechaEmision", FECHA.format(info.fechaEmision()));
        elementoOpcional(writer, Establecimiento.CAMPO_DIRECCION, nota.establecimiento().direccion());
        elemento(writer, "tipoIdentificacionComprador", comprador.tipoIdentificacion().codigo());
        elemento(writer, "razonSocialComprador", comprador.razonSocial());
        elemento(writer, "identificacionComprador", comprador.identificacion());
        Bloques.contribuyenteEspecial(writer, emisor.contribuyenteEspecial());
        elemento(writer, "obligadoContabilidad", emisor.obligadoContabilidad() ? SI : NO);
        elementoOpcional(writer, "rise", info.rise());
        elemento(writer, "codDocModificado", modificado.tipoComprobante().codigo());
        elemento(writer, "numDocModificado", modificado.numero());
        elemento(writer, "fechaEmisionDocSustento", FECHA.format(modificado.fechaEmision()));
        tras(writer, nota);
    }
}
