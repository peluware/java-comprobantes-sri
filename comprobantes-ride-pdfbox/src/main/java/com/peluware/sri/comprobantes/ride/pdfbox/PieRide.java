package com.peluware.sri.comprobantes.ride.pdfbox;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Pie del RIDE, al final de la página.
 *
 * <p>Es el sitio donde un proveedor de facturación electrónica pone su marca. No es un
 * requisito del Anexo 2: cabe en los «datos adicionales» que admite el numeral 9.19.
 *
 * <p>No confundirlo con el RUC del proveedor del sistema, que el Anexo 26 sí exige y que va
 * como campo adicional dentro del XML, no aquí.
 */
public final class PieRide {

    private final String texto;
    private final @Nullable LogoRide logo;
    private final Alineacion alineacion;

    private PieRide(String texto, @Nullable LogoRide logo, Alineacion alineacion) {
        this.texto = Objects.requireNonNull(texto, "texto");
        this.logo = logo;
        this.alineacion = Objects.requireNonNull(alineacion, "alineacion");
    }

    /**
     * Pie de solo texto, a la izquierda.
     */
    public static PieRide de(String texto) {
        return new PieRide(texto, null, Alineacion.IZQUIERDA);
    }

    /**
     * Pie con texto y logo propio, a la izquierda.
     */
    public static PieRide de(String texto, LogoRide logo) {
        return new PieRide(texto, Objects.requireNonNull(logo, "logo"), Alineacion.IZQUIERDA);
    }

    /**
     * El mismo pie en otra posición.
     */
    public PieRide alineado(Alineacion alineacion) {
        return new PieRide(texto, logo, alineacion);
    }

    /**
     * El mismo pie centrado.
     */
    public PieRide centrado() {
        return alineado(Alineacion.CENTRO);
    }

    /**
     * Texto del pie; cadena vacía si solo lleva logo.
     */
    public String texto() {
        return texto;
    }

    /**
     * Cómo crear la imagen del pie; {@code null} si solo lleva texto.
     */
    public @Nullable LogoRide logo() {
        return logo;
    }

    /**
     * Dónde se coloca dentro del ancho de la página.
     */
    public Alineacion alineacion() {
        return alineacion;
    }
}
