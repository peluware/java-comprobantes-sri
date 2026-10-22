package com.peluware.sri.comprobantes.notadebito;

import com.peluware.sri.comprobantes.ComprobanteNota;
import com.peluware.sri.comprobantes.ConfiguracionException;
import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Nota de débito electrónica.
 *
 * <p>Modifica al alza un documento ya emitido: el que indica su {@link DocumentoModificado}.
 * En un recargo no se entregó nada que detallar, así que justifica la corrección con
 * {@link Motivo motivos} —una razón y su importe— en vez de líneas de producto.
 *
 * <p>Los impuestos se declaran sobre una base indicada con
 * {@link Builder#totalSinImpuestos(Dinero)}.
 *
 * <p>Solo se construye con {@link #builder()}, que calcula la clave de acceso y el valor total.
 */
public final class NotaDebito extends ComprobanteNota {

    private final InfoNotaDebito infoNotaDebito;
    private final List<Motivo> motivos;

    private NotaDebito(Builder builder, InfoNotaDebito infoNotaDebito) {
        super(builder);
        this.infoNotaDebito = infoNotaDebito;
        this.motivos = List.copyOf(builder.motivos);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Tipo de comprobante, siempre {@link TipoComprobante#NOTA_DEBITO}. */
    @Override
    public TipoComprobante tipoComprobante() {
        return TipoComprobante.NOTA_DEBITO;
    }

    @Override
    public InfoNotaDebito info() {
        return infoNotaDebito;
    }

    /**
     * Razones por las que se emite, al menos una.
     */
    public List<Motivo> motivos() {
        return motivos;
    }

    /**
     * Construye una nota de débito calculando la clave de acceso y el valor total.
     *
     * <p>No es thread-safe.
     */
    public static final class Builder extends ComprobanteNota.Builder<Builder> {

        private @Nullable Dinero totalSinImpuestos;
        private @Nullable TotalesNotaDebito totales;
        private final List<Motivo> motivos = new ArrayList<>();
        private final List<Pago> pagos = new ArrayList<>();
        private final List<Impuesto> impuestos = new ArrayList<>();
        private final List<Pendiente> pendientes = new ArrayList<>();

        private Builder() {
        }

        @Override
        protected TipoComprobante tipoComprobante() {
            return TipoComprobante.NOTA_DEBITO;
        }

        /**
         * Base sobre la que se calculan los impuestos.
         */
        public Builder totalSinImpuestos(Dinero totalSinImpuestos) {
            this.totalSinImpuestos = totalSinImpuestos;
            return this;
        }

        /**
         * Aplica IVA sobre la base.
         */
        public Builder iva(TarifaIva tarifa) {
            pendientes.add(new Pendiente(CodigoImpuesto.IVA, tarifa.codigo(), tarifa.tarifa()));
            return this;
        }

        /**
         * Añade un impuesto ya calculado, con su propia base.
         */
        public Builder impuesto(Impuesto impuesto) {
            impuestos.add(Objects.requireNonNull(impuesto, "impuesto"));
            return this;
        }

        /**
         * Añade una razón por la que se emite, con su importe.
         */
        public Builder motivo(String razon, Dinero valor) {
            motivos.add(new Motivo(razon, valor));
            return this;
        }

        /**
         * Añade una forma de pago. Si se indica alguna, deben sumar el valor total.
         */
        public Builder pago(Pago pago) {
            pagos.add(Objects.requireNonNull(pago, "pago"));
            return this;
        }

        /**
         * Totales tal como los trae el XML, en vez de derivarlos.
         */
        Builder totales(TotalesNotaDebito totales) {
            this.totales = totales;
            return this;
        }

        /**
         * Construye la nota de débito, calculando la clave de acceso y el valor total.
         *
         * @throws ValidacionException    si falta algún dato obligatorio o no hay motivos
         * @throws ConfiguracionException si falta el código numérico o se indicaron dos a la vez
         */
        public NotaDebito build() {
            validarComun();
            if (motivos.isEmpty()) {
                throw new ValidacionException(new Validacion.CampoInvalido("motivos", "La nota de débito debe indicar al menos un motivo"));
            }

            var base = Objects.requireNonNull(totalSinImpuestos, "totalSinImpuestos");
            var calculados = new ArrayList<>(impuestos);
            for (var pendiente : pendientes) {
                calculados.add(Impuesto.of(pendiente.codigo(), pendiente.codigoPorcentaje(), pendiente.tarifa(), base));
            }
            calculados.sort(Comparator.comparing((Impuesto impuesto) -> impuesto.codigo().codigo()));

            var infoNotaDebito = new InfoNotaDebito(
                fechaEmisionExigida(),
                compradorExigido(),
                riseIndicado(),
                documentoModificadoExigido(),
                totales == null ? TotalesNotaDebito.de(base, calculados) : totales,
                pagos
            );

            return new NotaDebito(this, infoNotaDebito);
        }

        private record Pendiente(CodigoImpuesto codigo, String codigoPorcentaje, BigDecimal tarifa) {
        }
    }
}
