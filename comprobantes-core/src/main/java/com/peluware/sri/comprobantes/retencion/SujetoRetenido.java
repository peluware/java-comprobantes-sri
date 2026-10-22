package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Textos;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Sujeto al que se le retiene, la contraparte de un comprobante de retención.
 *
 * <p>Alimenta los tags {@code tipoIdentificacionSujetoRetenido}, {@code tipoSujetoRetenido},
 * {@code parteRel}, {@code razonSocialSujetoRetenido} e {@code identificacionSujetoRetenido}
 * de {@code infoCompRetencion}.
 *
 * @param tipoIdentificacion tipo de identificación, tag {@code <tipoIdentificacionSujetoRetenido>}
 * @param identificacion     número de identificación
 * @param razonSocial        razón social o nombres y apellidos
 * @param tipo               persona natural o sociedad, tag {@code <tipoSujetoRetenido>};
 *                           {@code null} si no se declara
 * @param parteRelacionada   si el sujeto es parte relacionada del agente de retención
 */
public record SujetoRetenido(
    TipoIdentificacion tipoIdentificacion,
    String identificacion,
    String razonSocial,
    @Nullable TipoSujetoRetenido tipo,
    boolean parteRelacionada
) {

    public static final int IDENTIFICACION_LONGITUD_MAXIMA = 20;
    public static final int RAZON_SOCIAL_LONGITUD_MAXIMA = 300;

    public SujetoRetenido {
        Objects.requireNonNull(tipoIdentificacion, "tipoIdentificacion");
        Textos.validar(identificacion, IDENTIFICACION_LONGITUD_MAXIMA, "identificacionSujetoRetenido").lanzarSiInvalida();
        Textos.validar(razonSocial, RAZON_SOCIAL_LONGITUD_MAXIMA, "razonSocialSujetoRetenido").lanzarSiInvalida();
    }

    /**
     * Sujeto retenido que no es parte relacionada y no declara su tipo.
     */
    public static SujetoRetenido of(TipoIdentificacion tipoIdentificacion, String identificacion, String razonSocial) {
        return new SujetoRetenido(tipoIdentificacion, identificacion, razonSocial, null, false);
    }
}
