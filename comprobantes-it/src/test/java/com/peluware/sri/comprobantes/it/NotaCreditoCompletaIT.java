package com.peluware.sri.comprobantes.it;

import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Emite una nota de crédito de principio a fin contra el ambiente de certificación del SRI.
 *
 * <p>Antes emite la factura que la nota modifica, para que el documento sustentado exista de
 * verdad: así se comprueba de paso si el SRI valida esa referencia.
 *
 * <p>Necesita credenciales y red, por lo que Maven no la recoge. Se lanza desde el IDE, o con
 * {@code mvn test -Dtest=NotaCreditoCompletaIT}. El recorrido está en {@link Emision}.
 */
class NotaCreditoCompletaIT {

    @Test
    @DisplayName("emite una nota de crédito sobre una factura recién autorizada")
    void emitirYAutorizar() throws Exception {
        var factura = ComprobantesDePrueba.factura();
        Emision.emitirYAutorizar(factura, "factura_V2.1.0.xsd", "nc-factura-sustentada");

        var modificado = new DocumentoModificado(TipoComprobante.FACTURA, factura.serie(), factura.secuencial(), factura.fechaEmision());
        var nota = ComprobantesDePrueba.notaCredito(modificado);

        var autorizado = Emision.emitirYAutorizar(nota, "NotaCredito_V1.1.0.xsd", "nota-credito");

        assertTrue(autorizado.comprobante().contains("<valorModificacion>28.75</valorModificacion>"), autorizado.comprobante());
        assertTrue(autorizado.comprobante().contains("<numDocModificado>" + modificado.numero() + "</numDocModificado>"), autorizado.comprobante());
    }
}
