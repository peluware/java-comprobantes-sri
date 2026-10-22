package com.peluware.sri.comprobantes.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Emite una factura de principio a fin contra el ambiente de certificación del SRI.
 *
 * <p>Recorre los tres módulos juntos con un certificado real. Necesita credenciales y red, por
 * lo que Maven no la recoge: su nombre termina en {@code IT} y no en {@code Test}. Se lanza
 * desde el IDE, o con {@code mvn test -Dtest=FacturacionCompletaIT}.
 *
 * <p>El certificado, su clave y el RUC se piden por teclado al arrancar; ver
 * {@link DatosDeEmision}. El recorrido está en {@link Emision}.
 */
class FacturacionCompletaIT {

    /**
     * Pide los datos al arrancar, antes de nada, para que el diálogo no salga a mitad de la
     * prueba. {@link DatosDeEmision#pedir()} los recuerda, así que las llamadas siguientes no
     * vuelven a preguntar.
     */
    @Test
    @DisplayName("emite una factura y obtiene su autorización del ambiente de certificación")
    void emitirYAutorizar() throws Exception {
        var factura = ComprobantesDePrueba.factura();

        var autorizado = Emision.emitirYAutorizar(factura, "factura_V2.1.0.xsd", "factura");

        assertTrue(autorizado.comprobante().contains("<importeTotal>70.50</importeTotal>"), autorizado.comprobante());
    }
}
