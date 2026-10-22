package com.peluware.sri.comprobantes.xades;

import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.ComprobanteFirmado;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.firma.OrigenClave;
import com.peluware.sri.comprobantes.firma.Firmador;
import com.peluware.sri.comprobantes.firma.ClaveFirma;
import com.peluware.sri.comprobantes.firma.FirmadorXml;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba el ciclo de vida de la clave en las sobrecargas de {@link Firmador} que reciben un
 * {@link OrigenClave}, con un firmador falso para no medir la firma en sí.
 */
class FirmaDeVariosTest {

    @Test
    @DisplayName("firmar varios comprobantes abre la clave una sola vez")
    void firmarVariosAbreLaClaveUnaVez() {
        var clave = new ClaveContada();
        var firmador = new FirmadorFalso();

        var firmados = firmador.firmar(clave, List.of(factura(1), factura(2), factura(3)));

        assertEquals(3, firmados.size());
        assertEquals(3, firmador.firmados);
        assertEquals(1, clave.aperturas);
    }

    @Test
    @DisplayName("firmar uno a uno abre la clave una vez por comprobante")
    void firmarUnoAUnoAbreCadaVez() {
        var clave = new ClaveContada();
        var firmador = new FirmadorFalso();

        firmador.firmar(clave, factura(1));
        firmador.firmar(clave, factura(2));

        assertEquals(2, clave.aperturas);
    }

    @Test
    @DisplayName("el comprobante firmado conserva el comprobante del que salió")
    void conservaElComprobante() {
        var factura = factura(1);

        var firmado = new FirmadorFalso().firmar(new ClaveContada(), factura);

        assertSame(factura, firmado.comprobante());
        assertEquals(factura.claveAcceso(), firmado.claveAcceso());
        assertTrue(new String(firmado.xml(), StandardCharsets.UTF_8).startsWith(FirmadorFalso.MARCA));
    }

    @Test
    @DisplayName("los comprobantes firmados salen en el mismo orden")
    void conservaElOrden() {
        List<Comprobante> facturas = List.of(factura(1), factura(2), factura(3));

        var firmados = new FirmadorFalso().firmar(new ClaveContada(), facturas);

        assertEquals(facturas.stream().map(Comprobante::claveAcceso).toList(), firmados.stream().map(ComprobanteFirmado::claveAcceso).toList());
    }

    private static Comprobante factura(long secuencial) {
        return Factura.builder()
            .emisor(Emisor.of(new Ruc("0000000000001"), "DISTRIBUIDORA FICTICIA S.A.", "AMAZONAS Y ROCA"))
            .comprador(Comprador.consumidorFinal())
            .fechaEmision(LocalDate.of(2026, 10, 5))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 1))
            .secuencial(Secuencial.of(secuencial))
            .codigoNumerico(CodigoNumerico.of(secuencial))
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Consulta veterinaria")
                .cantidad(1)
                .precioUnitario("10.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .build();
    }

    /**
     * Origen de clave que cuenta cuántas veces se abre.
     */
    private static final class ClaveContada implements OrigenClave {

        private final OrigenClave real = new OrigenClavePkcs12(CertificadosDePrueba.rsa2048(), CertificadosDePrueba.CLAVE);

        private int aperturas;

        @Override
        public ClaveFirma abrir() {
            aperturas++;
            return real.abrir();
        }
    }

    /**
     * Firmador que marca lo que firmó y cuenta las llamadas.
     */
    private static final class FirmadorFalso extends FirmadorXml {

        static final String MARCA = "FIRMADO:";

        private int firmados;

        @Override
        protected byte[] firmarXml(ClaveFirma clave, byte[] xml) {
            firmados++;
            var marca = MARCA.getBytes(StandardCharsets.UTF_8);
            var firmado = new byte[marca.length + xml.length];
            System.arraycopy(marca, 0, firmado, 0, marca.length);
            System.arraycopy(xml, 0, firmado, marca.length, xml.length);
            return firmado;
        }
    }
}
