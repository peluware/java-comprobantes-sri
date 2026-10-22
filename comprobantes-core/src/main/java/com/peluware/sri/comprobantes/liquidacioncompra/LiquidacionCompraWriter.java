package com.peluware.sri.comprobantes.liquidacioncompra;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteVentaWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static com.peluware.sri.comprobantes.interno.Bloques.elemento;
import static com.peluware.sri.comprobantes.interno.Bloques.elementoOpcional;

/**
 * Escribe una {@link LiquidacionCompra} como XML del esquema del SRI.
 *
 * <p>Produce el comprobante sin firmar, con el atributo {@code id="comprobante"} que la firma
 * necesita para referenciarlo.
 *
 * <p>El XML sale sin indentar.
 */
public final class LiquidacionCompraWriter extends ComprobanteVentaWriter<LiquidacionCompra> {

    /**
     * Única instancia.
     */
    public static final LiquidacionCompraWriter INSTANCIA = new LiquidacionCompraWriter();

    private LiquidacionCompraWriter() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.LIQUIDACION_COMPRA;
    }

    @Override
    protected void contraparte(XMLStreamWriter writer, LiquidacionCompra liquidacion) throws XMLStreamException {
        var proveedor = liquidacion.info().proveedor();

        elemento(writer, "tipoIdentificacionProveedor", proveedor.tipoIdentificacion().codigo());
        elemento(writer, "razonSocialProveedor", proveedor.razonSocial());
        elemento(writer, "identificacionProveedor", proveedor.identificacion());
        elementoOpcional(writer, "direccionProveedor", proveedor.direccion());
    }
}
