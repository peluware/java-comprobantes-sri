package com.peluware.sri.comprobantes;

/**
 * Resultado de validar un comprobante o uno de sus campos.
 */
public sealed interface Validacion {

    /**
     * La validación se cumple.
     */
    record Valida() implements Validacion {
    }

    /**
     * La validación no se cumple.
     */
    sealed interface Invalida extends Validacion {

        /**
         * Descripción técnica del defecto.
         */
        String mensaje();
    }

    /**
     * Un campo no cumple el formato que exige el XSD.
     *
     * @param campo   nombre del campo, tal como lo nombra el XSD
     * @param mensaje descripción del defecto
     */
    record CampoInvalido(String campo, String mensaje) implements Invalida {
    }

    /**
     * Una regla que abarca varios campos no se cumple, como la correspondencia entre los
     * detalles y los totales.
     *
     * @param mensaje descripción del defecto
     */
    record ReglaInvalida(String mensaje) implements Invalida {
    }

    /**
     * Resultado válido.
     */
    static Validacion valida() {
        return new Valida();
    }

    /**
     * Indica si la validación se cumple.
     */
    default boolean esValida() {
        return this instanceof Valida;
    }

    /**
     * Lanza una {@link ValidacionException} si la validación no se cumple.
     *
     * @throws ValidacionException si el resultado es {@link Invalida}
     */
    default void lanzarSiInvalida() {
        if (this instanceof Invalida invalida) {
            throw new ValidacionException(invalida);
        }
    }
}
