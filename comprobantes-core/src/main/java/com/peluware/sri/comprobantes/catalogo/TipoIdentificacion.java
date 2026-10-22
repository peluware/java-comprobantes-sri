package com.peluware.sri.comprobantes.catalogo;

import java.util.Objects;

/**
 * Tipo de identificación del comprador, sujeto retenido o destinatario.
 *
 * <p>Ficha Técnica v2.34, Tabla 6.
 */
public enum TipoIdentificacion {

    RUC("04"),
    CEDULA("05"),
    PASAPORTE("06"),
    CONSUMIDOR_FINAL("07"),
    IDENTIFICACION_EXTERIOR("08");

    /** Identificación que acompaña a {@link #CONSUMIDOR_FINAL}: trece nueves. */
    public static final String IDENTIFICACION_CONSUMIDOR_FINAL = "9999999999999";

    private final String codigo;

    TipoIdentificacion(String codigo) {
        this.codigo = codigo;
    }

    /** Código de dos dígitos que viaja en {@code <tipoIdentificacionComprador>}. */
    public String codigo() {
        return codigo;
    }

    /**
     * Resuelve el tipo de identificación a partir de su código.
     *
     * @throws CatalogoException si el código no pertenece a la Tabla 6
     */
    public static TipoIdentificacion of(String codigo) {
        Objects.requireNonNull(codigo, "codigo");
        for (var t : values()) {
            if (t.codigo.equals(codigo)) {
                return t;
            }
        }
        throw new CatalogoException("Código de tipo de identificación desconocido: " + codigo);
    }
}
