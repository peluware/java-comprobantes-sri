package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.notacredito.DetalleNotaCredito;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.PrecioUnitario;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Línea de un comprobante, correspondiente al bloque {@code <detalle>}.
 *
 * <p>Base de los detalles de factura, nota de crédito y liquidación de compra. Lleva
 * descripción, cantidad, precio unitario, descuento, precio total sin impuesto, datos
 * adicionales e impuestos. Los dos códigos del ítem los declara cada subtipo con el nombre de
 * su esquema.
 *
 * <p>Solo se construye con el builder del subtipo, que calcula el precio total sin impuesto y
 * la base de cada impuesto.
 */
public abstract sealed class Detalle extends Linea permits DetalleComprobanteVenta, DetalleNotaCredito {

    private final PrecioUnitario precioUnitario;
    private final Dinero descuento;
    private final Dinero precioTotalSinImpuesto;
    private final List<Impuesto> impuestos;

    /**
     * Toma del builder los campos comunes y los importes que acaba de calcular.
     */
    protected Detalle(Builder<?, ?> builder, Dinero precioTotalSinImpuesto, List<Impuesto> impuestos) {
        super(builder);
        this.precioUnitario = Objects.requireNonNull(builder.precioUnitario, "precioUnitario");
        this.descuento = builder.descuento;
        this.precioTotalSinImpuesto = precioTotalSinImpuesto;
        this.impuestos = List.copyOf(impuestos);
    }

    /**
     * Precio por unidad antes de descuento e impuestos.
     */
    public PrecioUnitario precioUnitario() {
        return precioUnitario;
    }

    public Dinero descuento() {
        return descuento;
    }

    /**
     * Cantidad por precio unitario, menos el descuento.
     */
    public Dinero precioTotalSinImpuesto() {
        return precioTotalSinImpuesto;
    }

    /**
     * Impuestos de la línea, al menos uno.
     */
    public List<Impuesto> impuestos() {
        return impuestos;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + descripcion() + ", " + cantidad() + " x " + precioUnitario + " = " + precioTotalSinImpuesto + "]";
    }

    /**
     * Importes que {@link Builder#calcular()} deriva de la línea.
     *
     * @param precioTotalSinImpuesto cantidad por precio unitario, menos el descuento
     * @param impuestos              impuestos con su base ya resuelta, ordenados por código
     */
    protected record Calculado(Dinero precioTotalSinImpuesto, List<Impuesto> impuestos) {
    }

    /**
     * Recoge los campos comunes de la línea y calcula el precio total y la base de cada
     * impuesto. El builder de cada subtipo hereda de esta clase y añade sus códigos.
     *
     * <p>El precio total sin impuesto es la cantidad por el precio unitario, redondeado a dos
     * decimales, menos el descuento.
     *
     * <p>Las bases imponibles siguen la cascada de la Tabla 21: el ICE se calcula sobre el
     * precio total sin impuesto y el IVA sobre ese precio más el ICE resultante.
     *
     * <p>No es thread-safe.
     *
     * @param <B> builder del subtipo
     * @param <D> detalle que construye
     */
    public abstract static sealed class Builder<B extends Builder<B, D>, D extends Detalle> extends Linea.Builder<B, D> permits DetalleComprobanteVenta.Builder, DetalleNotaCredito.Builder {

        private @Nullable PrecioUnitario precioUnitario;
        private Dinero descuento = Dinero.CERO;
        private final List<Impuesto> impuestos = new ArrayList<>();
        private final List<Pendiente> pendientes = new ArrayList<>();
        private @Nullable Calculado dados;

        protected Builder() {
        }

        public B precioUnitario(PrecioUnitario precioUnitario) {
            this.precioUnitario = precioUnitario;
            return self();
        }

        public B precioUnitario(String precioUnitario) {
            return precioUnitario(PrecioUnitario.of(precioUnitario));
        }

        /**
         * Por omisión, cero.
         */
        public B descuento(Dinero descuento) {
            this.descuento = Objects.requireNonNull(descuento, "descuento");
            return self();
        }

        /**
         * Aplica IVA sobre el precio total sin impuesto más el ICE.
         */
        public B iva(TarifaIva tarifa) {
            pendientes.add(new Pendiente(CodigoImpuesto.IVA, tarifa.codigo(), tarifa.tarifa()));
            return self();
        }

        /**
         * Aplica ICE ad valorem sobre el precio total sin impuesto.
         */
        public B ice(String codigoPorcentaje, BigDecimal tarifa) {
            pendientes.add(new Pendiente(CodigoImpuesto.ICE, codigoPorcentaje, tarifa));
            return self();
        }

        /**
         * Añade un impuesto ya calculado, con su propia base.
         *
         * <p>Para los impuestos cuya tarifa no es un porcentaje de la base: ICE específico e
         * IRBPNR. Su valor no entra en la cascada que alimenta la base del IVA.
         */
        public B impuesto(Impuesto impuesto) {
            impuestos.add(Objects.requireNonNull(impuesto, "impuesto"));
            return self();
        }

        /**
         * Toma los importes tal como vienen, sin derivarlos de la cantidad y el precio.
         *
         * <p>Para reconstruir un comprobante leído de su XML, donde los importes ya están
         * escritos y el archivo manda.
         */
        protected final B calculado(Dinero precioTotalSinImpuesto, List<Impuesto> impuestos) {
            this.dados = new Calculado(precioTotalSinImpuesto, List.copyOf(impuestos));
            return self();
        }

        /**
         * Construye el detalle del subtipo.
         *
         * @throws ValidacionException si falta algún dato obligatorio, no hay impuestos o el
         *                             descuento excede el precio de la línea
         */
        public abstract D build();

        /**
         * Valida los campos comunes y calcula el precio total y los impuestos. Los subtipos la
         * invocan al principio de su {@code build()}.
         */
        protected final Calculado calcular() {
            if (dados != null) {
                return dados;
            }

            var cantidad = Objects.requireNonNull(cantidadIndicada(), "cantidad");
            var precioUnitario = Objects.requireNonNull(this.precioUnitario, "precioUnitario");

            var bruto = Dinero.redondear(cantidad.valor().multiply(precioUnitario.valor()));
            if (descuento.compareTo(bruto) > 0) {
                throw new ValidacionException(new Validacion.CampoInvalido("descuento", "El descuento " + descuento + " excede el precio total de la línea " + bruto));
            }
            var precioTotalSinImpuesto = bruto.menos(descuento);

            var calculados = crearCalculados(precioTotalSinImpuesto);

            return new Calculado(precioTotalSinImpuesto, List.copyOf(calculados));
        }

        private ArrayList<Impuesto> crearCalculados(Dinero precioTotalSinImpuesto) {
            var calculados = new ArrayList<>(impuestos);
            var ice = Dinero.CERO;
            for (var pendiente : pendientes) {
                if (pendiente.codigo() == CodigoImpuesto.ICE) {
                    var impuesto = pendiente.calcular(precioTotalSinImpuesto);
                    calculados.add(impuesto);
                    ice = ice.mas(impuesto.valor());
                }
            }
            var baseIva = precioTotalSinImpuesto.mas(ice);
            for (var pendiente : pendientes) {
                if (pendiente.codigo() != CodigoImpuesto.ICE) {
                    calculados.add(pendiente.calcular(baseIva));
                }
            }
            calculados.sort(Comparator.comparing(impuesto -> impuesto.codigo().codigo()));
            return calculados;
        }

        private record Pendiente(CodigoImpuesto codigo, String codigoPorcentaje, BigDecimal tarifa) {

            Impuesto calcular(Dinero baseImponible) {
                return Impuesto.of(codigo, codigoPorcentaje, tarifa, baseImponible);
            }
        }
    }
}
