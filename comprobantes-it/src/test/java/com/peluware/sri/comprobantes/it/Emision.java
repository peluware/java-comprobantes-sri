package com.peluware.sri.comprobantes.it;

import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.ComprobanteWriter;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.client.ComprobanteDevuelto;
import com.peluware.sri.comprobantes.client.Mensaje;
import com.peluware.sri.comprobantes.client.RespuestaAutorizacion.Autorizado;
import com.peluware.sri.comprobantes.client.RespuestaAutorizacion;
import com.peluware.sri.comprobantes.client.RespuestaRecepcion.Recibida;
import com.peluware.sri.comprobantes.client.RespuestaRecepcion;
import com.peluware.sri.comprobantes.client.ClienteSri;
import com.peluware.sri.comprobantes.client.SriEndpoints;
import com.peluware.sri.comprobantes.firma.Firmador;

import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.xades.OrigenClavePkcs12;
import com.peluware.sri.comprobantes.xades.FirmadorXadesBes;
import org.junit.jupiter.api.Assumptions;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * El recorrido que comparten las pruebas de emisión: validar contra el XSD, firmar con el
 * certificado real, enviar a recepción, consultar hasta que el SRI resuelva y comprobar lo que
 * vale para cualquier tipo de comprobante.
 *
 * <p>Cada prueba aporta su comprobante y su esquema; lo demás es igual para todos.
 *
 * <p>Deja en {@code comprobantes-it/target} los tres XML de cada recorrido, con el prefijo que
 * se le indique: {@code <prefijo>-sin-firmar.xml}, {@code -firmada.xml} y
 * {@code -autorizada.xml}.
 */
final class Emision {

    /**
     * Intentos de consulta antes de dar la autorización por no resuelta.
     */
    static final int INTENTOS = 10;

    /**
     * Espera entre consultas. El numeral 7.5 admite hasta 24 horas, pero celcer tarda segundos.
     */
    static final long ESPERA_MS = 3_000;

    /**
     * Secreto de derivación del código numérico. Solo para estas pruebas.
     */
    static final byte[] SECRETO_DE_PRUEBA = "comprobantes-it-secreto-de-prueba".getBytes(Sri.CODIFICACION);

    private static final Firmador FIRMADOR = new FirmadorXadesBes();
    private static final ClienteSri CLIENTE = new ClienteSri(SriEndpoints.PRUEBAS);

    private Emision() {
    }

    /**
     * Emisor de prueba, con el RUC del certificado que indique el operador.
     */
    static Emisor emisor() {
        return Emisor.builder(DatosDeEmision.pedir().ruc(), "DISTRIBUIDORA FICTICIA S.A.", "AMAZONAS Y ROCA")
            .obligadoContabilidad(true)
            .build();
    }

    /**
     * Contador de secuenciales de la ejecución, sembrado con los segundos transcurridos desde
     * medianoche.
     *
     * <p>La semilla da un punto de partida distinto en cada ejecución sin llevar estado entre
     * ellas, y el contador garantiza que no se repita ningún secuencial dentro de una misma,
     * aunque dos pruebas arranquen en el mismo segundo.
     */
    private static final AtomicInteger SECUENCIALES = new AtomicInteger(LocalTime.now(Sri.ZONA).toSecondOfDay());

    /**
     * Secuencial sin usar en esta ejecución.
     *
     * <p>El SRI rechaza con error 45 un comprobante cuya clave de acceso ya recibió, y la clave
     * incluye la fecha y el secuencial. Un emisor real necesita secuenciales consecutivos desde
     * uno; esto vale solo para la prueba.
     */
    static Secuencial secuencial() {
        return Secuencial.of(SECUENCIALES.getAndIncrement());
    }

    /**
     * Recorre el comprobante de principio a fin y devuelve su autorización.
     *
     * @param xsd     nombre del esquema contra el que validar, en {@code /xsd/}
     * @param prefijo prefijo de los XML que deja en {@code target}
     * @throws org.opentest4j.TestAbortedException si el SRI no resuelve a tiempo
     */
    static Autorizado emitirYAutorizar(Comprobante comprobante, String xsd, String prefijo) throws Exception {
        assertEquals(Ambiente.PRUEBAS, comprobante.ambiente(), "estas pruebas nunca deben salir a producción");

        var clave = comprobante.claveAcceso();
        var esquema = compilar(xsd);

        byte[] sinFirmar = ComprobanteWriter.xml(comprobante);
        validar(esquema, sinFirmar, "el XML sin firmar");
        guardar(prefijo + "-sin-firmar.xml", sinFirmar);

        byte[] firmado = firmar(comprobante);
        validar(esquema, firmado, "el XML firmado");
        guardar(prefijo + "-firmada.xml", firmado);

        enviar(clave, firmado);

        var autorizacion = consultarHastaResolver(clave);
        var autorizado = assertInstanceOf(Autorizado.class, autorizacion, () -> "la clave " + clave + " no quedó autorizada: " + autorizacion);

        guardar(prefijo + "-autorizada.xml", autorizado.comprobanteEnBytes());

        assertEquals(clave, autorizado.numeroAutorizacion(), "en el esquema offline el número de autorización es la clave de acceso");
        assertEquals(Ambiente.PRUEBAS, autorizado.ambiente());
        assertTrue(autorizado.comprobante().contains(clave), "el comprobante autorizado debe contener su propia clave");
        assertTrue(autorizado.mensajes().stream().noneMatch(Mensaje::esError), () -> "la clave " + clave + " quedó autorizada con errores: " + autorizado.mensajes());

        return autorizado;
    }


    private static byte[] firmar(Comprobante comprobante) throws IOException {
        var datos = DatosDeEmision.pedir();
        var p12 = datos.p12();
        assertTrue(Files.isReadable(p12), () -> "no se puede leer el certificado en " + p12);

        try (InputStream entrada = Files.newInputStream(p12)) {
            return FIRMADOR.firmar(new OrigenClavePkcs12(entrada, datos.clave()), comprobante).xml();
        }
    }

    private static void enviar(String clave, byte[] firmado) {
        var recepcion = CLIENTE.enviar(firmado);
        if (recepcion instanceof RespuestaRecepcion.Devuelta(List<ComprobanteDevuelto> comprobantes)) {
            fail("el SRI rechazó en recepción la clave " + clave + ": " + detalle(comprobantes));
        }
        assertInstanceOf(Recibida.class, recepcion, () -> "respuesta inesperada de recepción para la clave " + clave + ": " + recepcion);
    }

    @SuppressWarnings("all")
    private static RespuestaAutorizacion consultarHastaResolver(String clave) throws InterruptedException {
        var respuesta = CLIENTE.consultarAutorizacion(clave);
        for (int intento = 1; intento < INTENTOS && respuesta.convieneReintentar(); intento++) {
            Thread.sleep(ESPERA_MS);
            respuesta = CLIENTE.consultarAutorizacion(clave);
        }
        if (respuesta.convieneReintentar()) {
            Assumptions.abort("el SRI no resolvió en " + (INTENTOS * ESPERA_MS / 1000) + " s; la clave " + clave + " sigue sin autorización");
        }
        return respuesta;
    }

    private static void validar(Schema esquema, byte[] xml, String que) {
        try {
            esquema.newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (Exception e) {
            fail(que + " no cumple el XSD: " + e.getMessage() + "\n" + new String(xml, Sri.CODIFICACION));
        }
    }

    private static Schema compilar(String nombre) {
        var xsd = Emision.class.getResource("/xsd/" + nombre);
        if (xsd == null) {
            throw new IllegalStateException("falta " + nombre + "; debería llegar del test-jar de comprobantes-core");
        }
        try {
            var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
            return factory.newSchema(new StreamSource(xsd.toExternalForm()));
        } catch (SAXException e) {
            throw new IllegalStateException("no se pudo compilar " + nombre, e);
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

    private static String detalle(List<ComprobanteDevuelto> comprobantes) {
        var texto = new StringBuilder();
        for (var comprobante : comprobantes) {
            texto.append("\n  clave ").append(comprobante.claveAcceso());
            for (var mensaje : comprobante.mensajes()) {
                texto.append("\n    ").append(mensaje);
            }
        }
        return texto.toString();
    }
}
