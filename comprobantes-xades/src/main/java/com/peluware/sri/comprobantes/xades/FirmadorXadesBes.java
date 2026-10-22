package com.peluware.sri.comprobantes.xades;

import com.peluware.sri.comprobantes.interno.xml.Dom;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.firma.FirmaException;
import com.peluware.sri.comprobantes.firma.Firmador;
import com.peluware.sri.comprobantes.firma.ClaveFirma;
import com.peluware.sri.comprobantes.firma.FirmadorXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.security.auth.x500.X500Principal;
import javax.xml.XMLConstants;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Firma XAdES_BES sobre el proveedor de XML-DSig del JDK.
 *
 * <p>Ficha Técnica v2.34, Tabla 7 y numeral 6.8: firma ENVELOPED, esquema XAdES 1.3.2,
 * canonicalización C14N inclusiva, RSA-SHA1 y SHA-1 en los tres digests.
 *
 * <p>Numeral 6.4: la firma cubre tres partes de la trama, en tres referencias: el comprobante
 * completo, el elemento {@code KeyInfo} con el certificado, y el contenedor
 * {@code SignedProperties}.
 */
public final class FirmadorXadesBes extends FirmadorXml {

    /**
     * Namespace del esquema XAdES 1.3.2.
     */
    public static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";

    /**
     * Valor del atributo {@code Type} de la referencia a SignedProperties.
     */
    public static final String SIGNED_PROPERTIES_TYPE = "http://uri.etsi.org/01903#SignedProperties";

    /**
     * Longitud mínima de la clave RSA, según el numeral 6.8.
     */
    public static final int LONGITUD_MINIMA_CLAVE = 2048;

    private final Clock clock;

    /**
     * Firmador cuyo {@code SigningTime} sale del reloj del sistema en {@link Sri#ZONA}.
     */
    public FirmadorXadesBes() {
        this(Clock.system(Sri.ZONA));
    }

    /**
     * Firmador con un reloj propio.
     *
     * @param clock reloj del que sale el {@code SigningTime}
     */
    public FirmadorXadesBes(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Firma un XML de comprobante que no construyó esta librería.
     *
     * <p>No forma parte del contrato de {@link Firmador}.
     */
    @Override
    public byte[] firmarXml(ClaveFirma clave, byte[] xml) {
        Objects.requireNonNull(clave, "clave");
        Objects.requireNonNull(xml, "xml");

        verificarCertificado(clave.certificado());
        var destino = new ByteArrayOutputStream();
        try {
            var doc = Dom.parse(new ByteArrayInputStream(xml));
            var raiz = doc.getDocumentElement();
            if (!Sri.ID_COMPROBANTE.equals(raiz.getAttribute("id"))) {
                throw new FirmaException.ComprobanteInvalido("El elemento raíz debe llevar id=\"" + Sri.ID_COMPROBANTE + "\", tiene: " + raiz.getAttribute("id"));
            }
            raiz.setIdAttribute("id", true);

            var sufijo = UUID.randomUUID().toString();
            var idSignature = "Signature-" + sufijo;
            var idKeyInfo = "KeyInfo-" + sufijo;
            var idSignedProperties = "SignedProperties-" + sufijo;

            var idObject = "Object-" + sufijo;
            var idReferenciaComprobante = "Reference-" + sufijo;

            var signedProperties = signedProperties(doc, clave.certificado(), idSignedProperties, idReferenciaComprobante);
            var qualifyingProperties = doc.createElementNS(XADES_NS, "xades:QualifyingProperties");
            qualifyingProperties.setAttribute("Target", "#" + idSignature);
            qualifyingProperties.appendChild(signedProperties);

            var fac = XMLSignatureFactory.getInstance("DOM");
            var sha1 = fac.newDigestMethod(DigestMethod.SHA1, null);

            var refComprobante = fac.newReference(
                "#" + Sri.ID_COMPROBANTE,
                sha1,
                List.of(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                null,
                idReferenciaComprobante
            );
            var refKeyInfo = fac.newReference("#" + idKeyInfo, sha1);
            var refSignedProperties = fac.newReference(
                "#" + idSignedProperties,
                sha1,
                null,
                SIGNED_PROPERTIES_TYPE,
                null
            );

            var signedInfo = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                List.of(
                    refComprobante,
                    refKeyInfo,
                    refSignedProperties
                )
            );

            var kif = fac.getKeyInfoFactory();
            var keyInfo = kif.newKeyInfo(List.of(
                kif.newX509Data(clave.cadena()),
                kif.newKeyValue(clave.certificado().getPublicKey())
            ), idKeyInfo);

            var object = fac.newXMLObject(List.of(new DOMStructure(qualifyingProperties)), idObject, null, null);

            var contexto = new DOMSignContext(clave.clavePrivada(), raiz);
            contexto.setDefaultNamespacePrefix("ds");
            contexto.setIdAttributeNS(signedProperties, null, "Id");

            fac.newXMLSignature(signedInfo, keyInfo, List.of(object), idSignature, null).sign(contexto);

            serialize(doc, destino);
        } catch (FirmaException e) {
            throw e;
        } catch (Exception e) {
            throw new FirmaException.Desconocida("No se pudo firmar el comprobante: " + e.getMessage(), e);
        }
        return destino.toByteArray();
    }

    /**
     * Comprueba vigencia y tipo de clave del certificado firmante.
     *
     * <p>Error 42 del SRI, asignado al emisor: certificado que ha superado su fecha de
     * caducidad. Numeral 6.8: algoritmo RSA con clave de 2048 bits.
     */
    private void verificarCertificado(X509Certificate cert) {
        try {
            cert.checkValidity(Date.from(clock.instant()));
        } catch (CertificateExpiredException e) {
            throw new FirmaException.CertificadoCaducado(enEcuador(cert.getNotAfter()), e);
        } catch (CertificateNotYetValidException e) {
            throw new FirmaException.CertificadoNoVigente(enEcuador(cert.getNotBefore()), e);
        }
        if (!(cert.getPublicKey() instanceof RSAPublicKey rsa)) {
            throw new FirmaException.AlgoritmoNoSoportado(cert.getPublicKey().getAlgorithm());
        }
        int bits = rsa.getModulus().bitLength();
        if (bits < LONGITUD_MINIMA_CLAVE) {
            throw new FirmaException.ClaveDemasiadoCorta(bits, LONGITUD_MINIMA_CLAVE);
        }
    }

    private static LocalDate enEcuador(Date fecha) {
        return fecha.toInstant().atZone(Sri.ZONA).toLocalDate();
    }

    private Element signedProperties(Document doc, X509Certificate cert, String id, String idReferenciaComprobante) throws Exception {
        var signedProperties = doc.createElementNS(XADES_NS, "xades:SignedProperties");
        signedProperties.setAttribute("Id", id);

        var signatureProperties = hijo(doc, signedProperties, XADES_NS, "xades:SignedSignatureProperties");

        var signingTime = hijo(doc, signatureProperties, XADES_NS, "xades:SigningTime");
        signingTime.setTextContent(OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        var signingCertificate = hijo(doc, signatureProperties, XADES_NS, "xades:SigningCertificate");
        var certElement = hijo(doc, signingCertificate, XADES_NS, "xades:Cert");

        var certDigest = hijo(doc, certElement, XADES_NS, "xades:CertDigest");
        hijo(doc, certDigest, XMLSignature.XMLNS, "ds:DigestMethod").setAttribute("Algorithm", DigestMethod.SHA1);
        hijo(doc, certDigest, XMLSignature.XMLNS, "ds:DigestValue")
            .setTextContent(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(cert.getEncoded())));

        var issuerSerial = hijo(doc, certElement, XADES_NS, "xades:IssuerSerial");
        hijo(doc, issuerSerial, XMLSignature.XMLNS, "ds:X509IssuerName").setTextContent(cert.getIssuerX500Principal().getName(X500Principal.RFC2253));
        hijo(doc, issuerSerial, XMLSignature.XMLNS, "ds:X509SerialNumber").setTextContent(cert.getSerialNumber().toString());

        var dataObjectProperties = hijo(doc, signedProperties, XADES_NS, "xades:SignedDataObjectProperties");
        var dataObjectFormat = hijo(doc, dataObjectProperties, XADES_NS, "xades:DataObjectFormat");
        dataObjectFormat.setAttribute("ObjectReference", "#" + idReferenciaComprobante);
        hijo(doc, dataObjectFormat, XADES_NS, "xades:MimeType").setTextContent("text/xml");

        return signedProperties;
    }

    private static Element hijo(Document doc, Element padre, String namespace, String nombre) {
        var elemento = doc.createElementNS(namespace, nombre);
        padre.appendChild(elemento);
        return elemento;
    }

    private static void serialize(Document doc, OutputStream out) throws Exception {
        var tf = TransformerFactory.newInstance();
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, Sri.CODIFICACION.name());
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(Sri.CODIFICACION));
        transformer.transform(new DOMSource(doc), new StreamResult(out));
    }
}
