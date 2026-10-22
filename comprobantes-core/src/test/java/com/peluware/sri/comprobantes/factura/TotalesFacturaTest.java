package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.factura.TotalesFactura;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TotalesFacturaTest {

    @Test
    @DisplayName("agrupa los impuestos por código de porcentaje y suma las bases")
    void agrupaPorCodigoPorcentaje() {
        var totales = TotalesFactura.de(List.of(
            detalle("10.00", TarifaIva.QUINCE),
            detalle("10.00", TarifaIva.QUINCE),
            detalle("20.00", TarifaIva.CERO))
        );

        assertEquals(Dinero.of("40.00"), totales.totalSinImpuestos());
        assertEquals(2, totales.totalConImpuestos().size());

        var cero = totales.totalConImpuestos().getFirst();
        assertEquals("0", cero.codigoPorcentaje());
        assertEquals(Dinero.of("20.00"), cero.baseImponible());
        assertEquals(Dinero.CERO, cero.valor());

        var quince = totales.totalConImpuestos().get(1);
        assertEquals("4", quince.codigoPorcentaje());
        assertEquals(Dinero.of("20.00"), quince.baseImponible());
        assertEquals(Dinero.of("3.00"), quince.valor());

        assertEquals(Dinero.of("43.00"), totales.importeTotal());
    }

    @Test
    @DisplayName("el subtotal suma los precios ya redondeados de cada línea, no redondea la suma")
    void sumaLineasRedondeadas() {
        var totales = TotalesFactura.de(List.of(
            detalle(3, "0.335", TarifaIva.CERO),
            detalle(3, "0.335", TarifaIva.CERO)));

        assertEquals(Dinero.of("2.02"), totales.totalSinImpuestos());
    }

    @Test
    @DisplayName("el valor agrupado suma los valores de cada línea, no se recalcula sobre la base agrupada")
    void sumaValoresDeCadaLinea() {
        var totales = TotalesFactura.de(List.of(
            detalle("0.10", TarifaIva.QUINCE),
            detalle("0.10", TarifaIva.QUINCE)));

        var iva = totales.totalConImpuestos().getFirst();

        assertEquals(Dinero.of("0.20"), iva.baseImponible());
        assertEquals(Dinero.of("0.04"), iva.valor());
        assertEquals(Dinero.of("0.24"), totales.importeTotal());
    }

    @Test
    @DisplayName("omite la tarifa agrupada cuando los detalles no la comparten")
    void omiteLaTarifaCuandoDifiere() {
        var totales = TotalesFactura.de(List.of(
            detalleDiferenciado("10.00", new BigDecimal("8.00")),
            detalleDiferenciado("10.00", new BigDecimal("4.00"))));

        var iva = totales.totalConImpuestos().getFirst();

        assertEquals("8", iva.codigoPorcentaje());
        assertNull(iva.tarifa());
        assertEquals(Dinero.of("1.20"), iva.valor());
    }

    @Test
    @DisplayName("conserva la tarifa agrupada cuando los detalles la comparten")
    void conservaLaTarifaCompartida() {
        var totales = TotalesFactura.de(List.of(detalle("10.00", TarifaIva.QUINCE), detalle("5.00", TarifaIva.QUINCE)));

        assertEquals(new BigDecimal("15.00"), totales.totalConImpuestos().getFirst().tarifa());
    }

    @Test
    @DisplayName("el importe total incluye la propina")
    void elImporteTotalIncluyeLaPropina() {
        var totales = TotalesFactura.de(List.of(detalle("40.00", TarifaIva.QUINCE)), Dinero.of("4.00"));

        assertEquals(Dinero.of("50.00"), totales.importeTotal());
    }

    @Test
    @DisplayName("no impone el tope de propina de la Tabla 21, que es prosa y no esquema")
    void noImponeElTopeDePropina() {
        var detalles = List.of(detalle("40.00", TarifaIva.QUINCE));
        var sobreElTope = Dinero.of("4.01");

        var totales = TotalesFactura.de(detalles, sobreElTope);

        assertEquals(sobreElTope, totales.propina());
        assertEquals(Dinero.of("50.01"), totales.importeTotal());
    }

    @Test
    @DisplayName("rechaza una factura sin detalles")
    void rechazaSinDetalles() {
        assertEquals("detalles", campoInvalido(() -> TotalesFactura.de(List.of())));
    }

    @Test
    @DisplayName("el total de descuentos suma los descuentos de las líneas")
    void sumaLosDescuentos() {
        var conDescuento = DetalleComprobanteVenta.builder()
            .descripcion("Consulta")
            .cantidad(1)
            .precioUnitario("100.00")
            .descuento(Dinero.of("10.00"))
            .iva(TarifaIva.QUINCE)
            .build();

        var totales = TotalesFactura.de(List.of(conDescuento, detalle("10.00", TarifaIva.QUINCE)));

        assertEquals(Dinero.of("10.00"), totales.totalDescuento());
        assertEquals(Dinero.of("100.00"), totales.totalSinImpuestos());
    }

    @Test
    @DisplayName("el ICE y el IVA se totalizan por separado")
    void iceEIvaSeparados() {
        var cerveza = DetalleComprobanteVenta.builder()
            .descripcion("Cerveza industrial")
            .cantidad(1)
            .precioUnitario("10.00")
            .ice("3077", new BigDecimal("75.00"))
            .iva(TarifaIva.QUINCE)
            .build();

        var totales = TotalesFactura.de(List.of(cerveza));

        assertEquals(2, totales.totalConImpuestos().size());
        assertEquals(CodigoImpuesto.IVA, totales.totalConImpuestos().get(0).codigo());
        assertEquals(CodigoImpuesto.ICE, totales.totalConImpuestos().get(1).codigo());
        assertEquals(Dinero.of("20.13"), totales.importeTotal());
    }

    private static DetalleComprobanteVenta detalle(String precioUnitario, TarifaIva tarifa) {
        return detalle(1, precioUnitario, tarifa);
    }

    private static DetalleComprobanteVenta detalle(long cantidad, String precioUnitario, TarifaIva tarifa) {
        return DetalleComprobanteVenta.builder()
            .descripcion("Producto")
            .cantidad(cantidad)
            .precioUnitario(precioUnitario)
            .iva(tarifa)
            .build();
    }

    private static DetalleComprobanteVenta detalleDiferenciado(String precioUnitario, BigDecimal porcentaje) {
        return DetalleComprobanteVenta.builder()
            .descripcion("Producto")
            .cantidad(1)
            .precioUnitario(precioUnitario)
            .iva(new TarifaIva(TarifaIva.CODIGO_DIFERENCIADO, porcentaje))
            .build();
    }

    private static String campoInvalido(Executable accion) {
        var e = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo();
    }
}
