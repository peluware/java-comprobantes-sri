package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Textos;

/**
 * Campo adicional del comprobante, correspondiente al elemento {@code <campoAdicional>}.
 *
 * <p>Ficha Técnica v2.34, numeral 9.11: hasta quince campos de 300 caracteres por comprobante.
 *
 * @param nombre nombre del campo, atributo {@code nombre}
 * @param valor  contenido del campo
 */
public record CampoAdicional(String nombre, String valor) {

    public static final int NOMBRE_LONGITUD_MAXIMA = 300;
    public static final int VALOR_LONGITUD_MAXIMA = 300;

    /** Máximo de campos adicionales por comprobante. */
    public static final int MAX_POR_COMPROBANTE = 15;

    /**
     * Nombre del campo que lleva el RUC del proveedor del sistema de facturación.
     *
     * <p>Ficha Técnica v2.34, Anexo 26, resolución NAC-DGERCGC26-00000027: obligatorio para los
     * emisores que usan sistemas de terceros de los que enumera la resolución 2718.
     */
    public static final String NOMBRE_RUC_PROVEEDOR_SISTEMA = "RUC Proveedor";

    public CampoAdicional {
        Textos.validar(nombre, NOMBRE_LONGITUD_MAXIMA, "campoAdicional.nombre").lanzarSiInvalida();
        Textos.validar(valor, VALOR_LONGITUD_MAXIMA, "campoAdicional.valor").lanzarSiInvalida();
    }
}
