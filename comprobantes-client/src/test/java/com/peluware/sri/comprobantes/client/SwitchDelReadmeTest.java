package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.client.RespuestaAutorizacion.Autorizado;
import com.peluware.sri.comprobantes.client.RespuestaAutorizacion.EnProceso;
import com.peluware.sri.comprobantes.client.RespuestaAutorizacion.NoAutorizado;
import com.peluware.sri.comprobantes.client.RespuestaRecepcion.Devuelta;
import com.peluware.sri.comprobantes.client.RespuestaRecepcion.Recibida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Los dos {@code switch} del README, copiados literalmente.
 *
 * <p>Dejan de compilar si una de las dos jerarquías gana o pierde un caso, o si cambia la forma
 * de uno de sus records: el README quedaría obsoleto sin que nadie se enterara.
 *
 * <p>El de la consulta de validez está en {@code SwitchDeValidezDelReadmeTest}, porque el README
 * lo escribe con los casos sin cualificar y {@link RespuestaValidez} comparte tres nombres con
 * {@link RespuestaAutorizacion}.
 */
class SwitchDelReadmeTest {

    private static final String CLAVE = "0211202401050306179800120010020000000677300995216";

    @Test
    @DisplayName("el switch sobre la respuesta de recepción es exhaustivo sin default")
    void recepcion() {
        assertEquals("recibida", trasEnviar(new Recibida()));
        assertEquals("devueltos 1", trasEnviar(new Devuelta(List.of(new ComprobanteDevuelto(CLAVE, List.of())))));
        assertEquals("desconocido RARO", trasEnviar(new RespuestaRecepcion.Desconocida("RARO", List.of())));
    }

    @Test
    @DisplayName("el switch sobre la respuesta de autorización es exhaustivo sin default")
    void autorizacion() {
        assertEquals("archivada", trasConsultar(new Autorizado(CLAVE, OffsetDateTime.now(), null, "<factura/>", List.of())));
        assertEquals("rechazada 0", trasConsultar(new NoAutorizado(CLAVE, null, null, null, List.of())));
        assertEquals("pendiente " + CLAVE, trasConsultar(new EnProceso(CLAVE)));
        assertEquals("desconocido RARO", trasConsultar(new RespuestaAutorizacion.Desconocida("RARO", List.of())));
    }

    private static String trasEnviar(RespuestaRecepcion respuesta) {
        return switch (respuesta) {
            case Recibida() -> "recibida";
            case Devuelta(var rechazados) -> "devueltos " + rechazados.size();
            case RespuestaRecepcion.Desconocida(var estado, var comprobantes) -> "desconocido " + estado;
        };
    }

    private static String trasConsultar(RespuestaAutorizacion respuesta) {
        return switch (respuesta) {
            case Autorizado a -> "archivada";
            case NoAutorizado n -> "rechazada " + n.errores().size();
            case EnProceso(var pendiente) -> "pendiente " + pendiente;
            case RespuestaAutorizacion.Desconocida(var estado, var mensajes) -> "desconocido " + estado;
        };
    }
}
