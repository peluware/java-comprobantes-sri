package com.peluware.sri.comprobantes.catalogo;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;

import java.util.List;
import java.util.Objects;

/**
 * Forma de pago, correspondiente al campo {@code <formaPago>}.
 *
 * <p>Ficha Técnica v2.34, Tabla 24, tomada del Catálogo del Anexo Transaccional Simplificado.
 * Las formas de la tabla están como constantes.
 *
 * <p>El esquema no acota el catálogo: admite los códigos del {@code 01} al {@code 21}, de los
 * que la tabla nombra ocho. Una forma que el SRI añada se construye con el constructor.
 */
public record FormaPago(String codigo) {

    public static final FormaPago SIN_UTILIZACION_SISTEMA_FINANCIERO = new FormaPago("01");
    public static final FormaPago COMPENSACION_DEUDAS = new FormaPago("15");
    public static final FormaPago TARJETA_DEBITO = new FormaPago("16");
    public static final FormaPago DINERO_ELECTRONICO = new FormaPago("17");
    public static final FormaPago TARJETA_PREPAGO = new FormaPago("18");
    public static final FormaPago TARJETA_CREDITO = new FormaPago("19");
    public static final FormaPago OTROS_CON_UTILIZACION_SISTEMA_FINANCIERO = new FormaPago("20");
    public static final FormaPago ENDOSO_TITULOS = new FormaPago("21");

    private static final int CODIGO_MIN = 1;
    private static final int CODIGO_MAX = 21;

    private static final List<FormaPago> VALORES = List.of(
        SIN_UTILIZACION_SISTEMA_FINANCIERO,
        COMPENSACION_DEUDAS,
        TARJETA_DEBITO,
        DINERO_ELECTRONICO,
        TARJETA_PREPAGO,
        TARJETA_CREDITO,
        OTROS_CON_UTILIZACION_SISTEMA_FINANCIERO,
        ENDOSO_TITULOS
    );

    public FormaPago {
        Objects.requireNonNull(codigo, "codigo");
        if (!esCodigoDelEsquema(codigo)) {
            throw new ValidacionException(new Validacion.CampoInvalido("formaPago", "formaPago admite dos dígitos del " + CODIGO_MIN + " al " + CODIGO_MAX + ": " + codigo));
        }
    }

    public static FormaPago of(String codigo) {
        return new FormaPago(codigo);
    }

    /**
     * Formas de pago que enumera la Tabla 24.
     */
    public static List<FormaPago> valores() {
        return VALORES;
    }

    /**
     * Indica si la forma de pago es una de las que enumera la Tabla 24.
     */
    public boolean esConocida() {
        return VALORES.contains(this);
    }

    private static boolean esCodigoDelEsquema(String codigo) {
        if (codigo.length() != 2 || !codigo.chars().allMatch(Character::isDigit)) {
            return false;
        }
        var valor = Integer.parseInt(codigo);
        return valor >= CODIGO_MIN && valor <= CODIGO_MAX;
    }
}
