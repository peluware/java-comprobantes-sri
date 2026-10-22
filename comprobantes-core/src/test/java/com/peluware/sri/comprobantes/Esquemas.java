package com.peluware.sri.comprobantes;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * Compila los XSD del SRI que están en los recursos de test.
 *
 * <p>La búsqueda del recurso se hace desde aquí a propósito: los XSD viajan en el test-jar de
 * este módulo, y un recurso de un módulo con nombre solo es visible desde el propio módulo. Si
 * otro módulo lo pidiera con su propia clase, obtendría {@code null}.
 */
public final class Esquemas {

    private Esquemas() {
    }

    /**
     * El esquema con ese nombre de archivo, por ejemplo {@code factura_V2.1.0.xsd}.
     *
     * @throws IllegalStateException si el recurso no está
     */
    public static Schema de(String nombre) {
        var xsd = Esquemas.class.getResource("/xsd/" + nombre);
        if (xsd == null) {
            throw new IllegalStateException("falta el esquema " + nombre + " en los recursos de test");
        }
        try {
            var fabrica = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
            return fabrica.newSchema(new StreamSource(xsd.toExternalForm()));
        } catch (SAXException e) {
            throw new IllegalStateException("el esquema " + nombre + " no compila", e);
        }
    }
}
