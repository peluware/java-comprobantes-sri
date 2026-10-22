package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Pago de un comprobante, correspondiente al bloque {@code <pago>}.
 *
 * @param formaPago forma de pago de la Tabla 24
 * @param total     importe pagado con esta forma
 * @param plazo     plazo concedido; {@code null} si el pago es inmediato
 */
public record Pago(FormaPago formaPago, Dinero total, @Nullable Plazo plazo) {

    public Pago {
        Objects.requireNonNull(formaPago, "formaPago");
        Objects.requireNonNull(total, "total");
    }

    /**
     * Pago sin plazo.
     */
    public static Pago of(FormaPago formaPago, Dinero total) {
        return new Pago(formaPago, total, null);
    }

    /**
     * Pago con un plazo expresado en días.
     */
    public static Pago aPlazo(FormaPago formaPago, Dinero total, int dias) {
        return new Pago(formaPago, total, Plazo.enDias(dias));
    }
}
