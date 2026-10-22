package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El tipo {@code contribuyenteEspecial} es idéntico en los esquemas de factura, nota de crédito
 * y nota de débito, así que se valida y se prueba una sola vez.
 */
class ContribuyenteEspecialTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("admite entre tres y trece caracteres alfanuméricos")
    @ValueSource(strings = {"12", "12345678901234", "1234-5678", "1234 5678", ""})
    void rechazaLoMalFormado(String resolucion) {
        var e = assertThrows(ValidacionException.class, () -> new ContribuyenteEspecial(resolucion));

        assertEquals(ContribuyenteEspecial.CAMPO, assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("admite los extremos del rango")
    void admiteLosExtremos() {
        assertEquals("123", new ContribuyenteEspecial("123").valor());
        assertEquals("1234567890123", new ContribuyenteEspecial("1234567890123").valor());
    }

    @Test
    @DisplayName("admite letras mezcladas con dígitos")
    void admiteLetras() {
        assertEquals("ABC123", new ContribuyenteEspecial("ABC123").valor());
    }

    @Test
    @DisplayName("isValid no lanza nunca, tampoco con null")
    void isValidNoLanza() {
        assertTrue(ContribuyenteEspecial.esValido("5368"));
        assertFalse(ContribuyenteEspecial.esValido(null));
        assertFalse(ContribuyenteEspecial.esValido("12"));
    }

    @Test
    @DisplayName("of devuelve null cuando no se indica ninguno")
    void ofAdmiteNull() {
        assertNull(ContribuyenteEspecial.of(null));
        assertEquals("5368", ContribuyenteEspecial.of("5368").valor());
    }

    @Test
    @DisplayName("se imprime como su valor")
    void seImprimeComoSuValor() {
        assertEquals("5368", new ContribuyenteEspecial("5368").toString());
    }
}
