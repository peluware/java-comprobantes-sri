package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.DetalleAdicional;

import java.util.List;

/**
 * Cómo se escriben en su columna los datos adicionales de una línea.
 *
 * <p>El Anexo 2 no le da columna propia a este dato, así que el formato es decisión de quien
 * emite. El esquema admite hasta {@value DetalleAdicional#MAX_POR_DETALLE} por línea, de modo que
 * la lista nunca es larga.
 */
public enum FormatoAdicionales {

    /**
     * Seguidos y separados por barras: {@code Lote: A-77 | Vence: 2027-03}.
     *
     * <p>Ocupa menos alto, pero el corte por ancho puede caer a mitad de un dato.
     */
    EN_LINEA(" | ", ""),

    /**
     * Uno por renglón, sin marca.
     */
    POR_RENGLON("\n", ""),

    /**
     * Uno por renglón, con una viñeta delante.
     */
    VINETAS("\n", "• ");

    private final String separador;
    private final String marca;

    FormatoAdicionales(String separador, String marca) {
        this.separador = separador;
        this.marca = marca;
    }

    /**
     * Escribe los adicionales de una línea.
     *
     * <p>Un {@code \n} en el resultado es un corte duro para
     * {@link Medidas#partir(org.apache.pdfbox.pdmodel.font.PDFont, float, String, float, int)}:
     * lo que sigue empieza en un renglón nuevo.
     */
    String escribir(List<DetalleAdicional> adicionales) {
        var texto = new StringBuilder();
        for (var adicional : adicionales) {
            if (!texto.isEmpty()) {
                texto.append(separador);
            }
            texto.append(marca).append(adicional.nombre()).append(": ").append(adicional.valor());
        }
        return texto.toString();
    }
}
