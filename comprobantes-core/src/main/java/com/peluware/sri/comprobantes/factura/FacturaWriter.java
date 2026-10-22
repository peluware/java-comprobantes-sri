package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteVentaWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static com.peluware.sri.comprobantes.interno.Bloques.elemento;
import static com.peluware.sri.comprobantes.interno.Bloques.elementoOpcional;

/**
 * Escribe una {@link Factura} como XML del esquema del SRI.
 *
 * <p>Produce el comprobante sin firmar, con el atributo {@code id="comprobante"} que la firma
 * necesita para referenciarlo. La firma la añade después un {@code Firmador}.
 *
 * <p>El XML sale sin indentar.
 */
public final class FacturaWriter extends ComprobanteVentaWriter<Factura> {

    /**
     * Única instancia.
     */
    public static final FacturaWriter INSTANCIA = new FacturaWriter();

    private FacturaWriter() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.FACTURA;
    }

    @Override
    protected void contraparte(XMLStreamWriter writer, Factura factura) throws XMLStreamException {
        var info = factura.info();
        var comprador = info.comprador();

        elemento(writer, "tipoIdentificacionComprador", comprador.tipoIdentificacion().codigo());
        elementoOpcional(writer, "guiaRemision", info.guiaRemision());
        elemento(writer, "razonSocialComprador", comprador.razonSocial());
        elemento(writer, "identificacionComprador", comprador.identificacion());
        elementoOpcional(writer, "direccionComprador", comprador.direccion());
    }

    @Override
    protected void sobreTotales(XMLStreamWriter writer, Factura factura) throws XMLStreamException {
        elemento(writer, "propina", factura.info().totales().propina().toString());
    }
}
