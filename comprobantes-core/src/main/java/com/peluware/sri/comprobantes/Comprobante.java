package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoEmision;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencion;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemision;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Comprobante electrónico, en cualquiera de los tipos de la Tabla 3.
 *
 * <p>Lleva emisor, ambiente, tipo de emisión, establecimiento, secuencial, fecha de emisión,
 * clave de acceso, máquina fiscal y campos adicionales. Cada subtipo añade los bloques de su
 * esquema.
 */
public abstract sealed class Comprobante permits ComprobanteNota, ComprobanteRetencion, ComprobanteVenta, GuiaRemision {

    private final Emisor emisor;
    private final Ambiente ambiente;
    private final TipoEmision tipoEmision;
    private final Establecimiento establecimiento;
    private final Secuencial secuencial;
    private final LocalDate fechaEmision;
    private final String claveAcceso;
    private final @Nullable MaquinaFiscal maquinaFiscal;
    private final List<CampoAdicional> infoAdicional;

    /**
     * Toma del builder la identidad del comprobante y calcula la clave de acceso.
     *
     * @param builder builder del subtipo
     */
    protected Comprobante(Builder<?> builder) {
        this.emisor = builder.emisorExigido();
        this.ambiente = Objects.requireNonNull(builder.ambiente, "ambiente");
        this.tipoEmision = builder.tipoEmision;
        this.establecimiento = Objects.requireNonNull(builder.establecimiento, "establecimiento");
        this.secuencial = Objects.requireNonNull(builder.secuencial, "secuencial");
        this.fechaEmision = builder.fechaEmisionExigida();
        this.claveAcceso = builder.generarClaveAcceso();
        this.maquinaFiscal = builder.maquinaFiscal;
        this.infoAdicional = List.copyOf(builder.infoAdicional);
    }

    /**
     * Contribuyente que emite el comprobante, cuyo RUC ocupa los dígitos 11 a 23 de la clave de
     * acceso.
     */
    public Emisor emisor() {
        return emisor;
    }

    /**
     * Ambiente al que se destina el comprobante, dígito 24 de la clave de acceso.
     */
    public Ambiente ambiente() {
        return ambiente;
    }

    /**
     * Tipo de emisión, dígito 48 de la clave de acceso.
     */
    public TipoEmision tipoEmision() {
        return tipoEmision;
    }

    /**
     * Establecimiento y punto de emisión desde los que se emitió.
     */
    public Establecimiento establecimiento() {
        return establecimiento;
    }

    /**
     * Establecimiento y punto de emisión como numeración de seis dígitos, tal como viaja en la
     * clave de acceso.
     */
    public Serie serie() {
        return establecimiento.serie();
    }

    /**
     * Máquina fiscal desde la que se emitió, o {@code null} si no se emitió desde una.
     *
     * <p>Ficha Técnica v2.34, Anexo 13.
     */
    public @Nullable MaquinaFiscal maquinaFiscal() {
        return maquinaFiscal;
    }

    /**
     * Secuencial de nueve dígitos, correlativo por establecimiento y punto de emisión.
     */
    public Secuencial secuencial() {
        return secuencial;
    }

    /**
     * Fecha de emisión, los ocho primeros dígitos de la clave de acceso.
     */
    public LocalDate fechaEmision() {
        return fechaEmision;
    }

    /**
     * Clave de acceso de 49 dígitos, que en el esquema offline es también el número de
     * autorización.
     */
    public String claveAcceso() {
        return claveAcceso;
    }

    /**
     * Campos adicionales del comprobante, hasta {@link CampoAdicional#MAX_POR_COMPROBANTE}.
     */
    public List<CampoAdicional> infoAdicional() {
        return infoAdicional;
    }

    /**
     * Tipo de comprobante dentro de la Tabla 3.
     */
    public abstract TipoComprobante tipoComprobante();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + claveAcceso + "]";
    }

    /**
     * Recoge la identidad de la que sale la clave de acceso y los campos adicionales. El builder
     * de cada subtipo hereda de esta clase y añade los suyos.
     *
     * <p>No es thread-safe.
     *
     * @param <B> builder del subtipo
     */
    public abstract static sealed class Builder<B extends Builder<B>> permits ComprobanteNota.Builder, ComprobanteRetencion.Builder, ComprobanteVenta.Builder, GuiaRemision.Builder {

        private @Nullable Emisor emisor;
        private @Nullable LocalDate fechaEmision;
        private @Nullable Ambiente ambiente;
        private @Nullable Establecimiento establecimiento;
        private @Nullable MaquinaFiscal maquinaFiscal;
        private @Nullable Secuencial secuencial;
        private @Nullable CodigoNumerico codigoNumerico;
        private @Nullable EstrategiaCodigoNumerico estrategiaCodigoNumerico;
        private TipoEmision tipoEmision = TipoEmision.NORMAL;
        private final List<CampoAdicional> infoAdicional = new ArrayList<>();

        protected Builder() {
        }

        public B emisor(Emisor emisor) {
            this.emisor = emisor;
            return self();
        }

        public B fechaEmision(LocalDate fechaEmision) {
            this.fechaEmision = fechaEmision;
            return self();
        }

        /**
         * La fecha de hoy en Ecuador, que es lo que casi siempre se quiere.
         *
         * <p>Atajo de {@code fechaEmision(LocalDate.now(Sri.ZONA))}, y no solo por brevedad:
         * {@link LocalDate#now()} toma la zona de la máquina, y en un servidor en UTC después de
         * las siete de la tarde de Ecuador devuelve el día siguiente. Esa fecha viaja dentro de
         * la clave de acceso.
         */
        public B fechaEmisionHoy() {
            return fechaEmision(LocalDate.now(Sri.ZONA));
        }

        public B ambiente(Ambiente ambiente) {
            this.ambiente = ambiente;
            return self();
        }

        /**
         * Establecimiento y punto de emisión desde los que se emite.
         */
        public B establecimiento(Establecimiento establecimiento) {
            this.establecimiento = establecimiento;
            return self();
        }

        /**
         * Declara la máquina fiscal desde la que se emite.
         *
         * <p>Ficha Técnica v2.34, Anexo 13. Solo la llevan los comprobantes emitidos desde una
         * máquina fiscal certificada.
         */
        public B maquinaFiscal(@Nullable MaquinaFiscal maquinaFiscal) {
            this.maquinaFiscal = maquinaFiscal;
            return self();
        }

        public B secuencial(Secuencial secuencial) {
            this.secuencial = secuencial;
            return self();
        }

        /**
         * Fija el código numérico de la clave de acceso. Excluyente con
         * {@link #codigoNumerico(EstrategiaCodigoNumerico)}.
         */
        public B codigoNumerico(CodigoNumerico codigoNumerico) {
            this.codigoNumerico = codigoNumerico;
            return self();
        }

        /**
         * Deriva el código numérico de la identidad del comprobante.
         */
        public B codigoNumerico(EstrategiaCodigoNumerico estrategia) {
            this.estrategiaCodigoNumerico = estrategia;
            return self();
        }

        /**
         * Por omisión, {@link TipoEmision#NORMAL}.
         */
        public B tipoEmision(TipoEmision tipoEmision) {
            this.tipoEmision = Objects.requireNonNull(tipoEmision, "tipoEmision");
            return self();
        }

        /**
         * Añade un campo adicional al bloque {@code <infoAdicional>}.
         */
        public B campoAdicional(String nombre, String valor) {
            infoAdicional.add(new CampoAdicional(nombre, valor));
            return self();
        }

        /**
         * Añade el RUC del proveedor del sistema de facturación como campo adicional.
         *
         * <p>Ficha Técnica v2.34, Anexo 26: obligatorio para los emisores que usan sistemas de
         * terceros de los que enumera la resolución 2718. No se valida su presencia.
         */
        public B rucProveedorSistema(Ruc rucProveedorSistema) {
            return campoAdicional(CampoAdicional.NOMBRE_RUC_PROVEEDOR_SISTEMA, rucProveedorSistema.valor());
        }

        /**
         * Tipo de comprobante que construye este builder.
         */
        protected abstract TipoComprobante tipoComprobante();

        /**
         * Valida los campos comunes. Los subtipos la invocan al principio de su {@code build()}.
         *
         * @throws ValidacionException si hay más campos adicionales de los que admite el esquema
         */
        protected final void validarComun() {
            if (infoAdicional.size() > CampoAdicional.MAX_POR_COMPROBANTE) {
                throw new ValidacionException(new Validacion.CampoInvalido("infoAdicional", "El esquema admite hasta " + CampoAdicional.MAX_POR_COMPROBANTE + " campos adicionales, se indicaron " + infoAdicional.size()));
            }
        }

        /**
         * Fecha de emisión indicada, para los subtipos que la escriben en sus propios bloques.
         */
        protected final LocalDate fechaEmisionExigida() {
            return Objects.requireNonNull(fechaEmision, "fechaEmision");
        }

        private Emisor emisorExigido() {
            return Objects.requireNonNull(emisor, "emisor");
        }

        /**
         * Compone la clave de acceso con la identidad indicada.
         *
         * @throws ConfiguracionException si falta el código numérico o se indicaron dos a la vez
         */
        private String generarClaveAcceso() {
            var emisor = emisorExigido();
            var serie = Objects.requireNonNull(this.establecimiento, "establecimiento").serie();
            var secuencial = Objects.requireNonNull(this.secuencial, "secuencial");

            return ClaveAcceso.generar(
                fechaEmisionExigida(),
                tipoComprobante(),
                emisor.ruc(),
                Objects.requireNonNull(ambiente, "ambiente"),
                serie,
                secuencial,
                resolverCodigoNumerico(new ComprobanteId(emisor.ruc(), tipoComprobante(), serie, secuencial)),
                tipoEmision
            );
        }

        private CodigoNumerico resolverCodigoNumerico(ComprobanteId id) {
            if (codigoNumerico != null && estrategiaCodigoNumerico != null) {
                throw new ConfiguracionException("Indique un código numérico o una EstrategiaCodigoNumerico, no ambos");
            }
            if (codigoNumerico != null) {
                return codigoNumerico;
            }
            if (estrategiaCodigoNumerico == null) {
                throw new ConfiguracionException("Falta el código numérico: indique uno o una EstrategiaCodigoNumerico");
            }
            return estrategiaCodigoNumerico.generar(id);
        }

        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
        }
    }
}
