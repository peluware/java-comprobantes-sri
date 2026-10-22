package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.DatosDePrueba;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo propio del modelo de la guía de remisión. Lo que comparte con los demás comprobantes lo
 * comprueba {@code ComprobanteContratoTest}.
 */
class GuiaRemisionTest {

    @Test
    @DisplayName("su clave de acceso lleva el código 06 y sí tiene fecha de emisión")
    void laClaveLlevaSuCodigoYSuFecha() {
        var guia = DatosDePrueba.guiaRemision().build();

        assertEquals(TipoComprobante.GUIA_REMISION, guia.tipoComprobante());
        assertEquals("06", guia.claveAcceso().substring(8, 10));
        assertEquals("05102026", guia.claveAcceso().substring(0, 8));
        assertEquals(DatosDePrueba.FECHA_EMISION, guia.fechaEmision());
    }

    @Test
    @DisplayName("exige al menos un destinatario")
    void exigeUnDestinatario() {
        var builder = DatosDePrueba.guiaRemisionSinDestinatarios().codigoNumerico(DatosDePrueba.CODIGO_NUMERICO);

        var e = assertThrows(ValidacionException.class, builder::build);

        assertEquals("destinatarios", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("cada destinatario exige al menos un detalle")
    void cadaDestinatarioExigeUnDetalle() {
        var builder = Destinatario.builder()
            .identificacion("1710034065")
            .razonSocial("CLIENTE")
            .direccion("CALLE UNO")
            .motivoTraslado("VENTA");

        var e = assertThrows(ValidacionException.class, builder::build);

        assertEquals("detalles", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @Test
    @DisplayName("el transporte no puede terminar antes de empezar")
    void elTransporteNoTerminaAntesDeEmpezar() {
        var builder = DatosDePrueba.guiaRemisionSinCodigoNumerico()
            .codigoNumerico(DatosDePrueba.CODIGO_NUMERICO)
            .transporte(DatosDePrueba.FECHA_EMISION, DatosDePrueba.FECHA_EMISION.minusDays(1));

        var e = assertThrows(ValidacionException.class, builder::build);

        assertInstanceOf(Validacion.ReglaInvalida.class, e.invalida());
    }

    @Test
    @DisplayName("exige la placa, la dirección de partida y el transportista")
    void exigeLosDatosDelTransporte() {
        assertEquals("placa", assertThrows(NullPointerException.class, () -> DatosDePrueba.guiaRemision().placa(null).build()).getMessage());
    }

    @Test
    @DisplayName("el documento de sustento compone su número como los demás")
    void elDocumentoDeSustentoComponeSuNumero() {
        var sustento = SustentoTraslado.of(TipoComprobante.FACTURA, Serie.of(1, 1), Secuencial.of(45));

        assertEquals("001-001-000000045", sustento.numero());
        assertTrue(sustento.toString().contains("001-001-000000045"));
    }
}
