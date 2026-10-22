package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.interno.xml.Dom;
import com.peluware.sri.comprobantes.Sri;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sobre SOAP de los servicios web del SRI: construcción de la petición y extracción del
 * contenido de la respuesta.
 *
 * <p>Los servicios son SOAP 1.1 estilo documento, con operaciones de un solo parámetro. Todos
 * los sobres tienen la misma forma y difieren únicamente en lo que describe
 * {@link OperacionSoap}:
 *
 * <pre>{@code
 * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
 *   <soapenv:Body>
 *     <ec:validarComprobante xmlns:ec="http://ec.gob.sri.ws.recepcion">
 *       <xml>PD94bWwgdmVyc2lvbj0i...</xml>
 *     </ec:validarComprobante>
 *   </soapenv:Body>
 * </soapenv:Envelope>
 * }</pre>
 *
 * <p>El elemento de la operación va cualificado en el namespace del servicio y el del parámetro
 * sin cualificar. Los esquemas de los dos WSDL declaran
 * {@code elementFormDefault="unqualified"}.
 *
 * <p>La respuesta se lee con {@link Dom}, por nombre local e ignorando el namespace. Los dos
 * WSDL declaran {@code comprobante}, {@code mensaje} y {@code autorizacion} como referencias a
 * elementos globales, que van cualificados; los ejemplos del numeral 7.2.3 los muestran sin
 * cualificar.
 */
final class SoapSri {

    private static final String ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * Bytes que se reservan para el sobre al dimensionar el destino, por encima del payload.
     *
     * <p>El sobre vacío ocupa 262 bytes en recepción y 313 en autorización.
     */
    static final int MARGEN_SOBRE = 512;

    private SoapSri() {
    }

    /**
     * Devuelve el contenido útil de la respuesta: el hijo del elemento de la operación, que es
     * {@code RespuestaRecepcionComprobante} o {@code RespuestaAutorizacionComprobante}.
     *
     * @throws ComunicacionException si la respuesta no es un sobre SOAP legible o trae un
     *                               {@code soap:Fault}
     */
    static Element contenido(byte[] respuesta) {
        var sobre = raiz(respuesta);
        var cuerpo = Dom.child(sobre, "Body");
        if (cuerpo == null) {
            throw new ComunicacionException("La respuesta del SRI no tiene soap:Body: " + Dom.preview(respuesta));
        }
        var fault = Dom.child(cuerpo, "Fault");
        if (fault != null) {
            var detalle = Dom.text(fault, "faultstring");
            throw new ComunicacionException("El SRI devolvió un soap:Fault: " + (detalle == null ? "sin detalle" : detalle));
        }
        var operacion = Dom.firstElement(cuerpo);
        if (operacion == null) {
            throw new ComunicacionException("La respuesta del SRI tiene un soap:Body vacío: " + Dom.preview(respuesta));
        }
        var contenido = Dom.firstElement(operacion);
        if (contenido == null) {
            throw new ComunicacionException("La respuesta del SRI no trae contenido en " + operacion.getLocalName() + ": " + Dom.preview(respuesta));
        }
        return contenido;
    }

    /**
     * Interpreta la respuesta y devuelve su elemento raíz.
     *
     * @throws ComunicacionException si la respuesta no es XML legible o no tiene elemento raíz
     */
    private static Element raiz(byte[] respuesta) {
        Element raiz;
        try {
            raiz = Dom.root(respuesta);
        } catch (SAXException | IOException e) {
            throw new ComunicacionException("No se pudo interpretar la respuesta del SRI: " + Dom.preview(respuesta), e);
        }
        if (raiz == null) {
            throw new ComunicacionException("La respuesta del SRI está vacía");
        }
        return raiz;
    }

    /**
     * Interpreta los mensajes que cuelgan de {@code contenedor}.
     */
    static List<Mensaje> mensajes(@Nullable Element contenedor) {
        if (contenedor == null) {
            return List.of();
        }
        var mensajes = Dom.child(contenedor, "mensajes");
        if (mensajes == null) {
            return List.of();
        }
        var resultado = new ArrayList<Mensaje>();
        for (var mensaje : Dom.children(mensajes, "mensaje")) {
            resultado.add(new Mensaje(
                Dom.textOrEmpty(mensaje, "identificador"),
                Dom.textOrEmpty(mensaje, "mensaje"),
                Dom.text(mensaje, "informacionAdicional"),
                TipoMensaje.of(Dom.text(mensaje, "tipo")))
            );
        }
        return List.copyOf(resultado);
    }

    /**
     * Envuelve el valor del único parámetro de la operación en su sobre SOAP.
     *
     * <p>El destino se dimensiona en {@code valor.length() + MARGEN_SOBRE}.
     *
     * @param valor contenido del parámetro, ASCII — base64 o dígitos —, de modo que su longitud
     *              en caracteres coincide con su tamaño en bytes UTF-8
     */
    static byte[] envolver(OperacionSoap operacion, String valor) {
        var destino = new ByteArrayOutputStream(valor.length() + MARGEN_SOBRE);
        var codificacion = Sri.CODIFICACION.name();
        try {
            var writer = XMLOutputFactory.newFactory().createXMLStreamWriter(destino, codificacion);
            writer.writeStartDocument(codificacion, "1.0");

            writer.setPrefix("soapenv", ENVELOPE_NS);
            writer.writeStartElement(ENVELOPE_NS, "Envelope");
            writer.writeNamespace("soapenv", ENVELOPE_NS);
            writer.writeStartElement(ENVELOPE_NS, "Body");

            writer.setPrefix("ec", operacion.namespace());
            writer.writeStartElement(operacion.namespace(), operacion.nombre());
            writer.writeNamespace("ec", operacion.namespace());
            writer.writeStartElement(operacion.parametro());
            writer.writeCharacters(valor);
            writer.writeEndElement();
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new ComunicacionException("No se pudo construir el sobre SOAP de " + operacion.nombre(), e);
        }
        return destino.toByteArray();
    }
}
