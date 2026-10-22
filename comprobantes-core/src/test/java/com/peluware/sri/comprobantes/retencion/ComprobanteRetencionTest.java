package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Secuencial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lo propio del modelo del comprobante de retención. Lo que comparte con los demás lo comprueba
 * {@code ComprobanteContratoTest}.
 */
class ComprobanteRetencionTest {

    @Test
    @DisplayName("su clave de acceso lleva el código 07")
    void laClaveLlevaSuCodigo() {
        var retencion = DatosDePrueba.comprobanteRetencion().build();

        assertEquals(TipoComprobante.COMPROBANTE_RETENCION, retencion.tipoComprobante());
        assertEquals("07", retencion.claveAcceso().substring(8, 10));
    }

    @Test
    @DisplayName("el valor retenido se calcula de la base y el porcentaje")
    void elValorRetenidoSeCalcula() {
        var retencion = Retencion.de(CodigoImpuestoRetencion.RENTA, "312", Dinero.of("1000.00"), "1.75");

        assertEquals(Dinero.of("17.50"), retencion.valorRetenido());
        assertEquals("1.75", retencion.porcentajeEscrito());
    }

    @Test
    @DisplayName("el total retenido suma todas las retenciones de todos los documentos")
    void elTotalRetenidoSuma() {
        var retencion = DatosDePrueba.comprobanteRetencionSinDocumentos()
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .documentoSustento(documento(Secuencial.of(45), "1.75"))
            .documentoSustento(documento(Secuencial.of(46), "2.75"))
            .build();

        assertEquals(Dinero.of("4.50"), retencion.totalRetenido());
    }

    @Test
    @DisplayName("exige al menos un documento sustento")
    void exigeUnDocumentoSustento() {
        var builder = DatosDePrueba.comprobanteRetencionSinDocumentos().codigoNumerico(DatosDePrueba.CODIGO_NUMERICO);

        var e = assertThrows(ValidacionException.class, builder::build);

        assertEquals("docsSustento", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("cada documento sustento exige impuestos, retenciones y pagos")
    void cadaDocumentoExigeSusBloques() {
        var sinRetenciones = DocumentoSustento.builder()
            .codigoSustento("01")
            .documento(TipoComprobante.FACTURA, DatosDePrueba.SERIE, Secuencial.of(45), DatosDePrueba.FECHA_EMISION)
            .totales(Dinero.of("100.00"), Dinero.of("115.00"))
            .impuesto(Impuesto.of(CodigoImpuesto.IVA, TarifaIva.QUINCE.codigo(), TarifaIva.QUINCE.tarifa(), Dinero.of("100.00")))
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("115.00")));

        var e = assertThrows(ValidacionException.class, sinRetenciones::build);

        assertEquals("retenciones", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("rechaza una forma de pago con plazo, que su esquema no declara")
    void rechazaUnPagoConPlazo() {
        var conPlazo = DocumentoSustento.builder()
            .codigoSustento("01")
            .documento(TipoComprobante.FACTURA, DatosDePrueba.SERIE, Secuencial.of(45), DatosDePrueba.FECHA_EMISION)
            .totales(Dinero.of("100.00"), Dinero.of("115.00"))
            .impuesto(Impuesto.of(CodigoImpuesto.IVA, TarifaIva.QUINCE.codigo(), TarifaIva.QUINCE.tarifa(), Dinero.of("100.00")))
            .retencion(Retencion.de(CodigoImpuestoRetencion.RENTA, "312", Dinero.of("100.00"), "1.75"))
            .pago(Pago.aPlazo(FormaPago.TARJETA_CREDITO, Dinero.of("115.00"), 30));

        var e = assertThrows(ValidacionException.class, conPlazo::build);

        assertInstanceOf(Validacion.ReglaInvalida.class, e.invalida());
    }

    @Test
    @DisplayName("el número del documento sustento son quince dígitos seguidos")
    void elNumeroSonQuinceDigitos() {
        var documento = documento(Secuencial.of(45), "1.75");

        assertEquals("001001000000045", documento.numero());
        assertEquals(15, documento.numero().length());
    }

    private static DocumentoSustento documento(Secuencial secuencial, String porcentaje) {
        return DocumentoSustento.builder()
            .codigoSustento("01")
            .documento(TipoComprobante.FACTURA, DatosDePrueba.SERIE, secuencial, DatosDePrueba.FECHA_EMISION)
            .totales(Dinero.of("100.00"), Dinero.of("115.00"))
            .impuesto(Impuesto.of(CodigoImpuesto.IVA, TarifaIva.QUINCE.codigo(), TarifaIva.QUINCE.tarifa(), Dinero.of("100.00")))
            .retencion(Retencion.de(CodigoImpuestoRetencion.RENTA, "312", Dinero.of("100.00"), porcentaje))
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("115.00")))
            .build();
    }
}
