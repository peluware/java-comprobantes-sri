package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.valores.Serie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * El establecimiento agrupa los campos 5, 6 y 7 de la Tabla 11, que el esquema escribe sueltos
 * y en dos bloques distintos.
 */
class EstablecimientoTest {

    @Test
    @DisplayName("rellena los dos códigos con ceros a la izquierda")
    void rellenaConCeros() {
        var establecimiento = Establecimiento.of(2, 1);

        assertEquals("002", establecimiento.codigo());
        assertEquals("001", establecimiento.puntoEmision());
        assertNull(establecimiento.direccion());
    }

    @Test
    @DisplayName("la serie se deriva de sus dos códigos")
    void laSerieSeDeriva() {
        var establecimiento = Establecimiento.of(2, 7, "AV. NORTE");

        assertEquals(new Serie("002", "007"), establecimiento.serie());
        assertEquals("002007", establecimiento.serie().valor());
    }

    @Test
    @DisplayName("la dirección no entra en la serie, que es la clave")
    void laDireccionNoEntraEnLaSerie() {
        var conDireccion = Establecimiento.of(1, 1, "AV. NORTE");
        var sinDireccion = Establecimiento.of(1, 1);

        assertEquals(sinDireccion.serie(), conDireccion.serie());
    }

    @Test
    @DisplayName("conDireccion deja los códigos intactos")
    void conDireccionDejaLosCodigos() {
        var mudado = Establecimiento.of(3, 2).conDireccion("AV. SUR");

        assertEquals("003", mudado.codigo());
        assertEquals("002", mudado.puntoEmision());
        assertEquals("AV. SUR", mudado.direccion());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("rechaza un código que no sean tres dígitos")
    @ValueSource(strings = {"1", "0001", "00A", ""})
    void rechazaUnCodigoMalFormado(String codigo) {
        var e = assertThrows(ValidacionException.class, () -> new Establecimiento(codigo, "001", null));

        assertEquals(Serie.CAMPO_ESTABLECIMIENTO, assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("rechaza un punto de emisión que no sean tres dígitos")
    void rechazaUnPuntoDeEmisionMalFormado() {
        var e = assertThrows(ValidacionException.class, () -> new Establecimiento("001", "1", null));

        assertEquals(Serie.CAMPO_PUNTO_EMISION, assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("rechaza una dirección más larga de lo que admite el esquema")
    void rechazaUnaDireccionLarga() {
        var larga = "A".repeat(Establecimiento.DIRECCION_LONGITUD_MAXIMA + 1);

        var e = assertThrows(ValidacionException.class, () -> new Establecimiento("001", "001", larga));

        assertEquals(Establecimiento.CAMPO_DIRECCION, assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("se imprime como los dos códigos separados")
    void seImprimeComoLosDosCodigos() {
        assertEquals("002-001", Establecimiento.of(2, 1, "AV. NORTE").toString());
    }
}
