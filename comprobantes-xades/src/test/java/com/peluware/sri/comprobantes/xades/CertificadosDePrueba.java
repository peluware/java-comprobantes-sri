package com.peluware.sri.comprobantes.xades;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Genera archivos PKCS#12 con las características que necesita cada prueba.
 */
final class CertificadosDePrueba {

    static final char[] CLAVE = "cambiame".toCharArray();

    private static final String SUJETO = "CN=PRUEBAS SERVICIO DE RENTAS INTERNAS,O=Pruebas,C=EC";
    private static final Instant AYER = Instant.now().minus(Duration.ofDays(1));
    private static final Instant EN_DIEZ_ANIOS = Instant.now().plus(Duration.ofDays(3650));

    private CertificadosDePrueba() {
    }

    /**
     * Clave RSA de 2048 bits vigente, con certificado autofirmado.
     */
    static InputStream rsa2048() {
        return conUnaClave("RSA", 2048, "SHA256withRSA", AYER, EN_DIEZ_ANIOS);
    }

    /**
     * Clave RSA de 2048 bits cuyo certificado ya caducó.
     */
    static InputStream caducado() {
        var haceDosAnios = Instant.now().minus(Duration.ofDays(730));
        return conUnaClave("RSA", 2048, "SHA256withRSA", haceDosAnios, haceDosAnios.plus(Duration.ofDays(365)));
    }

    /**
     * Clave RSA de 1024 bits, por debajo de lo que exige el numeral 6.8.
     */
    static InputStream rsa1024() {
        return conUnaClave("RSA", 1024, "SHA256withRSA", AYER, EN_DIEZ_ANIOS);
    }

    /**
     * Clave de curva elíptica, que el SRI no admite.
     */
    static InputStream curvaEliptica() {
        return conUnaClave("EC", 256, "SHA256withECDSA", AYER, EN_DIEZ_ANIOS);
    }

    /**
     * Dos claves privadas en el mismo archivo.
     */
    static InputStream dosClaves() {
        try {
            var almacen = vacio();
            for (var alias : new String[]{"primera", "segunda"}) {
                var par = par("RSA", 2048);
                var cert = certificado(par, SUJETO, par, SUJETO, "SHA256withRSA", AYER, EN_DIEZ_ANIOS, false);
                almacen.setKeyEntry(alias, par.getPrivate(), CLAVE, new Certificate[]{cert});
            }
            return serializar(almacen);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Cadena de tres certificados: raíz, intermedia y firmante.
     */
    static InputStream conCadenaDeTres() {
        try {
            var raiz = par("RSA", 2048);
            var sujetoRaiz = "CN=RAIZ DE PRUEBAS,O=Pruebas,C=EC";
            var certRaiz = certificado(raiz, sujetoRaiz, raiz, sujetoRaiz, "SHA256withRSA", AYER, EN_DIEZ_ANIOS, true);

            var intermedia = par("RSA", 2048);
            var sujetoIntermedia = "CN=INTERMEDIA DE PRUEBAS,O=Pruebas,C=EC";
            var certIntermedia = certificado(intermedia, sujetoIntermedia, raiz, sujetoRaiz, "SHA256withRSA", AYER, EN_DIEZ_ANIOS, true);

            var firmante = par("RSA", 2048);
            var certFirmante = certificado(firmante, SUJETO, intermedia, sujetoIntermedia, "SHA256withRSA", AYER, EN_DIEZ_ANIOS, false);

            var almacen = vacio();
            almacen.setKeyEntry("firma", firmante.getPrivate(), CLAVE, new Certificate[]{certFirmante, certIntermedia, certRaiz});
            return serializar(almacen);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static InputStream conUnaClave(String algoritmo, int bits, String algoritmoFirma, Instant desde, Instant hasta) {
        try {
            var par = par(algoritmo, bits);
            var cert = certificado(par, SUJETO, par, SUJETO, algoritmoFirma, desde, hasta, false);
            var almacen = vacio();
            almacen.setKeyEntry("firma", par.getPrivate(), CLAVE, new Certificate[]{cert});
            return serializar(almacen);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair par(String algoritmo, int bits) throws Exception {
        var generador = KeyPairGenerator.getInstance(algoritmo);
        generador.initialize(bits);
        return generador.generateKeyPair();
    }

    private static X509Certificate certificado(KeyPair sujeto, String nombreSujeto, KeyPair emisor, String nombreEmisor,
                                               String algoritmoFirma, Instant desde, Instant hasta, boolean autoridad) throws Exception {
        var builder = new JcaX509v3CertificateBuilder(
            new X500Name(nombreEmisor),
            BigInteger.valueOf(System.nanoTime()),
            Date.from(desde),
            Date.from(hasta),
            new X500Name(nombreSujeto),
            sujeto.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(autoridad));

        var firmante = new JcaContentSignerBuilder(algoritmoFirma).build(emisor.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(firmante));
    }

    private static KeyStore vacio() throws Exception {
        var almacen = KeyStore.getInstance("PKCS12");
        almacen.load(null, null);
        return almacen;
    }

    private static InputStream serializar(KeyStore almacen) throws Exception {
        var salida = new ByteArrayOutputStream();
        almacen.store(salida, CLAVE);
        return new ByteArrayInputStream(salida.toByteArray());
    }
}
