package com.peluware.sri.comprobantes.it;

import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.client.ClienteSri;
import com.peluware.sri.comprobantes.client.ObservadorSoap;
import com.peluware.sri.comprobantes.client.RespuestaValidez;
import com.peluware.sri.comprobantes.client.SriEndpoints;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Consulta la validez de un comprobante recién autorizado, contra el ambiente de certificación.
 *
 * <p>Comprueba lo que la Ficha Técnica no garantiza: que celcer sirva {@code ConsultaComprobante}
 * y que los nombres de elemento de la respuesta real sean los de los ejemplos del numeral 8.2.
 * Los sobres crudos quedan en {@code target}, con el prefijo de cada prueba, para poder mirarlos.
 *
 * <p>Necesita credenciales y red, por lo que Maven no la recoge. Se lanza con
 * {@code mvn test -Dtest=ConsultaValidezIT}.
 */
class ConsultaValidezIT {

    @Test
    @DisplayName("un comprobante autorizado consta como autorizado en la consulta de validez")
    void consultarValidezDeUnaFacturaAutorizada() throws Exception {
        var factura = ComprobantesDePrueba.factura();
        var autorizado = Emision.emitirYAutorizar(factura, "factura_V2.1.0.xsd", "validez-factura");

        var sobres = new SobresGuardados("validez-autorizada");
        var cliente = new ClienteSri(SriEndpoints.PRUEBAS, sobres);

        var validez = cliente.consultarValidez(factura.claveAcceso());

        assertNotNull(sobres.respuesta, "celcer no respondió a la consulta de validez");
        var crudo = new String(sobres.respuesta, Sri.CODIFICACION);

        var vigente = assertInstanceOf(RespuestaValidez.Autorizado.class, validez, () -> "la clave " + factura.claveAcceso() + " acaba de quedar autorizada, pero la consulta de validez devolvió " + validez + "\n" + crudo);

        assertAll(
            () -> assertTrue(validez.tieneValidez()),
            () -> assertEquals(factura.claveAcceso(), vigente.comprobante().claveAcceso()),
            () -> assertEquals(autorizado.numeroAutorizacion(), vigente.comprobante().claveAcceso(), "en el esquema offline el número de autorización es la clave de acceso"),
            () -> assertNotNull(vigente.comprobante().fechaAutorizacion(), () -> "la respuesta real no trae fechaAutorizacion:\n" + crudo),
            () -> assertEquals(Emision.emisor().ruc().valor(), vigente.comprobante().rucEmisor(), () -> "la respuesta real no trae rucEmisor:\n" + crudo),
            () -> assertNotNull(vigente.comprobante().tipoComprobante(), () -> "la respuesta real no trae tipoComprobante:\n" + crudo),
            () -> assertTrue(crudo.contains("estadoAutorizacion"), () -> "la respuesta real no trae estadoAutorizacion:\n" + crudo)
        );
    }

    @Test
    @DisplayName("una clave que el SRI no conoce no es un comprobante sin validez, es una consulta sin resolver")
    void claveQueNoExiste() {
        // Una factura que se construye y no se emite: su clave es válida en forma y el SRI no la
        // ha recibido nunca, porque el secuencial sale del contador de la ejecución.
        var inexistente = ComprobantesDePrueba.factura().claveAcceso();

        var sobres = new SobresGuardados("validez-inexistente");
        var validez = new ClienteSri(SriEndpoints.PRUEBAS, sobres).consultarValidez(inexistente);

        var crudo = sobres.respuesta == null ? "(sin respuesta)" : new String(sobres.respuesta, Sri.CODIFICACION);
        var noConsultable = assertInstanceOf(RespuestaValidez.NoConsultable.class, validez, () -> "una clave nunca emitida debería dar NoConsultable, dio " + validez + "\n" + crudo);

        assertAll(
            () -> assertEquals("99", noConsultable.mensajes().getFirst().identificador(), () -> crudo),
            () -> assertTrue(crudo.contains("<estadoConsulta>RECHAZADA</estadoConsulta>"), () -> "el rechazo venía en estadoConsulta, no en estadoAutorizacion; si esto cambia hay que revisar Respuestas.validez:\n" + crudo)
        );
    }

    /**
     * Guarda los sobres en {@code target} con el prefijo indicado y retiene la respuesta para
     * poder afirmar sobre ella.
     *
     * <p>El prefijo es por prueba: con un nombre fijo, la segunda consulta de la ejecución
     * pisaría el sobre de la primera.
     */
    private static final class SobresGuardados implements ObservadorSoap {

        private final String prefijo;

        private byte @Nullable [] respuesta;

        private SobresGuardados(String prefijo) {
            this.prefijo = prefijo;
        }

        @Override
        public void intercambio(String operacion, URI endpoint, byte[] peticion, byte @Nullable [] respuesta) {
            this.respuesta = respuesta;
            guardar(prefijo + "-peticion.xml", peticion);
            if (respuesta != null) {
                guardar(prefijo + "-respuesta.xml", respuesta);
            }
        }

        private static void guardar(String nombre, byte[] contenido) {
            try {
                var destino = Path.of("target", nombre);
                Files.createDirectories(destino.getParent());
                Files.write(destino, contenido);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
