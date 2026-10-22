package com.peluware.sri.comprobantes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba que la copia de los esquemas del SRI que guarda este repositorio sigue íntegra,
 * validando contra cada una el XML de ejemplo que el propio SRI publica con ella.
 *
 * <p>Es la única copia: la consumen también {@code comprobantes-xades} y
 * {@code comprobantes-it} a través del {@code test-jar}.
 *
 * <p>El ejemplo de la versión 2.0.0 que publica el SRI no cumple su propio esquema; queda
 * fijado aparte.
 */
class EsquemasDelSriTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "factura_V1.0.0", "factura_V1.1.0", "factura_V2.1.0",
        "NotaCredito_V1.0.0", "NotaCredito_V1.1.0",
        "NotaDebito_V1.0.0",
        "GuiaRemision_V1.0.0", "GuiaRemision_V1.1.0",
        "ComprobanteRetencion_V1.0.0", "ComprobanteRetencion_V2.0.0",
        "LiquidacionCompra_V1.0.0", "LiquidacionCompra_V1.1.0"})
    @DisplayName("el XML de ejemplo del SRI valida contra su propio esquema")
    void elEjemploValidaContraSuEsquema(String nombre) {
        var esquema = compilar(nombre + ".xsd");

        assertDoesNotThrow(() -> {
            try (InputStream ejemplo = recurso("/xsd/ejemplos/" + nombre + ".xml")) {
                esquema.newValidator().validate(new StreamSource(ejemplo));
            }
        });
    }

    @Test
    @DisplayName("el ejemplo de la 2.0.0 que publica el SRI no valida contra su propio esquema")
    void elEjemploDeLa200EsDefectuoso() {
        var esquema = compilar("factura_V2.0.0.xsd");

        var e = assertThrows(SAXParseException.class, () -> {
            try (InputStream ejemplo = recurso("/xsd/ejemplos/factura_V2.0.0.xml")) {
                esquema.newValidator().validate(new StreamSource(ejemplo));
            }
        });

        assertTrue(e.getMessage().contains("incoTermFactura"), e.getMessage());
    }

    private static Schema compilar(String nombre) {
        var xsd = EsquemasDelSriTest.class.getResource("/xsd/" + nombre);
        if (xsd == null) {
            throw new IllegalStateException("falta el esquema " + nombre);
        }
        try {
            var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
            return factory.newSchema(new StreamSource(xsd.toExternalForm()));
        } catch (SAXException e) {
            throw new IllegalStateException("no se pudo compilar " + nombre, e);
        }
    }

    private static InputStream recurso(String ruta) {
        var entrada = EsquemasDelSriTest.class.getResourceAsStream(ruta);
        if (entrada == null) {
            throw new UncheckedIOException(new IOException("falta el recurso " + ruta));
        }
        return entrada;
    }
}
