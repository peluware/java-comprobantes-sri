package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * El estilo aplicado a un lienzo.
 *
 * <p>{@link Lienzo} dibuja en crudo: rectángulos, líneas y texto en las coordenadas que se le
 * den. Esta clase decide, para cada una de esas operaciones, lo que manda {@link EstiloRide}: si
 * un bloque lleva recuadro o una regla, de qué color va un rótulo y en qué caja se escribe, qué
 * cuerpo de letra toca y cuánto mide un renglón.
 *
 * <p>Va una por página, porque el lienzo también.
 */
record Pluma(Lienzo lienzo, EstiloRide estilo) {

    /**
     * Siglas que no se bajan a minúscula al escribir un rótulo en forma de frase.
     */
    private static final Set<String> SIGLAS = Set.of("RUC", "IVA", "ICE", "IRBPNR", "RIMPE", "SRI", "SI", "NO");

    Pluma(Lienzo lienzo, EstiloRide estilo) {
        this.lienzo = Objects.requireNonNull(lienzo, "lienzo");
        this.estilo = Objects.requireNonNull(estilo, "estilo");
    }

    /**
     * El lienzo de debajo, para lo que no depende del estilo.
     */
    @Override
    public Lienzo lienzo() {
        return lienzo;
    }

    /**
     * Deja el lienzo con el color y el grosor que pide el estilo. Se llama al abrir cada página.
     */
    void preparar() throws IOException {
        lienzo.colorLinea(estilo.paleta().lineas());
        lienzo.grosor(estilo.grosorLineas());
        lienzo.colorRelleno(estilo.paleta().texto());
    }


    /**
     * Delimita un bloque según el contorno del estilo: su recuadro, una regla debajo o nada.
     */
    void bloque(float x, float y, float ancho, float alto) throws IOException {
        switch (estilo.contorno()) {
            case CAJA -> lienzo.rectangulo(x, y, ancho, alto, estilo.radioBordes());
            case REGLA -> lienzo.linea(x, y, x + ancho, y);
            case NINGUNO -> {
            }
        }
    }

    /**
     * Traza la raya que separa los rótulos de los datos en una tabla.
     */
    void rayaDeCabecera(float x1, float y, float x2) throws IOException {
        if (estilo.divisiones().bajoCabecera()) {
            lienzo.linea(x1, y, x2, y);
        }
    }

    void divisionHorizontal(float x1, float y, float x2) throws IOException {
        if (estilo.divisiones().entreFilas()) {
            lienzo.linea(x1, y, x2, y);
        }
    }

    void divisionVertical(float x, float y1, float y2) throws IOException {
        if (estilo.divisiones().entreColumnas()) {
            lienzo.linea(x, y1, x, y2);
        }
    }

    /**
     * Escribe un rótulo con su color y deja el lienzo en el del texto corriente.
     */
    void rotulo(float tamano, float x, float y, String texto) throws IOException {
        lienzo.colorRelleno(estilo.paleta().rotulo());
        lienzo.texto(fuenteRotulo(), tamano, x, y, comoRotulo(texto));
        lienzo.colorRelleno(estilo.paleta().texto());
    }

    /**
     * Pasa un rótulo a la caja que pide el estilo.
     *
     * <p>Las siglas se dejan intactas: bajarlas a minúscula convertiría «IVA 15%» en «Iva 15%».
     */
    String comoRotulo(String texto) {
        if (estilo.rotulos() == Rotulos.ANEXO_2) {
            return texto;
        }
        var frase = new StringBuilder();
        for (var palabra : texto.split(" ", -1)) {
            if (!frase.isEmpty()) {
                frase.append(' ');
            }
            frase.append(SIGLAS.contains(sinPuntuacion(palabra)) ? palabra : palabra.toLowerCase(Locale.ROOT));
        }
        for (int i = 0; i < frase.length(); i++) {
            if (Character.isLetter(frase.charAt(i))) {
                frase.setCharAt(i, Character.toUpperCase(frase.charAt(i)));
                break;
            }
        }
        return frase.toString();
    }

    static String sinPuntuacion(String palabra) {
        return palabra.replaceAll("[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ]", "");
    }

    /**
     * Fuente de los rótulos: la negrita solo cuando el énfasis va en ellos.
     */
    PDFont fuenteRotulo() {
        return estilo.enfasis() == Enfasis.ROTULO ? lienzo.negrita() : lienzo.normal();
    }

    /**
     * Fuente de los valores: la negrita solo cuando el énfasis va en ellos.
     */
    PDFont fuenteValor() {
        return estilo.enfasis() == Enfasis.VALOR ? lienzo.negrita() : lienzo.normal();
    }

    /**
     * Escribe «etiqueta: valor» en una línea, con el valor justo después de la etiqueta.
     *
     * <p>La posición del valor se mide, no se fija: con la letra escalada una posición fija
     * haría que la etiqueta se comiera el valor.
     */
    void par(float x, float y, float ancho, String etiqueta, String valor) throws IOException {
        rotulo(estilo.tamTexto(), x, y, etiqueta);
        float desde = Medidas.ancho(fuenteRotulo(), estilo.tamTexto(), etiqueta) + estilo.v(5);
        lienzo.textoRecortado(fuenteValor(), estilo.tamTexto(), x + desde, y, ancho - desde, valor);
    }


    /**
     * Escribe los renglones de una celda de arriba hacia abajo, desde el techo de su fila.
     */
    void renglones(PDFont fuente, float x, float techo, List<String> renglones) throws IOException {
        float y = techo;
        for (var renglon : renglones) {
            y -= estilo.espacio(9);
            lienzo.texto(fuente, estilo.tamChico(), x, y, renglon);
        }
    }
}
