package com.peluware.sri.comprobantes.notadebito;

import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.util.List;
import java.util.Objects;

/**
 * Campos que totalizan una nota de débito.
 *
 * <p>Su esquema no lleva detalles: los impuestos se declaran directamente sobre una base, y el
 * valor total es esa base más los impuestos.
 *
 * <p>Solo se obtiene con {@link #de(Dinero, List)}.
 */
public final class TotalesNotaDebito {

    private final Dinero totalSinImpuestos;
    private final List<Impuesto> impuestos;
    private final Dinero valorTotal;

    private TotalesNotaDebito(Dinero totalSinImpuestos, List<Impuesto> impuestos, Dinero valorTotal) {
        this.totalSinImpuestos = totalSinImpuestos;
        this.impuestos = List.copyOf(impuestos);
        this.valorTotal = valorTotal;
    }

    /**
     * Calcula el valor total a partir de la base y sus impuestos.
     *
     * @throws ValidacionException si no se declara ningún impuesto
     */
    /**
     * Totales tal como los trae el XML, sin recalcularlos desde los detalles.
     */
    static TotalesNotaDebito dados(Dinero totalSinImpuestos, List<Impuesto> impuestos, Dinero valorTotal) {
        return new TotalesNotaDebito(totalSinImpuestos, List.copyOf(impuestos), valorTotal);
    }

    public static TotalesNotaDebito de(Dinero totalSinImpuestos, List<Impuesto> impuestos) {
        Objects.requireNonNull(totalSinImpuestos, "totalSinImpuestos");
        if (impuestos.isEmpty()) {
            throw new ValidacionException(new Validacion.CampoInvalido("impuestos", "La nota de débito debe declarar al menos un impuesto"));
        }

        var valorTotal = totalSinImpuestos;
        for (var impuesto : impuestos) {
            valorTotal = valorTotal.mas(impuesto.valor());
        }
        return new TotalesNotaDebito(totalSinImpuestos, impuestos, valorTotal);
    }

    /**
     * Base sobre la que se calculan los impuestos.
     */
    public Dinero totalSinImpuestos() {
        return totalSinImpuestos;
    }

    /**
     * Impuestos que gravan la base.
     */
    public List<Impuesto> impuestos() {
        return impuestos;
    }

    /**
     * Base más impuestos.
     */
    public Dinero valorTotal() {
        return valorTotal;
    }

    @Override
    public String toString() {
        return "TotalesNotaDebito[valorTotal=" + valorTotal + "]";
    }
}
