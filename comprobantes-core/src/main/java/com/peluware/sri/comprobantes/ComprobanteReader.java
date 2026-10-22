package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.factura.FacturaReader;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemisionReader;
import com.peluware.sri.comprobantes.interno.xml.Dom;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompraReader;
import com.peluware.sri.comprobantes.notacredito.NotaCreditoReader;
import com.peluware.sri.comprobantes.notadebito.NotaDebitoReader;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencionReader;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Lee un comprobante de su XML, firmado o sin firmar.
 *
 * <p>Es el espejo de {@link ComprobanteWriter}. Esta clase conduce la lectura entera —comprobar
 * la raíz, localizar el bloque propio, crear el builder y poner lo que los seis esquemas
 * declaran igual— y cede dos huecos: {@link #familia} para lo que comparte una familia de
 * comprobantes y {@link #propio} para lo que solo declara uno.
 *
 * <p>Los importes se toman tal como vienen, sin recalcularlos: un comprobante autorizado por el
 * SRI manda sobre la aritmética de esta librería. La firma, si la lleva, se ignora.
 *
 * <p>Cada subtipo expone su única instancia. Para leer un XML del que no se sabe qué comprobante
 * trae está {@link #comprobante(InputStream)}, que lo reconoce por su raíz.
 *
 * @param <C> comprobante que lee
 */
public abstract sealed class ComprobanteReader<C extends Comprobante, B extends Comprobante.Builder<B>> permits ComprobanteNotaReader, ComprobanteRetencionReader, ComprobanteVentaReader, GuiaRemisionReader {

    protected ComprobanteReader() {
    }

    /**
     * Los seis, en una clase aparte a propósito.
     *
     * <p>La lista se arma la primera vez que se usa y no al cargar {@link ComprobanteReader}. Si
     * viviera en el inicializador de la base, tocar un subtipo primero —{@code
     * FacturaReader.INSTANCIA}— cargaría la base en mitad de la inicialización del subtipo y
     * leería su instancia todavía a null.
     */
    private static final class Todos {

        private static final List<ComprobanteReader<?, ?>> LISTA = List.of(
            FacturaReader.INSTANCIA,
            LiquidacionCompraReader.INSTANCIA,
            NotaCreditoReader.INSTANCIA,
            NotaDebitoReader.INSTANCIA,
            GuiaRemisionReader.INSTANCIA,
            ComprobanteRetencionReader.INSTANCIA
        );

        private Todos() {
        }
    }

    /**
     * Lee de ese flujo, que no se cierra, el comprobante que su raíz declare.
     *
     * <p>El XML declara su propia codificación, así que se lee de los bytes y no de un texto ya
     * decodificado.
     *
     * @throws LecturaXmlException si el XML está mal formado, su raíz no es la de ninguno de los
     *                             seis comprobantes, o le falta algo que su esquema exige
     */
    public static Comprobante comprobante(InputStream entrada) {
        var raiz = raizDe(entrada);
        var nombre = Dom.name(raiz);
        for (var reader : Todos.LISTA) {
            if (reader.esquema().raiz().equals(nombre)) {
                return reader.de(raiz);
            }
        }
        throw new LecturaXmlException("El XML no es ninguno de los seis comprobantes, su raíz es <" + nombre + ">");
    }

    /**
     * Lee de memoria el comprobante que su raíz declare.
     */
    public static Comprobante comprobante(byte[] xml) {
        return comprobante(new ByteArrayInputStream(xml));
    }

    /**
     * Lee el comprobante de ese flujo, que no se cierra.
     *
     * @throws LecturaXmlException si el XML está mal formado, no es este comprobante o le falta
     *                             algo que su esquema exige
     */
    public final C read(InputStream entrada) {
        return de(raizDe(entrada));
    }

    /**
     * El elemento raíz del XML, con la política de fallo de esta librería: cualquier problema al
     * analizarlo sale como {@link LecturaXmlException}.
     *
     * <p>{@code Dom} no lanza excepciones del dominio a propósito, porque lo comparten módulos
     * que fallan de otra manera.
     */
    private static Element raizDe(InputStream entrada) {
        Element raiz;
        try {
            raiz = Dom.root(entrada);
        } catch (SAXException | IOException e) {
            throw new LecturaXmlException("No se pudo leer el XML", e);
        }
        if (raiz == null) {
            throw new LecturaXmlException("El XML no tiene elemento raíz");
        }
        return raiz;
    }

    /**
     * Lee el comprobante de memoria.
     */
    public final C read(byte[] xml) {
        return read(new ByteArrayInputStream(xml));
    }

    /**
     * Arma el comprobante a partir del elemento raíz ya leído.
     *
     * @throws LecturaXmlException si la raíz no es la de este comprobante o le falta algo que su
     *                             esquema exige
     */
    public final C de(Element raiz) {
        var esquema = esquema();
        var nombre = Dom.name(raiz);
        if (!esquema.raiz().equals(nombre)) {
            throw new LecturaXmlException("El XML no es " + esquema.nombre() + ", su raíz es <" + nombre + ">");
        }
        return armar(raiz, Lecturas.hijoExigido(raiz, esquema.tagInfo()));
    }

    /**
     * Compone el comprobante en el único orden posible: la base crea el builder, pone lo que los
     * seis esquemas declaran igual, cede el hueco de la familia y por último el de este
     * comprobante, que es el que construye.
     *
     * <p>Que lo común lo aplique esta clase y no el subtipo es el punto: un reader nuevo no puede
     * olvidarse de la identidad ni de los campos adicionales, porque no es él quien los pone.
     */
    private C armar(Element raiz, Element info) {
        var builder = builder();

        Lecturas.identidad(builder, raiz, info).maquinaFiscal(Lecturas.maquinaFiscal(raiz));
        for (var campo : Lecturas.infoAdicional(raiz)) {
            builder.campoAdicional(campo.nombre(), campo.valor());
        }

        familia(builder, raiz, info);
        return propio(builder, raiz, info);
    }

    /**
     * Los nombres fijos del esquema que lee este reader.
     */
    protected abstract Esquema esquema();

    /**
     * Un builder vacío del comprobante que lee este reader.
     */
    protected abstract B builder();

    /**
     * Rellena lo que comparten los comprobantes de una misma familia. Por omisión, nada: la guía
     * de remisión y el comprobante de retención no están en ninguna.
     */
    protected void familia(B builder, Element raiz, Element info) {
    }

    /**
     * Rellena lo que solo este comprobante declara y lo construye.
     */
    protected abstract C propio(B builder, Element raiz, Element info);
}
