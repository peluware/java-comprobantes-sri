package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Textos;

import java.util.Objects;

/**
 * Transportista que traslada la mercadería de una guía de remisión.
 *
 * <p>Alimenta los tags {@code razonSocialTransportista},
 * {@code tipoIdentificacionTransportista} y {@code rucTransportista}, que el esquema escribe
 * sueltos dentro de {@code infoGuiaRemision}.
 *
 * <p>El tag {@code rucTransportista} admite veinte caracteres y su tipo de identificación los
 * códigos 04 a 08 de la Tabla 6: RUC, cédula, pasaporte, consumidor final o identificación del
 * exterior.
 *
 * @param tipoIdentificacion tipo de identificación del transportista
 * @param identificacion     número de identificación, tag {@code <rucTransportista>}
 * @param razonSocial        razón social o nombres y apellidos
 */
public record Transportista(TipoIdentificacion tipoIdentificacion, String identificacion, String razonSocial) {

    public static final int IDENTIFICACION_LONGITUD_MAXIMA = 20;
    public static final int RAZON_SOCIAL_LONGITUD_MAXIMA = 300;

    public Transportista {
        Objects.requireNonNull(tipoIdentificacion, "tipoIdentificacion");
        Textos.validar(identificacion, IDENTIFICACION_LONGITUD_MAXIMA, "rucTransportista").lanzarSiInvalida();
        Textos.validar(razonSocial, RAZON_SOCIAL_LONGITUD_MAXIMA, "razonSocialTransportista").lanzarSiInvalida();
    }
}
