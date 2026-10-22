package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.notacredito.DetalleNotaCredito;
import com.peluware.sri.comprobantes.notacredito.NotaCredito;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencion;
import com.peluware.sri.comprobantes.notadebito.NotaDebito;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompra;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemision;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El ejemplo del README, copiado literalmente.
 *
 * <p>Falla cuando un rename deja el README apuntando a un API que ya no existe, o cuando los
 * importes que anuncia dejan de cuadrar.
 */
class EjemploDelReadmeTest {

    private static final byte[] SECRETO = "un-secreto-de-al-menos-dieciseis-bytes".getBytes(StandardCharsets.UTF_8);

    private static Emisor emisor() {
        return Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA")
            .obligadoContabilidad(true)
            .build();
    }

    @Test
    @DisplayName("el ejemplo de la factura compila y totaliza 51,75")
    void factura() {

        var emisor = Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA")
            .obligadoContabilidad(true)
            .build();

        var factura = Factura.builder()
            .emisor(emisor)
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 1, "AMAZONAS Y ROCA"))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal("SKU-001")
                .descripcion("Alimento para perro 15kg")
                .unidadMedida("SACO")
                .cantidad(2)
                .precioUnitario("25.00")
                .descuento(Dinero.of("5.00"))
                .iva(TarifaIva.QUINCE)
                .build())
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("51.75")))
            .build();

        assertEquals(Dinero.of("51.75"), factura.info().totales().importeTotal());
        assertTrue(ClaveAcceso.esValido(factura.claveAcceso()), factura.claveAcceso());
        assertTrue(new String(ComprobanteWriter.xml(factura), Sri.CODIFICACION).contains("<importeTotal>51.75</importeTotal>"));
    }

    @Test
    @DisplayName("el ejemplo de la nota de crédito referencia a su factura por serie y secuencial")
    void notaCredito() {
        var emisor = emisor();
        var factura = facturaDelEjemplo();

        var notaCredito = NotaCredito.builder()
            .emisor(emisor)
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 1, "AMAZONAS Y ROCA"))
            .secuencial(Secuencial.of(7))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .documentoModificado(new DocumentoModificado(
                TipoComprobante.FACTURA,
                factura.serie(),
                factura.secuencial(),
                factura.fechaEmision()))
            .motivo("Devolución del saco de alimento")
            .detalle(DetalleNotaCredito.builder()
                .codigoInterno("SKU-001")
                .descripcion("Alimento para perro 15kg")
                .cantidad(1)
                .precioUnitario("25.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .build();

        var xml = new String(ComprobanteWriter.xml(notaCredito), Sri.CODIFICACION);
        assertTrue(xml.contains("<numDocModificado>001-001-000000130</numDocModificado>"), xml);
    }

    @Test
    @DisplayName("el ejemplo de leer compila y despacha al tipo que toca")
    void leer() {
        var original = facturaDelEjemplo();

        var comprobante = ComprobanteReader.comprobante(new ByteArrayInputStream(ComprobanteWriter.xml(original)));

        var registrado = switch (comprobante) {
            case Factura f -> f.claveAcceso();
            case NotaCredito n -> n.claveAcceso();
            case NotaDebito n -> n.claveAcceso();
            case LiquidacionCompra l -> l.claveAcceso();
            case GuiaRemision g -> g.claveAcceso();
            case ComprobanteRetencion r -> r.claveAcceso();
        };

        assertEquals(original.claveAcceso(), registrado);
    }

    @Test
    @DisplayName("lo leído vuelve a escribirse idéntico byte por byte")
    void leerYVolverAEscribir() {
        var xml = ComprobanteWriter.xml(facturaDelEjemplo());

        assertArrayEquals(xml, ComprobanteWriter.xml(ComprobanteReader.comprobante(xml)));
    }

    private static Factura facturaDelEjemplo() {
        return Factura.builder()
            .emisor(emisor())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 1, "AMAZONAS Y ROCA"))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Alimento para perro 15kg")
                .cantidad(2)
                .precioUnitario("25.00")
                .descuento(Dinero.of("5.00"))
                .iva(TarifaIva.QUINCE)
                .build())
            .build();
    }
}
