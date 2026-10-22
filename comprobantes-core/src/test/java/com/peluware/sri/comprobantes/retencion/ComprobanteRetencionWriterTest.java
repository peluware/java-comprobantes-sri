package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.Esquemas;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Secuencial;
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
 * Lo que el escritor del comprobante de retención hace distinto de los demás. Que su XML mínimo
 * valide contra el XSD lo comprueba {@code ComprobanteContratoTest}.
 */
class ComprobanteRetencionWriterTest {

    private static final Schema ESQUEMA = Esquemas.de("ComprobanteRetencion_V2.0.0.xsd");

    @Test
    @DisplayName("con todos los campos opcionales sigue validando contra el XSD")
    void conTodoLoOpcionalValida() {
        var completo = DatosDePrueba.comprobanteRetencionSinDocumentos()
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .emisor(DatosDePrueba.emisorCompleto())
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO_COMPLETO)
            .sujetoRetenido(new SujetoRetenido(TipoIdentificacion.RUC, "1790012345001", "PROVEEDOR DEL PACIFICO S.A.", TipoSujetoRetenido.SOCIEDAD, true))
            .maquinaFiscal(new MaquinaFiscal("SISPAU", "ABC1234", "CGMC1405"))
            .campoAdicional("Origen", "pruebas")
            .documentoSustento(DocumentoSustento.builder()
                .codigoSustento("01")
                .documento(TipoComprobante.FACTURA, DatosDePrueba.SERIE, Secuencial.of(45), DatosDePrueba.FECHA_EMISION.minusDays(2))
                .fechaRegistroContable(DatosDePrueba.FECHA_EMISION.minusDays(1))
                .numeroAutorizacion("1234567890")
                .pagoLocalExterior(DocumentoSustento.PagoLocalExterior.EXTERIOR)
                .exterior("01", "593")
                .aplicaConvenioDobleTributacion(true)
                .pagoExteriorSujetoRetencion(true)
                .pagoRegimenFiscalPreferente(true)
                .totales(Dinero.of("100.00"), Dinero.of("115.00"))
                .impuesto(Impuesto.of(CodigoImpuesto.IVA, TarifaIva.QUINCE.codigo(), TarifaIva.QUINCE.tarifa(), Dinero.of("100.00")))
                .retencion(Retencion.de(CodigoImpuestoRetencion.RENTA, "312", Dinero.of("100.00"), "1.75"))
                .retencion(Retencion.de(CodigoImpuestoRetencion.IVA, "721", Dinero.of("15.00"), "70.00"))
                .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("115.00")))
                .build())
            .build();

        byte[] xml = ComprobanteRetencionWriter.INSTANCIA.toByteArray(completo);
        try {
            ESQUEMA.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (Exception e) {
            fail("no cumple el XSD: " + e.getMessage() + "\n" + new String(xml, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("el número del documento sustento va sin guiones, con quince dígitos")
    void elNumeroVaSinGuiones() {
        var xml = texto(DatosDePrueba.comprobanteRetencion().build());

        assertTrue(xml.contains("<numDocSustento>001001000000045</numDocSustento>"), xml);
        assertFalse(xml.contains("001-001-000000045"), "este esquema es el único que no usa guiones: " + xml);
    }

    @Test
    @DisplayName("la contraparte se escribe como sujeto retenido")
    void laContraparteEsUnSujetoRetenido() {
        var xml = texto(DatosDePrueba.comprobanteRetencion().build());

        assertTrue(xml.contains("<tipoIdentificacionSujetoRetenido>04</tipoIdentificacionSujetoRetenido>"), xml);
        assertTrue(xml.contains("<razonSocialSujetoRetenido>PROVEEDOR DEL PACIFICO S.A.</razonSocialSujetoRetenido>"), xml);
        assertFalse(xml.contains("Comprador"), xml);
        assertFalse(xml.contains("Proveedor<"), xml);
    }

    @Test
    @DisplayName("el impuesto del documento usa los nombres de este esquema")
    void elImpuestoUsaSusNombres() {
        var xml = texto(DatosDePrueba.comprobanteRetencion().build());

        assertTrue(xml.contains("<codImpuestoDocSustento>2</codImpuestoDocSustento>"), xml);
        assertTrue(xml.contains("<valorImpuesto>15.00</valorImpuesto>"), xml);
    }

    @Test
    @DisplayName("la retención lleva su propio catálogo de impuesto")
    void laRetencionLlevaSuCatalogo() {
        var xml = texto(DatosDePrueba.comprobanteRetencion().build());

        assertTrue(xml.contains("<codigo>1</codigo>"), "renta es 1 en la Tabla 18, no en la de impuestos: " + xml);
        assertTrue(xml.contains("<codigoRetencion>312</codigoRetencion>"), xml);
        assertTrue(xml.contains("<porcentajeRetener>1.75</porcentajeRetener>"), xml);
        assertTrue(xml.contains("<valorRetenido>1.75</valorRetenido>"), xml);
    }

    @Test
    @DisplayName("las formas de pago no llevan plazo")
    void losPagosNoLlevanPlazo() {
        var xml = texto(DatosDePrueba.comprobanteRetencion().build());

        assertTrue(xml.contains("<formaPago>01</formaPago>"), xml);
        assertFalse(xml.contains("unidadTiempo"), xml);
        assertFalse(xml.contains("<plazo>"), xml);
    }

    @Test
    @DisplayName("las afirmaciones del exterior se omiten cuando no aplican")
    void lasAfirmacionesSeOmiten() {
        var xml = texto(DatosDePrueba.comprobanteRetencion().build());

        assertFalse(xml.contains("aplicConvDobTrib"), xml);
        assertFalse(xml.contains("pagExtSujRetNorLeg"), xml);
        assertFalse(xml.contains("pagoRegFis"), xml);
    }

    private static String texto(ComprobanteRetencion retencion) {
        return new String(ComprobanteRetencionWriter.INSTANCIA.toByteArray(retencion), StandardCharsets.UTF_8);
    }

}
