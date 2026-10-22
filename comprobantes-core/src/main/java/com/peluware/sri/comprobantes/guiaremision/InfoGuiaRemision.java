package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.interno.Textos;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bloque {@code <infoGuiaRemision>}.
 *
 * <p>Su esquema no declara {@code fechaEmision}. Las fechas que escribe son las del rango de
 * transporte, {@code <fechaIniTransporte>} y {@code <fechaFinTransporte>}.
 *
 * <p>Se obtiene de {@link GuiaRemision#infoGuiaRemision()}. No es un {@code record} para que el
 * esquema pueda ganar campos sin cambiar ninguna firma pública: su constructor no sale del
 * paquete.
 */
public final class InfoGuiaRemision {

    public static final int DIRECCION_PARTIDA_LONGITUD_MAXIMA = 300;
    public static final int PLACA_LONGITUD_MAXIMA = 20;
    public static final int RISE_LONGITUD_MAXIMA = 300;

    private final String direccionPartida;
    private final Transportista transportista;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final String placa;
    private final @Nullable String rise;

    InfoGuiaRemision(String direccionPartida, Transportista transportista, LocalDate fechaInicio, LocalDate fechaFin, String placa, @Nullable String rise) {
        Textos.validar(direccionPartida, DIRECCION_PARTIDA_LONGITUD_MAXIMA, "dirPartida").lanzarSiInvalida();
        Objects.requireNonNull(transportista, "transportista");
        Objects.requireNonNull(fechaInicio, "fechaInicio");
        Objects.requireNonNull(fechaFin, "fechaFin");
        Textos.validar(placa, PLACA_LONGITUD_MAXIMA, "placa").lanzarSiInvalida();
        Textos.validarOpcional(rise, RISE_LONGITUD_MAXIMA, "rise").lanzarSiInvalida();

        if (fechaFin.isBefore(fechaInicio)) {
            throw new ValidacionException(new Validacion.ReglaInvalida("El transporte termina el " + fechaFin + ", antes de empezar el " + fechaInicio));
        }

        this.direccionPartida = direccionPartida;
        this.transportista = transportista;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.placa = placa;
        this.rise = rise;
    }

    /**
     * Dirección desde la que parte la mercadería, tag {@code <dirPartida>}.
     */
    public String direccionPartida() {
        return direccionPartida;
    }

    /**
     * Quien traslada la mercadería.
     */
    public Transportista transportista() {
        return transportista;
    }

    /**
     * Fecha en que empieza el transporte, tag {@code <fechaIniTransporte>}.
     */
    public LocalDate fechaInicio() {
        return fechaInicio;
    }

    /**
     * Fecha en que termina el transporte, tag {@code <fechaFinTransporte>}.
     */
    public LocalDate fechaFin() {
        return fechaFin;
    }

    /**
     * Placa del vehículo, tag {@code <placa>}.
     */
    public String placa() {
        return placa;
    }

    /**
     * Leyenda del régimen simplificado, tag {@code <rise>}; {@code null} si no aplica.
     */
    public @Nullable String rise() {
        return rise;
    }
}
