package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.interno.Digitos;
import org.jspecify.annotations.Nullable;

/**
 * RUC del emisor: trece dígitos terminados en {@code 001}.
 *
 * <p>Corresponde al tipo {@code numeroRuc} del XSD, con patrón {@code [0-9]{10}001}, idéntico
 * en las versiones 1.0.0, 1.1.0, 2.0.0 y 2.1.0 del esquema. El establecimiento se identifica
 * en el tag {@code <estab>}, no en el sufijo del RUC.
 *
 * <p>No verifica el dígito verificador del RUC.
 */
public record Ruc(String valor) {

    public static final int LONGITUD = 13;

    /** Sufijo que exige el XSD para el RUC del emisor. */
    public static final String SUFIJO = "001";

    public Ruc {
        validar(valor).lanzarSiInvalida();
    }

    /**
     * Valida una cadena como RUC sin construirlo.
     */
    public static Validacion validar(@Nullable String valor) {
        var digitos = Digitos.validar(valor, LONGITUD, "ruc");
        if (valor == null || !digitos.esValida()) {
            return digitos;
        }
        if (!valor.endsWith(SUFIJO)) {
            return new Validacion.CampoInvalido("ruc", "ruc debe terminar en " + SUFIJO + ", según el patrón [0-9]{10}" + SUFIJO + " del tipo numeroRuc del XSD: " + valor);
        }
        return Validacion.valida();
    }

    /**
     * Indica si {@code valor} es un RUC válido.
     */
    public static boolean esValido(@Nullable String valor) {
        return validar(valor).esValida();
    }

    @Override
    public String toString() {
        return valor;
    }
}
