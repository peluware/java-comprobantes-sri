package com.peluware.sri.comprobantes;

import java.util.Objects;

/**
 * Un comprobante, o uno de sus campos, no pasa la validación.
 */
public final class ValidacionException extends ComprobanteException {

    private final Validacion.Invalida invalida;

    public ValidacionException(Validacion.Invalida invalida) {
        super(invalida.mensaje());
        this.invalida = Objects.requireNonNull(invalida, "invalida");
    }

    /**
     * Defecto que provocó el error: un {@link Validacion.CampoInvalido} o un
     * {@link Validacion.ReglaInvalida}.
     */
    public Validacion.Invalida invalida() {
        return invalida;
    }

    @Override
    public String mensajeUsuario() {
        return "Los datos del comprobante no son válidos. Revise la información ingresada.";
    }
}
