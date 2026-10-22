package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guarda lo que el README promete sobre la lectura: los importes se toman tal como vienen.
 */
class LecturaIncoherenteTest {

    @Test
    @DisplayName("un total que no cuadra con sus líneas se lee tal cual, sin recalcular")
    void losImportesSeTomanComoVienen() {
        var leida = (Factura) ComprobanteReader.comprobante(conOtroImporteTotal(DatosDePrueba.factura().build()));

        assertEquals(Dinero.of("999.00"), leida.info().totales().importeTotal());
    }

    @Test
    @DisplayName("pero las formas de pago sí tienen que sumar ese total")
    void lasFormasDePagoSiSeComprueban() {
        var conPago = DatosDePrueba.factura().pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("115.00"))).build();

        var defecto = assertThrows(ValidacionException.class, () -> ComprobanteReader.comprobante(conOtroImporteTotal(conPago)));

        assertEquals("Las formas de pago suman 115.00 y el importe total es 999.00", defecto.getMessage());
    }

    /**
     * El XML del comprobante con el importe total cambiado a mano, como vendría de fuera.
     */
    private static byte[] conOtroImporteTotal(Comprobante comprobante) {
        var xml = new String(ComprobanteWriter.xml(comprobante), StandardCharsets.UTF_8);
        return xml.replace("<importeTotal>115.00</importeTotal>", "<importeTotal>999.00</importeTotal>").getBytes(StandardCharsets.UTF_8);
    }
}
