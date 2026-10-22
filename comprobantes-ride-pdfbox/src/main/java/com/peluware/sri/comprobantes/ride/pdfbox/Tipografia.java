package com.peluware.sri.comprobantes.ride.pdfbox;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.util.Objects;

/**
 * Las dos fuentes del RIDE: la corriente y la de los rótulos.
 *
 * <p>Las implementaciones son dos y no más: {@link Estandar} usa las base-14 del PDF y
 * {@link Propia} carga las que indique el llamador.
 */
public sealed interface Tipografia {

    /**
     * Fuente del texto corriente.
     *
     * @throws IOException si la fuente no se puede cargar
     */
    PDFont normal(PDDocument documento) throws IOException;

    /**
     * Fuente de los rótulos y los títulos.
     *
     * @throws IOException si la fuente no se puede cargar
     */
    PDFont negrita(PDDocument documento) throws IOException;

    /**
     * Una de las familias base-14.
     */
    static Tipografia de(FamiliaTipografica familia) {
        return new Estandar(familia);
    }

    /**
     * Las fuentes que indique el llamador.
     */
    static Tipografia de(FuenteRide normal, FuenteRide negrita) {
        return new Propia(normal, negrita);
    }

    /**
     * Una familia base-14, que el PDF no incrusta.
     *
     * <p>No añade nada al tamaño del documento y su codificación WinAnsi cubre el español.
     */
    record Estandar(FamiliaTipografica familia) implements Tipografia {

        public Estandar {
            Objects.requireNonNull(familia, "familia");
        }

        @Override
        public PDFont normal(PDDocument documento) {
            return new PDType1Font(familia.normal());
        }

        @Override
        public PDFont negrita(PDDocument documento) {
            return new PDType1Font(familia.negrita());
        }
    }

    /**
     * Fuentes cargadas por el llamador, normalmente desde un TTF.
     *
     * <p>Se cargan una vez por documento y se incrustan en él, así que cada RIDE pesa lo que
     * pese el subconjunto de glifos que use. {@code PDType0Font.load} subcontrae por omisión;
     * aun así, una fuente propia engorda el documento frente a una base-14.
     *
     * <p>El llamador responde de que la fuente tenga los glifos del español.
     */
    record Propia(FuenteRide normal, FuenteRide negrita) implements Tipografia {

        public Propia {
            Objects.requireNonNull(normal, "normal");
            Objects.requireNonNull(negrita, "negrita");
        }

        @Override
        public PDFont normal(PDDocument documento) throws IOException {
            return normal.cargar(documento);
        }

        @Override
        public PDFont negrita(PDDocument documento) throws IOException {
            return negrita.cargar(documento);
        }
    }
}
