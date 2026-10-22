package com.peluware.sri.comprobantes.catalogo;

import java.util.Objects;

/**
 * Tipo de emisión del comprobante.
 *
 * <p>Ficha Técnica v2.34, Tabla 2. El esquema offline define únicamente la emisión normal.
 */
public enum TipoEmision {

    NORMAL("1");

    private final String codigo;

    TipoEmision(String codigo) {
        this.codigo = codigo;
    }

    /** Dígito que viaja en {@code <tipoEmision>} y en la clave de acceso. */
    public String codigo() {
        return codigo;
    }

    /**
     * Resuelve el tipo de emisión a partir de su código.
     *
     * @throws CatalogoException si el código no es {@code 1}
     */
    public static TipoEmision of(String codigo) {
        Objects.requireNonNull(codigo, "codigo");
        if (NORMAL.codigo.equals(codigo)) {
            return NORMAL;
        }
        throw new CatalogoException("Código de tipo de emisión desconocido: " + codigo);
    }
}
