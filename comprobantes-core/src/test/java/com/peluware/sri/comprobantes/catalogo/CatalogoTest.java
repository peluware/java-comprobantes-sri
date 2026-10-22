package com.peluware.sri.comprobantes.catalogo;

import com.peluware.sri.comprobantes.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogoTest {

    @ParameterizedTest
    @DisplayName("Tabla 3: los códigos de tipo de comprobante van y vuelven")
    @CsvSource({"01, FACTURA", "03, LIQUIDACION_COMPRA", "04, NOTA_CREDITO", "05, NOTA_DEBITO", "06, GUIA_REMISION", "07, COMPROBANTE_RETENCION"})
    void tiposDeComprobante(String codigo, TipoComprobante esperado) {
        assertEquals(esperado, TipoComprobante.of(codigo));
        assertEquals(codigo, esperado.codigo());
    }

    @Test
    @DisplayName("Tabla 3: el código 02 no está asignado")
    void codigo02NoAsignado() {
        assertThrows(CatalogoException.class, () -> TipoComprobante.of("02"));
    }

    @ParameterizedTest
    @DisplayName("Tabla 6: los códigos de tipo de identificación van y vuelven")
    @CsvSource({"04, RUC", "05, CEDULA", "06, PASAPORTE", "07, CONSUMIDOR_FINAL", "08, IDENTIFICACION_EXTERIOR"})
    void tiposDeIdentificacion(String codigo, TipoIdentificacion esperado) {
        assertEquals(esperado, TipoIdentificacion.of(codigo));
        assertEquals(codigo, esperado.codigo());
    }

    @Test
    @DisplayName("la identificación de consumidor final son trece nueves")
    void consumidorFinalSonTreceNueves() {
        var identificacion = TipoIdentificacion.IDENTIFICACION_CONSUMIDOR_FINAL;

        assertEquals(13, identificacion.length());
        assertTrue(identificacion.chars().allMatch(c -> c == '9'), identificacion);
    }

    @ParameterizedTest
    @DisplayName("Tabla 4: los códigos de ambiente van y vuelven")
    @CsvSource({"1, PRUEBAS", "2, PRODUCCION"})
    void ambientes(String codigo, Ambiente esperado) {
        assertEquals(esperado, Ambiente.of(codigo));
        assertEquals(codigo, esperado.codigo());
    }

    @ParameterizedTest
    @DisplayName("el ambiente también se resuelve desde la palabra con tilde")
    @CsvSource({"PRUEBAS, PRUEBAS", "PRODUCCIÓN, PRODUCCION", "PRODUCCION, PRODUCCION", "  producción  , PRODUCCION"})
    void ambienteFromDescripcion(String descripcion, Ambiente esperado) {
        assertEquals(esperado, Ambiente.ofDescripcion(descripcion));
    }

    @Test
    @DisplayName("Tabla 2: el esquema offline solo define la emisión normal")
    void soloEmisionNormal() {
        assertEquals(TipoEmision.NORMAL, TipoEmision.of("1"));
        assertThrows(CatalogoException.class, () -> TipoEmision.of("2"));
    }

    @ParameterizedTest
    @DisplayName("Tabla 16: los códigos de impuesto van y vuelven")
    @CsvSource({"2, IVA", "3, ICE", "5, IRBPNR"})
    void codigosDeImpuesto(String codigo, CodigoImpuesto esperado) {
        assertEquals(esperado, CodigoImpuesto.of(codigo));
        assertEquals(codigo, esperado.codigo());
    }

    @Test
    @DisplayName("Tabla 16: el código 4 corresponde a retenciones, no a un impuesto del comprobante")
    void codigo4NoEsImpuestoDelComprobante() {
        assertThrows(CatalogoException.class, () -> CodigoImpuesto.of("4"));
    }

    @ParameterizedTest
    @DisplayName("Tabla 17: cada código de IVA lleva su porcentaje")
    @CsvSource({"0, 0.00", "2, 12.00", "3, 14.00", "4, 15.00", "5, 5.00", "6, 0.00", "7, 0.00", "10, 13.00"})
    void tarifasDeIva(String codigo, BigDecimal porcentaje) {
        var tarifa = TarifaIva.of(codigo);

        assertAll(
            () -> assertEquals(codigo, tarifa.codigo()),
            () -> assertEquals(porcentaje, tarifa.tarifa()),
            () -> assertTrue(tarifa.esConocida())
        );
    }

    @Test
    @DisplayName("Tabla 17: las constantes son las ocho de la tabla")
    void lasConstantesSonLaTabla() {
        assertEquals(
            List.of(TarifaIva.CERO, TarifaIva.CINCO, TarifaIva.DOCE, TarifaIva.TRECE, TarifaIva.CATORCE, TarifaIva.QUINCE, TarifaIva.NO_OBJETO, TarifaIva.EXENTO),
            TarifaIva.valores()
        );
    }

    @Test
    @DisplayName("Tabla 17: el IVA diferenciado no tiene porcentaje fijo y se construye con el suyo")
    void ivaDiferenciado() {
        assertThrows(CatalogoException.class, () -> TarifaIva.of(TarifaIva.CODIGO_DIFERENCIADO));

        var diferenciado = TarifaIva.de(TarifaIva.CODIGO_DIFERENCIADO, "8.00");

        assertAll(
            () -> assertEquals("8", diferenciado.codigo()),
            () -> assertEquals(new BigDecimal("8.00"), diferenciado.tarifa()),
            () -> assertFalse(diferenciado.esConocida())
        );
    }

    @Test
    @DisplayName("admite una tarifa de IVA que el SRI añada, porque el esquema no acota el catálogo")
    void admiteUnaTarifaFueraDeLaTabla() {
        var futura = TarifaIva.de("11", "16.00");

        assertAll(
            () -> assertEquals("11", futura.codigo()),
            () -> assertEquals(new BigDecimal("16.00"), futura.tarifa()),
            () -> assertFalse(futura.esConocida())
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("rechaza tarifas de IVA que el esquema no admite")
    @CsvSource({"12345, 15.00", "'', 15.00", "4A, 15.00", "4, 100.00", "4, 15.123", "4, -15.00"})
    void rechazaTarifaFueraDelEsquema(String codigo, BigDecimal porcentaje) {
        assertThrows(ValidacionException.class, () -> new TarifaIva(codigo, porcentaje));
    }

    @Test
    @DisplayName("todo porcentaje de la Tabla 17 cabe en el campo tarifa del esquema")
    void todaTarifaDeLaTablaCabeEnElEsquema() {
        for (var tarifa : TarifaIva.valores()) {
            var porcentaje = tarifa.tarifa();
            assertTrue(porcentaje.precision() - porcentaje.scale() <= 2, tarifa + " no cabe en dos dígitos enteros");
        }
    }

    @ParameterizedTest
    @DisplayName("Tabla 24: las formas de pago van y vuelven")
    @CsvSource({"01", "15", "16", "17", "18", "19", "20", "21"})
    void formasDePago(String codigo) {
        var forma = FormaPago.of(codigo);

        assertAll(
            () -> assertEquals(codigo, forma.codigo()),
            () -> assertTrue(forma.esConocida())
        );
    }

    @Test
    @DisplayName("Tabla 24: las constantes son las ocho de la tabla")
    void lasConstantesSonLaTablaDePagos() {
        assertEquals(
            List.of(
                FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO,
                FormaPago.COMPENSACION_DEUDAS,
                FormaPago.TARJETA_DEBITO,
                FormaPago.DINERO_ELECTRONICO,
                FormaPago.TARJETA_PREPAGO,
                FormaPago.TARJETA_CREDITO,
                FormaPago.OTROS_CON_UTILIZACION_SISTEMA_FINANCIERO,
                FormaPago.ENDOSO_TITULOS
            ),
            FormaPago.valores()
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("admite códigos de pago que el esquema acepta aunque no estén en la tabla")
    @CsvSource({"02", "14"})
    void admiteCodigosFueraDeLaTabla(String codigo) {
        var forma = FormaPago.of(codigo);

        assertAll(
            () -> assertEquals(codigo, forma.codigo()),
            () -> assertFalse(forma.esConocida())
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("rechaza códigos de pago que el esquema no admite")
    @CsvSource({"00", "22", "1", "001", "AB"})
    void rechazaCodigoDePagoFueraDelEsquema(String codigo) {
        assertThrows(ValidacionException.class, () -> FormaPago.of(codigo));
    }
}
