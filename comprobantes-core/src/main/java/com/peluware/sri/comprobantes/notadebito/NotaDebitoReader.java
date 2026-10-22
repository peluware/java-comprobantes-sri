package com.peluware.sri.comprobantes.notadebito;

import com.peluware.sri.comprobantes.Esquema;
import com.peluware.sri.comprobantes.ComprobanteNotaReader;
import com.peluware.sri.comprobantes.interno.Lecturas;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.w3c.dom.Element;


import static com.peluware.sri.comprobantes.interno.xml.Dom.childrenIn;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee una {@link NotaDebito} de su XML, firmado o sin firmar.
 *
 * <p>Es el espejo de {@link NotaDebitoWriter}. Toma los importes tal como vienen. La firma, si la
 * lleva, se ignora.
 */
public final class NotaDebitoReader extends ComprobanteNotaReader<NotaDebito, NotaDebito.Builder> {

    /**
     * Única instancia.
     */
    public static final NotaDebitoReader INSTANCIA = new NotaDebitoReader();

    private NotaDebitoReader() {
    }

    @Override
    protected Esquema esquema() {
        return Esquema.NOTA_DEBITO;
    }


    /**
     * Arma la nota de débito a partir del elemento raíz ya leído.
     */
    @Override
    protected NotaDebito.Builder builder() {
        return NotaDebito.builder();
    }

    @Override
    protected NotaDebito propio(NotaDebito.Builder builder, Element raiz, Element info) {
        var totalSinImpuestos = Dinero.of(textoExigido(info, "totalSinImpuestos"));

        builder
            .totalSinImpuestos(totalSinImpuestos)
            .totales(TotalesNotaDebito.dados(totalSinImpuestos, Lecturas.impuestos(info), Dinero.of(textoExigido(info, "valorTotal"))));

        for (var pago : Lecturas.pagos(info)) {
            builder.pago(pago);
        }
        for (var motivo : childrenIn(raiz, "motivos", "motivo")) {
            builder.motivo(textoExigido(motivo, "razon"), Dinero.of(textoExigido(motivo, "valor")));
        }
        return builder.build();
    }
}
