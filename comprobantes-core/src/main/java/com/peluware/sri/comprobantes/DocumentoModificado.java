package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Documento al que una nota de crédito modifica.
 *
 * <p>Alimenta los tags {@code codDocModificado}, {@code numDocModificado} y
 * {@code fechaEmisionDocSustento}. El esquema restringe el número al patrón
 * {@code [0-9]{3}-[0-9]{3}-[0-9]{9}}: la serie y el secuencial del documento.
 *
 * @param tipoComprobante tipo del documento modificado, de la Tabla 3
 * @param serie           establecimiento y punto de emisión del documento modificado
 * @param secuencial      secuencial del documento modificado
 * @param fechaEmision    fecha de emisión del documento modificado
 */
public record DocumentoModificado(TipoComprobante tipoComprobante, Serie serie, Secuencial secuencial, LocalDate fechaEmision) {

    public DocumentoModificado {
        Objects.requireNonNull(tipoComprobante, "tipoComprobante");
        Objects.requireNonNull(serie, "serie");
        Objects.requireNonNull(secuencial, "secuencial");
        Objects.requireNonNull(fechaEmision, "fechaEmision");
    }

    /**
     * Número del documento modificado en el formato que exige el esquema, por ejemplo
     * {@code 001-001-000000001}.
     */
    public String numero() {
        return serie.numeroDocumento(secuencial);
    }

    @Override
    public String toString() {
        return "DocumentoModificado[" + tipoComprobante + " " + numero() + "]";
    }
}
