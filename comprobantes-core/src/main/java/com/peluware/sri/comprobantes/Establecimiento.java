package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Digitos;
import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.valores.Serie;
import org.jspecify.annotations.Nullable;

/**
 * Establecimiento desde el que se emite, con su punto de emisión.
 *
 * <p>Ficha Técnica v2.34, Tabla 11, campos 5, 6 y 7: código del establecimiento emisor, código
 * del punto de emisión y dirección del establecimiento emisor. El esquema los escribe en dos
 * bloques distintos: {@code <estab>} y {@code <ptoEmi>} en {@code infoTributaria},
 * {@code <dirEstablecimiento>} en el bloque de información del comprobante.
 *
 * <p>Los datos del contribuyente que no cambian por local están en {@link Emisor}.
 *
 * @param codigo       código del establecimiento, tres dígitos; {@code 001} es la matriz
 * @param puntoEmision código del punto de emisión, tres dígitos
 * @param direccion    dirección del establecimiento; {@code null} si no se declara
 */
public record Establecimiento(String codigo, String puntoEmision, @Nullable String direccion) {

    /**
     * Longitud máxima de la dirección, como {@code dirEstablecimiento} en el esquema.
     */
    public static final int DIRECCION_LONGITUD_MAXIMA = 300;

    /**
     * Nombre del campo de la dirección en los defectos de validación, que es su tag.
     */
    public static final String CAMPO_DIRECCION = "dirEstablecimiento";

    public Establecimiento {
        Serie.validar(codigo, puntoEmision).lanzarSiInvalida();
        if (direccion != null) {
            Textos.validar(direccion, DIRECCION_LONGITUD_MAXIMA, CAMPO_DIRECCION).lanzarSiInvalida();
        }
    }

    /**
     * Establecimiento sin dirección declarada.
     */
    public Establecimiento(String codigo, String puntoEmision) {
        this(codigo, puntoEmision, null);
    }

    /**
     * Establecimiento sin dirección, rellenando ambos códigos con ceros a la izquierda.
     */
    public static Establecimiento of(int codigo, int puntoEmision) {
        return of(codigo, puntoEmision, null);
    }

    /**
     * Establecimiento rellenando ambos códigos con ceros a la izquierda.
     */
    public static Establecimiento of(int codigo, int puntoEmision, @Nullable String direccion) {
        return new Establecimiento(Digitos.pad(codigo, Serie.ESTABLECIMIENTO_LONGITUD), Digitos.pad(puntoEmision, Serie.PUNTO_EMISION_LONGITUD), direccion);
    }

    /**
     * La numeración que forman sus dos códigos, tal como la lleva la clave de acceso.
     */
    public Serie serie() {
        return new Serie(codigo, puntoEmision);
    }

    /**
     * El mismo establecimiento con otra dirección.
     */
    public Establecimiento conDireccion(@Nullable String direccion) {
        return new Establecimiento(codigo, puntoEmision, direccion);
    }

    @Override
    public String toString() {
        return codigo + "-" + puntoEmision;
    }
}
