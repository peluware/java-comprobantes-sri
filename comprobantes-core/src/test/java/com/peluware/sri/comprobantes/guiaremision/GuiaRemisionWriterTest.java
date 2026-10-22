package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.Esquemas;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Lo que el escritor de la guía de remisión hace distinto de los demás. Que su XML mínimo valide
 * contra el XSD lo comprueba {@code ComprobanteContratoTest}.
 */
class GuiaRemisionWriterTest {

    private static final Schema ESQUEMA = Esquemas.de("GuiaRemision_V1.1.0.xsd");

    @Test
    @DisplayName("con todos los campos opcionales sigue validando contra el XSD")
    void conTodoLoOpcionalValida() {
        var completa = DatosDePrueba.guiaRemisionSinDestinatarios()
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .emisor(DatosDePrueba.emisorCompleto())
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO_COMPLETO)
            .rise("Contribuyente Régimen Simplificado RISE")
            .maquinaFiscal(new MaquinaFiscal("SISPAU", "ABC1234", "CGMC1405"))
            .campoAdicional("Origen", "pruebas")
            .destinatario(Destinatario.builder()
                .identificacion(DatosDePrueba.CEDULA_COMPRADOR)
                .razonSocial("CLIENTE DEL NORTE")
                .direccion("AV. NORTE Y LOS PINOS")
                .motivoTraslado("ENTREGA DE MERCADERIA VENDIDA")
                .documentoAduanero("DAU-00012345")
                .codigoEstablecimientoDestino("002")
                .ruta("QUITO - IBARRA")
                .sustentoTraslado(new SustentoTraslado(TipoComprobante.FACTURA, Serie.of(1, 1), Secuencial.of(45), "1234567890", DatosDePrueba.FECHA_EMISION))
                .detalle(DetalleGuiaRemision.builder()
                    .codigoInterno("SKU-001")
                    .codigoAdicional("ALT-001")
                    .descripcion("Alimento para perro 15kg")
                    .cantidad(2)
                    .detalleAdicional("Lote", "L-2026-01")
                    .build())
                .build())
            .build();

        byte[] xml = GuiaRemisionWriter.INSTANCIA.toByteArray(completa);
        try {
            ESQUEMA.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (Exception e) {
            fail("no cumple el XSD: " + e.getMessage() + "\n" + new String(xml, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("no escribe el elemento fechaEmision, que su esquema no declara")
    void noEscribeFechaEmision() {
        var xml = texto(DatosDePrueba.guiaRemision().build());

        assertFalse(xml.contains("<fechaEmision>"), xml);
    }

    @Test
    @DisplayName("en su lugar escribe el rango de fechas del transporte")
    void escribeElRangoDelTransporte() {
        var xml = texto(DatosDePrueba.guiaRemision().build());

        assertTrue(xml.contains("<fechaIniTransporte>05/10/2026</fechaIniTransporte>"), xml);
        assertTrue(xml.contains("<fechaFinTransporte>06/10/2026</fechaFinTransporte>"), xml);
    }

    @Test
    @DisplayName("no escribe importes en ninguna parte")
    void noEscribeImportes() {
        var xml = texto(DatosDePrueba.guiaRemision().build());

        assertFalse(xml.contains("precioUnitario"), xml);
        assertFalse(xml.contains("totalSinImpuestos"), xml);
        assertFalse(xml.contains("impuesto"), xml);
        assertFalse(xml.contains("importeTotal"), xml);
    }

    @Test
    @DisplayName("cada destinatario lleva sus propios detalles")
    void cadaDestinatarioLlevaSusDetalles() {
        var xml = texto(DatosDePrueba.guiaRemisionSinDestinatarios()
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .destinatario(Destinatario.builder().identificacion("1710034065").razonSocial("UNO").direccion("CALLE UNO").motivoTraslado("VENTA")
                .detalle(DetalleGuiaRemision.builder().descripcion("Primero").cantidad(1).build()).build())
            .destinatario(Destinatario.builder().identificacion("1790012345001").razonSocial("DOS").direccion("CALLE DOS").motivoTraslado("VENTA")
                .detalle(DetalleGuiaRemision.builder().descripcion("Segundo").cantidad(3).build()).build())
            .build());

        assertEquals(2, contar(xml, "<destinatario>"), xml);
        assertEquals(2, contar(xml, "<detalles>"), xml);
        assertTrue(xml.indexOf("Primero") < xml.indexOf("DOS"), "los detalles del primero van antes del segundo destinatario: " + xml);
    }

    @Test
    @DisplayName("el documento de sustento se omite si no se indica")
    void elDocumentoDeSustentoEsOpcional() {
        var xml = texto(DatosDePrueba.guiaRemision().build());

        assertFalse(xml.contains("codDocSustento"), xml);
        assertFalse(xml.contains("numDocSustento"), xml);
    }

    private static int contar(String xml, String aguja) {
        int total = 0;
        for (int i = xml.indexOf(aguja); i >= 0; i = xml.indexOf(aguja, i + 1)) {
            total++;
        }
        return total;
    }

    private static String texto(GuiaRemision guia) {
        return new String(GuiaRemisionWriter.INSTANCIA.toByteArray(guia), StandardCharsets.UTF_8);
    }

}
