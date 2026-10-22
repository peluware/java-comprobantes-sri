package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Detalle;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.TotalImpuesto;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Cómo se escribe cada valor en el papel: los rótulos que el Anexo 2 da a los códigos y la
 * forma de los números y las fechas.
 *
 * <p>No dibuja nada ni sabe de estilos; solo convierte datos del comprobante en texto.
 */
final class Formatos {

    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Formatos() {
    }
    static String rotuloImpuesto(TotalImpuesto impuesto) {
        return switch (impuesto.codigo()) {
            case IVA -> "IVA" + porcentaje(impuesto);
            case ICE -> "ICE";
            case IRBPNR -> "IRBPNR";
        };
    }

    /**
     * Porcentaje con que rotular la fila.
     *
     * <p>Sale del total agrupado cuando todos los detalles comparten tarifa. Si no, se busca en
     * la Tabla 17 por el código de porcentaje; un código que no esté en la tabla, como el del
     * IVA diferenciado, deja la fila sin porcentaje.
     */
    static String porcentaje(TotalImpuesto impuesto) {
        var tarifa = impuesto.tarifa();
        if (tarifa == null) {
            tarifa = TarifaIva.valores().stream()
                .filter(conocida -> conocida.codigo().equals(impuesto.codigoPorcentaje()))
                .map(TarifaIva::tarifa)
                .findFirst()
                .orElse(null);
        }
        return tarifa == null ? "" : " " + tarifa.stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * Forma de pago como la rotula el Anexo 2: el código, un guion y su denominación.
     *
     * <p>Ficha Técnica v2.34, Tabla 24. Un código que la tabla no enumera se imprime solo, sin
     * denominación: el esquema admite del {@code 01} al {@code 21} y el SRI puede añadir
     * cualquiera de ellos sin que esta librería cambie.
     */
    static String formaPago(String codigo) {
        var denominacion = switch (codigo) {
            case "01" -> "SIN UTILIZACION DEL SISTEMA FINANCIERO";
            case "15" -> "COMPENSACIÓN DE DEUDAS";
            case "16" -> "TARJETA DE DÉBITO";
            case "17" -> "DINERO ELECTRÓNICO";
            case "18" -> "TARJETA PREPAGO";
            case "19" -> "TARJETA DE CRÉDITO";
            case "20" -> "OTROS CON UTILIZACION DEL SISTEMA FINANCIERO";
            case "21" -> "ENDOSO DE TÍTULOS";
            default -> null;
        };
        return denominacion == null ? codigo : codigo + " - " + denominacion;
    }

    /**
     * Título del documento, tal como lo rotula el Anexo 2.
     *
     * <p>Los nombres legibles viven aquí y no en el catálogo, que solo conoce los códigos que
     * viajan en el XML.
     */
    static String titulo(TipoComprobante tipo) {
        return switch (tipo) {
            case FACTURA -> "FACTURA";
            case LIQUIDACION_COMPRA -> "LIQUIDACIÓN DE COMPRA";
            case NOTA_CREDITO -> "NOTA DE CRÉDITO";
            case NOTA_DEBITO -> "NOTA DE DÉBITO";
            case GUIA_REMISION -> "GUÍA DE REMISIÓN";
            case COMPROBANTE_RETENCION -> "COMPROBANTE DE RETENCIÓN";
        };
    }

    /**
     * Escribe una cantidad sin los ceros que el esquema admite y la impresión no necesita:
     * {@code 2.000000} se imprime {@code 2}.
     */
    static String cantidad(BigDecimal valor) {
        var limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0, RoundingMode.UNNECESSARY).toPlainString() : limpio.toPlainString();
    }

    /**
     * Escribe un importe con dos decimales como mínimo, conservando los que traiga de más.
     */
    static String importe(BigDecimal valor) {
        var limpio = valor.stripTrailingZeros();
        return limpio.setScale(Math.max(2, limpio.scale()), RoundingMode.UNNECESSARY).toPlainString();
    }

    /**
     * Los datos adicionales de una línea, como los escriba el formato elegido.
     */
    static String adicionales(Detalle detalle, FormatoAdicionales formato) {
        return formato.escribir(detalle.detallesAdicionales());
    }

    /**
     * Texto de un campo que el esquema admite vacío.
     */
    static String texto(@Nullable String valor) {
        return valor == null ? "" : valor;
    }
}
