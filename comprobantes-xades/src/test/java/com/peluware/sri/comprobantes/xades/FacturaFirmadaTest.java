package com.peluware.sri.comprobantes.xades;

import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.firma.Firmador;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import com.peluware.sri.comprobantes.valores.ContribuyenteEspecial;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba que el XML que escribe el core se puede firmar y que el resultado firmado sigue
 * cumpliendo el esquema del SRI.
 */
class FacturaFirmadaTest {

    private static final Ruc RUC = new Ruc("0000000000001");
    private static final String RAZON_SOCIAL = "DISTRIBUIDORA FICTICIA S.A.";
    private static final String DIRECCION_MATRIZ = "AMAZONAS Y ROCA";
    private static final String CEDULA_COMPRADOR = "1710034065";
    private static final LocalDate FECHA_EMISION = LocalDate.of(2026, 10, 5);

    private static final Firmador FIRMADOR = new FirmadorXadesBes();

    private static final Schema ESQUEMA = compilarEsquema();

    /**
     * El esquema de la factura, de los recursos de este módulo.
     *
     * <p>No se toman del test-jar de core: sus recursos viven en un paquete que el jar principal
     * ya declara, y JPMS no deja que un paquete viva en dos módulos.
     */
    private static Schema compilarEsquema() {
        var xsd = FacturaFirmadaTest.class.getResource("/xsd/factura_V2.1.0.xsd");
        if (xsd == null) {
            throw new IllegalStateException("falta el XSD en los recursos de test");
        }
        try {
            var fabrica = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
            return fabrica.newSchema(new StreamSource(xsd.toExternalForm()));
        } catch (SAXException e) {
            throw new IllegalStateException("el esquema de la factura no compila", e);
        }
    }


    @Test
    @DisplayName("la factura escrita por el core se firma y el resultado valida contra el XSD")
    void escribirFirmarYValidar() {
        byte[] firmada = escribirYFirmar(factura());

        var xml = new String(firmada, Sri.CODIFICACION);

        assertTrue(xml.contains("<ds:Signature"), xml);
        validar(firmada);
    }

    @Test
    @DisplayName("la firma queda como último hijo de factura, que es donde el esquema la admite")
    void laFirmaVaAlFinal() {
        var xml = new String(escribirYFirmar(factura()), Sri.CODIFICACION);

        int signature = xml.indexOf("<ds:Signature");
        int detalles = xml.indexOf("</detalles>");
        int infoAdicional = xml.indexOf("</infoAdicional>");

        assertTrue(detalles < signature, "la firma debe ir después de los detalles");
        assertTrue(infoAdicional < signature, "la firma debe ir después de infoAdicional");
        assertTrue(xml.trim().endsWith("</factura>"), xml.substring(Math.max(0, xml.length() - 80)));
    }

    @Test
    @DisplayName("una factura con todos los bloques opcionales también se firma y valida")
    void facturaCompletaFirmadaValida() {
        var completa = Factura.builder()
            .emisor(Emisor.builder(RUC, RAZON_SOCIAL, DIRECCION_MATRIZ)
                .nombreComercial("LA TIENDA")
                
                .contribuyenteEspecial(new ContribuyenteEspecial("12345"))
                .obligadoContabilidad(true)
                .agenteRetencion("12345678")
                .contribuyenteRimpe(true)
                .build())
            .comprador(new Comprador(TipoIdentificacion.CEDULA, CEDULA_COMPRADOR, Comprador.RAZON_SOCIAL_PRUEBAS, "AV. 6 DE DICIEMBRE"))
            .fechaEmision(FECHA_EMISION)
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 1))
            .secuencial(Secuencial.of(1))
            .codigoNumerico(CodigoNumerico.of(1))
            .guiaRemision("001-001-000000001")
            .detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal("SKU-001")
                .descripcion("Alimento para perro")
                .unidadMedida("SACO")
                .cantidad(2)
                .precioUnitario("25.00")
                .descuento(Dinero.of("5.00"))
                .detalleAdicional("Lote", "L-2026-01")
                .iva(TarifaIva.QUINCE)
                .build())
            .propina(Dinero.of("4.00"))
            .pago(Pago.aPlazo(FormaPago.TARJETA_CREDITO, Dinero.of("55.75"), 30))
            .campoAdicional("Email", "cliente@example.com")
            .build();

        validar(escribirYFirmar(completa));
    }

    private static Factura factura() {
        return Factura.builder()
            .emisor(Emisor.of(RUC, RAZON_SOCIAL, DIRECCION_MATRIZ))
            .comprador(Comprador.consumidorFinal())
            .fechaEmision(FECHA_EMISION)
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 1))
            .secuencial(Secuencial.of(1))
            .codigoNumerico(CodigoNumerico.of(1))
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Consulta veterinaria")
                .cantidad(1)
                .precioUnitario("10.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .build();
    }

    private static byte[] escribirYFirmar(Factura factura) {
        var clave = new OrigenClavePkcs12(CertificadosDePrueba.rsa2048(), CertificadosDePrueba.CLAVE);

        return FIRMADOR.firmar(clave, factura).xml();
    }

    private static void validar(byte[] xml) {
        assertDoesNotThrow(() -> {
            try {
                ESQUEMA.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
            } catch (SAXException e) {
                throw new AssertionError(e.getMessage() + "\n" + new String(xml, Sri.CODIFICACION), e);
            }
        });
    }
}
