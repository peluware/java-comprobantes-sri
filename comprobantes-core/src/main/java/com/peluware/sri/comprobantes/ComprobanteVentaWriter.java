package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.factura.FacturaWriter;
import com.peluware.sri.comprobantes.interno.Bloques;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompraWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import static com.peluware.sri.comprobantes.interno.Bloques.FECHA;
import static com.peluware.sri.comprobantes.interno.Bloques.NO;
import static com.peluware.sri.comprobantes.interno.Bloques.SI;
import static com.peluware.sri.comprobantes.interno.Bloques.elemento;
import static com.peluware.sri.comprobantes.interno.Bloques.elementoOpcional;

/**
 * Escribe el XML de un comprobante de venta.
 *
 * <p>La factura y la liquidación de compra declaran su bloque propio en el mismo orden y se
 * separan en dos sitios: el bloque de la contraparte, que una nombra del comprador y la otra del
 * proveedor, y lo que la factura intercala entre los totales y el importe. Esta clase escribe la
 * secuencia y deja esos dos a {@link #contraparte} y {@link #sobreTotales}.
 *
 * <p>Los detalles y los totales por impuesto sí son idénticos, así que los escribe entera.
 *
 * @param <C> comprobante de venta que escribe
 */
public abstract sealed class ComprobanteVentaWriter<C extends ComprobanteVenta> extends ComprobanteWriter<C> permits FacturaWriter, LiquidacionCompraWriter {

    protected ComprobanteVentaWriter() {
    }

    /**
     * Escribe el bloque de la contraparte, que va tras {@code obligadoContabilidad}.
     */
    protected abstract void contraparte(XMLStreamWriter writer, C comprobante) throws XMLStreamException;

    /**
     * Escribe lo que va entre {@code totalConImpuestos} e {@code importeTotal}. Por omisión,
     * nada: solo la factura declara propina ahí.
     */
    protected void sobreTotales(XMLStreamWriter writer, C comprobante) throws XMLStreamException {
    }

    @Override
    protected final void info(XMLStreamWriter writer, C comprobante) throws XMLStreamException {
        var emisor = comprobante.emisor();
        var info = comprobante.info();
        var totales = info.totales();

        elemento(writer, "fechaEmision", FECHA.format(info.fechaEmision()));
        elementoOpcional(writer, Establecimiento.CAMPO_DIRECCION, comprobante.establecimiento().direccion());
        Bloques.contribuyenteEspecial(writer, emisor.contribuyenteEspecial());
        elemento(writer, "obligadoContabilidad", emisor.obligadoContabilidad() ? SI : NO);
        contraparte(writer, comprobante);
        elemento(writer, "totalSinImpuestos", totales.totalSinImpuestos().toString());
        elemento(writer, "totalDescuento", totales.totalDescuento().toString());
        totalConImpuestos(writer, totales);
        sobreTotales(writer, comprobante);
        elemento(writer, "importeTotal", totales.importeTotal().toString());
        elemento(writer, "moneda", info.moneda());
        Bloques.pagos(writer, info.pagos());
    }

    /**
     * Escribe {@code <detalles>}, cuyas líneas los dos esquemas declaran igual.
     */
    @Override
    protected final void cuerpo(XMLStreamWriter writer, C comprobante) throws XMLStreamException {
        Bloques.detalles(writer, comprobante.detalles(), (w, detalle) -> {
            elementoOpcional(w, "codigoPrincipal", detalle.codigoPrincipal());
            elementoOpcional(w, "codigoAuxiliar", detalle.codigoAuxiliar());
            elemento(w, "descripcion", detalle.descripcion());
            elementoOpcional(w, "unidadMedida", detalle.unidadMedida());
        });
    }

    /**
     * Escribe {@code <totalConImpuestos>}, cuyo total declara {@code tarifa}.
     */
    private static void totalConImpuestos(XMLStreamWriter writer, TotalesComprobanteVenta totales) throws XMLStreamException {
        writer.writeStartElement("totalConImpuestos");
        for (var total : totales.totalConImpuestos()) {
            writer.writeStartElement("totalImpuesto");
            elemento(writer, "codigo", total.codigo().codigo());
            elemento(writer, "codigoPorcentaje", total.codigoPorcentaje());
            elemento(writer, "baseImponible", total.baseImponible().toString());
            var tarifa = total.tarifa();
            if (tarifa != null) {
                elemento(writer, "tarifa", tarifa.toPlainString());
            }
            elemento(writer, "valor", total.valor().toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }
}
