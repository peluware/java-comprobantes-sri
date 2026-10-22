package com.peluware.sri.comprobantes.catalogo;

import java.util.Objects;

/**
 * Tipo de comprobante, correspondiente al campo {@code <codDoc>}.
 *
 * <p>Ficha Técnica v2.34, Tabla 3. El código {@code 02} no está asignado.
 */
public enum TipoComprobante {

    FACTURA("01"),
    LIQUIDACION_COMPRA("03"),
    NOTA_CREDITO("04"),
    NOTA_DEBITO("05"),
    GUIA_REMISION("06"),
    COMPROBANTE_RETENCION("07");

    private final String codigo;

    TipoComprobante(String codigo) {
        this.codigo = codigo;
    }

    /** Código de dos dígitos que viaja en {@code <codDoc>} y en la clave de acceso. */
    public String codigo() {
        return codigo;
    }

    /**
     * Resuelve el tipo de comprobante a partir de su código.
     *
     * @throws CatalogoException si el código no pertenece a la Tabla 3
     */
    public static TipoComprobante of(String codigo) {
        Objects.requireNonNull(codigo, "codigo");
        for (var t : values()) {
            if (t.codigo.equals(codigo)) {
                return t;
            }
        }
        throw new CatalogoException("Código de tipo de comprobante desconocido: " + codigo);
    }
}
