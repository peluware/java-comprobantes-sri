package com.peluware.sri.comprobantes.firma;

import com.peluware.sri.comprobantes.ComprobanteException;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Error al abrir la clave de firma o al firmar un comprobante.
 *
 * <p>Cada subtipo lleva los datos de su caso y redacta su propio mensaje de usuario.
 *
 * <pre>{@code
 * catch (FirmaException e) {
 *     switch (e) {
 *         case FirmaException.CertificadoCaducado c -> avisarRenovacion(c.caducidad());
 *         case FirmaException.VariasClaves v        -> pedirAlias(v.alias());
 *         default                                   -> registrar(e);
 *     }
 * }
 * }</pre>
 */
public abstract sealed class FirmaException extends ComprobanteException {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FirmaException(String detalle) {
        super(detalle);
    }

    private FirmaException(String detalle, @Nullable Throwable causa) {
        super(detalle, causa);
    }

    /**
     * El certificado superó su fecha de caducidad.
     *
     * <p>Error 42 del SRI, asignado al emisor.
     */
    public static final class CertificadoCaducado extends FirmaException {

        private final LocalDate caducidad;

        public CertificadoCaducado(LocalDate caducidad, Throwable causa) {
            super("El certificado caducó el " + caducidad + "; el SRI lo rechaza con el error 42", causa);
            this.caducidad = Objects.requireNonNull(caducidad, "caducidad");
        }

        /**
         * Fecha en que caducó el certificado.
         */
        public LocalDate caducidad() {
            return caducidad;
        }

        @Override
        public String mensajeUsuario() {
            return "Su certificado de firma electrónica caducó el " + FECHA.format(caducidad) + ". Renuévelo con su entidad de certificación.";
        }
    }

    /**
     * El certificado todavía no ha entrado en vigencia.
     */
    public static final class CertificadoNoVigente extends FirmaException {

        private final LocalDate vigenteDesde;

        public CertificadoNoVigente(LocalDate vigenteDesde, Throwable causa) {
            super("El certificado no es válido hasta el " + vigenteDesde, causa);
            this.vigenteDesde = Objects.requireNonNull(vigenteDesde, "vigenteDesde");
        }

        /**
         * Fecha a partir de la cual el certificado es válido.
         */
        public LocalDate vigenteDesde() {
            return vigenteDesde;
        }

        @Override
        public String mensajeUsuario() {
            return "Su certificado de firma electrónica será válido a partir del " + FECHA.format(vigenteDesde) + ".";
        }
    }

    /**
     * El certificado usa un algoritmo distinto de RSA.
     *
     * <p>Ficha Técnica v2.34, numeral 6.8.
     */
    public static final class AlgoritmoNoSoportado extends FirmaException {

        private final String algoritmo;

        public AlgoritmoNoSoportado(String algoritmo) {
            super("El certificado debe tener clave RSA, tiene " + algoritmo);
            this.algoritmo = Objects.requireNonNull(algoritmo, "algoritmo");
        }

        /**
         * Algoritmo que trae el certificado.
         */
        public String algoritmo() {
            return algoritmo;
        }

        @Override
        public String mensajeUsuario() {
            return "Su certificado de firma electrónica no sirve para emitir comprobantes ante el SRI.";
        }
    }

    /**
     * La clave del certificado es más corta de lo que exige el numeral 6.8.
     */
    public static final class ClaveDemasiadoCorta extends FirmaException {

        private final int bits;
        private final int minimo;

        public ClaveDemasiadoCorta(int bits, int minimo) {
            super("La Ficha Técnica, numeral 6.8, exige clave RSA de " + minimo + " bits; el certificado tiene " + bits);
            this.bits = bits;
            this.minimo = minimo;
        }

        public int bits() {
            return bits;
        }

        public int minimo() {
            return minimo;
        }

        @Override
        public String mensajeUsuario() {
            return "Su certificado de firma electrónica no cumple los requisitos de seguridad del SRI.";
        }
    }

    /**
     * El archivo no se pudo abrir, normalmente por contraseña incorrecta.
     */
    public static final class ArchivoIlegible extends FirmaException {

        public ArchivoIlegible(Throwable causa) {
            super("No se pudo abrir el archivo PKCS#12: " + causa.getMessage(), causa);
        }

        @Override
        public String mensajeUsuario() {
            return "No se pudo abrir el archivo del certificado. Verifique la contraseña.";
        }
    }

    /**
     * El archivo, o la entrada indicada, no contiene una clave privada.
     */
    public static final class SinClavePrivada extends FirmaException {

        private final @Nullable String alias;

        public SinClavePrivada(@Nullable String alias) {
            super(alias == null
                ? "El archivo PKCS#12 no contiene ninguna clave privada"
                : "La entrada " + alias + " no contiene una clave privada");
            this.alias = alias;
        }

        /**
         * Alias consultado; {@code null} si se recorrió el archivo entero.
         */
        public @Nullable String alias() {
            return alias;
        }

        @Override
        public String mensajeUsuario() {
            return "El archivo del certificado no contiene una clave de firma.";
        }
    }

    /**
     * El certificado no trae cadena de certificados.
     */
    public static final class SinCadenaDeCertificados extends FirmaException {

        private final @Nullable String alias;

        public SinCadenaDeCertificados(@Nullable String alias) {
            super(alias == null
                ? "La cadena de certificados no puede estar vacía"
                : "La entrada " + alias + " no tiene cadena de certificados");
            this.alias = alias;
        }

        public @Nullable String alias() {
            return alias;
        }

        @Override
        public String mensajeUsuario() {
            return "El archivo del certificado está incompleto.";
        }
    }

    /**
     * El archivo contiene varias claves privadas y no se indicó cuál usar.
     */
    public static final class VariasClaves extends FirmaException {

        private final List<String> alias;

        public VariasClaves(List<String> alias) {
            super("El archivo PKCS#12 contiene " + alias.size() + " claves privadas; indique el alias: " + alias);
            this.alias = List.copyOf(alias);
        }

        /**
         * Alias de las claves privadas encontradas.
         */
        public List<String> alias() {
            return alias;
        }

        @Override
        public String mensajeUsuario() {
            return "El archivo del certificado contiene varias claves de firma. Indique cuál debe usarse.";
        }
    }

    /**
     * El XML a firmar no tiene la estructura que exige el SRI.
     */
    public static final class ComprobanteInvalido extends FirmaException {

        public ComprobanteInvalido(String detalle) {
            super(detalle);
        }

        @Override
        public String mensajeUsuario() {
            return "El comprobante no tiene el formato que exige el SRI.";
        }
    }

    /**
     * Fallo no previsto al abrir la clave o al firmar.
     */
    public static final class Desconocida extends FirmaException {

        public Desconocida(String detalle, @Nullable Throwable causa) {
            super(detalle, causa);
        }

        @Override
        public String mensajeUsuario() {
            return "No se pudo firmar el comprobante. Intente nuevamente o contacte al soporte.";
        }
    }
}
