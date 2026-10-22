package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Esquemas;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.factura.FacturaWriter;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacturaWriterTest {

    private static final Schema ESQUEMA = Esquemas.de("factura_V2.1.0.xsd");


    /** Anexo 13: el ejemplo de máquina fiscal de la ficha. */
    private static final MaquinaFiscal MAQUINA = new MaquinaFiscal("SISPAU", "ABC1234", "CGMC1405");

    @Test
    @DisplayName("una factura con todos los bloques opcionales valida contra el XSD del SRI")
    void facturaCompletaValida() {
        validar(facturaCompleta());
    }

    @Test
    @DisplayName("el validador rechaza un XML que incumple el esquema")
    void elValidadorDetectaErrores() {
        var incompleto = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<factura id=\"comprobante\" version=\"2.1.0\">"
            + "<infoTributaria><ambiente>1</ambiente></infoTributaria>"
            + "</factura>";

        assertThrows(SAXException.class, () -> ESQUEMA.newValidator().validate(fuente(incompleto.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("el XML empieza por la declaración con su codificación")
    void declaracionXml() {
        var xml = xml(DatosDePrueba.facturaSinDetalles().detalle(detalle()).build());

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), xml.substring(0, 60));
    }

    @Test
    @DisplayName("la fecha de emisión va en formato dd/MM/yyyy")
    void formatoDeFecha() {
        var xml = xml(DatosDePrueba.facturaSinDetalles().detalle(detalle()).build());

        assertTrue(xml.contains("<fechaEmision>05/10/2026</fechaEmision>"), xml);
    }

    @Test
    @DisplayName("en totalImpuesto la base imponible va antes de la tarifa, al revés que en el detalle")
    void ordenDeBaseImponibleYTarifa() {
        var xml = xml(DatosDePrueba.facturaSinDetalles().detalle(detalle()).build());

        var total = bloque(xml, "<totalImpuesto>", "</totalImpuesto>");
        var linea = bloque(xml, "<impuesto>", "</impuesto>");

        assertAll(
            () -> assertTrue(total.indexOf("<baseImponible>") < total.indexOf("<tarifa>"), "totalImpuesto: " + total),
            () -> assertTrue(linea.indexOf("<tarifa>") < linea.indexOf("<baseImponible>"), "impuesto: " + linea));
    }

    @Test
    @DisplayName("obligadoContabilidad se escribe como SI o NO")
    void obligadoContabilidadSiONo() {
        var noObligado = xml(DatosDePrueba.facturaSinDetalles().detalle(detalle()).build());
        var obligado = xml(DatosDePrueba.facturaSinDetalles().emisor(DatosDePrueba.emisorCompleto()).detalle(detalle()).build());

        assertAll(
            () -> assertTrue(noObligado.contains("<obligadoContabilidad>NO</obligadoContabilidad>"), noObligado),
            () -> assertTrue(obligado.contains("<obligadoContabilidad>SI</obligadoContabilidad>"), obligado));
    }

    @Test
    @DisplayName("los datos adicionales del detalle van como atributos")
    void datosAdicionalesComoAtributos() {
        var xml = xml(facturaCompleta());

        assertTrue(xml.contains("<detAdicional nombre=\"Lote\" valor=\"L-2026-01\"></detAdicional>")
            || xml.contains("<detAdicional nombre=\"Lote\" valor=\"L-2026-01\"/>"), xml);
    }

    @Test
    @DisplayName("los importes se escriben con dos decimales y las cantidades con seis")
    void escalaDeLosImportes() {
        var xml = xml(facturaCompleta());

        assertAll(
            () -> assertTrue(xml.contains("<cantidad>2.000000</cantidad>"), xml),
            () -> assertTrue(xml.contains("<precioUnitario>25.000000</precioUnitario>"), xml),
            () -> assertTrue(xml.contains("<precioTotalSinImpuesto>45.00</precioTotalSinImpuesto>"), xml),
            () -> assertTrue(xml.contains("<totalSinImpuestos>55.00</totalSinImpuestos>"), xml),
            () -> assertTrue(xml.contains("<importeTotal>66.75</importeTotal>"), xml));
    }

    private static Factura facturaCompleta() {
        var emisor = Emisor.builder(DatosDePrueba.RUC, DatosDePrueba.RAZON_SOCIAL, DatosDePrueba.DIRECCION_MATRIZ).nombreComercial("LA TIENDA").agenteRetencion("12345678").contribuyenteRimpe(true).build();
        var comprador = new Comprador(TipoIdentificacion.CEDULA, DatosDePrueba.CEDULA_COMPRADOR, Comprador.RAZON_SOCIAL_PRUEBAS, "AV. 6 DE DICIEMBRE");

        var conTodo = DetalleComprobanteVenta.builder()
            .codigoPrincipal("SKU-001")
            .codigoAuxiliar("AUX-001")
            .descripcion("Alimento para perro")
            .unidadMedida("SACO")
            .cantidad(2)
            .precioUnitario("25.00")
            .descuento(Dinero.of("5.00"))
            .detalleAdicional("Lote", "L-2026-01")
            .iva(TarifaIva.QUINCE)
            .build();

        return DatosDePrueba.facturaSinDetalles()
            .emisor(emisor)
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO_COMPLETO)
            .comprador(comprador)
            .guiaRemision("001-001-000000001")
            .detalle(conTodo)
            .detalle(detalle())
            .propina(Dinero.of("5.00"))
            .pago(Pago.aPlazo(FormaPago.TARJETA_CREDITO, Dinero.of("66.75"), 30))
            .maquinaFiscal(MAQUINA)
            .campoAdicional("Email", "cliente@example.com")
            .build();
    }

    private static DetalleComprobanteVenta detalle() {
        return DetalleComprobanteVenta.builder()
            .descripcion("Consulta veterinaria")
            .cantidad(1)
            .precioUnitario("10.00")
            .iva(TarifaIva.CERO)
            .build();
    }

    private static String xml(Factura factura) {
        return new String(FacturaWriter.INSTANCIA.toByteArray(factura), StandardCharsets.UTF_8);
    }

    private static void validar(Factura factura) {
        byte[] xml = FacturaWriter.INSTANCIA.toByteArray(factura);

        assertDoesNotThrow(() -> {
            try {
                ESQUEMA.newValidator().validate(fuente(xml));
            } catch (SAXException e) {
                throw new AssertionError(e.getMessage() + "\n" + new String(xml, StandardCharsets.UTF_8), e);
            }
        });
    }

    private static StreamSource fuente(byte[] xml) {
        return new StreamSource(new ByteArrayInputStream(xml));
    }

    private static String bloque(String xml, String apertura, String cierre) {
        int inicio = xml.indexOf(apertura);
        return xml.substring(inicio, xml.indexOf(cierre, inicio));
    }
}
