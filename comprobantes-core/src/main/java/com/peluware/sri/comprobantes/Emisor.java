package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Digitos;
import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.valores.ContribuyenteEspecial;
import com.peluware.sri.comprobantes.valores.Ruc;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Datos del contribuyente emisor.
 *
 * <p>Ficha Técnica v2.34, Tabla 11 y numeral 9.5: datos parametrizables del emisor. Los que
 * cambian por local están en {@link Establecimiento}.
 *
 * <p>Alimentan el bloque {@code <infoTributaria>}, el RUC de la clave de acceso, y los tags
 * {@code contribuyenteEspecial} y {@code obligadoContabilidad} del bloque de información
 * propio de cada comprobante.
 *
 * @param ruc                   RUC del emisor
 * @param razonSocial           razón social
 * @param nombreComercial       nombre comercial; {@code null} si no aplica
 * @param direccionMatriz       dirección de la matriz
 * @param contribuyenteEspecial número de resolución con que el SRI lo designa contribuyente
 *                              especial; {@code null} si no lo es
 * @param obligadoContabilidad  si está obligado a llevar contabilidad
 * @param agenteRetencion       número de resolución de agente de retención; {@code null} si no
 *                              lo es
 * @param contribuyenteRimpe    si pertenece al régimen RIMPE
 */
public record Emisor(
    Ruc ruc,
    String razonSocial,
    @Nullable String nombreComercial,
    String direccionMatriz,
    @Nullable ContribuyenteEspecial contribuyenteEspecial,
    boolean obligadoContabilidad,
    @Nullable String agenteRetencion,
    boolean contribuyenteRimpe
) {

    public static final int RAZON_SOCIAL_LONGITUD_MAXIMA = 300;
    public static final int NOMBRE_COMERCIAL_LONGITUD_MAXIMA = 300;
    public static final int DIRECCION_MATRIZ_LONGITUD_MAXIMA = 300;

    /** Nombres de los campos en los defectos de validación, que son los tags del esquema. */
    public static final String CAMPO_DIRECCION_MATRIZ = "dirMatriz";
    public static final int AGENTE_RETENCION_LONGITUD_MAXIMA = 8;

    public Emisor {
        Objects.requireNonNull(ruc, "ruc");
        Textos.validar(razonSocial, RAZON_SOCIAL_LONGITUD_MAXIMA, "razonSocial").lanzarSiInvalida();
        Textos.validarOpcional(nombreComercial, NOMBRE_COMERCIAL_LONGITUD_MAXIMA, "nombreComercial").lanzarSiInvalida();
        Textos.validar(direccionMatriz, DIRECCION_MATRIZ_LONGITUD_MAXIMA, CAMPO_DIRECCION_MATRIZ).lanzarSiInvalida();
        if (agenteRetencion != null) {
            Digitos.validar(agenteRetencion, 1, AGENTE_RETENCION_LONGITUD_MAXIMA, "agenteRetencion").lanzarSiInvalida();
        }
    }

    /**
     * Emisor con lo mínimo que exige el esquema.
     */
    public static Emisor of(Ruc ruc, String razonSocial, String direccionMatriz) {
        return new Emisor(ruc, razonSocial, null, direccionMatriz, null, false, null, false);
    }

    /**
     * Builder a partir de los campos obligatorios.
     */
    public static Builder builder(Ruc ruc, String razonSocial, String direccionMatriz) {
        return new Builder(ruc, razonSocial, direccionMatriz);
    }

    /**
     * Completa el emisor con los campos de la Tabla 11 que solo se declaran cuando aplican.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder {

        private final Ruc ruc;
        private final String razonSocial;
        private final String direccionMatriz;
        private @Nullable String nombreComercial;
        private @Nullable ContribuyenteEspecial contribuyenteEspecial;
        private boolean obligadoContabilidad;
        private @Nullable String agenteRetencion;
        private boolean contribuyenteRimpe;

        private Builder(Ruc ruc, String razonSocial, String direccionMatriz) {
            this.ruc = ruc;
            this.razonSocial = razonSocial;
            this.direccionMatriz = direccionMatriz;
        }

        public Builder nombreComercial(@Nullable String nombreComercial) {
            this.nombreComercial = nombreComercial;
            return this;
        }

        /**
         * Número de resolución con que el SRI designa al emisor contribuyente especial,
         * tag {@code <contribuyenteEspecial>}.
         */
        public Builder contribuyenteEspecial(@Nullable ContribuyenteEspecial contribuyenteEspecial) {
            this.contribuyenteEspecial = contribuyenteEspecial;
            return this;
        }

        /**
         * Por omisión, falso.
         */
        public Builder obligadoContabilidad(boolean obligadoContabilidad) {
            this.obligadoContabilidad = obligadoContabilidad;
            return this;
        }

        public Builder agenteRetencion(@Nullable String agenteRetencion) {
            this.agenteRetencion = agenteRetencion;
            return this;
        }

        /**
         * Por omisión, falso.
         */
        public Builder contribuyenteRimpe(boolean contribuyenteRimpe) {
            this.contribuyenteRimpe = contribuyenteRimpe;
            return this;
        }

        public Emisor build() {
            return new Emisor(ruc, razonSocial, nombreComercial, direccionMatriz, contribuyenteEspecial, obligadoContabilidad, agenteRetencion, contribuyenteRimpe);
        }
    }
}
