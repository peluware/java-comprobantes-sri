package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteReader;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.LecturaXmlException;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.retencion.DocumentoSustento.PagoLocalExterior;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.PeriodoFiscal;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.w3c.dom.Element;

import java.math.BigDecimal;

import static com.peluware.sri.comprobantes.interno.Bloques.SI;
import static com.peluware.sri.comprobantes.interno.xml.Dom.childrenIn;
import static com.peluware.sri.comprobantes.interno.xml.Dom.text;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee un {@link ComprobanteRetencion} de su XML, firmado o sin firmar.
 *
 * <p>Es el espejo de {@link ComprobanteRetencionWriter}. Toma los importes tal como vienen. La
 * firma, si la lleva, se ignora.
 */
public final class ComprobanteRetencionReader extends ComprobanteReader<ComprobanteRetencion, ComprobanteRetencion.Builder> {

    /**
     * Dígitos de {@code numDocSustento}, que el esquema escribe corridos y sin separadores:
     * establecimiento, punto de emisión y secuencial.
     */
    private static final int NUMERO_LONGITUD = 15;

    /**
     * Única instancia.
     */
    public static final ComprobanteRetencionReader INSTANCIA = new ComprobanteRetencionReader();

    private ComprobanteRetencionReader() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.COMPROBANTE_RETENCION;
    }


    /**
     * Arma el comprobante de retención a partir del elemento raíz ya leído.
     */
    @Override
    protected ComprobanteRetencion.Builder builder() {
        return ComprobanteRetencion.builder();
    }

    @Override
    protected ComprobanteRetencion propio(ComprobanteRetencion.Builder builder, Element raiz, Element info) {
        builder
            .fechaEmision(Lecturas.fecha(textoExigido(info, "fechaEmision"), "fechaEmision"))
            .sujetoRetenido(sujetoRetenido(info))
            .periodoFiscal(new PeriodoFiscal(textoExigido(info, "periodoFiscal")));

        for (var documento : childrenIn(raiz, "docsSustento", "docSustento")) {
            builder.documentoSustento(documentoSustento(documento));
        }
        return builder.build();
    }

    private static SujetoRetenido sujetoRetenido(Element info) {
        var tipo = text(info, "tipoSujetoRetenido");
        return new SujetoRetenido(
            TipoIdentificacion.of(textoExigido(info, "tipoIdentificacionSujetoRetenido")),
            textoExigido(info, "identificacionSujetoRetenido"),
            textoExigido(info, "razonSocialSujetoRetenido"),
            tipo == null ? null : TipoSujetoRetenido.of(tipo),
            SI.equals(text(info, "parteRel"))
        );
    }

    private static DocumentoSustento documentoSustento(Element documento) {
        var numero = textoExigido(documento, "numDocSustento");
        if (numero.length() != NUMERO_LONGITUD) {
            throw new LecturaXmlException("El campo numDocSustento tiene " + numero.length() + " dígitos y el esquema exige " + NUMERO_LONGITUD + ": " + numero);
        }

        var registro = text(documento, "fechaRegistroContable");
        var builder = DocumentoSustento.builder()
            .codigoSustento(textoExigido(documento, "codSustento"))
            .documento(
                TipoComprobante.of(textoExigido(documento, "codDocSustento")),
                Serie.of(Integer.parseInt(numero.substring(0, 3)), Integer.parseInt(numero.substring(3, 6))),
                Secuencial.of(Long.parseLong(numero.substring(6))),
                Lecturas.fecha(textoExigido(documento, "fechaEmisionDocSustento"), "fechaEmisionDocSustento")
            )
            .fechaRegistroContable(registro == null ? null : Lecturas.fecha(registro, "fechaRegistroContable"))
            .numeroAutorizacion(text(documento, "numAutDocSustento"))
            .pagoLocalExterior(PagoLocalExterior.of(textoExigido(documento, "pagoLocExt")))
            .exterior(text(documento, "tipoRegi"), text(documento, "paisEfecPago"))
            .aplicaConvenioDobleTributacion(SI.equals(text(documento, "aplicConvDobTrib")))
            .pagoExteriorSujetoRetencion(SI.equals(text(documento, "pagExtSujRetNorLeg")))
            .pagoRegimenFiscalPreferente(SI.equals(text(documento, "pagoRegFis")))
            .totales(
                Dinero.of(textoExigido(documento, "totalSinImpuestos")),
                Dinero.of(textoExigido(documento, "importeTotal"))
            );

        for (var impuesto : childrenIn(documento, "impuestosDocSustento", "impuestoDocSustento")) {
            builder.impuesto(new Impuesto(
                CodigoImpuesto.of(textoExigido(impuesto, "codImpuestoDocSustento")),
                textoExigido(impuesto, "codigoPorcentaje"),
                new BigDecimal(textoExigido(impuesto, "tarifa")),
                Dinero.of(textoExigido(impuesto, "baseImponible")),
                Dinero.of(textoExigido(impuesto, "valorImpuesto"))
            ));
        }
        for (var retencion : childrenIn(documento, "retenciones", "retencion")) {
            builder.retencion(new Retencion(
                CodigoImpuestoRetencion.of(textoExigido(retencion, "codigo")),
                textoExigido(retencion, "codigoRetencion"),
                Dinero.of(textoExigido(retencion, "baseImponible")),
                new BigDecimal(textoExigido(retencion, "porcentajeRetener")),
                Dinero.of(textoExigido(retencion, "valorRetenido"))
            ));
        }
        for (var pago : Lecturas.pagos(documento)) {
            builder.pago(pago);
        }
        return builder.build();
    }
}
