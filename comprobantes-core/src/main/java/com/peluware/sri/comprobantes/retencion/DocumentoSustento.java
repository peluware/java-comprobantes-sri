package com.peluware.sri.comprobantes.retencion;

import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.CatalogoException;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.interno.Digitos;
import com.peluware.sri.comprobantes.interno.Textos;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Documento sobre el que se retiene, bloque {@code <docSustento>}.
 *
 * <p>Un comprobante de retención agrupa uno o varios, y cada uno lleva sus impuestos, sus
 * retenciones y sus formas de pago.
 *
 * <p>Sus impuestos son {@link Impuesto}, con el catálogo {@code [235]} de
 * {@link com.peluware.sri.comprobantes.catalogo.CodigoImpuesto}. Su tarifa admite un
 * {@code digitosTotales} menos que la de la factura, 3 contra 4.
 *
 * <p>No se soportan {@code reembolsos}, {@code dividendos}, {@code compraCajBanano} ni los tres
 * totales de reembolso, todos opcionales en el esquema.
 */
public record DocumentoSustento(
    String codigoSustento,
    TipoComprobante tipoComprobante,
    Serie serie,
    Secuencial secuencial,
    LocalDate fechaEmision,
    @Nullable LocalDate fechaRegistroContable,
    @Nullable String numeroAutorizacion,
    PagoLocalExterior pagoLocalExterior,
    @Nullable String tipoRegimenFiscal,
    @Nullable String paisEfectuaPago,
    boolean aplicaConvenioDobleTributacion,
    boolean pagoExteriorSujetoRetencion,
    boolean pagoRegimenFiscalPreferente,
    Dinero totalSinImpuestos,
    Dinero importeTotal,
    List<Impuesto> impuestos,
    List<Retencion> retenciones,
    List<Pago> pagos
) {

    public static final int CODIGO_SUSTENTO_LONGITUD = 2;
    public static final int NUMERO_AUTORIZACION_LONGITUD_MINIMA = 10;
    public static final int NUMERO_AUTORIZACION_LONGITUD_MAXIMA = 49;
    public static final int TIPO_REGIMEN_LONGITUD_MAXIMA = 2;
    public static final int PAIS_LONGITUD_MAXIMA = 3;

    public DocumentoSustento {
        Digitos.validar(codigoSustento, CODIGO_SUSTENTO_LONGITUD, "codSustento").lanzarSiInvalida();
        Objects.requireNonNull(tipoComprobante, "tipoComprobante");
        Objects.requireNonNull(serie, "serie");
        Objects.requireNonNull(secuencial, "secuencial");
        Objects.requireNonNull(fechaEmision, "fechaEmision");
        Objects.requireNonNull(pagoLocalExterior, "pagoLocalExterior");
        Objects.requireNonNull(totalSinImpuestos, "totalSinImpuestos");
        Objects.requireNonNull(importeTotal, "importeTotal");
        if (numeroAutorizacion != null) {
            Digitos.validar(numeroAutorizacion, NUMERO_AUTORIZACION_LONGITUD_MINIMA, NUMERO_AUTORIZACION_LONGITUD_MAXIMA, "numAutDocSustento").lanzarSiInvalida();
        }
        Textos.validarOpcional(tipoRegimenFiscal, TIPO_REGIMEN_LONGITUD_MAXIMA, "tipoRegi").lanzarSiInvalida();
        Textos.validarOpcional(paisEfectuaPago, PAIS_LONGITUD_MAXIMA, "paisEfecPago").lanzarSiInvalida();

        impuestos = List.copyOf(impuestos);
        retenciones = List.copyOf(retenciones);
        pagos = List.copyOf(pagos);

        if (impuestos.isEmpty()) {
            throw new ValidacionException(new Validacion.CampoInvalido("impuestosDocSustento", "El documento sustento debe declarar al menos un impuesto"));
        }
        if (retenciones.isEmpty()) {
            throw new ValidacionException(new Validacion.CampoInvalido("retenciones", "El documento sustento debe declarar al menos una retención"));
        }
        if (pagos.isEmpty()) {
            throw new ValidacionException(new Validacion.CampoInvalido("pagos", "El documento sustento debe declarar al menos una forma de pago"));
        }
        for (var pago : pagos) {
            if (pago.plazo() != null) {
                throw new ValidacionException(new Validacion.ReglaInvalida("El esquema de la retención no declara plazo en sus formas de pago: " + pago));
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Número del documento en el formato que exige este esquema, {@code [0-9]{15}}: quince
     * dígitos seguidos, por ejemplo {@code 001001000000001}.
     *
     * <p>Los esquemas de nota de crédito, nota de débito y guía de remisión usan el patrón
     * {@code [0-9]{3}-[0-9]{3}-[0-9]{9}}, que compone {@link Serie#numeroDocumento(Secuencial)}.
     */
    public String numero() {
        return serie.valor() + secuencial.valor();
    }

    /**
     * Suma de lo retenido sobre este documento.
     */
    public Dinero totalRetenido() {
        var total = Dinero.CERO;
        for (var retencion : retenciones) {
            total = total.mas(retencion.valorRetenido());
        }
        return total;
    }

    /**
     * Si el pago se hizo en el país o al exterior, tag {@code <pagoLocExt>}.
     */
    public enum PagoLocalExterior {

        LOCAL("01"),
        EXTERIOR("02");

        private final String codigo;

        PagoLocalExterior(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }

        /**
         * El valor con ese código.
         *
         * @throws CatalogoException si el código no es de este catálogo
         */
        public static PagoLocalExterior of(String codigo) {
            Objects.requireNonNull(codigo, "codigo");
            for (var valor : values()) {
                if (valor.codigo.equals(codigo)) {
                    return valor;
                }
            }
            throw new CatalogoException("Código de pago local o exterior desconocido: " + codigo);
        }
    }

    /**
     * Construye un documento sustento con sus impuestos, retenciones y pagos.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder {

        private @Nullable String codigoSustento;
        private @Nullable TipoComprobante tipoComprobante;
        private @Nullable Serie serie;
        private @Nullable Secuencial secuencial;
        private @Nullable LocalDate fechaEmision;
        private @Nullable LocalDate fechaRegistroContable;
        private @Nullable String numeroAutorizacion;
        private PagoLocalExterior pagoLocalExterior = PagoLocalExterior.LOCAL;
        private @Nullable String tipoRegimenFiscal;
        private @Nullable String paisEfectuaPago;
        private boolean aplicaConvenioDobleTributacion;
        private boolean pagoExteriorSujetoRetencion;
        private boolean pagoRegimenFiscalPreferente;
        private @Nullable Dinero totalSinImpuestos;
        private @Nullable Dinero importeTotal;
        private final List<Impuesto> impuestos = new ArrayList<>();
        private final List<Retencion> retenciones = new ArrayList<>();
        private final List<Pago> pagos = new ArrayList<>();

        private Builder() {
        }

        /**
         * Código del sustento, de dos dígitos, de la Tabla 17.
         */
        public Builder codigoSustento(String codigoSustento) {
            this.codigoSustento = codigoSustento;
            return this;
        }

        /**
         * Identifica el documento sobre el que se retiene.
         */
        public Builder documento(TipoComprobante tipoComprobante, Serie serie, Secuencial secuencial, LocalDate fechaEmision) {
            this.tipoComprobante = tipoComprobante;
            this.serie = serie;
            this.secuencial = secuencial;
            this.fechaEmision = fechaEmision;
            return this;
        }

        public Builder fechaRegistroContable(@Nullable LocalDate fechaRegistroContable) {
            this.fechaRegistroContable = fechaRegistroContable;
            return this;
        }

        public Builder numeroAutorizacion(@Nullable String numeroAutorizacion) {
            this.numeroAutorizacion = numeroAutorizacion;
            return this;
        }

        /**
         * Por omisión, {@link PagoLocalExterior#LOCAL}.
         */
        public Builder pagoLocalExterior(PagoLocalExterior pagoLocalExterior) {
            this.pagoLocalExterior = Objects.requireNonNull(pagoLocalExterior, "pagoLocalExterior");
            return this;
        }

        /**
         * Régimen fiscal del exterior y país en que se efectuó el pago, para pagos al exterior.
         */
        public Builder exterior(@Nullable String tipoRegimenFiscal, @Nullable String paisEfectuaPago) {
            this.tipoRegimenFiscal = tipoRegimenFiscal;
            this.paisEfectuaPago = paisEfectuaPago;
            return this;
        }

        public Builder aplicaConvenioDobleTributacion(boolean aplica) {
            this.aplicaConvenioDobleTributacion = aplica;
            return this;
        }

        public Builder pagoExteriorSujetoRetencion(boolean sujeto) {
            this.pagoExteriorSujetoRetencion = sujeto;
            return this;
        }

        public Builder pagoRegimenFiscalPreferente(boolean preferente) {
            this.pagoRegimenFiscalPreferente = preferente;
            return this;
        }

        /**
         * Totales del documento sobre el que se retiene, tal como los declara él mismo.
         */
        public Builder totales(Dinero totalSinImpuestos, Dinero importeTotal) {
            this.totalSinImpuestos = totalSinImpuestos;
            this.importeTotal = importeTotal;
            return this;
        }

        public Builder impuesto(Impuesto impuesto) {
            impuestos.add(Objects.requireNonNull(impuesto, "impuesto"));
            return this;
        }

        public Builder retencion(Retencion retencion) {
            retenciones.add(Objects.requireNonNull(retencion, "retencion"));
            return this;
        }

        /**
         * Añade una forma de pago. El esquema de la retención no admite plazo.
         */
        public Builder pago(Pago pago) {
            pagos.add(Objects.requireNonNull(pago, "pago"));
            return this;
        }

        public DocumentoSustento build() {
            return new DocumentoSustento(
                Objects.requireNonNull(codigoSustento, "codigoSustento"),
                Objects.requireNonNull(tipoComprobante, "tipoComprobante"),
                Objects.requireNonNull(serie, "serie"),
                Objects.requireNonNull(secuencial, "secuencial"),
                Objects.requireNonNull(fechaEmision, "fechaEmision"),
                fechaRegistroContable,
                numeroAutorizacion,
                pagoLocalExterior,
                tipoRegimenFiscal,
                paisEfectuaPago,
                aplicaConvenioDobleTributacion,
                pagoExteriorSujetoRetencion,
                pagoRegimenFiscalPreferente,
                Objects.requireNonNull(totalSinImpuestos, "totalSinImpuestos"),
                Objects.requireNonNull(importeTotal, "importeTotal"),
                impuestos,
                retenciones,
                pagos
            );
        }
    }
}
