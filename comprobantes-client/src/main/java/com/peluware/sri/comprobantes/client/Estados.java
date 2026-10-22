package com.peluware.sri.comprobantes.client;

import org.jspecify.annotations.Nullable;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalización del texto de los estados que devuelve el SRI.
 *
 * <p>La Ficha Técnica usa vocabularios distintos para el mismo estado: el numeral 7.4 indica
 * {@code PPR}, {@code AUT} y {@code NAT}; los ejemplos XML del numeral 7.2.3 devuelven
 * {@code AUTORIZADO} y {@code RECHAZADO}. Los servicios añaden {@code NO AUTORIZADO} y
 * {@code EN PROCESAMIENTO}.
 */
final class Estados {

    private Estados() {
    }

    /**
     * Pasa el texto a mayúsculas sin tildes, con los espacios internos colapsados a uno.
     *
     * @return cadena vacía si {@code texto} es nulo o solo tiene espacios
     */
    static String normalizar(@Nullable String texto) {
        if (texto == null) {
            return "";
        }
        var descompuesto = Normalizer.normalize(texto.strip(), Normalizer.Form.NFD);
        var sinTildes = new StringBuilder(descompuesto.length());
        descompuesto.codePoints()
            .filter(cp -> Character.getType(cp) != Character.NON_SPACING_MARK)
            .forEach(sinTildes::appendCodePoint);

        return sinTildes.toString().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
