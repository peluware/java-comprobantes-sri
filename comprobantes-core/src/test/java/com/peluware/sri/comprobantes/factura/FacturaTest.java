package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo propio de la factura. Lo común a todo comprobante está en
 * {@code ComprobanteContratoTest}.
 */
class FacturaTest {

    @Test
    @DisplayName("calcula los totales a partir de los detalles")
    void calculaLosTotales() {
        var factura = DatosDePrueba.factura().build();

        var totales = factura.info().totales();
        assertEquals(Dinero.of("100.00"), totales.totalSinImpuestos());
        assertEquals(Dinero.of("115.00"), totales.importeTotal());
        assertEquals(Sri.MONEDA, factura.info().moneda());
    }

    @Test
    @DisplayName("la propina entra en el importe total")
    void laPropinaEntraEnElTotal() {
        var factura = DatosDePrueba.factura().propina(Dinero.of("10.00")).build();

        assertEquals(Dinero.of("125.00"), factura.info().totales().importeTotal());
    }

    @Test
    @DisplayName("el descuento de los detalles se totaliza aparte")
    void elDescuentoSeTotaliza() {
        var factura = DatosDePrueba.facturaSinDetalles()
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Consulta veterinaria")
                .cantidad(1)
                .precioUnitario("100.00")
                .descuento(Dinero.of("10.00"))
                .iva(TarifaIva.QUINCE)
                .build())
            .build();

        var totales = factura.info().totales();
        assertEquals(Dinero.of("10.00"), totales.totalDescuento());
        assertEquals(Dinero.of("90.00"), totales.totalSinImpuestos());
    }

    @Test
    @DisplayName("la fecha de emisión de la factura y la de infoFactura son la misma")
    void laFechaEsLaMisma() {
        var factura = DatosDePrueba.factura().build();

        assertEquals(factura.fechaEmision(), factura.info().fechaEmision());
    }

    @Test
    @DisplayName("admite formas de pago que suman el importe total")
    void admiteFormasDePagoQueCuadran() {
        var factura = DatosDePrueba.factura()
            .pago(Pago.of(FormaPago.TARJETA_CREDITO, Dinero.of("50.00")))
            .pago(Pago.aPlazo(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("65.00"), 30))
            .build();

        assertEquals(2, factura.info().pagos().size());
    }

    @Test
    @DisplayName("rechaza formas de pago que no suman el importe total")
    void rechazaFormasDePagoQueNoCuadran() {
        var builder = DatosDePrueba.factura().pago(Pago.of(FormaPago.TARJETA_CREDITO, Dinero.of("100.00")));

        assertTrue(reglaInvalida(builder::build).contains("115.00"));
    }

    @Test
    @DisplayName("rechaza una factura sin detalles")
    void rechazaUnaFacturaSinDetalles() {
        var e = assertThrows(ValidacionException.class, () -> DatosDePrueba.facturaSinDetalles().build());

        assertEquals("detalles", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    private static String reglaInvalida(Executable accion) {
        var e = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.ReglaInvalida.class, e.invalida()).mensaje();
    }

    @Test
    @DisplayName("si ningún detalle tributa, el comprobante no se puede construir")
    void exigeQueAlgoTribute() {
        var builder = DatosDePrueba.facturaSinDetalles()
            .detalle(DetalleComprobanteVenta.builder().descripcion("Consulta").cantidad(1).precioUnitario("10.00").build());

        var e = assertThrows(ValidacionException.class, builder::build);

        assertEquals("totalConImpuestos", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }
}
