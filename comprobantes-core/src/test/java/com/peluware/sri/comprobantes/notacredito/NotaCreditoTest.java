package com.peluware.sri.comprobantes.notacredito;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lo propio de la nota de crédito. Lo común a todo comprobante está en
 * {@code ComprobanteContratoTest}.
 */
class NotaCreditoTest {

    @Test
    @DisplayName("el valor de modificación suma el total sin impuestos y los impuestos")
    void valorModificacion() {
        var totales = DatosDePrueba.notaCredito().build().info().totales();

        assertEquals(Dinero.of("100.00"), totales.totalSinImpuestos());
        assertEquals(Dinero.of("115.00"), totales.valorModificacion());
    }

    @Test
    @DisplayName("el número del documento modificado se compone de su serie y su secuencial")
    void numeroDelDocumentoModificado() {
        var modificado = new DocumentoModificado(TipoComprobante.FACTURA, Serie.of(2, 7), Secuencial.of(45), LocalDate.of(2026, 10, 3));

        assertEquals("002-007-000000045", modificado.numero());
    }

    @Test
    @DisplayName("rechaza una nota de crédito sin detalles")
    void rechazaSinDetalles() {
        var builder = NotaCredito.builder()
            .emisor(DatosDePrueba.emisor())
            .comprador(DatosDePrueba.comprador())
            .fechaEmision(DatosDePrueba.FECHA_EMISION)
            .ambiente(DatosDePrueba.AMBIENTE)
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO)
            .secuencial(DatosDePrueba.SECUENCIAL)
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .documentoModificado(DatosDePrueba.documentoModificado())
            .motivo("DEVOLUCIÓN");

        assertEquals("detalles", campoInvalido(builder));
    }

    @Test
    @DisplayName("rechaza un motivo con salto de línea, que el esquema no admite")
    void rechazaMotivoConSaltoDeLinea() {
        var builder = DatosDePrueba.notaCredito().motivo("DEVOLUCIÓN\nPARCIAL");

        assertEquals("motivo", campoInvalido(builder));
    }

    @Test
    @DisplayName("rechaza un motivo más largo de lo que admite el esquema")
    void rechazaMotivoDemasiadoLargo() {
        var builder = DatosDePrueba.notaCredito().motivo("X".repeat(InfoNotaCredito.MOTIVO_LONGITUD_MAXIMA + 1));

        assertEquals("motivo", campoInvalido(builder));
    }

    private static String campoInvalido(NotaCredito.Builder builder) {
        var e = assertThrows(ValidacionException.class, builder::build);
        return assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo();
    }

    @Test
    @DisplayName("si ningún detalle tributa, el comprobante no se puede construir")
    void exigeQueAlgoTribute() {
        var builder = DatosDePrueba.notaCreditoSinDetalles()
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .detalle(DetalleNotaCredito.builder().descripcion("Consulta").cantidad(1).precioUnitario("10.00").build());

        var e = assertThrows(ValidacionException.class, builder::build);

        assertEquals("totalConImpuestos", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }
}
