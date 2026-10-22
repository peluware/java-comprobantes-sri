package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.notacredito.InfoNotaCredito;
import com.peluware.sri.comprobantes.notadebito.InfoNotaDebito;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * La cabecera que los bloques {@code <infoNotaCredito>} e {@code <infoNotaDebito>} declaran
 * igual: a quién va dirigida la nota y qué comprobante corrige.
 *
 * <p>Los dos esquemas la declaran en el mismo orden, que no es el de la factura: en las notas el
 * contribuyente especial va después del bloque del comprador y en la factura va antes.
 *
 * <p>Los totales no están aquí: la nota de crédito declara un valor de modificación y la de
 * débito un valor total, y no tienen tipo común.
 */
public abstract sealed class InfoNota permits InfoNotaCredito, InfoNotaDebito {

    public static final int RISE_LONGITUD_MAXIMA = 40;

    private final LocalDate fechaEmision;
    private final Comprador comprador;
    private final @Nullable String rise;
    private final DocumentoModificado documentoModificado;

    /**
     * @throws ValidacionException si el RISE no cabe
     */
    protected InfoNota(LocalDate fechaEmision, Comprador comprador, @Nullable String rise, DocumentoModificado documentoModificado) {
        Objects.requireNonNull(fechaEmision, "fechaEmision");
        Objects.requireNonNull(comprador, "comprador");
        Objects.requireNonNull(documentoModificado, "documentoModificado");
        Textos.validarOpcional(rise, RISE_LONGITUD_MAXIMA, "rise").lanzarSiInvalida();

        this.fechaEmision = fechaEmision;
        this.comprador = comprador;
        this.rise = rise;
        this.documentoModificado = documentoModificado;
    }

    /**
     * Fecha de emisión, tag {@code <fechaEmision>}.
     */
    public final LocalDate fechaEmision() {
        return fechaEmision;
    }

    /**
     * Datos del adquirente al que se dirige la nota.
     */
    public final Comprador comprador() {
        return comprador;
    }

    /**
     * Número de RISE, tag {@code <rise>}; {@code null} si no aplica.
     */
    public final @Nullable String rise() {
        return rise;
    }

    /**
     * El comprobante que esta nota corrige.
     */
    public final DocumentoModificado documentoModificado() {
        return documentoModificado;
    }
}
