package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompradorTest {

    @Test
    @DisplayName("consumidorFinal usa el tipo 07 y los trece nueves del numeral 9.10")
    void consumidorFinal() {
        var comprador = Comprador.consumidorFinal();

        assertAll(
            () -> assertEquals(TipoIdentificacion.CONSUMIDOR_FINAL, comprador.tipoIdentificacion()),
            () -> assertEquals(TipoIdentificacion.IDENTIFICACION_CONSUMIDOR_FINAL, comprador.identificacion()),
            () -> assertEquals(Comprador.RAZON_SOCIAL_CONSUMIDOR_FINAL, comprador.razonSocial()),
            () -> assertTrue(comprador.esConsumidorFinal()),
            () -> assertNull(comprador.direccion())
        );
    }

    @Test
    @DisplayName("un comprador identificado no es consumidor final")
    void identificadoNoEsConsumidorFinal() {
        var comprador = Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ");

        assertFalse(comprador.esConsumidorFinal());
    }

    @ParameterizedTest(name = "[{index}]")
    @DisplayName("rechaza identificaciones que el XSD no admite")
    @ValueSource(strings = {"", "123456789012345678901", "17100\n34065"})
    void rechazaIdentificacionInvalida(String identificacion) {
        assertEquals("identificacionComprador", campoInvalido(() -> Comprador.of(TipoIdentificacion.CEDULA, identificacion, "JUAN PEREZ")));
    }

    @Test
    @DisplayName("la identificación admite hasta veinte caracteres")
    void identificacionHastaVeinte() {
        var veinte = "1".repeat(Comprador.IDENTIFICACION_LONGITUD_MAXIMA);

        assertEquals(veinte, Comprador.of(TipoIdentificacion.IDENTIFICACION_EXTERIOR, veinte, "CLIENTE DEL EXTERIOR").identificacion());
    }

    @Test
    @DisplayName("la razón social es obligatoria")
    void razonSocialObligatoria() {
        assertEquals("razonSocialComprador", campoInvalido(() -> Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "")));
    }

    @Test
    @DisplayName("rechaza una razón social de más de 300 caracteres")
    void rechazaRazonSocialDemasiadoLarga() {
        var larga = "X".repeat(Comprador.RAZON_SOCIAL_LONGITUD_MAXIMA + 1);

        assertEquals("razonSocialComprador", campoInvalido(() -> Comprador.of(TipoIdentificacion.CEDULA, "1710034065", larga)));
    }

    @Test
    @DisplayName("la dirección es opcional pero no admite saltos de línea")
    void direccionOpcional() {
        assertNull(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ").direccion());

        assertEquals("direccionComprador", campoInvalido(() -> new Comprador(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ", "AV. AMAZONAS\nN34")));
    }

    private static String campoInvalido(Executable accion) {
        var e = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo();
    }
}
