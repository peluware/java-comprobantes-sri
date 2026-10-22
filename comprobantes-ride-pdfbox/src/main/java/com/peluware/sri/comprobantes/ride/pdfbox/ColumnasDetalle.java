package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Detalle;

import java.util.List;
import java.util.function.Function;

/**
 * Las columnas de la tabla de detalle de la factura, la nota de crédito y la liquidación de compra.
 *
 * <p>Anexo 2, páginas 60, 61 y 65.
 */
final class ColumnasDetalle {

    private ColumnasDetalle() {
    }

    /**
     * Las ocho columnas en el orden en que se dibujan. Las cinco que {@link Detalle} declara en la
     * clase base se leen igual en los tres comprobantes; las otras tres las nombra cada esquema a
     * su manera.
     *
     * @param codigoPrincipal código del ítem
     * @param codigoAuxiliar  código alterno del ítem
     * @param cantidad        cantidad, con la unidad de medida cuando el comprobante la admite
     */
    static <D extends Detalle> List<Columna<D>> de(FormatoAdicionales formato, Function<D, String> codigoPrincipal, Function<D, String> codigoAuxiliar, Function<D, String> cantidad) {
        return List.of(
            Columna.opcional("Cod. Principal", 48, codigoPrincipal),
            Columna.opcional("Cod. Auxiliar", 42, codigoAuxiliar),
            Columna.derecha("Cantidad", 48, cantidad),
            Columna.elastica("Descripción", Detalle::descripcion),
            Columna.opcional("Detalle Adicional", 74, linea -> Formatos.adicionales(linea, formato)),
            Columna.derecha("Precio Unitario", 50, linea -> Formatos.importe(linea.precioUnitario().valor())),
            Columna.derecha("Descuento", 52, linea -> linea.descuento().toString()),
            Columna.derecha("Precio Total", 54, linea -> linea.precioTotalSinImpuesto().toString())
        );
    }
}
