package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.EstrategiaCodigoNumerico;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La sección «Imprimirla» del README, copiada literalmente.
 *
 * <p>Falla cuando un rename deja el README apuntando a un API que ya no existe.
 */
class EjemploDelReadmeTest {

    private static final byte[] SECRETO = "un-secreto-de-al-menos-dieciseis-bytes".getBytes(StandardCharsets.UTF_8);
    private static final OffsetDateTime FECHA_AUTORIZACION = OffsetDateTime.of(2026, 10, 20, 11, 5, 0, 0, ZoneOffset.ofHours(-5));

    @Test
    @DisplayName("el ejemplo de escribir el RIDE compila y produce un PDF")
    void escribir() {
        var factura = factura();

        var generador = new RidePdfBox();

        var salida = new ByteArrayOutputStream();
        generador.escribir(factura, FECHA_AUTORIZACION, salida);

        assertTrue(salida.size() > 1000, "el PDF sale vacío: " + salida.size() + " bytes");
    }

    @Test
    @DisplayName("el ejemplo sin fecha de autorización compila")
    void sinAutorizacion() {
        var generador = new RidePdfBox();

        var salida = new ByteArrayOutputStream();
        generador.escribir(factura(), salida);

        assertTrue(salida.size() > 1000, "el PDF sale vacío: " + salida.size() + " bytes");
    }

    @Test
    @DisplayName("el ejemplo del estilo compila")
    void estilo() {
        var estilo = EstiloRide.builder()
            .logo(logo())
            .paleta(Paleta.deMarca(0x1F4E79))
            .familia(FamiliaTipografica.HELVETICA)
            .codigoBarras(true)
            .build();

        var generador = new RidePdfBox(estilo);

        var salida = new ByteArrayOutputStream();
        generador.escribir(factura(), FECHA_AUTORIZACION, salida);

        assertTrue(salida.size() > 1000, "el PDF sale vacío: " + salida.size() + " bytes");
    }

    private static Factura factura() {
        return Factura.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").obligadoContabilidad(true).build())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Alimento para perro 15kg")
                .cantidad(2)
                .precioUnitario("25.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("57.50")))
            .build();
    }

    private static byte[] logo() {
        try (var entrada = EjemploDelReadmeTest.class.getResourceAsStream("/logo-cuadrado.jpg")) {
            assertNotNull(entrada, "falta el recurso logo-cuadrado.jpg");
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
