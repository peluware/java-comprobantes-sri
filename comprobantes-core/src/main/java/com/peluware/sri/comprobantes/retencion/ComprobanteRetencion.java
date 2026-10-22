package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.ConfiguracionException;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.PeriodoFiscal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Comprobante de retención: declara el impuesto que un agente de retención descuenta a su
 * proveedor.
 *
 * <p>Su contraparte es un {@link SujetoRetenido}. Agrupa uno o varios
 * {@link DocumentoSustento}, cada uno con sus impuestos, sus retenciones y sus formas de pago.
 *
 * <p>Solo se construye con {@link #builder()}, que calcula la clave de acceso.
 */
public final class ComprobanteRetencion extends Comprobante {

    private final InfoComprobanteRetencion infoCompRetencion;
    private final List<DocumentoSustento> documentosSustento;

    private ComprobanteRetencion(Builder builder, InfoComprobanteRetencion infoCompRetencion) {
        super(builder);
        this.infoCompRetencion = infoCompRetencion;
        this.documentosSustento = List.copyOf(builder.documentosSustento);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Tipo de comprobante, siempre {@link TipoComprobante#COMPROBANTE_RETENCION}. */
    @Override
    public TipoComprobante tipoComprobante() {
        return TipoComprobante.COMPROBANTE_RETENCION;
    }

    public InfoComprobanteRetencion infoCompRetencion() {
        return infoCompRetencion;
    }

    public List<DocumentoSustento> documentosSustento() {
        return documentosSustento;
    }

    /**
     * Suma de lo retenido en todos sus documentos sustento.
     */
    public Dinero totalRetenido() {
        var total = Dinero.CERO;
        for (var documento : documentosSustento) {
            total = total.mas(documento.totalRetenido());
        }
        return total;
    }

    /**
     * Construye un comprobante de retención calculando la clave de acceso.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends Comprobante.Builder<Builder> {

        private @Nullable SujetoRetenido sujetoRetenido;
        private @Nullable PeriodoFiscal periodoFiscal;
        private final List<DocumentoSustento> documentosSustento = new ArrayList<>();

        private Builder() {
        }

        @Override
        protected TipoComprobante tipoComprobante() {
            return TipoComprobante.COMPROBANTE_RETENCION;
        }

        public Builder sujetoRetenido(SujetoRetenido sujetoRetenido) {
            this.sujetoRetenido = sujetoRetenido;
            return this;
        }

        /**
         * Periodo fiscal al que corresponde la retención, en {@code MM/AAAA}.
         */
        public Builder periodoFiscal(PeriodoFiscal periodoFiscal) {
            this.periodoFiscal = periodoFiscal;
            return this;
        }

        public Builder documentoSustento(DocumentoSustento documento) {
            documentosSustento.add(Objects.requireNonNull(documento, "documentoSustento"));
            return this;
        }

        /**
         * Construye el comprobante de retención, calculando la clave de acceso.
         *
         * @throws ValidacionException    si falta algún dato obligatorio o no hay documentos
         * @throws ConfiguracionException si falta el código numérico o se indicaron dos a la vez
         */
        public ComprobanteRetencion build() {
            validarComun();
            if (documentosSustento.isEmpty()) {
                throw new ValidacionException(new Validacion.CampoInvalido("docsSustento", "El comprobante de retención debe llevar al menos un documento sustento"));
            }

            var info = new InfoComprobanteRetencion(
                fechaEmisionExigida(),
                Objects.requireNonNull(sujetoRetenido, "sujetoRetenido"),
                Objects.requireNonNull(periodoFiscal, "periodoFiscal")
            );

            return new ComprobanteRetencion(this, info);
        }
    }
}
