package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.notacredito.DetalleNotaCredito;
import com.peluware.sri.comprobantes.valores.Cantidad;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lo que cumple todo {@link Detalle}, sea de la factura o de la nota de crédito.
 *
 * <p>Aquí vive la aritmética de la línea y la cascada de impuestos de la Tabla 21, que es
 * común. Un tipo nuevo con detalles se añade en {@link #tipos()}.
 *
 * <p>Lo propio de cada tipo, como la unidad de medida de la factura, se prueba en el paquete
 * de ese tipo.
 */
class DetalleContratoTest {

    record Tipo(String nombre, Supplier<Detalle.Builder<?, ?>> builder) {

        Detalle.Builder<?, ?> nuevo() {
            return builder.get();
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    static List<Tipo> tipos() {
        return List.of(
            new Tipo("factura", DetalleComprobanteVenta::builder),
            new Tipo("liquidación de compra", DetalleComprobanteVenta::builder),
            new Tipo("nota de crédito", DetalleNotaCredito::builder));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("reproduce el detalle del ejemplo de comercio exterior de la ficha")
    void reproduceElEjemploDeLaFicha(Tipo tipo) {
        var detalle = tipo.nuevo()

            .descripcion("FROZEN MOONFISH WR")
            .cantidad(Cantidad.of("2.542563"))
            .precioUnitario("25.542365")
            .iva(TarifaIva.CERO)
            .build();

        assertEquals(Dinero.of("64.94"), detalle.precioTotalSinImpuesto());
        assertEquals(Dinero.of("64.94"), detalle.impuestos().getFirst().baseImponible());
        assertEquals(Dinero.CERO, detalle.impuestos().getFirst().valor());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("el precio total redondea el producto a dos decimales")
    void redondeaElProducto(Tipo tipo) {
        var detalle = tipo.nuevo()
            .descripcion("Tornillos")
            .cantidad(3)
            .precioUnitario("0.335")
            .iva(TarifaIva.QUINCE)
            .build();

        assertEquals(Dinero.of("1.01"), detalle.precioTotalSinImpuesto());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("el descuento se resta antes de calcular los impuestos")
    void elDescuentoSeRestaAntes(Tipo tipo) {
        var detalle = tipo.nuevo()
            .descripcion("Consulta veterinaria")
            .cantidad(1)
            .precioUnitario("100.00")
            .descuento(Dinero.of("10.00"))
            .iva(TarifaIva.QUINCE)
            .build();

        assertEquals(Dinero.of("90.00"), detalle.precioTotalSinImpuesto());
        assertEquals(Dinero.of("90.00"), detalle.impuestos().getFirst().baseImponible());
        assertEquals(Dinero.of("13.50"), detalle.impuestos().getFirst().valor());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("el IVA se calcula sobre el precio total más el ICE, según la Tabla 21")
    void elIvaIncluyeElIceEnSuBase(Tipo tipo) {
        var detalle = conIceYIva(tipo);

        var iva = impuesto(detalle, CodigoImpuesto.IVA);
        var ice = impuesto(detalle, CodigoImpuesto.ICE);

        assertEquals(Dinero.of("10.00"), ice.baseImponible());
        assertEquals(Dinero.of("7.50"), ice.valor());
        assertEquals(Dinero.of("17.50"), iva.baseImponible());
        assertEquals(Dinero.of("2.63"), iva.valor());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("los impuestos salen ordenados por código de impuesto")
    void losImpuestosSalenOrdenados(Tipo tipo) {
        var detalle = conIceYIva(tipo);

        assertEquals(CodigoImpuesto.IVA, detalle.impuestos().get(0).codigo());
        assertEquals(CodigoImpuesto.ICE, detalle.impuestos().get(1).codigo());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("un impuesto explícito no entra en la base del IVA")
    void elImpuestoExplicitoNoEntraEnLaCascada(Tipo tipo) {
        var detalle = tipo.nuevo()
            .descripcion("Agua en botella plastica")
            .cantidad(1)
            .precioUnitario("10.00")
            .impuesto(new Impuesto(CodigoImpuesto.IRBPNR, "5001", new BigDecimal("0.02"), Dinero.of("10.00"), Dinero.of("0.02")))
            .iva(TarifaIva.QUINCE)
            .build();

        assertEquals(Dinero.of("10.00"), impuesto(detalle, CodigoImpuesto.IVA).baseImponible());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("el IVA diferenciado se construye con su porcentaje, que no está en la tabla")
    void elIvaDiferenciadoLlevaSuPorcentaje(Tipo tipo) {
        var detalle = tipo.nuevo()
            .descripcion("Consulta")
            .cantidad(1)
            .precioUnitario("10.00")
            .iva(TarifaIva.de(TarifaIva.CODIGO_DIFERENCIADO, "8.00"))
            .build();

        assertEquals("8", detalle.impuestos().getFirst().codigoPorcentaje());
        assertEquals(Dinero.of("0.80"), detalle.impuestos().getFirst().valor());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("rechaza un descuento mayor que el precio de la línea")
    void rechazaDescuentoExcesivo(Tipo tipo) {
        assertEquals("descuento", campoInvalido(() -> tipo.nuevo()
            .descripcion("Consulta")
            .cantidad(1)
            .precioUnitario("10.00")
            .descuento(Dinero.of("10.01"))
            .iva(TarifaIva.QUINCE)
            .build()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("un detalle sin impuestos se puede construir: lo exige el comprobante, no la línea")
    void admiteDetalleSinImpuestos(Tipo tipo) {
        var detalle = tipo.nuevo()
            .descripcion("Consulta")
            .cantidad(1)
            .precioUnitario("10.00")
            .build();

        assertEquals(Dinero.of("10.00"), detalle.precioTotalSinImpuesto());
        assertTrue(detalle.impuestos().isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("rechaza más de tres datos adicionales")
    void rechazaMasDeTresDatosAdicionales(Tipo tipo) {
        var builder = tipo.nuevo()
            .descripcion("Consulta")
            .cantidad(1)
            .precioUnitario("10.00")
            .iva(TarifaIva.QUINCE)
            .detalleAdicional("a", "1")
            .detalleAdicional("b", "2")
            .detalleAdicional("c", "3")
            .detalleAdicional("d", "4");

        assertEquals("detallesAdicionales", campoInvalido(builder::build));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("rechaza una descripción más larga de lo que admite el esquema")
    void rechazaDescripcionDemasiadoLarga(Tipo tipo) {
        var larga = "X".repeat(Detalle.DESCRIPCION_LONGITUD_MAXIMA + 1);

        assertEquals("descripcion", campoInvalido(() -> tipo.nuevo()
            .descripcion(larga)
            .cantidad(1)
            .precioUnitario("10.00")
            .iva(TarifaIva.QUINCE)
            .build()));
    }

    private static Detalle conIceYIva(Tipo tipo) {
        return tipo.nuevo()
            .descripcion("Cerveza industrial")
            .cantidad(1)
            .precioUnitario("10.00")
            .ice("3077", new BigDecimal("75.00"))
            .iva(TarifaIva.QUINCE)
            .build();
    }

    private static Impuesto impuesto(Detalle detalle, CodigoImpuesto codigo) {
        return detalle.impuestos().stream().filter(i -> i.codigo() == codigo).findFirst().orElseThrow();
    }

    private static String campoInvalido(Executable accion) {
        var e = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo();
    }
}
