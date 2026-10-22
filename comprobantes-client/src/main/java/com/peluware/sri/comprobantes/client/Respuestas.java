package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.interno.xml.Dom;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.CatalogoException;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpretación de las respuestas de los servicios del SRI.
 *
 * <p>Un estado no reconocido se devuelve tal cual. Ver {@link Estados} para los vocabularios
 * que la Ficha Técnica usa.
 */
final class Respuestas {

    private Respuestas() {
    }

    /**
     * Interpreta un {@code RespuestaRecepcionComprobante}.
     */
    static RespuestaRecepcion recepcion(Element contenido) {
        var estado = Estados.normalizar(Dom.text(contenido, "estado"));
        var comprobantes = comprobantes(contenido);
        return switch (estado) {
            case "RECIBIDA", "RECIBIDO" -> new RespuestaRecepcion.Recibida();
            case "DEVUELTA", "DEVUELTO" -> new RespuestaRecepcion.Devuelta(comprobantes);
            default -> new RespuestaRecepcion.Desconocida(estado, comprobantes);
        };
    }
    /**
     * Interpreta un {@code RespuestaAutorizacionComprobante}.
     *
     * <p>Una respuesta sin autorizaciones significa que el SRI todavía no ha resuelto: devuelve
     * {@code numeroComprobantes} en cero mientras el comprobante está en la cola.
     *
     * @param consultada clave que se consultó, que en el esquema offline es también el número de
     *                   autorización (numeral 5.9) y sirve de respaldo si el servicio no lo envía
     */
    static RespuestaAutorizacion autorizacion(Element contenido, String consultada) {
        var autorizaciones = autorizaciones(contenido);
        if (autorizaciones.isEmpty()) {
            return new RespuestaAutorizacion.EnProceso(consultada);
        }
        return una(autorizaciones.getFirst(), consultada);
    }

    /**
     * Interpreta un {@code EstadoAutorizacionComprobante}.
     *
     * <p>El servicio usa una etiqueta u otra según el caso: una consulta resuelta trae
     * {@code estadoAutorizacion} y ninguna {@code estadoConsulta}, y una rechazada lo contrario.
     * La ficha describe el rechazo en {@code estadoAutorizacion}, pero el ambiente de
     * certificación lo devuelve en {@code estadoConsulta}, como sus ejemplos XML. Se leen las
     * dos: el estado del comprobante sale de la primera que corresponda a un estado conocido, y
     * {@code RECHAZADA} en cualquiera da {@link RespuestaValidez.NoConsultable}.
     *
     * @param consultada clave que se consultó, que se usa si el servicio no la devuelve
     */
    static RespuestaValidez validez(Element contenido, String consultada) {
        var mensajes = SoapSri.mensajes(contenido);
        var clave = Dom.text(contenido, "claveAcceso");
        var comprobante = new ComprobanteConsultado(
            clave == null ? consultada : clave,
            fecha(Dom.text(contenido, "fechaAutorizacion")),
            Dom.text(contenido, "tipoComprobante"),
            Dom.text(contenido, "rucEmisor"),
            mensajes
        );

        var declarados = List.of(
            Estados.normalizar(Dom.text(contenido, "estadoAutorizacion")),
            Estados.normalizar(Dom.text(contenido, "estadoConsulta"))
        );

        for (var estado : declarados) {
            var reconocida = reconocer(estado, comprobante);
            if (reconocida != null) {
                return reconocida;
            }
        }
        if (declarados.contains("RECHAZADA") || declarados.contains("RECHAZADO")) {
            return new RespuestaValidez.NoConsultable(comprobante.claveAcceso(), mensajes);
        }
        return new RespuestaValidez.Desconocida(declarados.getFirst(), mensajes);
    }

    private static @Nullable RespuestaValidez reconocer(String estado, ComprobanteConsultado comprobante) {
        return switch (estado) {
            case "AUTORIZADO", "AUT" -> new RespuestaValidez.Autorizado(comprobante);
            case "NO AUTORIZADO", "NOAUTORIZADO", "NAT" -> new RespuestaValidez.NoAutorizado(comprobante);
            case "PENDIENTE DE ANULAR", "PENDIENTE ANULAR" -> new RespuestaValidez.PendienteDeAnular(comprobante);
            case "ANULADO", "ANULADA" -> new RespuestaValidez.Anulado(comprobante);
            default -> null;
        };
    }

    private static List<Element> autorizaciones(Element contenido) {
        var contenedor = Dom.child(contenido, "autorizaciones");
        return contenedor == null ? List.of() : Dom.children(contenedor, "autorizacion");
    }

    /**
     * Interpreta un elemento {@code autorizacion}.
     *
     * @param respaldo clave que se usa como número de autorización si el servicio no lo envía
     */
    private static RespuestaAutorizacion una(Element autorizacion, String respaldo) {
        var estado = Estados.normalizar(Dom.text(autorizacion, "estado"));
        var mensajes = SoapSri.mensajes(autorizacion);
        var fecha = fecha(Dom.text(autorizacion, "fechaAutorizacion"));
        var ambiente = ambiente(Dom.text(autorizacion, "ambiente"));
        var comprobante = Dom.text(autorizacion, "comprobante");
        var numero = Dom.text(autorizacion, "numeroAutorizacion");
        var clave = numero == null ? respaldo : numero;

        return switch (estado) {
            case "AUTORIZADO", "AUT" -> autorizado(clave, fecha, ambiente, comprobante, mensajes, estado);
            case "RECHAZADO", "NO AUTORIZADO", "NOAUTORIZADO", "NAT" -> new RespuestaAutorizacion.NoAutorizado(numero, fecha, ambiente, comprobante, mensajes);
            case "EN PROCESAMIENTO", "EN PROCESO", "PROCESAMIENTO", "PPR" -> clave == null ? new RespuestaAutorizacion.Desconocida(estado, mensajes) : new RespuestaAutorizacion.EnProceso(clave);
            default -> new RespuestaAutorizacion.Desconocida(estado, mensajes);
        };
    }

    /**
     * Construye el resultado autorizado.
     *
     * <p>Si falta el número de autorización, la fecha o el comprobante, devuelve
     * {@link RespuestaAutorizacion.Desconocida}.
     */
    private static RespuestaAutorizacion autorizado(@Nullable String numeroAutorizacion, @Nullable OffsetDateTime fecha, @Nullable Ambiente ambiente, @Nullable String comprobante, List<Mensaje> mensajes, String estado) {
        if (numeroAutorizacion == null || fecha == null || comprobante == null) {
            return new RespuestaAutorizacion.Desconocida(estado, mensajes);
        }
        return new RespuestaAutorizacion.Autorizado(numeroAutorizacion, fecha, ambiente, comprobante, mensajes);
    }

    private static List<ComprobanteDevuelto> comprobantes(Element contenido) {
        var contenedor = Dom.child(contenido, "comprobantes");
        if (contenedor == null) {
            return List.of();
        }
        var devueltos = new ArrayList<ComprobanteDevuelto>();
        for (var comprobante : Dom.children(contenedor, "comprobante")) {
            var clave = Dom.text(comprobante, "claveAcceso");
            devueltos.add(new ComprobanteDevuelto(clave == null ? "" : clave, SoapSri.mensajes(comprobante)));
        }
        return List.copyOf(devueltos);
    }

    private static @Nullable OffsetDateTime fecha(@Nullable String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static @Nullable Ambiente ambiente(@Nullable String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Ambiente.ofDescripcion(valor);
        } catch (CatalogoException e) {
            return null;
        }
    }
}
