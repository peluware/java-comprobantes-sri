package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Medida y partido de texto, sin página de por medio.
 *
 * <p>Está aparte de {@link Lienzo} porque el reparto en páginas se calcula antes de abrir
 * ninguna: hay que saber cuánto ocupa cada fila para decidir dónde se corta.
 */
final class Medidas {

    private Medidas() {
    }

    /**
     * Ancho de {@code texto} en puntos.
     *
     * <p>Un carácter que la fuente no sepa medir deja de contarse en vez de romper el dibujo.
     */
    static float ancho(PDFont fuente, float tamano, String texto) {
        try {
            return fuente.getStringWidth(limpiar(texto)) / 1000 * tamano;
        } catch (IOException | IllegalArgumentException e) {
            return texto.length() * tamano * 0.5f;
        }
    }

    /**
     * Recorta {@code texto} para que no exceda {@code ancho}, terminándolo en puntos suspensivos.
     */
    static String recortar(PDFont fuente, float tamano, String texto, float ancho) {
        var limpio = limpiar(texto);
        if (ancho(fuente, tamano, limpio) <= ancho) {
            return limpio;
        }
        var corto = limpio.endsWith("…") ? limpio.substring(0, limpio.length() - 1) : limpio;
        while (!corto.isEmpty() && ancho(fuente, tamano, corto + "…") > ancho) {
            corto = corto.substring(0, corto.length() - 1);
        }
        return corto + "…";
    }

    /**
     * Parte {@code texto} en los renglones que quepan en {@code ancho}, cortando entre palabras.
     *
     * <p>Una palabra más ancha que la caja se recorta en vez de partirse: un código o un número
     * partido por la mitad se lee mal. Lo que pase de {@code maximo} renglones se recorta también,
     * con puntos suspensivos al final del último.
     *
     * <p>Un salto de línea en {@code texto} es un corte duro: lo que sigue empieza en un renglón
     * nuevo aunque quepa en el anterior. Es lo que permite que un campo se liste por renglones en
     * vez de seguido.
     *
     * @return al menos un renglón; uno vacío si el texto lo estaba
     */
    static List<String> partir(PDFont fuente, float tamano, String texto, float ancho, int maximo) {
        var renglones = new ArrayList<String>();
        for (var parrafo : texto.split("\n", -1)) {
            envolver(fuente, tamano, parrafo, ancho, renglones);
        }
        if (renglones.isEmpty()) {
            return List.of("");
        }
        if (renglones.size() > maximo) {
            var ultimo = recortar(fuente, tamano, renglones.get(maximo - 1) + "…", ancho);
            renglones = new ArrayList<>(renglones.subList(0, maximo));
            renglones.set(maximo - 1, ultimo);
        }
        return List.copyOf(renglones);
    }

    /**
     * Envuelve un párrafo por ancho y añade sus renglones a {@code renglones}.
     */
    private static void envolver(PDFont fuente, float tamano, String parrafo, float ancho, List<String> renglones) {
        var actual = new StringBuilder();
        for (var palabra : limpiar(parrafo).trim().split(" +")) {
            if (palabra.isEmpty()) {
                continue;
            }
            var probar = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (ancho(fuente, tamano, probar) <= ancho) {
                actual.setLength(0);
                actual.append(probar);
                continue;
            }
            if (!actual.isEmpty()) {
                renglones.add(actual.toString());
                actual.setLength(0);
            }
            actual.append(recortar(fuente, tamano, palabra, ancho));
        }
        if (!actual.isEmpty()) {
            renglones.add(actual.toString());
        }
    }

    /**
     * Sustituye lo que las fuentes base-14 no pueden escribir.
     *
     * <p>Su codificación es WinAnsi, que cubre el español completo pero no, por ejemplo, un
     * salto de línea dentro de un campo.
     */
    static String limpiar(String texto) {
        return texto.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    }
}
