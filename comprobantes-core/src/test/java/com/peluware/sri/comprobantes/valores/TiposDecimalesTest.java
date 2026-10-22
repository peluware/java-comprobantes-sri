package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.valores.Cantidad;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.PrecioUnitario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TiposDecimalesTest {

    @Test
    @DisplayName("rechaza un importe con más de dos decimales en vez de redondearlo")
    void rechazaImporteConTresDecimales() {
        assertEquals("importe", campoInvalido(() -> Dinero.of("1.005")));
    }

    @Test
    @DisplayName("rechaza importes y cantidades negativas")
    void rechazaNegativos() {
        assertEquals("importe", campoInvalido(() -> Dinero.of("-0.01")));
        assertEquals("cantidad", campoInvalido(() -> Cantidad.of("-1")));
        assertEquals("precioUnitario", campoInvalido(() -> PrecioUnitario.of("-1")));
    }

    @Test
    @DisplayName("rechaza un importe que no cabe en catorce dígitos")
    void rechazaImporteDemasiadoGrande() {
        assertEquals(Dinero.of("999999999999.99"), Dinero.of("999999999999.99"));

        assertEquals("importe", campoInvalido(() -> Dinero.of("1000000000000.00")));
    }

    @ParameterizedTest
    @DisplayName("redondear aplica redondeo comercial")
    @CsvSource({"1.004, 1.00", "1.005, 1.01", "1.006, 1.01", "2.675, 2.68", "0.005, 0.01"})
    void redondearEsHalfUp(String crudo, String esperado) {
        assertEquals(Dinero.of(esperado), Dinero.redondear(new BigDecimal(crudo)));
    }

    @Test
    @DisplayName("dos importes con el mismo valor y distinta escala son iguales")
    void normalizaLaEscala() {
        assertEquals(Dinero.of("5"), Dinero.of("5.00"));
        assertEquals(Dinero.of("5.0"), Dinero.of("5.00"));
        assertEquals(Dinero.of(5), Dinero.of("5.00"));
        assertEquals(Dinero.of("5.00").hashCode(), Dinero.of("5").hashCode());
    }

    @Test
    @DisplayName("una cantidad admite seis decimales y los conserva")
    void laCantidadAdmiteSeisDecimales() {
        assertEquals("2.542563", Cantidad.of("2.542563").toString());

        assertEquals("cantidad", campoInvalido(() -> Cantidad.of("2.5425631")));
    }

    @Test
    @DisplayName("restar por debajo de cero falla en vez de devolver un importe negativo")
    void restarPorDebajoDeCeroFalla() {
        assertEquals(Dinero.CERO, Dinero.of("5.00").menos(Dinero.of("5.00")));

        assertEquals("importe", campoInvalido(() -> Dinero.of("5.00").menos(Dinero.of("5.01"))));
    }

    @Test
    @DisplayName("el porcentaje redondea a dos decimales")
    void elPorcentajeRedondea() {
        assertEquals(Dinero.of("0.02"), Dinero.of("0.10").porcentaje(new BigDecimal("15.00")));
        assertEquals(Dinero.of("2.63"), Dinero.of("17.50").porcentaje(new BigDecimal("15.00")));
    }

    private static String campoInvalido(Executable accion) {
        var e = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo();
    }
}
