package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.notacredito.NotaCreditoReader;
import com.peluware.sri.comprobantes.notadebito.NotaDebitoReader;
import org.w3c.dom.Element;

import static com.peluware.sri.comprobantes.interno.xml.Dom.text;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee de su XML una nota que corrige un comprobante.
 *
 * <p>Es el espejo de {@link ComprobanteNotaWriter}. Las dos notas declaran la misma cabecera
 * —a quién va dirigida y qué comprobante corrige— y se separan en lo que viene después, así que
 * esta clase rellena la cabecera y devuelve el builder.
 *
 * <p>Las formas de pago no están aquí: solo la nota de débito las declara.
 *
 * @param <C> nota que lee
 */
public abstract sealed class ComprobanteNotaReader<C extends ComprobanteNota, B extends ComprobanteNota.Builder<B>> extends ComprobanteReader<C, B> permits NotaCreditoReader, NotaDebitoReader {

    protected ComprobanteNotaReader() {
    }

    /**
     * Rellena la cabecera que las dos notas declaran igual.
     *
     * <p>Es {@code final}: la base la invoca siempre y una hoja no puede saltársela ni cambiarla.
     */
    @Override
    protected final void familia(B builder, Element raiz, Element info) {
        builder
            .fechaEmision(Lecturas.fecha(textoExigido(info, "fechaEmision"), "fechaEmision"))
            .comprador(comprador(info))
            .rise(text(info, "rise"))
            .documentoModificado(Lecturas.documentoModificado(info, "codDocModificado", "numDocModificado", "fechaEmisionDocSustento"));
    }

    /**
     * El adquirente, que las dos notas declaran sin dirección.
     */
    private static Comprador comprador(Element info) {
        return Comprador.of(
            TipoIdentificacion.of(textoExigido(info, "tipoIdentificacionComprador")),
            textoExigido(info, "identificacionComprador"),
            textoExigido(info, "razonSocialComprador")
        );
    }
}
