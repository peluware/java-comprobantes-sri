package com.peluware.sri.comprobantes.it;

import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Emite un comprobante de retención de principio a fin contra el ambiente de certificación del
 * SRI.
 *
 * <p>Antes emite la factura sobre la que se retiene, para que el documento sustento exista de
 * verdad y se vea si el SRI valida esa referencia.
 *
 * <p>Necesita credenciales y red, por lo que Maven no la recoge. Se lanza desde el IDE, o con
 * {@code mvn test -Dtest=ComprobanteRetencionCompletaIT}. El recorrido está en {@link Emision}.
 */
class ComprobanteRetencionCompletaIT {

    @Test
    @DisplayName("emite una retención sobre una factura recién autorizada")
    void emitirYAutorizar() throws Exception {
        var factura = ComprobantesDePrueba.factura();
        var autorizadaLaFactura = Emision.emitirYAutorizar(factura, "factura_V2.1.0.xsd", "retencion-factura-sustentada");

        var retencion = ComprobantesDePrueba.comprobanteRetencion(
            TipoComprobante.FACTURA,
            factura.serie(),
            factura.secuencial(),
            factura.fechaEmision(),
            autorizadaLaFactura.numeroAutorizacion()
        );

        var autorizado = Emision.emitirYAutorizar(retencion, "ComprobanteRetencion_V2.0.0.xsd", "comprobante-retencion");

        var numeroSinGuiones = factura.serie().valor() + factura.secuencial().valor();
        assertTrue(autorizado.comprobante().contains("<numDocSustento>" + numeroSinGuiones + "</numDocSustento>"), autorizado.comprobante());
        assertFalse(autorizado.comprobante().contains("001-001-"), "este esquema escribe el número sin guiones: " + autorizado.comprobante());
        assertTrue(autorizado.comprobante().contains("<codigoRetencion>312</codigoRetencion>"), autorizado.comprobante());
        assertTrue(autorizado.comprobante().contains("<razonSocialSujetoRetenido>"), autorizado.comprobante());
    }
}
