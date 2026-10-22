package com.peluware.sri.comprobantes.liquidacioncompra;

import com.peluware.sri.comprobantes.Contraparte;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Textos;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Proveedor de una liquidación de compra.
 *
 * <p>Alimenta los tags {@code tipoIdentificacionProveedor}, {@code razonSocialProveedor},
 * {@code identificacionProveedor} y {@code direccionProveedor} de
 * {@code infoLiquidacionCompra}.
 *
 * @param tipoIdentificacion tipo de identificación, tag {@code <tipoIdentificacionProveedor>}
 * @param identificacion     número de identificación
 * @param razonSocial        razón social o nombres y apellidos
 * @param direccion          dirección del proveedor; {@code null} si no se declara
 * @see com.peluware.sri.comprobantes.Comprador
 */
public record Proveedor(TipoIdentificacion tipoIdentificacion, String identificacion, String razonSocial, @Nullable String direccion) implements Contraparte {

    public static final int IDENTIFICACION_LONGITUD_MAXIMA = 20;
    public static final int RAZON_SOCIAL_LONGITUD_MAXIMA = 300;
    public static final int DIRECCION_LONGITUD_MAXIMA = 300;

    public Proveedor {
        Objects.requireNonNull(tipoIdentificacion, "tipoIdentificacion");
        Textos.validar(identificacion, IDENTIFICACION_LONGITUD_MAXIMA, "identificacionProveedor").lanzarSiInvalida();
        Textos.validar(razonSocial, RAZON_SOCIAL_LONGITUD_MAXIMA, "razonSocialProveedor").lanzarSiInvalida();
        Textos.validarOpcional(direccion, DIRECCION_LONGITUD_MAXIMA, "direccionProveedor").lanzarSiInvalida();
    }

    /**
     * Proveedor sin dirección declarada.
     */
    public static Proveedor of(TipoIdentificacion tipoIdentificacion, String identificacion, String razonSocial) {
        return new Proveedor(tipoIdentificacion, identificacion, razonSocial, null);
    }
}
