package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * La hoja A4 y lo que la rodea: sus márgenes, el filete que la bordea, el pie, el número de
 * página y el reparto de las filas de un comprobante entre varias.
 *
 * <p>Nada de esto depende del tipo de comprobante: las seis formas del Anexo 2 se imprimen en la
 * misma hoja.
 */
final class Hoja {

    /** Margen de la hoja, en puntos. */
    static final float MARGEN = 40;

    /** Ancho que queda entre márgenes. */
    static final float ANCHO_UTIL = PDRectangle.A4.getWidth() - 2 * MARGEN;

    /** Ancho de cada una de las dos columnas de la cabecera, con un hueco de 12 entre ellas. */
    static final float MEDIO = ANCHO_UTIL / 2 - 6;

    /** Distancia del filete exterior al borde de la hoja, en puntos. */
    private static final float MARCO_EXTERIOR = 24;

    /** Distancia del filete interior, cuando el marco es doble. */
    private static final float MARCO_INTERIOR = 29;

    /** Ancho de la caja del logo del pie, antes de escalar. */
    private static final float PIE_LOGO_ANCHO = 70;

    /** Alto del pie de página, antes de escalar. */
    private static final float PIE_ALTO = 16;

    private final EstiloRide estilo;

    Hoja(EstiloRide estilo) {
        this.estilo = Objects.requireNonNull(estilo, "estilo");
    }

    /**
     * Altura a la que empieza el contenido.
     */
    float techo() {
        return PDRectangle.A4.getHeight() - MARGEN;
    }

    void marco(Pluma pluma) throws IOException {
        if (estilo.marcoPagina() == MarcoPagina.NINGUNO) {
            return;
        }
        float ancho = PDRectangle.A4.getWidth();
        float alto = PDRectangle.A4.getHeight();

        if (estilo.marcoPagina() == MarcoPagina.DOBLE) {
            pluma.lienzo().grosor(estilo.grosorLineas() * 2);
            pluma.lienzo().rectangulo(MARCO_EXTERIOR, MARCO_EXTERIOR, ancho - 2 * MARCO_EXTERIOR, alto - 2 * MARCO_EXTERIOR, estilo.radioBordes());
            pluma.lienzo().grosor(estilo.grosorLineas());
            pluma.lienzo().rectangulo(MARCO_INTERIOR, MARCO_INTERIOR, ancho - 2 * MARCO_INTERIOR, alto - 2 * MARCO_INTERIOR, estilo.radioBordes());
        } else {
            pluma.lienzo().rectangulo(MARCO_EXTERIOR, MARCO_EXTERIOR, ancho - 2 * MARCO_EXTERIOR, alto - 2 * MARCO_EXTERIOR, estilo.radioBordes());
        }
    }

    static List<int[]> repartir(List<Float> altos, float enLaPrimera, float enLasDemas, float cierre) {
        var paginas = new ArrayList<int[]>();
        int desde = 0;
        while (desde < altos.size()) {
            float disponible = paginas.isEmpty() ? enLaPrimera : enLasDemas;
            int hasta = desde;
            float usado = 0;
            while (hasta < altos.size() && usado + altos.get(hasta) <= disponible) {
                usado += altos.get(hasta);
                hasta++;
            }
            paginas.add(new int[] {desde, Math.max(hasta, desde + 1)});
            desde = paginas.getLast()[1];
        }
        if (paginas.isEmpty()) {
            paginas.add(new int[] {0, 0});
        }

        var ultima = paginas.getLast();
        float usadoEnLaUltima = 0;
        for (int i = ultima[0]; i < ultima[1]; i++) {
            usadoEnLaUltima += altos.get(i);
        }
        float disponible = paginas.size() == 1 ? enLaPrimera : enLasDemas;
        if (usadoEnLaUltima + cierre <= disponible) {
            return List.copyOf(paginas);
        }

        int corte = ultima[1];
        float enLaNueva = cierre;
        while (corte > ultima[0] && usadoEnLaUltima + cierre > disponible && enLaNueva + altos.get(corte - 1) <= enLasDemas) {
            corte--;
            usadoEnLaUltima -= altos.get(corte);
            enLaNueva += altos.get(corte);
        }
        paginas.set(paginas.size() - 1, new int[] {ultima[0], corte});
        paginas.add(new int[] {corte, ultima[1]});
        return List.copyOf(paginas);
    }

    /**
     * Altura por debajo de la cual no se dibuja nada: la reservan el pie de página y el número de
     * hoja.
     */
    float suelo() {
        float suelo = MARGEN + estilo.espacio(12);
        return estilo.pie() == null ? suelo : suelo + estilo.v(PIE_ALTO) + 6;
    }

    /**
     * Número de hoja, centrado sobre el pie. Solo se escribe cuando el comprobante pasa de una.
     */
    void numero(Pluma pluma, int numero, int total) throws IOException {
        var texto = numero + " de " + total;
        float x = MARGEN + (ANCHO_UTIL - Medidas.ancho(pluma.lienzo().normal(), estilo.tamChico(), texto)) / 2;
        float y = estilo.pie() == null ? MARGEN : MARGEN + estilo.v(PIE_ALTO) + 6;
        pluma.rotulo(estilo.tamChico(), x, y, texto);
    }

    void pie(Pluma pluma, @Nullable PDImageXObject imagen) throws IOException {
        var pie = estilo.pie();
        if (pie == null) {
            return;
        }
        float y = MARGEN;

        float anchoLogo = 0;
        float altoLogo = 0;
        if (imagen != null) {
            float escala = Math.min(estilo.v(PIE_LOGO_ANCHO) / imagen.getWidth(), estilo.v(PIE_ALTO) / imagen.getHeight());
            anchoLogo = imagen.getWidth() * escala;
            altoLogo = imagen.getHeight() * escala;
        }

        float anchoTexto = pie.texto().isEmpty() ? 0 : Medidas.ancho(pluma.lienzo().normal(), estilo.tamChico(), pie.texto());
        float separacion = imagen != null && !pie.texto().isEmpty() ? estilo.v(6) : 0;
        float total = anchoLogo + separacion + anchoTexto;

        float x = switch (pie.alineacion()) {
            case IZQUIERDA -> MARGEN;
            case CENTRO -> MARGEN + (ANCHO_UTIL - total) / 2;
            case DERECHA -> MARGEN + ANCHO_UTIL - total;
        };

        if (imagen != null) {
            pluma.lienzo().imagen(imagen, x, y, anchoLogo, altoLogo);
            x += anchoLogo + separacion;
        }
        if (!pie.texto().isEmpty()) {
            pluma.lienzo().textoRecortado(pluma.lienzo().normal(), estilo.tamChico(), x, y + estilo.espacio(3), MARGEN + ANCHO_UTIL - x, pie.texto());
        }
    }
}
