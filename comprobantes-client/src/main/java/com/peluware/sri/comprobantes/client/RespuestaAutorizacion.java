package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Respuesta del servicio {@code AutorizacionComprobantesOffline}.
 *
 * <p>Ficha Técnica v2.34, numerales 7.2.3 y 7.4. El SRI dispone de hasta 24 horas para
 * resolver; {@link EnProceso} es un resultado normal y requiere volver a consultar.
 */
public sealed interface RespuestaAutorizacion {

    /**
     * El comprobante quedó autorizado y tiene validez tributaria.
     *
     * @param numeroAutorizacion número de autorización, que en el esquema offline coincide con
     *                           la clave de acceso
     * @param fechaAutorizacion  fecha y hora de la autorización
     * @param ambiente           ambiente que resolvió la autorización; {@code null} si el texto
     *                           no se pudo interpretar
     * @param comprobante        XML firmado y autorizado, para archivar y para generar el RIDE
     * @param mensajes           advertencias que acompañan a la autorización
     */
    record Autorizado(String numeroAutorizacion, OffsetDateTime fechaAutorizacion, @Nullable Ambiente ambiente, String comprobante, List<Mensaje> mensajes) implements RespuestaAutorizacion {

        public Autorizado {
            Objects.requireNonNull(numeroAutorizacion, "numeroAutorizacion");
            Objects.requireNonNull(fechaAutorizacion, "fechaAutorizacion");
            Objects.requireNonNull(comprobante, "comprobante");
            mensajes = List.copyOf(mensajes);
        }

        /**
         * El comprobante autorizado en {@link Sri#CODIFICACION}, la codificación que declara su
         * propia cabecera XML.
         */
        public byte[] comprobanteEnBytes() {
            return comprobante.getBytes(Sri.CODIFICACION);
        }
    }

    /**
     * El SRI rechazó el comprobante. El rechazo es definitivo para esa clave de acceso.
     *
     * @param numeroAutorizacion clave de acceso del comprobante rechazado, tal como la envió el
     *                           servicio; {@code null} si no la envió
     * @param fechaAutorizacion momento del rechazo; {@code null} si el servicio no lo envió
     * @param ambiente          ambiente que resolvió el rechazo; {@code null} si no se pudo
     *                          interpretar
     * @param comprobante       XML que se había enviado; {@code null} si el servicio no lo
     *                          devolvió
     * @param mensajes          motivos del rechazo
     */
    record NoAutorizado(@Nullable String numeroAutorizacion, @Nullable OffsetDateTime fechaAutorizacion, @Nullable Ambiente ambiente, @Nullable String comprobante, List<Mensaje> mensajes) implements RespuestaAutorizacion {

        public NoAutorizado {
            mensajes = List.copyOf(mensajes);
        }

        /**
         * Motivos con severidad de error.
         */
        public List<Mensaje> errores() {
            return mensajes.stream().filter(Mensaje::esError).toList();
        }
    }

    /**
     * El SRI no tiene todavía una autorización para esa clave de acceso.
     *
     * <p>El servicio devuelve {@code numeroComprobantes} en cero y la lista de autorizaciones
     * vacía tanto para un comprobante en cola como para una clave que nunca recibió, sin
     * distinguirlos. La consulta requiere un límite de intentos.
     *
     * @param claveAccesoConsultada clave que se consultó
     */
    record EnProceso(String claveAccesoConsultada) implements RespuestaAutorizacion {

        public EnProceso {
            Objects.requireNonNull(claveAccesoConsultada, "claveAccesoConsultada");
        }
    }

    /**
     * Estado que el servicio devolvió y esta librería no reconoce.
     *
     * @param estado   texto del estado tal como llegó
     * @param mensajes mensajes que acompañaban a la respuesta, si los hubo
     */
    record Desconocida(String estado, List<Mensaje> mensajes) implements RespuestaAutorizacion {

        public Desconocida {
            Objects.requireNonNull(estado, "estado");
            mensajes = List.copyOf(mensajes);
        }
    }

    /**
     * Indica si el comprobante quedó autorizado.
     */
    default boolean fueAutorizado() {
        return this instanceof Autorizado;
    }

    /**
     * Indica si vale la pena volver a consultar.
     *
     * <p>Cierto solo para {@link EnProceso}.
     */
    default boolean convieneReintentar() {
        return this instanceof EnProceso;
    }
}
