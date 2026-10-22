package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;

/**
 * Operaciones de dibujo sobre una página, en puntos y con el origen abajo a la izquierda.
 *
 * <p>Recorta el texto que no cabe en el ancho indicado y escribe los importes alineados a la
 * derecha, que es como los lleva el Anexo 2.
 */
final class Lienzo {

    private final PDPageContentStream contenido;
    private final PDFont normal;
    private final PDFont negrita;

    Lienzo(PDPageContentStream contenido, PDFont normal, PDFont negrita) {
        this.contenido = contenido;
        this.normal = normal;
        this.negrita = negrita;
    }

    /**
     * Fuente del texto corriente, ya cargada en el documento.
     */
    PDFont normal() {
        return normal;
    }

    /**
     * Fuente de los rótulos, ya cargada en el documento.
     */
    PDFont negrita() {
        return negrita;
    }

    /**
     * Escribe {@code texto} con su esquina inferior izquierda en {@code (x, y)}.
     */
    void texto(PDFont fuente, float tamano, float x, float y, String texto) throws IOException {
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(Medidas.limpiar(texto));
        contenido.endText();
    }

    /**
     * Escribe {@code texto} terminando en {@code x}, para alinear importes a la derecha.
     */
    void textoDerecha(PDFont fuente, float tamano, float x, float y, String texto) throws IOException {
        var limpio = Medidas.limpiar(texto);
        texto(fuente, tamano, x - Medidas.ancho(fuente, tamano, limpio), y, limpio);
    }

    /**
     * Escribe {@code texto} recortándolo para que no exceda {@code ancho}.
     */
    void textoRecortado(PDFont fuente, float tamano, float x, float y, float ancho, String texto) throws IOException {
        texto(fuente, tamano, x, y, Medidas.recortar(fuente, tamano, texto, ancho));
    }

    /**
     * Dibuja una imagen en el rectángulo indicado. El rectángulo manda: la imagen se estira
     * para llenarlo, así que quien llama debe haber calculado la proporción.
     */
    void imagen(PDImageXObject imagen, float x, float y, float ancho, float alto) throws IOException {
        contenido.drawImage(imagen, x, y, ancho, alto);
    }

    /**
     * Pinta las barras de un código de barras: una franja por cada tramo contiguo de módulos
     * encendidos, todas del alto indicado y con el color de relleno vigente.
     */
    void barras(boolean[] modulos, float x, float y, float modulo, float alto) throws IOException {
        int i = 0;
        while (i < modulos.length) {
            if (!modulos[i]) {
                i++;
                continue;
            }
            int fin = i;
            while (fin < modulos.length && modulos[fin]) {
                fin++;
            }
            contenido.addRect(x + i * modulo, y, (fin - i) * modulo, alto);
            i = fin;
        }
        contenido.fill();
    }

    void rectangulo(float x, float y, float ancho, float alto) throws IOException {
        contenido.addRect(x, y, ancho, alto);
        contenido.stroke();
    }

    /**
     * Rectángulo con las esquinas redondeadas.
     *
     * <p>PDF no tiene primitiva para esto: cada esquina es una curva de Bézier cúbica que
     * aproxima un cuarto de círculo. La constante {@code 0.5523} es la que hace que la
     * aproximación sea visualmente indistinguible de un arco.
     *
     * <p>Un radio de cero, o mayor que la mitad del lado más corto, cae al rectángulo recto.
     */
    void rectangulo(float x, float y, float ancho, float alto, float radio) throws IOException {
        if (!trazarRedondeado(x, y, ancho, alto, radio)) {
            rectangulo(x, y, ancho, alto);
            return;
        }
        contenido.closeAndStroke();
    }

    /**
     * Recorta al interior de un rectángulo redondeado todo lo que se dibuje hasta
     * {@link #finRecorte()}.
     *
     * <p>Las filas de cabecera y los separadores de una tabla son rectas que llegan al borde;
     * sin recortarlos asoman por fuera de las esquinas.
     */
    void recortarA(float x, float y, float ancho, float alto, float radio) throws IOException {
        contenido.saveGraphicsState();
        if (trazarRedondeado(x, y, ancho, alto, radio)) {
            contenido.closePath();
        } else {
            contenido.addRect(x, y, ancho, alto);
        }
        contenido.clip();
    }

    /**
     * Levanta el recorte y devuelve los colores que había antes de {@link #recortarA}.
     */
    void finRecorte() throws IOException {
        contenido.restoreGraphicsState();
    }


    /**
     * Traza el contorno redondeado sin pintarlo.
     *
     * @return {@code false} si el radio no da para redondear nada
     */
    private boolean trazarRedondeado(float x, float y, float ancho, float alto, float radio) throws IOException {
        float r = Math.min(radio, Math.min(ancho, alto) / 2);
        if (r <= 0) {
            return false;
        }
        float k = r * 0.5523f;
        float derecha = x + ancho;
        float arriba = y + alto;

        contenido.moveTo(x + r, y);
        contenido.lineTo(derecha - r, y);
        contenido.curveTo(derecha - r + k, y, derecha, y + r - k, derecha, y + r);
        contenido.lineTo(derecha, arriba - r);
        contenido.curveTo(derecha, arriba - r + k, derecha - r + k, arriba, derecha - r, arriba);
        contenido.lineTo(x + r, arriba);
        contenido.curveTo(x + r - k, arriba, x, arriba - r + k, x, arriba - r);
        contenido.lineTo(x, y + r);
        contenido.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        return true;
    }

    /**
     * Rellena un rectángulo con el color de relleno vigente y restaura el del texto.
     */
    void relleno(float x, float y, float ancho, float alto, int color, int colorTexto) throws IOException {
        colorRelleno(color);
        contenido.addRect(x, y, ancho, alto);
        contenido.fill();
        colorRelleno(colorTexto);
    }

    void linea(float x1, float y1, float x2, float y2) throws IOException {
        contenido.moveTo(x1, y1);
        contenido.lineTo(x2, y2);
        contenido.stroke();
    }

    /**
     * Color de las líneas y los recuadros, en RGB de 24 bits.
     */
    void colorLinea(int color) throws IOException {
        contenido.setStrokingColor(rojo(color), verde(color), azul(color));
    }

    /**
     * Grosor de las líneas y los recuadros, en puntos.
     */
    void grosor(float puntos) throws IOException {
        contenido.setLineWidth(puntos);
    }

    /**
     * Color del texto y de los rellenos, en RGB de 24 bits.
     */
    void colorRelleno(int color) throws IOException {
        contenido.setNonStrokingColor(rojo(color), verde(color), azul(color));
    }

    private static float rojo(int color) {
        return ((color >> 16) & 0xFF) / 255f;
    }

    private static float verde(int color) {
        return ((color >> 8) & 0xFF) / 255f;
    }

    private static float azul(int color) {
        return (color & 0xFF) / 255f;
    }

}
