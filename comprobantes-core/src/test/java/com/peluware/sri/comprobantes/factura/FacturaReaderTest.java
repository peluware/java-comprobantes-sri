package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.ComprobanteWriter;
import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.EstrategiaCodigoNumerico;
import com.peluware.sri.comprobantes.LecturaXmlException;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.valores.ContribuyenteEspecial;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La vuelta: del XML a la factura.
 *
 * <p>La prueba fuerte es la ida y vuelta con todos los campos opcionales llenos. Si el XML que
 * sale de lo leído no es idéntico al que entró, el lector perdió algo.
 */
class FacturaReaderTest {

    private static final byte[] SECRETO = "un-secreto-de-al-menos-dieciseis-bytes".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("una factura con todos los opcionales vuelve del XML idéntica, byte por byte")
    void idaYVuelta() {
        var original = completa();
        var xml = ComprobanteWriter.xml(original);

        var leida = FacturaReader.INSTANCIA.read(xml);

        assertArrayEquals(xml, ComprobanteWriter.xml(leida), "el XML de la factura leída no coincide con el de origen");
    }

    @Test
    @DisplayName("se lee de un flujo, que es lo que entrega una respuesta HTTP")
    void desdeUnFlujo() {
        var original = completa();

        var leida = FacturaReader.INSTANCIA.read(new ByteArrayInputStream(ComprobanteWriter.xml(original)));

        assertArrayEquals(ComprobanteWriter.xml(original), ComprobanteWriter.xml(leida));
    }

    @Test
    @DisplayName("la clave de acceso se reconstruye igual, con su código numérico y su verificador")
    void claveAcceso() {
        var original = completa();

        var leida = FacturaReader.INSTANCIA.read(ComprobanteWriter.xml(original));

        assertEquals(original.claveAcceso(), leida.claveAcceso());
    }

    @Test
    @DisplayName("los importes se toman del archivo, no se recalculan")
    void importesDelArchivo() {
        var original = completa();
        var xml = new String(ComprobanteWriter.xml(original), StandardCharsets.UTF_8)
            .replace("<importeTotal>", "<importeTotal>")
            .replace("<totalSinImpuestos>170.00</totalSinImpuestos>", "<totalSinImpuestos>999.99</totalSinImpuestos>");
        assertTrue(xml.contains("999.99"), "el XML de prueba no se pudo alterar");

        var leida = FacturaReader.INSTANCIA.read(xml.getBytes(StandardCharsets.UTF_8));

        assertEquals(Dinero.of("999.99"), leida.info().totales().totalSinImpuestos());
    }

    @Test
    @DisplayName("la firma no estorba: un ds:Signature dentro del comprobante se ignora")
    void conFirma() {
        var original = completa();
        var xml = new String(ComprobanteWriter.xml(original), StandardCharsets.UTF_8)
            .replace("</factura>", "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignatureValue>x</ds:SignatureValue></ds:Signature></factura>");

        var leida = FacturaReader.INSTANCIA.read(xml.getBytes(StandardCharsets.UTF_8));

        assertEquals(original.claveAcceso(), leida.claveAcceso());
    }

    @Test
    @DisplayName("un XML que no es una factura falla diciendo qué raíz encontró")
    void otraRaiz() {
        var xml = "<notaCredito><infoTributaria/></notaCredito>".getBytes(StandardCharsets.UTF_8);

        var error = assertThrows(LecturaXmlException.class, () -> FacturaReader.INSTANCIA.read(xml));

        assertTrue(error.getMessage().contains("notaCredito"), error.getMessage());
    }

    @Test
    @DisplayName("un XML mal formado falla al leerse")
    void malFormado() {
        var xml = "<factura><infoTributaria>".getBytes(StandardCharsets.UTF_8);

        assertThrows(LecturaXmlException.class, () -> FacturaReader.INSTANCIA.read(xml));
    }

    @Test
    @DisplayName("si falta un elemento que el esquema exige, el error dice cuál")
    void faltaUnElemento() {
        var xml = new String(ComprobanteWriter.xml(completa()), StandardCharsets.UTF_8)
            .replaceAll("<razonSocialComprador>[^<]*</razonSocialComprador>", "");

        var error = assertThrows(LecturaXmlException.class, () -> FacturaReader.INSTANCIA.read(xml.getBytes(StandardCharsets.UTF_8)));

        assertTrue(error.getMessage().contains("razonSocialComprador"), error.getMessage());
    }

    @Test
    @DisplayName("lo que el archivo no trae, no se inventa")
    void opcionalesVacios() {
        var minima = Factura.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").build())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .detalle(DetalleComprobanteVenta.builder().descripcion("Consulta").cantidad(1).precioUnitario("100.00").iva(TarifaIva.QUINCE).build())
            .build();

        var leida = FacturaReader.INSTANCIA.read(ComprobanteWriter.xml(minima));

        assertAll(
            () -> assertArrayEquals(ComprobanteWriter.xml(minima), ComprobanteWriter.xml(leida)),
            () -> assertNull(leida.info().guiaRemision()),
            () -> assertNull(leida.maquinaFiscal()),
            () -> assertNull(leida.emisor().nombreComercial()),
            () -> assertTrue(leida.infoAdicional().isEmpty()),
            () -> assertTrue(leida.info().pagos().isEmpty())
        );
    }

    /**
     * Una factura con todos los campos que el esquema admite vacíos, llenos.
     */
    private static Factura completa() {
        var emisor = Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AV. AMAZONAS N34-451")
            .nombreComercial("PeluWare")
            .contribuyenteEspecial(new ContribuyenteEspecial("11245"))
            .agenteRetencion("1")
            .contribuyenteRimpe(true)
            .obligadoContabilidad(true)
            .build();

        return Factura.builder()
            .emisor(emisor)
            .comprador(new Comprador(TipoIdentificacion.RUC, "0992345678001", "DISTRIBUIDORA DEL SUR S.A.", "AV. QUITO Y BOLIVAR"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2, "SUCURSAL NORTE"))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .maquinaFiscal(new MaquinaFiscal("EPSON", "TM-U220", "ABC123"))
            .guiaRemision("001-002-000000045")
            .propina(Dinero.of("5.00"))
            .detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal("ALM-15")
                .codigoAuxiliar("7891234567890")
                .unidadMedida("saco")
                .descripcion("Alimento balanceado para perro adulto")
                .cantidad(2)
                .precioUnitario("50.00")
                .descuento(Dinero.of("10.00"))
                .detalleAdicional("marca", "PeluPet")
                .detalleAdicional("presentacion", "15 kg")
                .ice("3051", new BigDecimal("10.00"))
                .iva(TarifaIva.QUINCE)
                .build())
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Consulta veterinaria")
                .cantidad(1)
                .precioUnitario("80.00")
                .iva(TarifaIva.CERO)
                .build())
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("100.00")))
            .pago(Pago.aPlazo(FormaPago.OTROS_CON_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("98.85"), 30))
            .campoAdicional("Teléfono", "02 2345678")
            .campoAdicional("Email", "ventas@peluware.ec")
            .build();
    }
}
