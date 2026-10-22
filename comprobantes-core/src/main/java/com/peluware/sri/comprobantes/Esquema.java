package com.peluware.sri.comprobantes;

/**
 * Los nombres fijos del esquema de un comprobante.
 *
 * <p>Uno por tipo, y lo usan las dos direcciones: el writer para componer el documento y el
 * reader para reconocerlo. Antes el nombre de la raíz y el del bloque propio estaban escritos en
 * los dos sitios.
 *
 * @param raiz    elemento raíz, que es también el nombre del esquema
 * @param version para el atributo {@code version} del raíz; el reader no lo mira
 * @param tagInfo bloque propio del comprobante: {@code infoFactura}, {@code infoCompRetencion}…
 * @param nombre  el comprobante en los defectos, con artículo: «la factura»
 */
public record Esquema(String raiz, String version, String tagInfo, String nombre) {

    public static final Esquema FACTURA = new Esquema("factura", "2.1.0", "infoFactura", "la factura");

    public static final Esquema LIQUIDACION_COMPRA = new Esquema("liquidacionCompra", "1.1.0", "infoLiquidacionCompra", "la liquidación de compra");

    public static final Esquema NOTA_CREDITO = new Esquema("notaCredito", "1.1.0", "infoNotaCredito", "la nota de crédito");

    public static final Esquema NOTA_DEBITO = new Esquema("notaDebito", "1.0.0", "infoNotaDebito", "la nota de débito");

    public static final Esquema GUIA_REMISION = new Esquema("guiaRemision", "1.1.0", "infoGuiaRemision", "la guía de remisión");

    public static final Esquema COMPROBANTE_RETENCION = new Esquema("comprobanteRetencion", "2.0.0", "infoCompRetencion", "el comprobante de retención");
}
