package com.peluware.sri.comprobantes.xades;

import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.firma.FirmaException;
import com.peluware.sri.comprobantes.firma.OrigenClave;
import com.peluware.sri.comprobantes.firma.ClaveFirma;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirmadorXadesBesTest {

    private static final String DSIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final String C14N_INCLUSIVA = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    private static final FirmadorXadesBes FIRMADOR = new FirmadorXadesBes();

    private static final OrigenClave origenClave = new OrigenClavePkcs12(CertificadosDePrueba.rsa2048(), CertificadosDePrueba.CLAVE);
    private static final ClaveFirma clave = origenClave.abrir();
    private static final byte[] firmado = firmarFixture(origenClave);
    private static final Document doc = parse(firmado);

    @Test
    @DisplayName("la firma es el último hijo del elemento raíz")
    void firmaComoUltimoHijo() {
        var ultimo = (Element) doc.getDocumentElement().getLastChild();

        assertEquals(DSIG_NS, ultimo.getNamespaceURI());
        assertEquals("Signature", ultimo.getLocalName());
    }

    @Test
    @DisplayName("numeral 6.4: tres referencias, al comprobante, al KeyInfo y a SignedProperties")
    void tresReferencias() {
        var referencias = elementos(doc, DSIG_NS, "Reference");
        assertEquals(3, referencias.size());

        assertEquals("#" + Sri.ID_COMPROBANTE, referencias.get(0).getAttribute("URI"));
        assertEquals("#" + unico(doc, DSIG_NS, "KeyInfo").getAttribute("Id"), referencias.get(1).getAttribute("URI"));
        assertEquals("#" + unico(doc, FirmadorXadesBes.XADES_NS, "SignedProperties").getAttribute("Id"), referencias.get(2).getAttribute("URI"));
        assertEquals(FirmadorXadesBes.SIGNED_PROPERTIES_TYPE, referencias.get(2).getAttribute("Type"));
    }

    @Test
    @DisplayName("el transform enveloped va solo en la referencia al comprobante")
    void transformSoloEnElComprobante() {
        var transforms = elementos(doc, DSIG_NS, "Transform");

        assertEquals(1, transforms.size());
        assertEquals(DSIG_NS + "enveloped-signature", transforms.getFirst().getAttribute("Algorithm"));
        assertEquals("#" + Sri.ID_COMPROBANTE, ((Element) transforms.getFirst().getParentNode().getParentNode()).getAttribute("URI"));
    }

    @Test
    @DisplayName("Tabla 7 y numeral 6.8: C14N inclusiva, RSA-SHA1 y SHA-1 en los digests")
    void algoritmos() {
        assertEquals(C14N_INCLUSIVA, unico(doc, DSIG_NS, "CanonicalizationMethod").getAttribute("Algorithm"));
        assertEquals(DSIG_NS + "rsa-sha1", unico(doc, DSIG_NS, "SignatureMethod").getAttribute("Algorithm"));

        var digests = elementos(doc, DSIG_NS, "DigestMethod");
        assertEquals(4, digests.size(), "tres de las referencias más el del CertDigest");
        assertTrue(digests.stream().allMatch(d -> (DSIG_NS + "sha1").equals(d.getAttribute("Algorithm"))));
    }

    @Test
    @DisplayName("SignedProperties enlaza con la firma y con la referencia al comprobante")
    void contenidoDeSignedProperties() {
        var signedProperties = unico(doc, FirmadorXadesBes.XADES_NS, "SignedProperties");
        var qualifyingProperties = (Element) signedProperties.getParentNode();

        assertEquals("#" + unico(doc, DSIG_NS, "Signature").getAttribute("Id"), qualifyingProperties.getAttribute("Target"));
        assertEquals(clave.certificado().getSerialNumber().toString(), unico(doc, DSIG_NS, "X509SerialNumber").getTextContent());
        assertEquals("text/xml", unico(doc, FirmadorXadesBes.XADES_NS, "MimeType").getTextContent());
        assertEquals("#" + elementos(doc, DSIG_NS, "Reference").getFirst().getAttribute("Id"),
            unico(doc, FirmadorXadesBes.XADES_NS, "DataObjectFormat").getAttribute("ObjectReference"));
    }

    @Test
    @DisplayName("la firma verifica criptográficamente")
    void firmaVerifica() throws Exception {
        var copia = parse(firmado);
        copia.getDocumentElement().setIdAttribute("id", true);

        var firma = (Element) copia.getElementsByTagNameNS(DSIG_NS, "Signature").item(0);
        var contexto = new DOMValidateContext(KeySelector.singletonKeySelector(clave.certificado().getPublicKey()), firma);
        // El JDK prohíbe SHA-1 con la validación segura activa; el SRI exige SHA-1.
        contexto.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE);
        contexto.setIdAttributeNS(unico(copia, FirmadorXadesBes.XADES_NS, "SignedProperties"), null, "Id");

        var signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto);

        assertTrue(signature.validate(contexto), "la firma no valida");
    }

    @Test
    @DisplayName("el XML firmado empieza por la declaración en UTF-8")
    void declaracionUtf8() {
        assertTrue(new String(firmado, Sri.CODIFICACION).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
    }

    @Test
    @DisplayName("KeyInfo lleva la cadena completa de certificados")
    void cadenaCompletaEnKeyInfo() {
        var conCadena = parse(firmarFixture(new OrigenClavePkcs12(CertificadosDePrueba.conCadenaDeTres(), CertificadosDePrueba.CLAVE)));

        assertEquals(3, elementos(conCadena, DSIG_NS, "X509Certificate").size());
    }

    @Test
    @DisplayName("la fuente se puede abrir varias veces y devuelve la misma clave")
    void fuenteMultiUso() {
        assertSame(clave, origenClave.abrir());
        assertSame(clave, origenClave.abrir());
    }

    @Test
    @DisplayName("error 42 del SRI: no firma con un certificado caducado")
    void rechazaCertificadoCaducado() {
        var caducado = new OrigenClavePkcs12(CertificadosDePrueba.caducado(), CertificadosDePrueba.CLAVE);

        FirmaException.CertificadoCaducado e = assertThrows(FirmaException.CertificadoCaducado.class, () -> firmarFixture(caducado));

        assertEquals(caducado.abrir().certificado().getNotAfter().toInstant().atZone(Sri.ZONA).toLocalDate(), e.caducidad());
        assertTrue(e.mensajeUsuario().contains("Renuévelo"), e.mensajeUsuario());
    }

    @Test
    @DisplayName("numeral 6.8: rechaza una clave RSA de menos de 2048 bits")
    void rechazaClaveCorta() {
        var corta = new OrigenClavePkcs12(CertificadosDePrueba.rsa1024(), CertificadosDePrueba.CLAVE);

        FirmaException.ClaveDemasiadoCorta e = assertThrows(FirmaException.ClaveDemasiadoCorta.class, () -> firmarFixture(corta));

        assertEquals(1024, e.bits());
        assertEquals(FirmadorXadesBes.LONGITUD_MINIMA_CLAVE, e.minimo());
    }

    @Test
    @DisplayName("numeral 6.8: rechaza una clave que no sea RSA")
    void rechazaCurvaEliptica() {
        var ec = new OrigenClavePkcs12(CertificadosDePrueba.curvaEliptica(), CertificadosDePrueba.CLAVE);

        FirmaException.AlgoritmoNoSoportado e = assertThrows(FirmaException.AlgoritmoNoSoportado.class, () -> firmarFixture(ec));

        assertEquals("EC", e.algoritmo());
    }

    @Test
    @DisplayName("con varias claves en el archivo exige el alias y las enumera")
    void exigeAliasConVariasClaves() {
        FirmaException.VariasClaves e = assertThrows(FirmaException.VariasClaves.class,
            () -> new OrigenClavePkcs12(CertificadosDePrueba.dosClaves(), CertificadosDePrueba.CLAVE));

        assertEquals(List.of("primera", "segunda"), e.alias().stream().sorted().toList());
    }

    @Test
    @DisplayName("el alias indicado selecciona la clave cuando hay varias")
    void aliasSeleccionaLaClave() {
        var fuente = new OrigenClavePkcs12(CertificadosDePrueba.dosClaves(), CertificadosDePrueba.CLAVE, "segunda");

        assertEquals(1, fuente.abrir().cadena().size());
    }

    @Test
    @DisplayName("la contraseña se puede dar como texto")
    void contrasenaComoTexto() {
        var fuente = new OrigenClavePkcs12(CertificadosDePrueba.rsa2048(), new String(CertificadosDePrueba.CLAVE));

        assertEquals(1, fuente.abrir().cadena().size());
    }

    @Test
    @DisplayName("rechaza un comprobante cuya raíz no lleve id=comprobante")
    void rechazaRaizSinIdComprobante() {
        var sinId = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><factura id=\"factura\" version=\"1.1.0\"><infoTributaria/></factura>";

        assertThrows(FirmaException.ComprobanteInvalido.class,
            () -> FIRMADOR.firmarXml(origenClave.abrir(), sinId.getBytes(Sri.CODIFICACION)));
    }

    @Test
    @DisplayName("con la contraseña equivocada el mensaje de usuario no filtra detalles técnicos")
    void mensajeDeUsuarioSinTecnicismos() {
        var e = assertThrows(FirmaException.ArchivoIlegible.class,
            () -> new OrigenClavePkcs12(CertificadosDePrueba.rsa2048(), "equivocada".toCharArray()));

        assertTrue(e.getMessage().contains("PKCS#12"), e.getMessage());
        assertFalse(e.mensajeUsuario().contains("PKCS#12"), e.mensajeUsuario());
        assertTrue(e.mensajeUsuario().contains("contraseña"), e.mensajeUsuario());
    }

    private static byte[] firmarFixture(OrigenClave fuente) {
        try (InputStream entrada = FirmadorXadesBesTest.class.getResourceAsStream("/factura-sin-firmar.xml")) {
            return FIRMADOR.firmarXml(fuente.abrir(), Objects.requireNonNull(entrada, "falta el recurso factura-sin-firmar.xml").readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Element> elementos(Document documento, String namespace, String nombre) {
        var nodos = documento.getElementsByTagNameNS(namespace, nombre);
        var resultado = new ArrayList<Element>(nodos.getLength());
        for (int i = 0; i < nodos.getLength(); i++) {
            resultado.add((Element) nodos.item(i));
        }
        return resultado;
    }

    private static Element unico(Document documento, String namespace, String nombre) {
        var encontrados = elementos(documento, namespace, nombre);
        assertEquals(1, encontrados.size(), "se esperaba un único " + nombre);
        return encontrados.getFirst();
    }

    private static Document parse(byte[] xml) {
        try {
            var dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("no se pudo interpretar el XML firmado", e);
        }
    }
}
