package com.peluware.sri.comprobantes.catalogo;

import java.util.Objects;

/**
 * Impuesto al que corresponde un {@code <impuesto>} del comprobante.
 *
 * <p>Ficha Técnica v2.34, Tabla 16.
 */
public enum CodigoImpuesto {

    IVA("2"),
    ICE("3"),

    /** Impuesto Redimible a las Botellas Plásticas no Retornables. */
    IRBPNR("5");

    private final String codigo;

    CodigoImpuesto(String codigo) {
        this.codigo = codigo;
    }

    /** Código de un dígito que viaja en {@code <codigo>}. */
    public String codigo() {
        return codigo;
    }

    /**
     * Resuelve el impuesto a partir de su código.
     *
     * @throws CatalogoException si el código no pertenece a la Tabla 16
     */
    public static CodigoImpuesto of(String codigo) {
        Objects.requireNonNull(codigo, "codigo");
        for (var c : values()) {
            if (c.codigo.equals(codigo)) {
                return c;
            }
        }
        throw new CatalogoException("Código de impuesto desconocido: " + codigo);
    }
}
