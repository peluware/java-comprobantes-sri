package com.peluware.sri.comprobantes.interno;

import com.peluware.sri.comprobantes.ClaveAcceso;
import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.interno.DigitoVerificador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DigitoVerificadorTest {

    @Test
    @DisplayName("ejemplo oficial de la Ficha Técnica v2.34, numeral 5.2")
    void ejemploOficialDeLaFicha() {
        // 4x3 + 1x2 + 2x7 + 6x6 + 1x5 + 5x4 + 3x3 + 3x2 = 104; 104 mod 11 = 5; 11 - 5 = 6
        assertEquals('6', DigitoVerificador.modulo11("41261533"));
    }

    @ParameterizedTest(name = "{1} cierra {0}")
    @DisplayName("claves de 48 dígitos")
    @CsvSource({
            "051020260100000000000011001001000000001000000011, 4",
            "311220260100000000000012002005000123456876543211, 2",
    })
    void clavesCompletas(String primeros48, char esperado) {
        assertEquals(esperado, DigitoVerificador.modulo11(primeros48));
    }

    @Test
    @DisplayName("un resultado de 11 devuelve 0")
    void onceDevuelveCero() {
        // 0 x 2 = 0; 0 mod 11 = 0; 11 - 0 = 11 -> 0
        assertEquals('0', DigitoVerificador.modulo11("0"));
    }

    @Test
    @DisplayName("un resultado de 10 devuelve 1")
    void diezDevuelveUno() {
        // 6 x 2 = 12; 12 mod 11 = 1; 11 - 1 = 10 -> 1
        assertEquals('1', DigitoVerificador.modulo11("6"));
    }

    @Test
    @DisplayName("detecta la transposición de dos dígitos contiguos")
    void detectaTransposicion() {
        var original = DatosDePrueba.CLAVE_ACCESO.substring(0, ClaveAcceso.LONGITUD_SIN_DV);
        var transpuesta = original.charAt(1) + "" + original.charAt(0) + original.substring(2);

        assertNotEquals(DigitoVerificador.modulo11(original), DigitoVerificador.modulo11(transpuesta));
    }

    @Test
    @DisplayName("rechaza una letra en lugar de un dígito")
    void rechazaLetras() {
        var conLetra = "O" + DatosDePrueba.CLAVE_ACCESO.substring(1, ClaveAcceso.LONGITUD_SIN_DV);

        assertThrows(ValidacionException.class, () -> DigitoVerificador.modulo11(conLetra));
    }

    @Test
    @DisplayName("rechaza la cadena vacía en lugar de devolver 0")
    void rechazaCadenaVacia() {
        // Sin la comprobación, la suma sería 0 y devolvería '0' en silencio.
        assertThrows(ValidacionException.class, () -> DigitoVerificador.modulo11(""));
    }
}
