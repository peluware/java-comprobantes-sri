package com.peluware.sri.comprobantes.it;

import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.guiaremision.SustentoTraslado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Emite una guía de remisión de principio a fin contra el ambiente de certificación del SRI.
 *
 * <p>Antes emite la factura que sustenta el traslado, para que el documento referenciado exista
 * de verdad y se vea si el SRI valida esa referencia.
 *
 * <p>Necesita credenciales y red, por lo que Maven no la recoge. Se lanza desde el IDE, o con
 * {@code mvn test -Dtest=GuiaRemisionCompletaIT}. El recorrido está en {@link Emision}.
 */
class GuiaRemisionCompletaIT {

    @Test
    @DisplayName("emite una guía de remisión sustentada en una factura recién autorizada")
    void emitirYAutorizar() throws Exception {
        var factura = ComprobantesDePrueba.factura();
        var autorizadaLaFactura = Emision.emitirYAutorizar(factura, "factura_V2.1.0.xsd", "guia-factura-sustentada");

        var sustento = new SustentoTraslado(
            TipoComprobante.FACTURA,
            factura.serie(),
            factura.secuencial(),
            autorizadaLaFactura.numeroAutorizacion(),
            factura.fechaEmision()
        );
        var guia = ComprobantesDePrueba.guiaRemision(sustento);

        var autorizado = Emision.emitirYAutorizar(guia, "GuiaRemision_V1.1.0.xsd", "guia-remision");

        assertTrue(autorizado.comprobante().contains("<placa>PBA1234</placa>"), autorizado.comprobante());
        assertTrue(autorizado.comprobante().contains("<numDocSustento>" + sustento.numero() + "</numDocSustento>"), autorizado.comprobante());
        assertFalse(autorizado.comprobante().contains("<fechaEmision>"), "la guía es el único comprobante sin el elemento fechaEmision en su XML: " + autorizado.comprobante());
    }
}
