package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoEmision;
import com.peluware.sri.comprobantes.interno.DigitoVerificador;
import com.peluware.sri.comprobantes.interno.Digitos;
import com.peluware.sri.comprobantes.valores.Cantidad;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Clave de acceso: identificador de 49 dígitos del comprobante.
 *
 * <p>Ficha Técnica v2.34, numeral 5.2 y Tabla 1. Composición, en orden: fecha de emisión (8),
 * tipo de comprobante (2), RUC (13), ambiente (1), serie (6), secuencial (9), código
 * numérico (8), tipo de emisión (1) y dígito verificador (1).
 *
 * <p>En el esquema offline este valor constituye también el número de autorización
 * (numeral 5.9).
 */
public final class ClaveAcceso {

    public static final int LONGITUD = 49;

    /**
     * Cantidad de dígitos sobre los que se calcula el dígito verificador.
     */
    public static final int LONGITUD_SIN_DV = 48;

    /**
     * Nombre del campo en los defectos de validación.
     */
    public static final String CAMPO = "claveAcceso";

    /** Nombre del campo del código de documento en los defectos, que es su tag del esquema. */
    public static final String CAMPO_CODIGO_DOCUMENTO = "codDoc";

    /** La fecha de emisión tal como viaja en la clave: pegada y sin separadores. */
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("ddMMuuuu");

    private static final int FECHA_DESDE = 0;
    private static final int FECHA_LONGITUD = 8;
    private static final int CODIGO_NUMERICO_DESDE = 39;

    private ClaveAcceso() {
    }

    /**
     * Compone los 49 dígitos y calcula el dígito verificador.
     */
    public static String generar(LocalDate fechaEmision, TipoComprobante tipoComprobante, Ruc ruc, Ambiente ambiente, Serie serie, Secuencial secuencial, CodigoNumerico codigoNumerico, TipoEmision tipoEmision) {
        return generar(FECHA.format(fechaEmision), tipoComprobante.codigo(), ruc, ambiente, serie, secuencial, codigoNumerico, tipoEmision);
    }

    /**
     * Los ocho dígitos de fecha de emisión con que abre la clave, en el formato {@link #FECHA}.
     */
    public static String fecha(String clave) {
        return clave.substring(FECHA_DESDE, FECHA_DESDE + FECHA_LONGITUD);
    }

    /**
     * Los ocho dígitos de código numérico que la clave lleva antes del tipo de emisión.
     */
    public static String codigoNumerico(String clave) {
        return clave.substring(CODIGO_NUMERICO_DESDE, CODIGO_NUMERICO_DESDE + CodigoNumerico.LONGITUD);
    }

    /**
     * Valida una cadena como clave de acceso.
     *
     * <p>Comprueba longitud, conjunto de caracteres y dígito verificador, en ese orden, y
     * devuelve el primer defecto encontrado.
     */
    public static Validacion validar(@Nullable String valor) {
        var digitos = Digitos.validar(valor, LONGITUD, CAMPO);
        if (valor == null || !digitos.esValida()) {
            return digitos;
        }
        var esperado = DigitoVerificador.modulo11(valor.substring(0, LONGITUD_SIN_DV));
        var encontrado = valor.charAt(LONGITUD_SIN_DV);
        if (encontrado != esperado) {
            return new Validacion.CampoInvalido(CAMPO, "Dígito verificador incorrecto: se esperaba " + esperado + " y se encontró " + encontrado);
        }
        return Validacion.valida();
    }

    /**
     * Indica si {@code valor} es una clave de acceso válida.
     */
    public static boolean esValido(@Nullable String valor) {
        return validar(valor).esValida();
    }

    /**
     * Devuelve {@code valor} si es una clave de acceso válida.
     *
     * @throws ValidacionException si no lo es, indicando el motivo
     */
    public static String exigir(@Nullable String valor) {
        validar(valor).lanzarSiInvalida();
        return Objects.requireNonNull(valor);
    }

    private static String generar(String fecha, String codigoDocumento, Ruc ruc, Ambiente ambiente, Serie serie, Secuencial secuencial, CodigoNumerico codigoNumerico, TipoEmision tipoEmision) {
        Digitos.validar(codigoDocumento, 2, CAMPO_CODIGO_DOCUMENTO).lanzarSiInvalida();

        var sinDv = fecha
            + codigoDocumento
            + ruc.valor()
            + ambiente.codigo()
            + serie.valor()
            + secuencial.valor()
            + codigoNumerico.valor()
            + tipoEmision.codigo();

        return sinDv + DigitoVerificador.modulo11(sinDv);
    }
}
