package com.peluware.sri.comprobantes.notadebito;

import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.InfoNota;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Bloque {@code <infoNotaDebito>}.
 *
 * <p>Añade a {@link InfoNota} los totales y las formas de pago: el recargo que declara esta nota
 * es un importe nuevo y sí se puede pagar, al contrario que una nota de crédito.
 *
 * <p>Su esquema no declara moneda.
 *
 * <p>Se obtiene de {@link NotaDebito#info()}. No es un {@code record} para que el esquema
 * pueda ganar campos sin cambiar ninguna firma pública: su constructor no sale del paquete.
 */
public final class InfoNotaDebito extends InfoNota {

    private final TotalesNotaDebito totales;
    private final List<Pago> pagos;

    InfoNotaDebito(LocalDate fechaEmision, Comprador comprador, @Nullable String rise, DocumentoModificado documentoModificado, TotalesNotaDebito totales, List<Pago> pagos) {
        super(fechaEmision, comprador, rise, documentoModificado);
        Objects.requireNonNull(totales, "totales");

        var copia = List.copyOf(pagos);
        if (!copia.isEmpty()) {
            var pagado = Dinero.CERO;
            for (var pago : copia) {
                pagado = pagado.mas(pago.total());
            }
            if (!pagado.equals(totales.valorTotal())) {
                throw new ValidacionException(new Validacion.ReglaInvalida("Las formas de pago suman " + pagado + " y el valor total es " + totales.valorTotal()));
            }
        }

        this.totales = totales;
        this.pagos = copia;
    }

    /**
     * Campos que totalizan la nota de débito.
     */
    public TotalesNotaDebito totales() {
        return totales;
    }

    /**
     * Formas de pago; vacío si la nota no las detalla.
     */
    public List<Pago> pagos() {
        return pagos;
    }
}
