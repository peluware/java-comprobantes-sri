package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.interno.Digitos;
import org.jspecify.annotations.Nullable;

/**
 * Serie: establecimiento de tres dígitos y punto de emisión de tres dígitos.
 *
 * <p>Ficha Técnica v2.34, Tabla 1, campo 5. Corresponde a los tags {@code <estab>}
 * y {@code <ptoEmi>}, que es como los abrevia el esquema.
 *
 * @param establecimiento código del establecimiento emisor, tag {@code <estab>}
 * @param puntoEmision    código del punto de emisión, tag {@code <ptoEmi>}
 */
public record Serie(String establecimiento, String puntoEmision) {

    public static final int ESTABLECIMIENTO_LONGITUD = 3;
    public static final int PUNTO_EMISION_LONGITUD = 3;

    /** Nombres de los campos en los defectos de validación, que son los tags del esquema. */
    public static final String CAMPO_ESTABLECIMIENTO = "estab";
    public static final String CAMPO_PUNTO_EMISION = "ptoEmi";

    /** Separador entre establecimiento, punto de emisión y secuencial. */
    public static final String SEPARADOR = "-";

    public Serie {
        validar(establecimiento, puntoEmision).lanzarSiInvalida();
    }

    /**
     * Construye la serie rellenando ambos campos con ceros a la izquierda.
     */
    public static Serie of(int establecimiento, int puntoEmision) {
        return new Serie(Digitos.pad(establecimiento, ESTABLECIMIENTO_LONGITUD), Digitos.pad(puntoEmision, PUNTO_EMISION_LONGITUD));
    }

    /**
     * Valida un par de cadenas como serie sin construirla.
     */
    public static Validacion validar(@Nullable String establecimiento, @Nullable String puntoEmision) {
        var primero = Digitos.validar(establecimiento, ESTABLECIMIENTO_LONGITUD, CAMPO_ESTABLECIMIENTO);
        return primero.esValida() ? Digitos.validar(puntoEmision, PUNTO_EMISION_LONGITUD, CAMPO_PUNTO_EMISION) : primero;
    }

    /**
     * Indica si el par es una serie válida.
     */
    public static boolean esValido(@Nullable String establecimiento, @Nullable String puntoEmision) {
        return validar(establecimiento, puntoEmision).esValida();
    }

    /**
     * Los seis dígitos tal como aparecen en la clave de acceso.
     */
    public String valor() {
        return establecimiento + puntoEmision;
    }

    /**
     * El número completo de un documento de esta serie, por ejemplo {@code 001-001-000000001}.
     *
     * <p>Es el formato que los esquemas exigen en {@code numDocModificado} y en
     * {@code numDocSustento}, los dos con el patrón {@code [0-9]{3}-[0-9]{3}-[0-9]{9}}.
     */
    public String numeroDocumento(Secuencial secuencial) {
        return establecimiento + SEPARADOR + puntoEmision + SEPARADOR + secuencial.valor();
    }

    @Override
    public String toString() {
        return establecimiento + "-" + puntoEmision;
    }
}
