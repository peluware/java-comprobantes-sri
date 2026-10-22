package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.catalogo.CatalogoException;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;

/**
 * Impuesto sobre el que se retiene, en el elemento {@code <codigo>} de una retención.
 *
 * <p>Ficha Técnica v2.34, Tabla 18. El esquema restringe este campo al patrón {@code [126]}.
 * No es {@link CodigoImpuesto}, que es el del impuesto del documento sustento y su patrón es
 * {@code [235]}.
 */
public enum CodigoImpuestoRetencion {

    RENTA("1"),
    IVA("2"),
    ISD("6");

    private final String codigo;

    CodigoImpuestoRetencion(String codigo) {
        this.codigo = codigo;
    }

    /**
     * Código de un dígito que viaja en el XML.
     */
    public String codigo() {
        return codigo;
    }

    /**
     * Resuelve el impuesto a partir de su código.
     *
     * @throws CatalogoException si el código no corresponde a ninguno
     */
    public static CodigoImpuestoRetencion of(String codigo) {
        for (var valor : values()) {
            if (valor.codigo.equals(codigo)) {
                return valor;
            }
        }
        throw new CatalogoException("Código de impuesto de retención desconocido: " + codigo);
    }
}
