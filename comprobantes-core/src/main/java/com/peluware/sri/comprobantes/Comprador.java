package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Textos;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Datos del adquirente.
 *
 * <p>Alimentan los tags {@code tipoIdentificacionComprador}, {@code razonSocialComprador},
 * {@code identificacionComprador} y {@code direccionComprador} del bloque
 * {@code <infoFactura>}.
 *
 * @param tipoIdentificacion tipo de identificación de la Tabla 6
 * @param identificacion     número de identificación
 * @param razonSocial        nombre o razón social
 * @param direccion          dirección; {@code null} si no aplica
 */
public record Comprador(TipoIdentificacion tipoIdentificacion, String identificacion, String razonSocial, @Nullable String direccion) implements Contraparte {

    public static final int IDENTIFICACION_LONGITUD_MAXIMA = 20;
    public static final int RAZON_SOCIAL_LONGITUD_MAXIMA = 300;
    public static final int DIRECCION_LONGITUD_MAXIMA = 300;

    /**
     * Razón social que acompaña a una venta a consumidor final.
     *
     * <p>Ficha Técnica v2.34, numeral 9.10 y Anexo 1.
     */
    public static final String RAZON_SOCIAL_CONSUMIDOR_FINAL = "CONSUMIDOR FINAL";

    /**
     * Razón social admitida para el receptor en el ambiente de pruebas.
     *
     * <p>Ficha Técnica v2.34, numeral 5.6 y Tabla 5: admitida en la razón social del receptor,
     * destinatario y agente retenido del ambiente de pruebas. No es obligatoria y no aplica al
     * emisor.
     */
    public static final String RAZON_SOCIAL_PRUEBAS = "PRUEBAS SERVICIO DE RENTAS INTERNAS";

    public Comprador {
        Objects.requireNonNull(tipoIdentificacion, "tipoIdentificacion");
        Textos.validar(identificacion, IDENTIFICACION_LONGITUD_MAXIMA, "identificacionComprador").lanzarSiInvalida();
        Textos.validar(razonSocial, RAZON_SOCIAL_LONGITUD_MAXIMA, "razonSocialComprador").lanzarSiInvalida();
        Textos.validarOpcional(direccion, DIRECCION_LONGITUD_MAXIMA, "direccionComprador").lanzarSiInvalida();
    }

    /**
     * Construye el comprador sin dirección.
     */
    public static Comprador of(TipoIdentificacion tipoIdentificacion, String identificacion, String razonSocial) {
        return new Comprador(tipoIdentificacion, identificacion, razonSocial, null);
    }

    /**
     * Comprador de una venta a consumidor final: trece nueves como identificación.
     *
     * <p>Ficha Técnica v2.34, numeral 9.10: solo aplicable cuando la factura no excede los
     * 50 USD.
     */
    public static Comprador consumidorFinal() {
        return of(TipoIdentificacion.CONSUMIDOR_FINAL, TipoIdentificacion.IDENTIFICACION_CONSUMIDOR_FINAL, RAZON_SOCIAL_CONSUMIDOR_FINAL);
    }

    /**
     * Indica si se trata de una venta a consumidor final.
     */
    public boolean esConsumidorFinal() {
        return tipoIdentificacion == TipoIdentificacion.CONSUMIDOR_FINAL;
    }
}
