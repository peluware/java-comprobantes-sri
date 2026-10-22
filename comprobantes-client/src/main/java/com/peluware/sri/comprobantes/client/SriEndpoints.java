package com.peluware.sri.comprobantes.client;

import com.peluware.sri.comprobantes.catalogo.Ambiente;

import java.net.URI;
import java.util.Objects;

/**
 * Direcciones de los servicios web del SRI.
 *
 * <p>Ficha Técnica v2.34, numerales 7.2.1 y 7.2.2. Son los endpoints de los servicios, sin el
 * sufijo {@code ?wsdl} que aparece en la ficha.
 *
 * <p>El numeral 7.1.4 advierte que el SRI puede cambiar sus certificados y direcciones sin
 * aviso. El constructor admite otras direcciones.
 *
 * @param recepcion      servicio que recibe el comprobante firmado
 * @param autorizacion   servicio que informa del resultado de la autorización
 * @param consultaValidez servicio que informa del estado en que está el comprobante
 */
public record SriEndpoints(URI recepcion, URI autorizacion, URI consultaValidez) {

    private static final String PRUEBAS_BASE = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/";
    private static final String PRODUCCION_BASE = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/";

    /** Ambiente de certificación, donde el contribuyente prueba su aplicación. */
    public static final SriEndpoints PRUEBAS = base(PRUEBAS_BASE);

    /** Ambiente de producción, con validez tributaria. */
    public static final SriEndpoints PRODUCCION = base(PRODUCCION_BASE);

    public SriEndpoints {
        Objects.requireNonNull(recepcion, "recepcion");
        Objects.requireNonNull(autorizacion, "autorizacion");
        Objects.requireNonNull(consultaValidez, "consultaValidez");
    }

    /**
     * Endpoints que corresponden a un ambiente.
     */
    public static SriEndpoints of(Ambiente ambiente) {
        Objects.requireNonNull(ambiente, "ambiente");
        return switch (ambiente) {
            case PRUEBAS -> PRUEBAS;
            case PRODUCCION -> PRODUCCION;
        };
    }

    private static SriEndpoints base(String base) {
        return new SriEndpoints(
            URI.create(base + "RecepcionComprobantesOffline"),
            URI.create(base + "AutorizacionComprobantesOffline"),
            URI.create(base + "ConsultaComprobante")
        );
    }
}
