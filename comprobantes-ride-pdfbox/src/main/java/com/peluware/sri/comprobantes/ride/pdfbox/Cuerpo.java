package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Comprobante;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * El cuerpo del RIDE de un comprobante: lo que va entre la cabecera y el pie.
 *
 * <p>La cabecera, el marco, la numeración y el pie son iguales en los seis modelos del Anexo 2 y
 * los pone {@link RidePdfBox}. Lo que hay en medio no se parece: la factura lleva comprador,
 * detalles y totales; la guía de remisión lleva transportista y un bloque por destinatario; el
 * comprobante de retención no lleva totales.
 *
 * <p>Contiene el comprobante y lo que hace falta para escribirlo, de modo que cada subtipo lea de
 * él lo suyo en vez de recibirlo troceado por el constructor. Lo que un comprobante no declara se
 * expresa no sobreescribiendo el hueco correspondiente, y no pasando {@code null}.
 *
 * <p>La maquetación y el cierre se arman <b>una sola vez</b>, la primera vez que se piden:
 * medirlos recorre las filas y calcula anchos, y el reparto en páginas los consulta muchas veces.
 * No es thread-safe, como el resto de la librería: un cuerpo sirve para dibujar un documento.
 *
 * <p>Los subtipos siguen las familias del dominio —venta, nota, retención y guía— y no cómo se
 * dibuja cada uno: de eso se encarga la {@link Maquetacion}, que es una pieza y no un nivel de
 * herencia. La nota de crédito es la razón: significa como una nota de débito y se dibuja como
 * una factura.
 *
 * @param <C> comprobante que dibuja
 */
abstract sealed class Cuerpo<C extends Comprobante> permits CuerpoVenta, CuerpoNota, CuerpoRetencion, CuerpoGuia {

    protected final C comprobante;
    protected final EstiloRide estilo;
    protected final PDFont normal;
    protected final PDFont negrita;

    private @Nullable Maquetacion maquetacion;
    private Cierre.@Nullable Contenido contenido;
    private @Nullable Cierre bloqueCierre;

    protected Cuerpo(C comprobante, PDFont normal, PDFont negrita, EstiloRide estilo) {
        this.comprobante = Objects.requireNonNull(comprobante, "comprobante");
        this.normal = Objects.requireNonNull(normal, "normal");
        this.negrita = Objects.requireNonNull(negrita, "negrita");
        this.estilo = Objects.requireNonNull(estilo, "estilo");
    }

    /**
     * Arma cómo se reparte y se dibuja este cuerpo. Se invoca una sola vez.
     */
    protected abstract Maquetacion armarMaquetacion();

    /**
     * Arma lo que lleva el bloque de cierre. Se invoca una sola vez.
     */
    protected abstract Cierre.Contenido armarContenido();

    /**
     * Alto del bloque que encabeza el cuerpo y solo sale en la primera página, con el aire que lo
     * separa de lo que sigue.
     */
    final float altoEncabezado() {
        return maquetacion().altoPropio() + estilo.hueco();
    }

    /**
     * Alto del bloque que cierra el cuerpo y solo sale en la última página.
     */
    final float altoCierre() {
        return cierre().alto(contenido()) + estilo.hueco();
    }

    /**
     * Dibuja el cierre en la última página.
     */
    final void cierre(Pluma pluma, float arriba) throws IOException {
        cierre().dibujar(pluma, contenido(), arriba);
    }

    /**
     * Cómo se reparte y se dibuja este cuerpo. Se arma la primera vez que se pide y se guarda.
     *
     * <p>Quien compone la página la usa directamente en vez de pasar por este cuerpo: las
     * preguntas de geometría son suyas.
     */
    final Maquetacion maquetacion() {
        if (maquetacion == null) {
            maquetacion = armarMaquetacion();
        }
        return maquetacion;
    }

    private Cierre.Contenido contenido() {
        if (contenido == null) {
            contenido = armarContenido();
        }
        return contenido;
    }

    private Cierre cierre() {
        if (bloqueCierre == null) {
            bloqueCierre = new Cierre(estilo);
        }
        return bloqueCierre;
    }
}
