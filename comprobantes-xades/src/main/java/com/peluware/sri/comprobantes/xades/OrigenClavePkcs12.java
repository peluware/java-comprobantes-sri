package com.peluware.sri.comprobantes.xades;

import com.peluware.sri.comprobantes.firma.FirmaException;
import com.peluware.sri.comprobantes.firma.OrigenClave;
import com.peluware.sri.comprobantes.firma.ClaveFirma;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Clave de firma contenida en un archivo PKCS#12.
 *
 * <p>Ficha Técnica v2.34, numeral 6.8: el archivo de intercambio de información es PKCS12
 * con extensión {@code .p12}.
 *
 * <p>El constructor consume el flujo entero y <b>lo cierra</b>. La contraseña no se retiene.
 * La clave privada y la cadena de certificados quedan en el objeto, y {@link #abrir()} las
 * devuelve en cada llamada.
 *
 * <p>Si el flujo forma parte de uno mayor que debe seguir abierto, como una entrada de un
 * archivo ZIP, pase una copia: {@code new ByteArrayInputStream(entrada.readAllBytes())}.
 */
public final class OrigenClavePkcs12 implements OrigenClave {

    private final ClaveFirma clave;

    /**
     * Abre el archivo seleccionando automáticamente su única entrada con clave privada.
     *
     * @param pkcs12   flujo con el contenido del archivo .p12; se consume entero y se cierra
     * @param password contraseña del archivo; no se conserva ni se modifica
     */
    public OrigenClavePkcs12(InputStream pkcs12, char[] password) {
        this(pkcs12, password, null);
    }

    /**
     * Igual que {@link #OrigenClavePkcs12(InputStream, char[])}, con la contraseña como texto.
     */
    public OrigenClavePkcs12(InputStream pkcs12, CharSequence password) {
        this(pkcs12, password, null);
    }

    /**
     * Abre el archivo usando la entrada indicada, con la contraseña como texto.
     *
     * @param pkcs12   flujo con el contenido del archivo .p12; se consume entero y se cierra
     * @param password contraseña del archivo; no se conserva ni se modifica
     * @param alias    alias de la entrada a usar; {@code null} para seleccionarla
     *                 automáticamente
     */
    public OrigenClavePkcs12(InputStream pkcs12, CharSequence password, @Nullable String alias) {
        this(pkcs12, caracteres(password), alias);
    }

    /**
     * Abre el archivo usando la entrada indicada.
     *
     * @param pkcs12   flujo con el contenido del archivo .p12; se consume entero y se cierra
     * @param password contraseña del archivo; no se conserva ni se modifica
     * @param alias    alias de la entrada a usar; {@code null} para seleccionarla
     *                 automáticamente
     */
    public OrigenClavePkcs12(InputStream pkcs12, char[] password, @Nullable String alias) {
        Objects.requireNonNull(pkcs12, "pkcs12");
        Objects.requireNonNull(password, "password");
        try (pkcs12) {
            var almacen = KeyStore.getInstance("PKCS12");
            almacen.load(pkcs12, password);

            var entrada = alias != null ? alias : unicoAliasConClavePrivada(almacen);
            var clavePrivada = (PrivateKey) almacen.getKey(entrada, password);
            if (clavePrivada == null) {
                throw new FirmaException.SinClavePrivada(entrada);
            }
            this.clave = new ClaveFirma(clavePrivada, cadena(almacen, entrada));
        } catch (GeneralSecurityException | IOException e) {
            throw new FirmaException.ArchivoIlegible(e);
        }
    }

    @Override
    public ClaveFirma abrir() {
        return clave;
    }

    private static char[] caracteres(CharSequence password) {
        Objects.requireNonNull(password, "password");
        char[] copia = new char[password.length()];
        for (int i = 0; i < copia.length; i++) {
            copia[i] = password.charAt(i);
        }
        return copia;
    }

    private static String unicoAliasConClavePrivada(KeyStore almacen) throws GeneralSecurityException {
        var candidatos = new ArrayList<String>();
        for (var e = almacen.aliases(); e.hasMoreElements(); ) {
            var a = e.nextElement();
            if (almacen.isKeyEntry(a)) {
                candidatos.add(a);
            }
        }
        if (candidatos.isEmpty()) {
            throw new FirmaException.SinClavePrivada(null);
        }
        if (candidatos.size() > 1) {
            throw new FirmaException.VariasClaves(candidatos);
        }
        return candidatos.getFirst();
    }

    private static List<X509Certificate> cadena(KeyStore almacen, String entrada) throws GeneralSecurityException {
        var certificados = almacen.getCertificateChain(entrada);
        if (certificados == null || certificados.length == 0) {
            throw new FirmaException.SinCadenaDeCertificados(entrada);
        }
        return Arrays.stream(certificados).map(X509Certificate.class::cast).toList();
    }
}
