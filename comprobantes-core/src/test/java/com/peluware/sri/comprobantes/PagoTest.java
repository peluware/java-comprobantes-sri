package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PagoTest {

    private static final Dinero TOTAL = Dinero.of("115.00");

    @Test
    @DisplayName("of construye un pago sin plazo")
    void sinPlazo() {
        var pago = Pago.of(FormaPago.TARJETA_CREDITO, TOTAL);

        assertAll(
            () -> assertEquals(TOTAL, pago.total()),
            () -> assertNull(pago.plazo()));
    }

    @Test
    @DisplayName("aPlazo expresa el plazo en días con dos decimales")
    void aPlazoEnDias() {
        var pago = Pago.aPlazo(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, TOTAL, 30);

        var plazo = pago.plazo();
        assertAll(
            () -> assertEquals(new BigDecimal("30.00"), noNulo(plazo).valor()),
            () -> assertEquals(Plazo.DIAS, noNulo(plazo).unidadTiempo()));
    }

    @Test
    @DisplayName("rechaza un plazo negativo")
    void rechazaPlazoNegativo() {
        var e = assertThrows(ValidacionException.class, () -> new Plazo(new BigDecimal("-1"), Plazo.DIAS));

        assertEquals("plazo", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("rechaza una unidad de tiempo de más de diez caracteres")
    void rechazaUnidadDemasiadoLarga() {
        var larga = "X".repeat(Plazo.UNIDAD_TIEMPO_LONGITUD_MAXIMA + 1);

        var e = assertThrows(ValidacionException.class, () -> new Plazo(BigDecimal.TEN, larga));

        assertEquals("unidadTiempo", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("rechaza una unidad de tiempo vacía")
    void rechazaUnidadVacia() {
        var e = assertThrows(ValidacionException.class, () -> new Plazo(BigDecimal.TEN, ""));

        assertEquals("unidadTiempo", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    private static Plazo noNulo(@Nullable Plazo plazo) {
        return Objects.requireNonNull(plazo, "el pago debía llevar plazo");
    }
}
