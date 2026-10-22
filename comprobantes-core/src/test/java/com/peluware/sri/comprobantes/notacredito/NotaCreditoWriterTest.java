package com.peluware.sri.comprobantes.notacredito;

import com.peluware.sri.comprobantes.Esquemas;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
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
 * Lo que el escritor de la nota de crédito hace distinto del de la factura. Que su XML mínimo
 * valide contra el XSD lo comprueba {@code ComprobanteContratoTest}.
 */
class NotaCreditoWriterTest {

    private static final Schema ESQUEMA = Esquemas.de("NotaCredito_V1.1.0.xsd");

    /** Anexo 13: el ejemplo de máquina fiscal de la ficha. */
    private static final MaquinaFiscal MAQUINA = new MaquinaFiscal("SISPAU", "ABC1234", "CGMC1405");


    @Test
    @DisplayName("con todos los campos opcionales sigue validando contra el XSD")
    void conTodoLoOpcionalValida() {
        var completa = DatosDePrueba.notaCredito()
            .emisor(DatosDePrueba.emisorCompleto())
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO_COMPLETO)
            .rise("Contribuyente Régimen Simplificado RISE")
            .campoAdicional("Origen", "pruebas")
            .maquinaFiscal(MAQUINA)
            .detalle(DetalleNotaCredito.builder()
                .codigoInterno("SKU-001")
                .codigoAdicional("ALT-001")
                .descripcion("Alimento para perro 15kg")
                .cantidad(2)
                .precioUnitario("25.00")
                .descuento(Dinero.of("5.00"))
                .detalleAdicional("Marca", "Genérica")
                .iva(TarifaIva.QUINCE)
                .build())
            .build();

        byte[] xml = NotaCreditoWriter.INSTANCIA.toByteArray(completa);
        try {
            ESQUEMA.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (Exception e) {
            fail("no cumple el XSD: " + e.getMessage() + "\n" + new String(xml, StandardCharsets.UTF_8));
        }

    }

    @Test
    @DisplayName("los códigos del detalle se escriben con los nombres de este esquema")
    void losCodigosUsanElNombreDeEsteEsquema() {
        var nota = DatosDePrueba.notaCredito()
            .detalle(DetalleNotaCredito.builder()
                .codigoInterno("SKU-001")
                .codigoAdicional("ALT-001")
                .descripcion("Alimento para perro 15kg")
                .cantidad(1)
                .precioUnitario("10.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .build();

        var xml = texto(nota);

        assertTrue(xml.contains("<codigoInterno>SKU-001</codigoInterno>"), xml);
        assertTrue(xml.contains("<codigoAdicional>ALT-001</codigoAdicional>"), xml);
        assertFalse(xml.contains("codigoPrincipal"), xml);
        assertFalse(xml.contains("codigoAuxiliar"), xml);
    }

    @Test
    @DisplayName("el total de impuestos no lleva tarifa, que este esquema no declara")
    void elTotalNoLlevaTarifa() {
        var xml = texto(DatosDePrueba.notaCredito().build());

        var total = xml.substring(xml.indexOf("<totalConImpuestos>"), xml.indexOf("</totalConImpuestos>"));
        assertFalse(total.contains("<tarifa>"), total);
        assertTrue(xml.contains("<tarifa>"), "el impuesto del detalle sí la lleva");
    }

    @Test
    @DisplayName("el documento modificado se escribe con el formato del esquema")
    void documentoModificado() {
        var xml = texto(DatosDePrueba.notaCredito().build());

        assertTrue(xml.contains("<codDocModificado>01</codDocModificado>"), xml);
        assertTrue(xml.contains("<numDocModificado>001-001-000000045</numDocModificado>"), xml);
        assertTrue(xml.contains("<fechaEmisionDocSustento>03/10/2026</fechaEmisionDocSustento>"), xml);
    }

    private static String texto(NotaCredito nota) {
        return new String(NotaCreditoWriter.INSTANCIA.toByteArray(nota), StandardCharsets.UTF_8);
    }

}
