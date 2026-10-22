package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteReader;
import com.peluware.sri.comprobantes.LecturaXmlException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.valores.Cantidad;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.w3c.dom.Element;


import static com.peluware.sri.comprobantes.interno.xml.Dom.childrenIn;
import static com.peluware.sri.comprobantes.interno.xml.Dom.child;
import static com.peluware.sri.comprobantes.interno.xml.Dom.text;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee una {@link GuiaRemision} de su XML, firmado o sin firmar.
 *
 * <p>Es el espejo de {@link GuiaRemisionWriter}. Su esquema no declara la fecha de emisión en el
 * bloque de información, así que se toma de la clave de acceso. La firma, si la lleva, se ignora.
 */
public final class GuiaRemisionReader extends ComprobanteReader<GuiaRemision, GuiaRemision.Builder> {

    /**
     * Única instancia.
     */
    public static final GuiaRemisionReader INSTANCIA = new GuiaRemisionReader();

    private GuiaRemisionReader() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.GUIA_REMISION;
    }


    /**
     * Arma la guía de remisión a partir del elemento raíz ya leído.
     */
    @Override
    protected GuiaRemision.Builder builder() {
        return GuiaRemision.builder();
    }

    @Override
    protected GuiaRemision propio(GuiaRemision.Builder builder, Element raiz, Element info) {
        builder
            .direccionPartida(textoExigido(info, "dirPartida"))
            .transportista(new Transportista(
                TipoIdentificacion.of(textoExigido(info, "tipoIdentificacionTransportista")),
                textoExigido(info, "rucTransportista"),
                textoExigido(info, "razonSocialTransportista")
            ))
            .transporte(
                Lecturas.fecha(textoExigido(info, "fechaIniTransporte"), "fechaIniTransporte"),
                Lecturas.fecha(textoExigido(info, "fechaFinTransporte"), "fechaFinTransporte")
            )
            .placa(textoExigido(info, "placa"))
            .rise(text(info, "rise"));

        for (var destinatario : childrenIn(raiz, "destinatarios", "destinatario")) {
            builder.destinatario(destinatario(destinatario));
        }
        return builder.build();
    }

    private static Destinatario destinatario(Element destinatario) {
        var builder = Destinatario.builder()
            .identificacion(textoExigido(destinatario, "identificacionDestinatario"))
            .razonSocial(textoExigido(destinatario, "razonSocialDestinatario"))
            .direccion(textoExigido(destinatario, "dirDestinatario"))
            .motivoTraslado(textoExigido(destinatario, "motivoTraslado"))
            .documentoAduanero(text(destinatario, "docAduaneroUnico"))
            .codigoEstablecimientoDestino(text(destinatario, "codEstabDestino"))
            .ruta(text(destinatario, "ruta"))
            .sustentoTraslado(sustento(destinatario));

        for (var detalle : childrenIn(destinatario, "detalles", "detalle")) {
            builder.detalle(detalle(detalle));
        }
        return builder.build();
    }

    /**
     * El comprobante de venta que sustenta el traslado, o {@code null} si no viene.
     */
    private static @org.jspecify.annotations.Nullable SustentoTraslado sustento(Element destinatario) {
        if (child(destinatario, "numDocSustento") == null) {
            return null;
        }

        var numero = textoExigido(destinatario, "numDocSustento");
        var partes = numero.split(Serie.SEPARADOR, -1);
        if (partes.length != 3) {
            throw new LecturaXmlException("El campo numDocSustento no tiene el formato 000-000-000000000: " + numero);
        }

        var fecha = text(destinatario, "fechaEmisionDocSustento");
        return new SustentoTraslado(
            TipoComprobante.of(textoExigido(destinatario, "codDocSustento")),
            Serie.of(Integer.parseInt(partes[0]), Integer.parseInt(partes[1])),
            Secuencial.of(Long.parseLong(partes[2])),
            text(destinatario, "numAutDocSustento"),
            fecha == null ? null : Lecturas.fecha(fecha, "fechaEmisionDocSustento")
        );
    }

    private static DetalleGuiaRemision detalle(Element detalle) {
        var builder = DetalleGuiaRemision.builder()
            .codigoInterno(text(detalle, "codigoInterno"))
            .codigoAdicional(text(detalle, "codigoAdicional"))
            .descripcion(textoExigido(detalle, "descripcion"))
            .cantidad(Cantidad.of(textoExigido(detalle, "cantidad")));

        for (var adicional : Lecturas.detallesAdicionales(detalle)) {
            builder.detalleAdicional(adicional.nombre(), adicional.valor());
        }
        return builder.build();
    }
}
