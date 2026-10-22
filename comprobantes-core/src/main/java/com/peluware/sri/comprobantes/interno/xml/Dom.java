package com.peluware.sri.comprobantes.interno.xml;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Recorre el DOM del JDK buscando solo entre los hijos directos.
 *
 * <p>{@link Element#getElementsByTagName(String)} busca en todos los descendientes, y los esquemas
 * del SRI repiten nombres a distinta profundidad: {@code codigo} y {@code valor} están dentro de
 * {@code <impuesto>} y de {@code <totalImpuesto>}. Por eso aquí nada es recursivo.
 *
 * <p>No sabe nada del SRI: no valida, no exige campos y no lanza excepciones del dominio. Lo que
 * falta se devuelve como {@code null} y quien llama decide si eso es un defecto. Por eso la usan
 * los tres módulos que leen XML, cada uno con su propia política de fallo.
 *
 * <p>El documento se analiza sin DTD, sin XInclude y sin expandir entidades, porque el XML que se
 * lee viene de fuera. Esos ajustes estaban escritos tres veces antes de vivir aquí.
 */
public final class Dom {

    private static final int VISTA_PREVIA_MAX_BYTES = 512;

    private Dom() {
    }

    /**
     * Analiza el documento entero.
     *
     * @throws SAXException          si no es XML bien formado
     * @throws IOException           si el flujo no se puede leer
     * @throws IllegalStateException si el parser del JDK rechaza los ajustes de seguridad
     */
    public static Document parse(InputStream input) throws SAXException, IOException {
        try {
            return factory().newDocumentBuilder().parse(input);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("El parser XML del JDK no admite los ajustes de seguridad exigidos", e);
        }
    }

    /**
     * El elemento raíz del documento, ya normalizado, o {@code null} si no tiene ninguno.
     *
     * @throws SAXException si no es XML bien formado
     * @throws IOException  si el flujo no se puede leer
     */
    public static @Nullable Element root(InputStream input) throws SAXException, IOException {
        var document = parse(input);
        var root = document.getDocumentElement();
        if (root != null) {
            root.normalize();
        }
        return root;
    }

    /**
     * El elemento raíz de esos bytes.
     *
     * @throws SAXException si no es XML bien formado
     * @throws IOException  si los bytes no se pueden leer
     */
    public static @Nullable Element root(byte[] xml) throws SAXException, IOException {
        return root(new ByteArrayInputStream(xml));
    }

    /**
     * Nombre del elemento, sin su prefijo de espacio de nombres.
     */
    public static String name(Element element) {
        return element.getLocalName() == null ? element.getNodeName() : element.getLocalName();
    }

    /**
     * Los hijos directos con ese nombre, en el orden en que vienen.
     */
    public static List<Element> children(Element parent, String name) {
        var found = new ArrayList<Element>();
        var nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element child && name.equals(name(child))) {
                found.add(child);
            }
        }
        return List.copyOf(found);
    }

    /**
     * El primer hijo directo con ese nombre, o {@code null} si no lo hay.
     */
    public static @Nullable Element child(Element parent, String name) {
        var children = children(parent, name);
        return children.isEmpty() ? null : children.getFirst();
    }

    /**
     * El primer hijo elemento, cualquiera que sea su nombre, o {@code null} si no lo hay.
     */
    public static @Nullable Element firstElement(Element parent) {
        var nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element child) {
                return child;
            }
        }
        return null;
    }

    /**
     * Los nietos con ese nombre dentro del hijo {@code container}; vacío si el contenedor no
     * viene.
     */
    public static List<Element> childrenIn(Element parent, String container, String name) {
        var child = child(parent, container);
        return child == null ? List.of() : children(child, name);
    }

    /**
     * Texto del primer hijo directo con ese nombre, o {@code null} si no lo hay o está vacío.
     */
    public static @Nullable String text(Element parent, String name) {
        var child = child(parent, name);
        if (child == null) {
            return null;
        }
        var text = text(child);
        return text.isEmpty() ? null : text;
    }

    /**
     * Igual que {@link #text(Element, String)}, con cadena vacía en lugar de {@code null}.
     */
    public static String textOrEmpty(Element parent, String name) {
        var text = text(parent, name);
        return text == null ? "" : text;
    }

    /**
     * Texto de un elemento, sin los espacios de los extremos.
     */
    public static String text(Element element) {
        var text = new StringBuilder();
        var nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            var node = nodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(node.getNodeValue());
            }
        }
        return text.toString().trim();
    }

    /**
     * Valor de un atributo, o {@code null} si no lo lleva.
     */
    public static @Nullable String attribute(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : null;
    }

    /**
     * Los primeros bytes como texto, para los mensajes de error.
     *
     * <p>Se decodifica como UTF-8 tolerando bytes inválidos.
     */
    public static String preview(byte[] xml) {
        if (xml.length == 0) {
            return "vacío";
        }
        var text = new String(xml, 0, Math.min(xml.length, VISTA_PREVIA_MAX_BYTES), StandardCharsets.UTF_8).strip();
        return xml.length > VISTA_PREVIA_MAX_BYTES ? text + "… (" + xml.length + " bytes en total)" : text;
    }

    /**
     * Fábrica endurecida contra entidades externas.
     *
     * <p>Prohíbe {@code DOCTYPE}, lo que deshabilita la expansión de entidades. No afecta a los
     * datos de carácter.
     */
    private static DocumentBuilderFactory factory() throws ParserConfigurationException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }
}
