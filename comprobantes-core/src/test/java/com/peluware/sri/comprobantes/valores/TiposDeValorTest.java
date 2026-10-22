package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TiposDeValorTest {

    @Test
    @DisplayName("rechaza un secuencial sin rellenar con ceros")
    void rechazaSecuencialSinRellenar() {
        assertFalse(Secuencial.esValido("1"));
        assertTrue(Secuencial.esValido(Secuencial.of(1).valor()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("rechaza lo que el patrón numeroRuc del XSD no admite")
    @ValueSource(strings = {"0000000000002", "1790012345", "17900123450012", "179001234500A"})
    void rechazaRucQueNoCumpleElPatron(String ruc) {
        assertFalse(Ruc.esValido(ruc), ruc);

        assertTrue(Ruc.esValido("0000000000001"));
    }

    @Test
    @DisplayName("isValid devuelve false ante null en vez de lanzar")
    void isValidConNull() {
        assertFalse(Ruc.esValido(null));
        assertFalse(Secuencial.esValido(null));
        assertFalse(CodigoNumerico.esValido(null));
        assertFalse(Serie.esValido(null, "001"));
    }

    @Test
    @DisplayName("of rellena con ceros a la izquierda")
    void ofRellenaConCeros() {
        assertEquals("000052135", Secuencial.of(52135).valor());
        assertEquals("00000001", CodigoNumerico.of(1).valor());
        assertEquals("001901", Serie.of(1, 901).valor());
    }

    @Test
    @DisplayName("cada tipo señala su propio campo al fallar")
    void cadaTipoSenalaSuCampo() {
        assertEquals("ruc", campoInvalido(() -> new Ruc("1790012345")));
        assertEquals("secuencial", campoInvalido(() -> Secuencial.of(-5)));
        assertEquals("codigoNumerico", campoInvalido(() -> CodigoNumerico.of(123_456_789L)));
    }

    @Test
    @DisplayName("la serie señala cuál de los dos campos está mal")
    void serieSenalaElCampo() {
        assertEquals("estab", campoInvalido(() -> new Serie("1", "001")));
        assertEquals("ptoEmi", campoInvalido(() -> new Serie("001", "1")));
    }

    @Test
    @DisplayName("el mensaje técnico incluye el valor rechazado para poder diagnosticarlo")
    void elMensajeIncluyeElValorRechazado() {
        assertTrue(mensaje(() -> Secuencial.of(-5)).contains("-5"));
        assertTrue(mensaje(() -> CodigoNumerico.of(123_456_789L)).contains("123456789"));
    }

    private static String campoInvalido(Executable accion) {
        var e = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo();
    }

    private static String mensaje(Executable accion) {
        return assertThrows(ValidacionException.class, accion).getMessage();
    }
}
