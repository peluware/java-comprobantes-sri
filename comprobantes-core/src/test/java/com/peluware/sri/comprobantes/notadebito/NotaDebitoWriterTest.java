package com.peluware.sri.comprobantes.notadebito;

import com.peluware.sri.comprobantes.Esquemas;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
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
 * Lo que el escritor de la nota de débito hace distinto de los demás. Que su XML mínimo valide
 * contra el XSD lo comprueba {@code ComprobanteContratoTest}.
 */
class NotaDebitoWriterTest {

    private static final Schema ESQUEMA = Esquemas.de("NotaDebito_V1.0.0.xsd");

    /** Anexo 13: el ejemplo de máquina fiscal de la ficha. */
    private static final MaquinaFiscal MAQUINA = new MaquinaFiscal("SISPAU", "ABC1234", "CGMC1405");


    @Test
    @DisplayName("con todos los campos opcionales sigue validando contra el XSD")
    void conTodoLoOpcionalValida() {
        var completa = DatosDePrueba.notaDebito()
            .emisor(DatosDePrueba.emisorCompleto())
            .establecimiento(DatosDePrueba.ESTABLECIMIENTO_COMPLETO)
            .rise("Contribuyente Régimen Simplificado RISE")
            .motivo("GASTOS ADMINISTRATIVOS", Dinero.of("10.00"))
            .pago(Pago.aPlazo(FormaPago.TARJETA_CREDITO, Dinero.of("57.50"), 30))
            .campoAdicional("Origen", "pruebas")
            .maquinaFiscal(MAQUINA)
            .build();

        byte[] xml = NotaDebitoWriter.INSTANCIA.toByteArray(completa);
        try {
            ESQUEMA.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (Exception e) {
            fail("no cumple el XSD: " + e.getMessage() + "\n" + new String(xml, StandardCharsets.UTF_8));
        }

    }

    @Test
    @DisplayName("los impuestos cuelgan de infoNotaDebito, no de un detalle")
    void losImpuestosNoVanEnUnDetalle() {
        var xml = texto(DatosDePrueba.notaDebito().build());

        assertFalse(xml.contains("<detalles>"), xml);
        assertTrue(xml.indexOf("<impuestos>") > xml.indexOf("<infoNotaDebito>"), xml);
        assertTrue(xml.indexOf("<impuestos>") < xml.indexOf("</infoNotaDebito>"), xml);
    }

    @Test
    @DisplayName("los motivos salen con su razón y su valor")
    void motivos() {
        var xml = texto(DatosDePrueba.notaDebito().build());

        assertTrue(xml.contains("<razon>INTERESES POR MORA</razon>"), xml);
        assertTrue(xml.contains("<valor>50.00</valor>"), xml);
    }

    @Test
    @DisplayName("no escribe moneda, que este esquema no declara")
    void noEscribeMoneda() {
        var xml = texto(DatosDePrueba.notaDebito().build());

        assertFalse(xml.contains("<moneda>"), xml);
    }

    private static String texto(NotaDebito nota) {
        return new String(NotaDebitoWriter.INSTANCIA.toByteArray(nota), StandardCharsets.UTF_8);
    }

}
