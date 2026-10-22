package com.peluware.sri.comprobantes;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;

/**
 * Constantes del esquema de comprobantes electrónicos que no pertenecen a ningún tipo concreto.
 */
public final class Sri {

    /**
     * Zona horaria de Ecuador, aplicable a la fecha de emisión, al SigningTime de la firma y a
     * la fecha de autorización.
     */
    public static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    /**
     * Valor obligatorio del atributo {@code id} del elemento raíz del comprobante.
     *
     * <p>El XSD lo restringe por enumeración a este único valor.
     */
    public static final String ID_COMPROBANTE = "comprobante";

    /**
     * Valor del tag {@code <moneda>}, único en circulación.
     */
    public static final String MONEDA = "DOLAR";

    /**
     * Longitud máxima del tag {@code <moneda>}, igual en los esquemas que lo declaran.
     */
    public static final int MONEDA_LONGITUD_MAXIMA = 15;

    /**
     * Leyenda que el esquema exige literalmente en {@code <contribuyenteRimpe>}.
     *
     * <p>Ficha Técnica v2.34, Anexo 22.
     */
    public static final String LEYENDA_RIMPE = "CONTRIBUYENTE RÉGIMEN RIMPE";

    /**
     * Codificación de los comprobantes.
     *
     * <p>Ficha Técnica v2.34, Tabla 7.
     */
    public static final Charset CODIFICACION = StandardCharsets.UTF_8;

    private Sri() {
    }
}
