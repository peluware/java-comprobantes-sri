package com.peluware.sri.comprobantes.notacredito;

import com.peluware.sri.comprobantes.ComprobanteNota;
import com.peluware.sri.comprobantes.ConfiguracionException;
import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Nota de crédito electrónica.
 *
 * <p>Modifica a la baja un documento ya emitido: el que indica su {@link DocumentoModificado}.
 * Justifica la corrección detallando lo que vuelve, así que lleva líneas de producto.
 *
 * <p>Solo se construye con {@link #builder()}, que calcula la clave de acceso y los totales a
 * partir de los demás campos.
 */
public final class NotaCredito extends ComprobanteNota {

    private final InfoNotaCredito infoNotaCredito;
    private final List<DetalleNotaCredito> detalles;

    private NotaCredito(Builder builder, InfoNotaCredito infoNotaCredito) {
        super(builder);
        this.infoNotaCredito = infoNotaCredito;
        this.detalles = List.copyOf(builder.detalles);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Tipo de comprobante, siempre {@link TipoComprobante#NOTA_CREDITO}. */
    @Override
    public TipoComprobante tipoComprobante() {
        return TipoComprobante.NOTA_CREDITO;
    }

    @Override
    public InfoNotaCredito info() {
        return infoNotaCredito;
    }

    /**
     * Líneas de lo que se corrige, al menos una.
     */
    public List<DetalleNotaCredito> detalles() {
        return detalles;
    }

    /**
     * Construye una nota de crédito calculando la clave de acceso y los totales.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends ComprobanteNota.Builder<Builder> {

        private @Nullable String motivo;
        private @Nullable TotalesNotaCredito totales;
        private String moneda = Sri.MONEDA;
        private final List<DetalleNotaCredito> detalles = new ArrayList<>();

        private Builder() {
        }

        @Override
        protected TipoComprobante tipoComprobante() {
            return TipoComprobante.NOTA_CREDITO;
        }

        /**
         * Razón por la que se emite.
         */
        public Builder motivo(String motivo) {
            this.motivo = motivo;
            return this;
        }

        /**
         * Por omisión, {@link Sri#MONEDA}.
         */
        public Builder moneda(String moneda) {
            this.moneda = Objects.requireNonNull(moneda, "moneda");
            return this;
        }

        public Builder detalle(DetalleNotaCredito detalle) {
            detalles.add(Objects.requireNonNull(detalle, "detalle"));
            return this;
        }

        public Builder detalles(List<DetalleNotaCredito> detalles) {
            this.detalles.addAll(detalles);
            return this;
        }

        /**
         * Totales tal como los trae el XML, en vez de derivarlos de los detalles.
         */
        Builder totales(TotalesNotaCredito totales) {
            this.totales = totales;
            return this;
        }

        /**
         * Construye la nota de crédito, calculando la clave de acceso y los totales.
         *
         * @throws ValidacionException    si falta algún dato obligatorio o los detalles están vacíos
         * @throws ConfiguracionException si falta el código numérico o se indicaron dos a la vez
         */
        public NotaCredito build() {
            validarComun();
            if (detalles.isEmpty()) {
                throw new ValidacionException(new Validacion.CampoInvalido("detalles", "La nota de crédito debe tener al menos un detalle"));
            }

            var infoNotaCredito = new InfoNotaCredito(
                fechaEmisionExigida(),
                compradorExigido(),
                riseIndicado(),
                documentoModificadoExigido(),
                totales == null ? TotalesNotaCredito.de(detalles) : totales,
                moneda,
                Objects.requireNonNull(motivo, "motivo")
            );

            return new NotaCredito(this, infoNotaCredito);
        }
    }
}
