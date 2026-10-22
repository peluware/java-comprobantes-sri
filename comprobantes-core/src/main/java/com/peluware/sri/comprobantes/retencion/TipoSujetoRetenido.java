package com.peluware.sri.comprobantes.retencion;

import java.util.Objects;
import com.peluware.sri.comprobantes.catalogo.CatalogoException;
/**
 * Naturaleza del sujeto retenido, tag {@code <tipoSujetoRetenido>}.
 *
 * <p>El esquema lo restringe al patrón {@code 01|02} y lo declara opcional.
 */
public enum TipoSujetoRetenido {

    PERSONA_NATURAL("01"),
    SOCIEDAD("02");

    private final String codigo;

    TipoSujetoRetenido(String codigo) {
        this.codigo = codigo;
    }

    /**
     * Código de dos dígitos que viaja en el XML.
     */
    /**
     * El valor con ese código.
     *
     * @throws CatalogoException si el código no es de este catálogo
     */
    public static TipoSujetoRetenido of(String codigo) {
        Objects.requireNonNull(codigo, "codigo");
        for (var valor : values()) {
            if (valor.codigo.equals(codigo)) {
                return valor;
            }
        }
        throw new CatalogoException("Código de tipo de sujeto retenido desconocido: " + codigo);
    }

    public String codigo() {
        return codigo;
    }
}
