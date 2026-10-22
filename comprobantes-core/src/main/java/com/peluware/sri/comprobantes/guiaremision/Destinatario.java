package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.interno.Digitos;
import com.peluware.sri.comprobantes.interno.Textos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Destinatario de una guía de remisión, con la mercadería que se le traslada.
 *
 * <p>Una guía lleva uno o varios, y cada uno agrupa los detalles que se le trasladan.
 *
 * <p>El esquema pide el número de identificación del destinatario y no su tipo.
 *
 * @param identificacion        número de identificación del destinatario
 * @param razonSocial           razón social o nombres y apellidos
 * @param direccion             dirección del destinatario
 * @param motivoTraslado        motivo del traslado, texto libre
 * @param documentoAduanero     documento aduanero único; {@code null} si no aplica
 * @param codigoEstablecimientoDestino código del establecimiento de destino, tres dígitos;
 *                              {@code null} si no aplica
 * @param ruta                  ruta del traslado; {@code null} si no se declara
 * @param sustentoTraslado     documento que sustenta el traslado; {@code null} si no se declara
 * @param detalles              mercadería que se le traslada, al menos una línea
 */
public record Destinatario(
    String identificacion,
    String razonSocial,
    String direccion,
    String motivoTraslado,
    @Nullable String documentoAduanero,
    @Nullable String codigoEstablecimientoDestino,
    @Nullable String ruta,
    @Nullable SustentoTraslado sustentoTraslado,
    List<DetalleGuiaRemision> detalles
) {

    public static final int IDENTIFICACION_LONGITUD_MAXIMA = 20;
    public static final int RAZON_SOCIAL_LONGITUD_MAXIMA = 300;
    public static final int DIRECCION_LONGITUD_MAXIMA = 300;
    public static final int MOTIVO_TRASLADO_LONGITUD_MAXIMA = 300;
    public static final int DOCUMENTO_ADUANERO_LONGITUD_MAXIMA = 20;
    public static final int RUTA_LONGITUD_MAXIMA = 300;
    public static final int CODIGO_ESTABLECIMIENTO_DESTINO_LONGITUD = 3;

    public Destinatario {
        Textos.validar(identificacion, IDENTIFICACION_LONGITUD_MAXIMA, "identificacionDestinatario").lanzarSiInvalida();
        Textos.validar(razonSocial, RAZON_SOCIAL_LONGITUD_MAXIMA, "razonSocialDestinatario").lanzarSiInvalida();
        Textos.validar(direccion, DIRECCION_LONGITUD_MAXIMA, "dirDestinatario").lanzarSiInvalida();
        Textos.validar(motivoTraslado, MOTIVO_TRASLADO_LONGITUD_MAXIMA, "motivoTraslado").lanzarSiInvalida();
        Textos.validarOpcional(documentoAduanero, DOCUMENTO_ADUANERO_LONGITUD_MAXIMA, "docAduaneroUnico").lanzarSiInvalida();
        Textos.validarOpcional(ruta, RUTA_LONGITUD_MAXIMA, "ruta").lanzarSiInvalida();
        if (codigoEstablecimientoDestino != null) {
            Digitos.validar(codigoEstablecimientoDestino, CODIGO_ESTABLECIMIENTO_DESTINO_LONGITUD, "codEstabDestino").lanzarSiInvalida();
        }

        detalles = List.copyOf(detalles);
        if (detalles.isEmpty()) {
            throw new ValidacionException(new Validacion.CampoInvalido("detalles", "Cada destinatario debe llevar al menos un detalle"));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Construye un destinatario con su mercadería.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder {

        private @Nullable String identificacion;
        private @Nullable String razonSocial;
        private @Nullable String direccion;
        private @Nullable String motivoTraslado;
        private @Nullable String documentoAduanero;
        private @Nullable String codigoEstablecimientoDestino;
        private @Nullable String ruta;
        private @Nullable SustentoTraslado sustentoTraslado;
        private final List<DetalleGuiaRemision> detalles = new ArrayList<>();

        private Builder() {
        }

        public Builder identificacion(String identificacion) {
            this.identificacion = identificacion;
            return this;
        }

        public Builder razonSocial(String razonSocial) {
            this.razonSocial = razonSocial;
            return this;
        }

        public Builder direccion(String direccion) {
            this.direccion = direccion;
            return this;
        }

        public Builder motivoTraslado(String motivoTraslado) {
            this.motivoTraslado = motivoTraslado;
            return this;
        }

        public Builder documentoAduanero(@Nullable String documentoAduanero) {
            this.documentoAduanero = documentoAduanero;
            return this;
        }

        public Builder codigoEstablecimientoDestino(@Nullable String codigoEstablecimientoDestino) {
            this.codigoEstablecimientoDestino = codigoEstablecimientoDestino;
            return this;
        }

        public Builder ruta(@Nullable String ruta) {
            this.ruta = ruta;
            return this;
        }

        public Builder sustentoTraslado(@Nullable SustentoTraslado sustentoTraslado) {
            this.sustentoTraslado = sustentoTraslado;
            return this;
        }

        public Builder detalle(DetalleGuiaRemision detalle) {
            detalles.add(Objects.requireNonNull(detalle, "detalle"));
            return this;
        }

        public Destinatario build() {
            return new Destinatario(
                Objects.requireNonNull(identificacion, "identificacion"),
                Objects.requireNonNull(razonSocial, "razonSocial"),
                Objects.requireNonNull(direccion, "direccion"),
                Objects.requireNonNull(motivoTraslado, "motivoTraslado"),
                documentoAduanero,
                codigoEstablecimientoDestino,
                ruta,
                sustentoTraslado,
                detalles
            );
        }
    }
}
