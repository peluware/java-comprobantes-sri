package com.peluware.sri.comprobantes.catalogo;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Ambiente de emisión del comprobante.
 *
 * <p>Ficha Técnica v2.34, Tabla 4.
 */
public enum Ambiente {

    PRUEBAS("1"),
    PRODUCCION("2");

    private final String codigo;

    Ambiente(String codigo) {
        this.codigo = codigo;
    }

    /** Dígito que viaja en {@code <ambiente>} y en la clave de acceso. */
    public String codigo() {
        return codigo;
    }

    /**
     * Resuelve el ambiente a partir de su código.
     *
     * @throws CatalogoException si el código no es {@code 1} ni {@code 2}
     */
    public static Ambiente of(String codigo) {
        Objects.requireNonNull(codigo, "codigo");
        for (var a : values()) {
            if (a.codigo.equals(codigo)) {
                return a;
            }
        }
        throw new CatalogoException("Código de ambiente desconocido: " + codigo);
    }

    /**
     * Resuelve el ambiente a partir de la palabra que devuelve el web service de autorización,
     * por ejemplo {@code PRODUCCIÓN}.
     *
     * <p>Ignora mayúsculas, espacios y tildes.
     *
     * @throws CatalogoException si la descripción no corresponde a ningún ambiente
     */
    public static Ambiente ofDescripcion(String descripcion) {
        Objects.requireNonNull(descripcion, "descripcion");
        var descompuesta = Normalizer.normalize(descripcion.strip(), Normalizer.Form.NFD);
        var sinTildes = new StringBuilder(descompuesta.length());
        descompuesta.codePoints()
                .filter(cp -> Character.getType(cp) != Character.NON_SPACING_MARK)
                .forEach(sinTildes::appendCodePoint);

        return switch (sinTildes.toString().toUpperCase(Locale.ROOT)) {
            case "PRUEBAS" -> PRUEBAS;
            case "PRODUCCION" -> PRODUCCION;
            default -> throw new CatalogoException("Descripción de ambiente desconocida: " + descripcion);
        };
    }
}
