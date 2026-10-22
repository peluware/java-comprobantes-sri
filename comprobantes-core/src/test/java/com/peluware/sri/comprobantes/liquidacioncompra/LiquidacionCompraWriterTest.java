package com.peluware.sri.comprobantes.liquidacioncompra;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Esquemas;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Lo que el escritor de la liquidación de compra hace distinto del de la factura. Que su XML
 * mínimo valide contra el XSD lo comprueba {@code ComprobanteContratoTest}.
 */
class LiquidacionCompraWriterTest {

    private static final Schema ESQUEMA = Esquemas.de("LiquidacionCompra_V1.1.0.xsd");

    @Test
    @DisplayName("con todos los campos opcionales sigue validando contra el XSD")
    void conTodoLoOpcionalValida() {
        var completa = DatosDePrueba.liquidacionCompraSinDetalles()
            .emisor(DatosDePrueba.emisorCompleto())
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO_COMPLETO)
            .proveedor(new Proveedor(TipoIdentificacion.RUC, "1790012345001", "AGRICOLA LA ESPERANZA", "VIA A DAULE KM 12"))
            .maquinaFiscal(new MaquinaFiscal("SISPAU", "ABC1234", "CGMC1405"))
            .detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal("MAIZ-01")
                .codigoAuxiliar("ALT-01")
                .descripcion("Quintal de maíz")
                .unidadMedida("QUINTAL")
                .cantidad(2)
                .precioUnitario("50.00")
                .descuento(Dinero.of("10.00"))
                .detalleAdicional("Cosecha", "2026")
                .iva(TarifaIva.QUINCE)
                .build())
            .pago(Pago.aPlazo(FormaPago.TARJETA_CREDITO, Dinero.of("103.50"), 30))
            .campoAdicional("Origen", "pruebas")
            .build();

        byte[] xml = LiquidacionCompraWriter.INSTANCIA.toByteArray(completa);
        try {
            ESQUEMA.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (Exception e) {
            fail("no cumple el XSD: " + e.getMessage() + "\n" + new String(xml, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("la contraparte se escribe como proveedor, no como comprador")
    void laContraparteEsUnProveedor() {
        var xml = texto(DatosDePrueba.liquidacionCompra().build());

        assertTrue(xml.contains("<tipoIdentificacionProveedor>04</tipoIdentificacionProveedor>"), xml);
        assertTrue(xml.contains("<razonSocialProveedor>AGRICOLA LA ESPERANZA</razonSocialProveedor>"), xml);
        assertTrue(xml.contains("<identificacionProveedor>1790012345001</identificacionProveedor>"), xml);
        assertFalse(xml.contains("Comprador"), xml);
    }

    @Test
    @DisplayName("la dirección del proveedor se omite si no se indica")
    void laDireccionDelProveedorEsOpcional() {
        var sin = texto(DatosDePrueba.liquidacionCompra().build());
        var con = texto(DatosDePrueba.liquidacionCompraSinDetalles()
            .proveedor(new Proveedor(TipoIdentificacion.RUC, "1790012345001", "AGRICOLA LA ESPERANZA", "VIA A DAULE KM 12"))
            .detalle(DatosDePrueba.detalleLiquidacionCompra())
            .build());

        assertFalse(sin.contains("<direccionProveedor>"), sin);
        assertTrue(con.contains("<direccionProveedor>VIA A DAULE KM 12</direccionProveedor>"), con);
    }

    @Test
    @DisplayName("no escribe propina, que su esquema no declara")
    void noEscribePropina() {
        var xml = texto(DatosDePrueba.liquidacionCompra().build());

        assertFalse(xml.contains("propina"), xml);
    }

    @Test
    @DisplayName("el importe total suma el subtotal y los impuestos")
    void elImporteTotalSumaLosImpuestos() {
        var xml = texto(DatosDePrueba.liquidacionCompra().build());

        assertTrue(xml.contains("<totalSinImpuestos>100.00</totalSinImpuestos>"), xml);
        assertTrue(xml.contains("<importeTotal>115.00</importeTotal>"), xml);
    }

    @Test
    @DisplayName("los códigos del detalle se escriben con los nombres de este esquema")
    void losCodigosDelDetalle() {
        var xml = texto(DatosDePrueba.liquidacionCompraSinDetalles()
            .detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal("MAIZ-01")
                .codigoAuxiliar("ALT-01")
                .descripcion("Quintal de maíz")
                .cantidad(1)
                .precioUnitario("100.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .build());

        assertTrue(xml.contains("<codigoPrincipal>MAIZ-01</codigoPrincipal>"), xml);
        assertTrue(xml.contains("<codigoAuxiliar>ALT-01</codigoAuxiliar>"), xml);
        assertFalse(xml.contains("codigoInterno"), xml);
    }

    private static String texto(LiquidacionCompra liquidacion) {
        return new String(LiquidacionCompraWriter.INSTANCIA.toByteArray(liquidacion), StandardCharsets.UTF_8);
    }

}
