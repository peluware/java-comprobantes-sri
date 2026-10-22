package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Lo propio de la línea de factura. La aritmética y los impuestos, que son comunes, están en
 * {@code DetalleContratoTest}.
 */
class DetalleComprobanteVentaTest {

    @Test
    @DisplayName("la unidad de medida es opcional")
    void laUnidadDeMedidaEsOpcional() {
        assertNull(minimo().build().unidadMedida());
    }

    @Test
    @DisplayName("conserva la unidad de medida indicada")
    void conservaLaUnidadDeMedida() {
        assertEquals("SACO", minimo().unidadMedida("SACO").build().unidadMedida());
    }

    @Test
    @DisplayName("rechaza una unidad de medida más larga de lo que admite el esquema")
    void rechazaUnidadDemasiadoLarga() {
        var larga = "X".repeat(DetalleComprobanteVenta.UNIDAD_MEDIDA_LONGITUD_MAXIMA + 1);

        var e = assertThrows(ValidacionException.class, () -> minimo().unidadMedida(larga).build());

        assertEquals("unidadMedida", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    private static DetalleComprobanteVenta.Builder minimo() {
        return DetalleComprobanteVenta.builder()
            .descripcion("Consulta veterinaria")
            .cantidad(1)
            .precioUnitario("100.00")
            .iva(TarifaIva.QUINCE);
    }
}
