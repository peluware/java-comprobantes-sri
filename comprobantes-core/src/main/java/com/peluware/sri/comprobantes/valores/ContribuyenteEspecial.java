package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Número de resolución con que el SRI designa a un contribuyente especial.
 *
 * <p>Corresponde al tipo {@code contribuyenteEspecial} del XSD, con longitud entre 3 y 13 y
 * patrón {@code ([A-Za-z0-9])*}, idéntico en los esquemas de factura, nota de crédito y nota
 * de débito.
 */
public record ContribuyenteEspecial(String valor) {

    public static final int LONGITUD_MINIMA = 3;
    public static final int LONGITUD_MAXIMA = 13;

    /** Nombre del campo en los defectos de validación. */
    public static final String CAMPO = "contribuyenteEspecial";

    private static final Pattern ALFANUMERICO = Pattern.compile("[A-Za-z0-9]+");

    public ContribuyenteEspecial {
        validar(valor).lanzarSiInvalida();
    }

    /**
     * Valida una cadena como número de resolución sin construirlo.
     */
    public static Validacion validar(@Nullable String valor) {
        if (valor == null || valor.length() < LONGITUD_MINIMA || valor.length() > LONGITUD_MAXIMA || !ALFANUMERICO.matcher(valor).matches()) {
            return new Validacion.CampoInvalido(CAMPO, CAMPO + " admite entre " + LONGITUD_MINIMA + " y " + LONGITUD_MAXIMA + " caracteres alfanuméricos: " + valor);
        }
        return Validacion.valida();
    }

    /**
     * Indica si {@code valor} es un número de resolución válido.
     */
    public static boolean esValido(@Nullable String valor) {
        return validar(valor).esValida();
    }

    /**
     * Construye el número de resolución, o {@code null} si no se indicó ninguno.
     */
    public static @Nullable ContribuyenteEspecial of(@Nullable String valor) {
        return valor == null ? null : new ContribuyenteEspecial(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
