package com.peluware.sri.comprobantes.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Emite una liquidación de compra de principio a fin contra el ambiente de certificación del
 * SRI: valida contra el XSD, firma, envía a recepción y consulta hasta que el SRI resuelva.
 *
 * <p>Necesita credenciales y red, por lo que Maven no la recoge. Se lanza desde el IDE, o con
 * {@code mvn test -Dtest=LiquidacionCompraCompletaIT}. El recorrido está en {@link Emision}.
 */
class LiquidacionCompraCompletaIT {

    @Test
    @DisplayName("emite una liquidación de compra a un proveedor")
    void emitirYAutorizar() throws Exception {
        var liquidacion = ComprobantesDePrueba.liquidacionCompra();

        var autorizado = Emision.emitirYAutorizar(liquidacion, "LiquidacionCompra_V1.1.0.xsd", "liquidacion-compra");

        assertTrue(autorizado.comprobante().contains("<razonSocialProveedor>AGRICOLA LA ESPERANZA</razonSocialProveedor>"), autorizado.comprobante());
        assertTrue(autorizado.comprobante().contains("<importeTotal>115.00</importeTotal>"), autorizado.comprobante());
    }
}
