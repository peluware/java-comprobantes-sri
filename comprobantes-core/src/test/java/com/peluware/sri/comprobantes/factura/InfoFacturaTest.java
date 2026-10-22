package com.peluware.sri.comprobantes.factura;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.factura.InfoFactura;
import com.peluware.sri.comprobantes.factura.TotalesFactura;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validaciones propias del bloque, que el esquema expresa como patrones.
 *
 * <p>La regla de que las formas de pago sumen el importe total se prueba desde
 * {@link FacturaTest}, que es el camino por el que se construye.
 */
class InfoFacturaTest {

    private static final TotalesFactura TOTALES = TotalesFactura.de(List.of(DetalleComprobanteVenta.builder()
        .descripcion("Consulta veterinaria")
        .cantidad(1)
        .precioUnitario("20.00")
        .iva(TarifaIva.QUINCE)
        .build()));

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("la guía de remisión exige el formato 000-000-000000000")
    @ValueSource(strings = {"1-1-1", "001001000000001", "001-001-00000001", "001-001-0000000012", "ABC-001-000000001"})
    void rechazaGuiaDeRemisionMalFormada(String guia) {
        assertEquals("guiaRemision", campoInvalido(() -> conGuia(guia)));
    }

    @Test
    @DisplayName("admite una guía de remisión bien formada")
    void admiteGuiaBienFormada() {
        assertEquals("001-001-000000001", conGuia("001-001-000000001").guiaRemision());
    }

    @Test
    @DisplayName("rechaza una moneda de más de quince caracteres")
    void rechazaMonedaDemasiadoLarga() {
        var larga = "X".repeat(Sri.MONEDA_LONGITUD_MAXIMA + 1);

        assertEquals("moneda", campoInvalido(() -> info(null, larga)));
    }

    @Test
    @DisplayName("sin formas de pago no comprueba la suma")
    void sinPagosNoComprueba() {
        assertTrue(info(null, Sri.MONEDA).pagos().isEmpty());
    }

    private static InfoFactura conGuia(String guia) {
        return info(guia, Sri.MONEDA);
    }

    private static InfoFactura info(@Nullable String guia, String moneda) {
        return new InfoFactura(DatosDePrueba.FECHA_EMISION, DatosDePrueba.comprador(), guia, TOTALES, moneda, List.of());
    }

    private static String campoInvalido(Executable accion) {
        var e = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo();
    }
}
