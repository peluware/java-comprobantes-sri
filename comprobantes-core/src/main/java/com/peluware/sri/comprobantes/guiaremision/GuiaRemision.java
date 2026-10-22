package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.ConfiguracionException;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Guía de remisión: acompaña mercadería en tránsito.
 *
 * <p>Su esquema no declara importes, impuestos ni totales. Lleva la dirección de partida, el
 * transportista, el rango de fechas del transporte, la placa del vehículo y uno o varios
 * destinatarios, cada uno con sus propios detalles.
 *
 * <p>Tampoco declara {@code fechaEmision}, pero la clave de acceso la exige.
 *
 * <p>Solo se construye con {@link #builder()}, que calcula la clave de acceso.
 */
public final class GuiaRemision extends Comprobante {

    private final InfoGuiaRemision infoGuiaRemision;
    private final List<Destinatario> destinatarios;

    private GuiaRemision(Builder builder, InfoGuiaRemision infoGuiaRemision) {
        super(builder);
        this.infoGuiaRemision = infoGuiaRemision;
        this.destinatarios = List.copyOf(builder.destinatarios);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Tipo de comprobante, siempre {@link TipoComprobante#GUIA_REMISION}. */
    @Override
    public TipoComprobante tipoComprobante() {
        return TipoComprobante.GUIA_REMISION;
    }

    public InfoGuiaRemision infoGuiaRemision() {
        return infoGuiaRemision;
    }

    public List<Destinatario> destinatarios() {
        return destinatarios;
    }

    /**
     * Construye una guía de remisión calculando la clave de acceso.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends Comprobante.Builder<Builder> {

        private @Nullable String direccionPartida;
        private @Nullable Transportista transportista;
        private @Nullable LocalDate fechaInicio;
        private @Nullable LocalDate fechaFin;
        private @Nullable String placa;
        private @Nullable String rise;
        private final List<Destinatario> destinatarios = new ArrayList<>();

        private Builder() {
        }

        @Override
        protected TipoComprobante tipoComprobante() {
            return TipoComprobante.GUIA_REMISION;
        }

        /**
         * Dirección desde la que parte la mercadería.
         */
        public Builder direccionPartida(String direccionPartida) {
            this.direccionPartida = direccionPartida;
            return this;
        }

        public Builder transportista(Transportista transportista) {
            this.transportista = transportista;
            return this;
        }

        /**
         * Rango de fechas del transporte, tags {@code <fechaIniTransporte>} y
         * {@code <fechaFinTransporte>}.
         */
        public Builder transporte(LocalDate fechaInicio, LocalDate fechaFin) {
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
            return this;
        }

        public Builder placa(String placa) {
            this.placa = placa;
            return this;
        }

        /**
         * Leyenda del régimen simplificado RISE, tag {@code <rise>}.
         */
        public Builder rise(@Nullable String rise) {
            this.rise = rise;
            return this;
        }

        public Builder destinatario(Destinatario destinatario) {
            destinatarios.add(Objects.requireNonNull(destinatario, "destinatario"));
            return this;
        }

        /**
         * Construye la guía de remisión, calculando la clave de acceso.
         *
         * @throws ValidacionException    si falta algún dato obligatorio o no hay destinatarios
         * @throws ConfiguracionException si falta el código numérico o se indicaron dos a la vez
         */
        public GuiaRemision build() {
            validarComun();
            if (destinatarios.isEmpty()) {
                throw new ValidacionException(new Validacion.CampoInvalido("destinatarios", "La guía de remisión debe llevar al menos un destinatario"));
            }

            var info = new InfoGuiaRemision(
                Objects.requireNonNull(direccionPartida, "direccionPartida"),
                Objects.requireNonNull(transportista, "transportista"),
                Objects.requireNonNull(fechaInicio, "fechaInicio"),
                Objects.requireNonNull(fechaFin, "fechaFin"),
                Objects.requireNonNull(placa, "placa"),
                rise
            );

            return new GuiaRemision(this, info);
        }
    }
}
