package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemision;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompra;
import com.peluware.sri.comprobantes.notacredito.NotaCredito;
import com.peluware.sri.comprobantes.notadebito.NotaDebito;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencion;
import com.peluware.sri.comprobantes.ride.BloqueAutorizacion;
import com.peluware.sri.comprobantes.ride.GeneradorRide;
import com.peluware.sri.comprobantes.ride.RideException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Dibuja el RIDE en PDF siguiendo el Anexo 2 de la Ficha Técnica v2.34.
 *
 * <p>Arma lo que los seis modelos tienen igual —el marco, la cabecera, la numeración y el pie— y
 * le pide el cuerpo al {@link Cuerpo} que corresponda al tipo de comprobante. Reparte las
 * filas de ese cuerpo entre tantas páginas como hagan falta.
 *
 * <p>Usa las fuentes base-14 del PDF, que no se incrustan y cuya codificación WinAnsi cubre el
 * español.
 *
 * <p>Es inmutable y se puede compartir entre hilos.
 */
public final class RidePdfBox implements GeneradorRide {

    private final EstiloRide estilo;
    private final Hoja hoja;
    private final Cabecera cabecera;

    /**
     * Generador con el estilo por omisión: sin logo y sin código de barras.
     */
    public RidePdfBox() {
        this(EstiloRide.porDefecto());
    }

    /**
     * Generador con el estilo indicado.
     */
    public RidePdfBox(EstiloRide estilo) {
        this.estilo = Objects.requireNonNull(estilo, "estilo");
        this.hoja = new Hoja(estilo);
        this.cabecera = new Cabecera(estilo);
    }

    @Override
    public void escribir(Comprobante comprobante, @Nullable OffsetDateTime fechaAutorizacion, OutputStream destino) {
        Objects.requireNonNull(comprobante, "comprobante");
        Objects.requireNonNull(destino, "destino");

        try (var documento = new PDDocument()) {
            var tipografia = estilo.tipografia();
            var logo = estilo.logo();
            var pie = estilo.pie();
            var logoPie = pie == null || pie.logo() == null ? null : pie.logo().crear(documento);
            var recursos = new Recursos(tipografia.normal(documento), tipografia.negrita(documento),
                logo == null ? null : logo.crear(documento), logoPie);

            dibujar(documento, recursos, comprobante, cuerpoDe(comprobante, recursos), fechaAutorizacion);
            documento.save(destino);
        } catch (IOException e) {
            throw new RideException("No se pudo generar el RIDE del comprobante " + comprobante.claveAcceso(), e);
        }
    }

    /**
     * El cuerpo que corresponde a este tipo de comprobante.
     */
    private Cuerpo<?> cuerpoDe(Comprobante comprobante, Recursos recursos) {
        return switch (comprobante) {
            case Factura factura -> new CuerpoFactura(factura, recursos.normal(), recursos.negrita(), estilo);
            case NotaCredito nota -> new CuerpoNotaCredito(nota, recursos.normal(), recursos.negrita(), estilo);
            case NotaDebito nota -> new CuerpoNotaDebito(nota, recursos.normal(), recursos.negrita(), estilo);
            case LiquidacionCompra liquidacion -> new CuerpoLiquidacionCompra(liquidacion, recursos.normal(), recursos.negrita(), estilo);
            case GuiaRemision guia -> new CuerpoGuia(guia, recursos.normal(), recursos.negrita(), estilo);
            case ComprobanteRetencion retencion -> new CuerpoRetencion(retencion, recursos.normal(), recursos.negrita(), estilo);
        };
    }

    /**
     * Reparte el cuerpo en páginas y dibuja cada una.
     *
     * <p>Cada página repite la cabecera, de modo que una hoja suelta siga identificando al emisor
     * y al comprobante. El encabezado del cuerpo solo va en la primera y el cierre solo en la
     * última.
     */
    private void dibujar(PDDocument documento, Recursos recursos, Comprobante comprobante, Cuerpo<?> cuerpo, @Nullable OffsetDateTime fechaAutorizacion) throws IOException {
        float techo = hoja.techo();
        var maquetacion = cuerpo.maquetacion();
        float alturaUtil = techo - hoja.suelo() - maquetacion.altoPorPagina();
        float enLaPrimera = alturaUtil - cabecera.alto(comprobante, recursos.normal(), recursos.negrita()) - estilo.hueco() - cuerpo.altoEncabezado();
        float enLasDemas = alturaUtil - cabecera.altoFranja() - estilo.hueco();
        var paginas = Hoja.repartir(maquetacion.altos(), enLaPrimera, enLasDemas, cuerpo.altoCierre());

        for (int numero = 0; numero < paginas.size(); numero++) {
            var pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            try (var contenido = new PDPageContentStream(documento, pagina)) {
                var pluma = new Pluma(new Lienzo(contenido, recursos.normal(), recursos.negrita()), estilo);
                pluma.preparar();
                hoja.marco(pluma);

                float y;
                if (numero == 0) {
                    y = cabecera.dibujar(pluma, recursos.logo(), comprobante, fechaAutorizacion, techo) - estilo.hueco();
                    y = maquetacion.encabezado(pluma, y) - estilo.hueco();
                } else {
                    y = cabecera.franja(pluma, BloqueAutorizacion.de(comprobante, fechaAutorizacion), techo) - estilo.hueco();
                }

                var corte = paginas.get(numero);
                if (corte[1] > corte[0]) {
                    y = maquetacion.filas(pluma, y, corte[0], corte[1]);
                }
                if (numero == paginas.size() - 1) {
                    cuerpo.cierre(pluma, y - estilo.hueco());
                }

                hoja.pie(pluma, recursos.logoPie());
                if (paginas.size() > 1) {
                    hoja.numero(pluma, numero + 1, paginas.size());
                }
            }
        }
    }

    /**
     * Lo que se crea una vez por documento y se reusa en todas sus páginas: una fuente o una
     * imagen pertenecen al documento, y duplicarlas por página lo engorda sin motivo.
     */
    private record Recursos(PDFont normal, PDFont negrita, @Nullable PDImageXObject logo, @Nullable PDImageXObject logoPie) {
    }
}
