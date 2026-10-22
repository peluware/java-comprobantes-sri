package com.peluware.sri.comprobantes.liquidacioncompra;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lo propio del modelo de la liquidación de compra. Lo que comparte con los demás comprobantes
 * lo comprueba {@code ComprobanteContratoTest}.
 */
class LiquidacionCompraTest {

    @Test
    @DisplayName("su clave de acceso lleva el código 03")
    void laClaveLlevaSuCodigo() {
        var liquidacion = DatosDePrueba.liquidacionCompra().build();

        assertEquals(TipoComprobante.LIQUIDACION_COMPRA, liquidacion.tipoComprobante());
        assertEquals("03", liquidacion.claveAcceso().substring(8, 10));
    }

    @Test
    @DisplayName("exige el proveedor")
    void exigeElProveedor() {
        var builder = LiquidacionCompra.builder()
            .emisor(DatosDePrueba.emisor())
            .fechaEmision(DatosDePrueba.FECHA_EMISION)
            .ambiente(DatosDePrueba.AMBIENTE)
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO)
            .secuencial(DatosDePrueba.SECUENCIAL)
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .detalle(DatosDePrueba.detalleLiquidacionCompra());

        assertEquals("proveedor", assertThrows(NullPointerException.class, builder::build).getMessage());
    }

    @Test
    @DisplayName("exige al menos un detalle")
    void exigeUnDetalle() {
        var builder = DatosDePrueba.liquidacionCompraSinDetalles();

        var e = assertThrows(ValidacionException.class, builder::build);

        assertEquals("detalles", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("los totales se calculan de los detalles, sin propina")
    void losTotalesSeCalculan() {
        var liquidacion = DatosDePrueba.liquidacionCompraSinDetalles()
            .detalle(DetalleComprobanteVenta.builder().descripcion("Maíz").cantidad(2).precioUnitario("50.00").iva(TarifaIva.QUINCE).build())
            .detalle(DetalleComprobanteVenta.builder().descripcion("Transporte").cantidad(1).precioUnitario("20.00").iva(TarifaIva.CERO).build())
            .build();

        var totales = liquidacion.info().totales();
        assertEquals(Dinero.of("120.00"), totales.totalSinImpuestos());
        assertEquals(Dinero.of("135.00"), totales.importeTotal());
    }

    @Test
    @DisplayName("el descuento del detalle baja el subtotal y se totaliza")
    void elDescuentoSeTotaliza() {
        var liquidacion = DatosDePrueba.liquidacionCompraSinDetalles()
            .detalle(DetalleComprobanteVenta.builder().descripcion("Maíz").cantidad(2).precioUnitario("50.00").descuento(Dinero.of("10.00")).iva(TarifaIva.QUINCE).build())
            .build();

        var totales = liquidacion.info().totales();
        assertEquals(Dinero.of("90.00"), totales.totalSinImpuestos());
        assertEquals(Dinero.of("10.00"), totales.totalDescuento());
        assertEquals(Dinero.of("103.50"), totales.importeTotal());
    }

    @Test
    @DisplayName("si se detallan pagos deben sumar el importe total")
    void losPagosDebenSumarElTotal() {
        var builder = DatosDePrueba.liquidacionCompra().pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("100.00")));

        var e = assertThrows(ValidacionException.class, builder::build);

        assertInstanceOf(Validacion.ReglaInvalida.class, e.invalida());
    }

    @Test
    @DisplayName("si ningún detalle tributa, el comprobante no se puede construir")
    void exigeQueAlgoTribute() {
        var builder = DatosDePrueba.liquidacionCompraSinDetalles()
            .detalle(DetalleComprobanteVenta.builder().descripcion("Maíz").cantidad(1).precioUnitario("10.00").build());

        var e = assertThrows(ValidacionException.class, builder::build);

        assertEquals("totalConImpuestos", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }
}
