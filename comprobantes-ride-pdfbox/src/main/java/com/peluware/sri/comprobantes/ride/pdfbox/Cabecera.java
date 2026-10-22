package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.ride.BloqueAutorizacion;
import com.peluware.sri.comprobantes.ride.CabeceraEmisor;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;
import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.MARGEN;
import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.MEDIO;

/**
 * El bloque superior del RIDE: el logo, la caja del emisor y la de autorización.
 *
 * <p>Es idéntico en los seis modelos del Anexo 2, y todo lo que necesita está en
 * {@link Comprobante}: ni {@link CabeceraEmisor} ni {@link BloqueAutorizacion} preguntan de qué
 * tipo es el comprobante. Lo único que varía entre uno y otro es el nombre que lo rotula.
 */
final class Cabecera {

    /** Aire entre el logo y la caja del emisor. */
    private static final float LOGO_HUECO = 6;

    /** Alto del bloque de autorización: lo que ocupan sus ocho líneas más los márgenes. */
    private static final float ALTO_AUTORIZACION = 106;

    /** Lo que el código de barras añade a ese alto, sin contar las barras. */
    private static final float ALTO_ROTULOS_BARRAS = 26;

    /** Alto de las barras, en puntos. */
    private static final float ALTO_BARRAS = 30;

    /** Alto de la franja que encabeza las páginas siguientes a la primera. */
    private static final float ALTO_FRANJA = 26;

    private final EstiloRide estilo;

    Cabecera(EstiloRide estilo) {
        this.estilo = Objects.requireNonNull(estilo, "estilo");
    }

    /**
     * Alto de la franja de continuación.
     */
    float altoFranja() {
        return estilo.espacio(ALTO_FRANJA);
    }

    /**
     * Cabecera: el logo sobre la mitad izquierda, la caja del emisor debajo y la de autorización
     * a la derecha.
     *
     * <p>Las dos cajas cierran a la misma altura pero no arrancan a la misma: la del emisor
     * empieza bajo el logo, como en los modelos del Anexo 2. Cada una se dimensiona por lo que
     * lleva dentro, así que un emisor sin sucursal ni agente de retención no deja hueco.
     */
    float dibujar(Pluma pluma, @Nullable PDImageXObject logo, Comprobante comprobante, @Nullable OffsetDateTime fechaAutorizacion, float arriba) throws IOException {
        var datos = medirEmisor(CabeceraEmisor.de(comprobante), pluma.lienzo().normal(), pluma.lienzo().negrita());

        float banda = logo == null ? 0 : estilo.v(estilo.altoLogo() + LOGO_HUECO);
        float altoEmisor = altoBloqueEmisor(datos);
        float alto = Math.max(altoBloqueAutorizacion(), banda + altoEmisor);
        float abajo = arriba - alto;

        if (logo != null) {
            dibujarLogo(pluma, logo, MARGEN, arriba, MEDIO);
        }
        if (estilo.contorno() == Contorno.REGLA) {
            pluma.lienzo().linea(MARGEN, abajo, MARGEN + ANCHO_UTIL, abajo);
        } else {
            pluma.bloque(MARGEN, abajo, MEDIO, alto - banda);
            pluma.bloque(MARGEN + MEDIO + 12, abajo, MEDIO, alto);
        }

        emisor(pluma, datos, MARGEN + 6, arriba - banda - estilo.espacio(12));
        autorizacion(pluma, BloqueAutorizacion.de(comprobante, fechaAutorizacion), MARGEN + MEDIO + 18, arriba - estilo.espacio(12));
        return abajo;
    }


    /**
     * Alto de un renglón del bloque del emisor.
     */
    private static final float ALTO_RENGLON = 10;

    /**
     * Renglones que admite la razón social o el nombre comercial.
     */
    private static final int RENGLONES_NOMBRE = 3;

    /**
     * El bloque del emisor con su texto ya partido en renglones.
     */
    record Emisor(List<String> razonSocial, List<String> nombreComercial, Pares etiquetas) {

        int renglones() {
            return razonSocial.size() + nombreComercial.size() + etiquetas.renglones();
        }
    }

    /**
     * Mide el bloque del emisor contra el ancho de que dispone.
     */
    Emisor medirEmisor(CabeceraEmisor emisor, PDFont normal, PDFont negrita) {
        float ancho = MEDIO - 12;
        var razonSocial = Medidas.partir(negrita, estilo.tamTexto() + 1, emisor.razonSocial(), ancho, RENGLONES_NOMBRE);
        var nombreComercial = emisor.nombreComercial() == null
            ? List.<String>of()
            : Medidas.partir(normal, estilo.tamTexto(), emisor.nombreComercial(), ancho, RENGLONES_NOMBRE);

        var etiquetas = Pares.constructor(normal, negrita, estilo, estilo.tamChico(), ALTO_RENGLON)
            .fila(MARGEN + 6, ancho, "Dirección Matriz:", emisor.direccionMatriz())
            .fila(MARGEN + 6, ancho, "Dirección Sucursal:", emisor.direccionSucursal())
            .fila(MARGEN + 6, ancho, "Contribuyente Especial Nro:", emisor.contribuyenteEspecial())
            .fila(MARGEN + 6, ancho, "OBLIGADO A LLEVAR CONTABILIDAD:", emisor.obligadoContabilidad() ? "SI" : "NO")
            .fila(MARGEN + 6, ancho, "Agente de Retención Resolución Nro:", emisor.agenteRetencion())
            .fila(MARGEN + 6, ancho, "CONTRIBUYENTE RÉGIMEN RIMPE", emisor.contribuyenteRimpe() ? "" : null)
            .build();

        return new Emisor(razonSocial, nombreComercial, etiquetas);
    }

    float altoBloqueEmisor(Emisor bloque) {
        return estilo.espacio(18) + estilo.espacio(ALTO_RENGLON) * bloque.renglones();
    }

    void emisor(Pluma pluma, Emisor bloque, float x, float arriba) throws IOException {
        float y = arriba - estilo.espacio(ALTO_RENGLON);
        for (var renglon : bloque.razonSocial()) {
            pluma.lienzo().texto(pluma.lienzo().negrita(), estilo.tamTexto() + 1, x, y, renglon);
            y -= estilo.espacio(ALTO_RENGLON);
        }
        for (var renglon : bloque.nombreComercial()) {
            pluma.lienzo().texto(pluma.fuenteValor(), estilo.tamTexto(), x, y, renglon);
            y -= estilo.espacio(ALTO_RENGLON);
        }
        bloque.etiquetas().dibujar(pluma, y + estilo.espacio(9));
    }

    /**
     * Dibuja el logo centrado en la banda que hay sobre la caja del emisor.
     *
     * <p>No lo deforma: toma la menor de las dos escalas, de modo que el lado que se queda corto
     * manda y el otro deja aire a ambos costados.
     */
    void dibujarLogo(Pluma pluma, PDImageXObject imagen, float x, float arriba, float ancho) throws IOException {
        float alto = estilo.v(estilo.altoLogo());
        float escala = Math.min(ancho / imagen.getWidth(), alto / imagen.getHeight());
        float dibujoAncho = imagen.getWidth() * escala;
        float dibujoAlto = imagen.getHeight() * escala;
        float sobra = ancho - dibujoAncho;
        float desde = switch (estilo.alineacionLogo()) {
            case IZQUIERDA -> 0;
            case CENTRO -> sobra / 2;
            case DERECHA -> sobra;
        };
        pluma.lienzo().imagen(imagen, x + desde, arriba - alto + (alto - dibujoAlto) / 2, dibujoAncho, dibujoAlto);
    }

    void autorizacion(Pluma pluma, BloqueAutorizacion bloque, float x, float arriba) throws IOException {
        float y = arriba;
        pluma.lienzo().texto(pluma.lienzo().negrita(), estilo.tamTexto(), x, y, "R.U.C.: " + bloque.ruc());
        y -= estilo.espacio(15);
        pluma.lienzo().colorRelleno(estilo.paleta().titulo());
        pluma.lienzo().texto(pluma.lienzo().negrita(), estilo.tamTitulo(), x, y, Formatos.titulo(bloque.tipoComprobante()));
        pluma.lienzo().colorRelleno(estilo.paleta().texto());
        y -= estilo.espacio(12);
        pluma.lienzo().texto(pluma.lienzo().negrita(), estilo.tamTexto(), x, y, "No. " + bloque.numeroDocumento());
        y -= estilo.espacio(12);
        pluma.rotulo(estilo.tamChico(), x, y, "NÚMERO DE AUTORIZACIÓN");
        y -= estilo.espacio(9);
        pluma.lienzo().texto(pluma.fuenteValor(), estilo.tamChico(), x, y, bloque.claveAcceso());
        y -= estilo.espacio(12);
        pluma.rotulo(estilo.tamChico(), x, y, "FECHA Y HORA DE AUTORIZACIÓN");
        y -= estilo.espacio(9);
        var fecha = bloque.fechaAutorizacion();
        pluma.lienzo().texto(pluma.fuenteValor(), estilo.tamChico(), x, y, fecha == null ? "—" : Formatos.FECHA_HORA.format(fecha));
        float columna = Math.max(Medidas.ancho(pluma.fuenteRotulo(), estilo.tamChico(), "AMBIENTE:"), Medidas.ancho(pluma.fuenteRotulo(), estilo.tamChico(), "EMISIÓN:")) + estilo.v(8);
        y -= estilo.espacio(11);
        pluma.rotulo(estilo.tamChico(), x, y, "AMBIENTE:");
        pluma.lienzo().texto(pluma.fuenteValor(), estilo.tamChico(), x + columna, y, bloque.ambiente() == Ambiente.PRODUCCION ? "PRODUCCIÓN" : "PRUEBAS");
        y -= estilo.espacio(9);
        pluma.rotulo(estilo.tamChico(), x, y, "EMISIÓN:");
        pluma.lienzo().texto(pluma.fuenteValor(), estilo.tamChico(), x + columna, y, bloque.tipoEmision().name());

        if (estilo.codigoBarras()) {
            codigoBarras(pluma, bloque.claveAcceso(), x, y - estilo.espacio(14), MEDIO - 24);
        }
    }

    /**
     * Código de barras de la clave de acceso, con la clave repetida debajo en texto.
     *
     * <p>Numeral 9.20: es opcional y lo que lleva es la clave de acceso. Las barras ocupan
     * el ancho disponible, para que el módulo salga lo más ancho posible y un lector lo lea.
     */
    void codigoBarras(Pluma pluma, String claveAcceso, float x, float arriba, float ancho) throws IOException {
        pluma.rotulo(estilo.tamChico(), x, arriba, "CLAVE DE ACCESO");

        var modulos = Code128.modulos(claveAcceso);
        float modulo = ancho / modulos.length;
        float alto = estilo.v(ALTO_BARRAS);
        float y = arriba - estilo.espacio(4) - alto;
        pluma.lienzo().barras(modulos, x, y, modulo, alto);

        float sangria = Code128.ZONA_MUDA * modulo;
        pluma.lienzo().textoRecortado(pluma.fuenteValor(), estilo.tamChico(), x + sangria, y - estilo.espacio(8), ancho - 2 * sangria, claveAcceso);
    }

    /**
     * Alto que ocupa la cabecera del comprobante, que se repite en todas las páginas.
     */
    float alto(Comprobante comprobante, PDFont normal, PDFont negrita) {
        float banda = estilo.logo() == null ? 0 : estilo.v(estilo.altoLogo() + LOGO_HUECO);
        return Math.max(altoBloqueAutorizacion(), banda + altoBloqueEmisor(medirEmisor(CabeceraEmisor.de(comprobante), normal, negrita)));
    }

    /**
     * Franja que encabeza las páginas siguientes a la primera.
     *
     * <p>Ni la Ficha Técnica ni la Resolución NAC-DGERCGC18-00000233 dicen nada del RIDE de
     * varias hojas: los requisitos se exigen del comprobante, no de cada hoja. Lleva lo justo
     * para que una hoja suelta siga diciendo de qué comprobante es.
     */
    float franja(Pluma pluma, BloqueAutorizacion bloque, float arriba) throws IOException {
        float alto = estilo.espacio(ALTO_FRANJA);
        float abajo = arriba - alto;
        pluma.bloque(MARGEN, abajo, ANCHO_UTIL, alto);

        float y = arriba - estilo.espacio(10);
        pluma.lienzo().texto(pluma.lienzo().negrita(), estilo.tamTexto(), MARGEN + 6, y, Formatos.titulo(bloque.tipoComprobante()) + " No. " + bloque.numeroDocumento());
        pluma.lienzo().textoDerecha(pluma.lienzo().negrita(), estilo.tamTexto(), MARGEN + ANCHO_UTIL - 6, y, "R.U.C.: " + bloque.ruc());

        pluma.par(MARGEN + 6, y - estilo.espacio(10), ANCHO_UTIL - 12, "Clave de Acceso:", bloque.claveAcceso());
        return abajo;
    }

    /**
     * Alto del bloque de autorización, con el código de barras cuando lo lleva.
     */
    float altoBloqueAutorizacion() {
        float alto = estilo.espacio(ALTO_AUTORIZACION);
        if (estilo.codigoBarras()) {
            alto += estilo.espacio(ALTO_ROTULOS_BARRAS) + estilo.v(ALTO_BARRAS);
        }
        return alto;
    }
}
