package com.peluware.sri.comprobantes.interno;

import com.peluware.sri.comprobantes.ClaveAcceso;
import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.Detalle;
import com.peluware.sri.comprobantes.DetalleAdicional;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.Linea;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.valores.ContribuyenteEspecial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.jspecify.annotations.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bloques que el esquema repite igual en todos los comprobantes.
 *
 * <p>Los bloques que difieren entre tipos, como {@code totalConImpuestos}, los escribe cada
 * escritor.
 */
public final class Bloques {

    /**
     * Formato de fecha de los comprobantes.
     */
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public static final String SI = "SI";
    public static final String NO = "NO";

    private Bloques() {
    }

    /**
     * Escribe {@code <infoTributaria>}, idéntico en todos los comprobantes.
     */
    public static void infoTributaria(XMLStreamWriter writer, Comprobante comprobante) throws XMLStreamException {
        var emisor = comprobante.emisor();
        var serie = comprobante.serie();

        writer.writeStartElement("infoTributaria");
        elemento(writer, "ambiente", comprobante.ambiente().codigo());
        elemento(writer, "tipoEmision", comprobante.tipoEmision().codigo());
        elemento(writer, "razonSocial", emisor.razonSocial());
        elementoOpcional(writer, "nombreComercial", emisor.nombreComercial());
        elemento(writer, "ruc", emisor.ruc().valor());
        elemento(writer, "claveAcceso", comprobante.claveAcceso());
        elemento(writer, ClaveAcceso.CAMPO_CODIGO_DOCUMENTO, comprobante.tipoComprobante().codigo());
        elemento(writer, Serie.CAMPO_ESTABLECIMIENTO, serie.establecimiento());
        elemento(writer, Serie.CAMPO_PUNTO_EMISION, serie.puntoEmision());
        elemento(writer, "secuencial", comprobante.secuencial().valor());
        elemento(writer, Emisor.CAMPO_DIRECCION_MATRIZ, emisor.direccionMatriz());
        elementoOpcional(writer, "agenteRetencion", emisor.agenteRetencion());
        if (emisor.contribuyenteRimpe()) {
            elemento(writer, "contribuyenteRimpe", Sri.LEYENDA_RIMPE);
        }
        writer.writeEndElement();
    }

    /**
     * Escribe el bloque {@code <pagos>}, o nada si no se detalló ninguna forma de pago.
     *
     * <p>Ficha Técnica v2.34, Tabla 24. Es idéntico en los esquemas que lo admiten.
     */
    public static void pagos(XMLStreamWriter writer, List<Pago> pagos) throws XMLStreamException {
        if (pagos.isEmpty()) {
            return;
        }
        writer.writeStartElement("pagos");
        for (var pago : pagos) {
            writer.writeStartElement("pago");
            elemento(writer, "formaPago", pago.formaPago().codigo());
            elemento(writer, "total", pago.total().toString());
            var plazo = pago.plazo();
            if (plazo != null) {
                elemento(writer, "plazo", plazo.valor().toPlainString());
                elemento(writer, "unidadTiempo", plazo.unidadTiempo());
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Escribe {@code <maquinaFiscal>}, o nada si el comprobante no se emitió desde una.
     *
     * <p>Ficha Técnica v2.34, Anexo 13. Va entre {@code </detalles>} e {@code <infoAdicional>}.
     */
    public static void maquinaFiscal(XMLStreamWriter writer, @Nullable MaquinaFiscal maquina) throws XMLStreamException {
        if (maquina == null) {
            return;
        }
        writer.writeStartElement("maquinaFiscal");
        elemento(writer, "marca", maquina.marca());
        elemento(writer, "modelo", maquina.modelo());
        elemento(writer, "serie", maquina.serie());
        writer.writeEndElement();
    }

    /**
     * Escribe {@code <infoAdicional>}, o nada si el comprobante no lleva campos.
     */
    public static void infoAdicional(XMLStreamWriter writer, Comprobante comprobante) throws XMLStreamException {
        if (comprobante.infoAdicional().isEmpty()) {
            return;
        }
        writer.writeStartElement("infoAdicional");
        for (var campo : comprobante.infoAdicional()) {
            writer.writeStartElement("campoAdicional");
            writer.writeAttribute("nombre", campo.nombre());
            writer.writeCharacters(campo.valor());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Escribe {@code <detallesAdicionales>} de una línea, o nada si no lleva.
     */
    public static void detallesAdicionales(XMLStreamWriter writer, List<DetalleAdicional> adicionales) throws XMLStreamException {
        if (adicionales.isEmpty()) {
            return;
        }
        writer.writeStartElement("detallesAdicionales");
        for (var adicional : adicionales) {
            writer.writeEmptyElement("detAdicional");
            writer.writeAttribute("nombre", adicional.nombre());
            writer.writeAttribute("valor", adicional.valor());
        }
        writer.writeEndElement();
    }

    /**
     * Lo que cada esquema escribe dentro de su {@code <detalle>}, que es lo único en que se
     * diferencian.
     *
     * @param <L> línea que escribe
     */
    @FunctionalInterface
    public interface Contenido<L extends Linea> {

        void escribir(XMLStreamWriter writer, L linea) throws XMLStreamException;
    }

    /**
     * Escribe {@code <detalles>}, con un {@code <detalle>} por línea, y deja el contenido de
     * cada uno a {@code contenido}.
     *
     * <p>Los seis esquemas nombran igual ese par de elementos, incluido el de la guía de
     * remisión, cuyas líneas no llevan precio.
     */
    public static <L extends Linea> void lineas(XMLStreamWriter writer, List<L> lineas, Contenido<L> contenido) throws XMLStreamException {
        writer.writeStartElement("detalles");
        for (var linea : lineas) {
            writer.writeStartElement("detalle");
            contenido.escribir(writer, linea);
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Escribe {@code <detalles>} de un comprobante cuyas líneas llevan precio.
     *
     * <p>De {@code cantidad} en adelante esos esquemas coinciden, así que eso lo escribe esta
     * clase; lo de antes lo pone {@code cabecera}.
     */
    public static <D extends Detalle> void detalles(XMLStreamWriter writer, List<D> detalles, Contenido<D> cabecera) throws XMLStreamException {
        lineas(writer, detalles, (w, detalle) -> {
            cabecera.escribir(w, detalle);
            elemento(w, "cantidad", detalle.cantidad().toString());
            elemento(w, "precioUnitario", detalle.precioUnitario().toString());
            elemento(w, "descuento", detalle.descuento().toString());
            elemento(w, "precioTotalSinImpuesto", detalle.precioTotalSinImpuesto().toString());
            detallesAdicionales(w, detalle.detallesAdicionales());
            impuestos(w, detalle.impuestos());
        });
    }

    /**
     * Escribe un bloque {@code <impuestos>}, cuyo tipo es el mismo en todos los esquemas, tanto
     * si cuelga de un detalle como del comprobante entero.
     */
    public static void impuestos(XMLStreamWriter writer, List<Impuesto> impuestos) throws XMLStreamException {
        writer.writeStartElement("impuestos");
        for (var impuesto : impuestos) {
            writer.writeStartElement("impuesto");
            elemento(writer, "codigo", impuesto.codigo().codigo());
            elemento(writer, "codigoPorcentaje", impuesto.codigoPorcentaje());
            elemento(writer, "tarifa", impuesto.tarifa().toPlainString());
            elemento(writer, "baseImponible", impuesto.baseImponible().toString());
            elemento(writer, "valor", impuesto.valor().toString());
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    /**
     * Escribe {@code <contribuyenteEspecial>}, o nada si el emisor no lo es.
     */
    public static void contribuyenteEspecial(XMLStreamWriter writer, @Nullable ContribuyenteEspecial valor) throws XMLStreamException {
        if (valor != null) {
            elemento(writer, ContribuyenteEspecial.CAMPO, valor.valor());
        }
    }

    public static void elemento(XMLStreamWriter writer, String nombre, String valor) throws XMLStreamException {
        writer.writeStartElement(nombre);
        writer.writeCharacters(valor);
        writer.writeEndElement();
    }

    public static void elementoOpcional(XMLStreamWriter writer, String nombre, @Nullable String valor) throws XMLStreamException {
        if (valor != null) {
            elemento(writer, nombre, valor);
        }
    }
}
