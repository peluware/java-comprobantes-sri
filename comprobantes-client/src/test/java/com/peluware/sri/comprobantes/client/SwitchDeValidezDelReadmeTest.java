package com.peluware.sri.comprobantes.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.peluware.sri.comprobantes.client.RespuestaValidez.Anulado;
import static com.peluware.sri.comprobantes.client.RespuestaValidez.Autorizado;
import static com.peluware.sri.comprobantes.client.RespuestaValidez.Desconocida;
import static com.peluware.sri.comprobantes.client.RespuestaValidez.NoAutorizado;
import static com.peluware.sri.comprobantes.client.RespuestaValidez.NoConsultable;
import static com.peluware.sri.comprobantes.client.RespuestaValidez.PendienteDeAnular;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El {@code switch} de la consulta de validez del README, copiado literalmente.
 *
 * <p>Vive en su propio archivo porque el README importa los casos de {@link RespuestaValidez} sin
 * cualificar, y {@link RespuestaAutorizacion} declara tres con el mismo nombre: los dos juegos no
 * caben sin cualificar en una misma clase. El otro {@code switch} está en
 * {@code SwitchDelReadmeTest}.
 */
class SwitchDeValidezDelReadmeTest {

    private static final String CLAVE = "0510202601000000000000110010010000000010000000114";

    private static final ComprobanteConsultado COMPROBANTE =
        new ComprobanteConsultado(CLAVE, null, "Factura", "1790012345001", List.of());

    @Test
    @DisplayName("el switch sobre la consulta de validez es exhaustivo sin default")
    void validez() {
        assertEquals("vale", trasConsultar(new Autorizado(COMPROBANTE)));
        assertEquals("pendiente, vale", trasConsultar(new PendienteDeAnular(COMPROBANTE)));
        assertEquals("anulado", trasConsultar(new Anulado(COMPROBANTE)));
        assertEquals("no autorizado", trasConsultar(new NoAutorizado(COMPROBANTE)));
        assertEquals("no consultable", trasConsultar(new NoConsultable(CLAVE, List.of())));
        assertEquals("desconocido RARO", trasConsultar(new Desconocida("RARO", List.of())));
    }

    private static String trasConsultar(RespuestaValidez validez) {
        return switch (validez) {
            case Autorizado(var c) -> "vale";
            case PendienteDeAnular(var c) -> "pendiente, " + (validez.tieneValidez() ? "vale" : "no vale");
            case Anulado(var c) -> "anulado";
            case NoAutorizado(var c) -> "no autorizado";
            case NoConsultable(var clave, var mensajes) -> "no consultable";
            case Desconocida(var estado, var mensajes) -> "desconocido " + estado;
        };
    }
}
