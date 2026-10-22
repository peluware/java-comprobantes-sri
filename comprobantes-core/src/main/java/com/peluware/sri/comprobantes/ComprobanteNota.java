package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.notacredito.NotaCredito;
import com.peluware.sri.comprobantes.notadebito.NotaDebito;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Nota que corrige un comprobante ya emitido.
 *
 * <p>De los documentos complementarios, los dos que no documentan una operación nueva sino que
 * modifican una anterior: la {@link NotaCredito}, que corrige a la baja, y la {@link NotaDebito},
 * que corrige al alza. La guía de remisión queda fuera porque acompaña a una mercadería, no
 * corrige nada.
 *
 * <p>Lo que comparten es la cabecera, en {@link InfoNota}: a quién va dirigida y qué comprobante
 * corrige. Lo que sigue no se comparte, y está bien que no: la nota de crédito justifica la
 * corrección **detallando lo que vuelve**, con líneas de producto, y la de débito **listando
 * razones con su importe**, porque en un recargo no se entregó nada que detallar.
 */
public abstract sealed class ComprobanteNota extends Comprobante permits NotaCredito, NotaDebito {

    protected ComprobanteNota(Builder<?> builder) {
        super(builder);
    }

    /**
     * Cabecera de la nota, donde está el comprobante que corrige. Cada subtipo la estrecha a su
     * propio tipo.
     */
    public abstract InfoNota info();

    /**
     * Construye una nota.
     *
     * <p>No es thread-safe.
     *
     * @param <B> builder del subtipo
     */
    public abstract static sealed class Builder<B extends Builder<B>> extends Comprobante.Builder<B> permits NotaCredito.Builder, NotaDebito.Builder {

        private @Nullable Comprador comprador;
        private @Nullable DocumentoModificado documentoModificado;
        private @Nullable String rise;

        protected Builder() {
        }

        public B comprador(Comprador comprador) {
            this.comprador = comprador;
            return self();
        }

        /**
         * Documento al que modifica esta nota.
         */
        public B documentoModificado(DocumentoModificado documentoModificado) {
            this.documentoModificado = documentoModificado;
            return self();
        }

        public B rise(@Nullable String rise) {
            this.rise = rise;
            return self();
        }

        /**
         * El adquirente indicado, para el bloque propio de cada subtipo.
         */
        protected final Comprador compradorExigido() {
            return Objects.requireNonNull(comprador, "comprador");
        }

        /**
         * El documento corregido, para el bloque propio de cada subtipo.
         */
        protected final DocumentoModificado documentoModificadoExigido() {
            return Objects.requireNonNull(documentoModificado, "documentoModificado");
        }

        /**
         * El RISE indicado, para el bloque propio de cada subtipo.
         */
        protected final @Nullable String riseIndicado() {
            return rise;
        }
    }
}
