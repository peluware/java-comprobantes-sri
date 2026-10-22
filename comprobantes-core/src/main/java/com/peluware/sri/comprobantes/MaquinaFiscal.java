package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Textos;

/**
 * Máquina fiscal desde la que se emitió el comprobante, elemento {@code <maquinaFiscal>}.
 *
 * <p>Ficha Técnica v2.34, Anexo 13. Obligatorio para los comprobantes emitidos desde una
 * máquina fiscal certificada. El bloque es opcional en el esquema y sus tres campos
 * obligatorios cuando está.
 *
 * <p>Se escribe entre {@code </detalles>} e {@code <infoAdicional>}. Lo admiten los seis
 * esquemas.
 *
 * @param marca  marca de la máquina
 * @param modelo modelo de la máquina
 * @param serie  número de serie de la máquina
 */
public record MaquinaFiscal(String marca, String modelo, String serie) {

    public static final int LONGITUD_MAXIMA = 30;

    public MaquinaFiscal {
        Textos.validar(marca, LONGITUD_MAXIMA, "maquinaFiscal.marca").lanzarSiInvalida();
        Textos.validar(modelo, LONGITUD_MAXIMA, "maquinaFiscal.modelo").lanzarSiInvalida();
        Textos.validar(serie, LONGITUD_MAXIMA, "maquinaFiscal.serie").lanzarSiInvalida();
    }
}
