package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.interno.Digitos;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Documento que sustenta el traslado de un destinatario de la guía de remisión.
 *
 * <p>Alimenta los tags {@code codDocSustento}, {@code numDocSustento},
 * {@code numAutDocSustento} y {@code fechaEmisionDocSustento} de un destinatario.
 *
 * <p>El esquema declara los cuatro tags opcionales e independientes. Esta clase exige juntos el
 * tipo, la serie y el secuencial, y deja opcionales el número de autorización y la fecha.
 *
 * @param tipoComprobante    tipo del documento que sustenta, de la Tabla 3
 * @param serie              establecimiento y punto de emisión del documento
 * @param secuencial         secuencial del documento
 * @param numeroAutorizacion número de autorización del documento, de 10 a 49 dígitos;
 *                           {@code null} si no se declara
 * @param fechaEmision       fecha de emisión del documento; {@code null} si no se declara
 */
public record SustentoTraslado(
    TipoComprobante tipoComprobante,
    Serie serie,
    Secuencial secuencial,
    @Nullable String numeroAutorizacion,
    @Nullable LocalDate fechaEmision
) {

    public static final int NUMERO_AUTORIZACION_LONGITUD_MINIMA = 10;
    public static final int NUMERO_AUTORIZACION_LONGITUD_MAXIMA = 49;

    public SustentoTraslado {
        Objects.requireNonNull(tipoComprobante, "tipoComprobante");
        Objects.requireNonNull(serie, "serie");
        Objects.requireNonNull(secuencial, "secuencial");
        if (numeroAutorizacion != null) {
            Digitos.validar(numeroAutorizacion, NUMERO_AUTORIZACION_LONGITUD_MINIMA, NUMERO_AUTORIZACION_LONGITUD_MAXIMA, "numAutDocSustento").lanzarSiInvalida();
        }
    }

    /**
     * Documento sin número de autorización ni fecha.
     */
    public static SustentoTraslado of(TipoComprobante tipoComprobante, Serie serie, Secuencial secuencial) {
        return new SustentoTraslado(tipoComprobante, serie, secuencial, null, null);
    }

    /**
     * Número del documento en el formato que exige el esquema, por ejemplo
     * {@code 001-001-000000001}.
     */
    public String numero() {
        return serie.numeroDocumento(secuencial);
    }

    @Override
    public String toString() {
        return "SustentoTraslado[" + tipoComprobante + " " + numero() + "]";
    }
}
