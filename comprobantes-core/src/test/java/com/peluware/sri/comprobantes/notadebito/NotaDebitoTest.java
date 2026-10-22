package com.peluware.sri.comprobantes.notadebito;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo propio de la nota de débito. Lo común a todo comprobante está en
 * {@code ComprobanteContratoTest}.
 */
class NotaDebitoTest {

    @Test
    @DisplayName("el valor total es la base más los impuestos")
    void valorTotal() {
        var totales = DatosDePrueba.notaDebito().build().info().totales();

        assertEquals(Dinero.of("50.00"), totales.totalSinImpuestos());
        assertEquals(Dinero.of("57.50"), totales.valorTotal());
    }

    @Test
    @DisplayName("los impuestos se calculan sobre la base indicada, sin detalles")
    void losImpuestosSeCalculanSobreLaBase() {
        var impuestos = DatosDePrueba.notaDebito().build().info().totales().impuestos();

        assertEquals(1, impuestos.size());
        assertEquals(Dinero.of("50.00"), impuestos.getFirst().baseImponible());
        assertEquals(Dinero.of("7.50"), impuestos.getFirst().valor());
    }

    @Test
    @DisplayName("conserva los motivos en el orden en que se añadieron")
    void conservaLosMotivos() {
        var nota = DatosDePrueba.notaDebito().motivo("GASTOS ADMINISTRATIVOS", Dinero.of("10.00")).build();

        assertEquals(2, nota.motivos().size());
        assertEquals("INTERESES POR MORA", nota.motivos().get(0).razon());
        assertEquals("GASTOS ADMINISTRATIVOS", nota.motivos().get(1).razon());
    }

    @Test
    @DisplayName("rechaza una nota de débito sin motivos")
    void rechazaSinMotivos() {
        var builder = NotaDebito.builder()
            .emisor(DatosDePrueba.emisor())
            .comprador(DatosDePrueba.comprador())
            .fechaEmision(DatosDePrueba.FECHA_EMISION)
            .ambiente(DatosDePrueba.AMBIENTE)
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO)
            .secuencial(DatosDePrueba.SECUENCIAL)
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .documentoModificado(DatosDePrueba.documentoModificado())
            .totalSinImpuestos(Dinero.of("50.00"))
            .iva(TarifaIva.QUINCE);

        var e = assertThrows(ValidacionException.class, builder::build);

        assertEquals("motivos", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("rechaza formas de pago que no suman el valor total")
    void rechazaPagosQueNoCuadran() {
        var builder = DatosDePrueba.notaDebito().pago(Pago.of(FormaPago.TARJETA_CREDITO, Dinero.of("50.00")));

        var e = assertThrows(ValidacionException.class, builder::build);

        assertTrue(assertInstanceOf(Validacion.ReglaInvalida.class, e.invalida()).mensaje().contains("57.50"));
    }

    @Test
    @DisplayName("rechaza una razón con salto de línea, que el esquema no admite")
    void rechazaRazonConSaltoDeLinea() {
        var e = assertThrows(ValidacionException.class, () -> new Motivo("MORA\nPARCIAL", Dinero.of("10.00")));

        assertEquals("razon", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }
}
