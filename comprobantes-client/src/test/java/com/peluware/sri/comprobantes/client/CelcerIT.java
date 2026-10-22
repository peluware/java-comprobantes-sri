package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.ClaveAcceso;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprueba contra el ambiente de certificación del SRI lo único que no se puede verificar con
 * respuestas enlatadas: que el servicio real acepte el sobre SOAP que construye esta librería.
 *
 * <p>No necesita certificado. Envía un comprobante deliberadamente inválido, de modo que el
 * SRI lo rechace por su contenido: lo que se verifica es que entienda la petición, en lugar de
 * devolver un {@code soap:Fault} por un namespace o una cabecera mal puestos.
 *
 * <p>Requiere red, así que Maven no la recoge: su nombre termina en {@code IT} y no en
 * {@code Test}. Se lanza desde el IDE, o con {@code mvn test -Dtest=CelcerIT}. Apunta siempre
 * al ambiente de pruebas.
 */
class CelcerIT {

    private static final ClienteSri CLIENTE = ClienteSri.of(Ambiente.PRUEBAS);

    private static final byte[] COMPROBANTE_INVALIDO = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><factura id=\"comprobante\" version=\"2.1.0\"/>".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("el servicio de recepción acepta el sobre y rechaza el comprobante por su contenido")
    void elSobreDeRecepcionEsAceptado() {
        var respuesta = CLIENTE.enviar(COMPROBANTE_INVALIDO);

        RespuestaRecepcion.Devuelta devuelta = assertInstanceOf(RespuestaRecepcion.Devuelta.class, respuesta, "si el sobre estuviera mal, llegaría un soap:Fault");

        assertAll(
            () -> assertFalse(devuelta.mensajes().isEmpty(), "el rechazo debe venir motivado"),
            () -> assertEquals("35", devuelta.mensajes().getFirst().identificador()),
            () -> assertTrue(devuelta.mensajes().getFirst().esError())
        );
    }

    @Test
    @DisplayName("la clave de acceso de un rechazo no siempre es una clave")
    void laClaveDeUnRechazoNoSiempreEsUnaClave() {
        RespuestaRecepcion.Devuelta devuelta = assertInstanceOf(RespuestaRecepcion.Devuelta.class, CLIENTE.enviar(COMPROBANTE_INVALIDO));

        var clave = devuelta.comprobantes().getFirst().claveAcceso();

        assertFalse(ClaveAcceso.esValido(clave), "el SRI devuelve N/A cuando no pudo leer la clave: " + clave);
    }

    @Test
    @DisplayName("los acentos de los mensajes llegan bien decodificados")
    void losAcentosLleganBien() {
        RespuestaRecepcion.Devuelta devuelta = assertInstanceOf(RespuestaRecepcion.Devuelta.class, CLIENTE.enviar(COMPROBANTE_INVALIDO));
        var informacion = devuelta.mensajes().getFirst().informacionAdicional();

        assertAll(
            () -> assertTrue(informacion != null && !informacion.isEmpty(), "se esperaba información adicional"),
            () -> assertFalse(informacion != null && informacion.contains("�"), informacion)
        );
    }

    @Test
    @DisplayName("el servicio de autorización acepta el sobre y no distingue una clave inexistente de una en cola")
    void elSobreDeAutorizacionEsAceptado() {
        var jamasEnviada = "0510202601000000000000110010010000000010000000114";

        var respuesta = CLIENTE.consultarAutorizacion(jamasEnviada);

        assertInstanceOf(RespuestaAutorizacion.EnProceso.class, respuesta, "el SRI responde igual para una clave inexistente que para una en cola");
    }
}
