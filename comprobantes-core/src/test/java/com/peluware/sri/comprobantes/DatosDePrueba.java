package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.liquidacioncompra.Proveedor;
import com.peluware.sri.comprobantes.retencion.CodigoImpuestoRetencion;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencion;
import com.peluware.sri.comprobantes.retencion.DocumentoSustento;
import com.peluware.sri.comprobantes.retencion.Retencion;
import com.peluware.sri.comprobantes.retencion.SujetoRetenido;
import com.peluware.sri.comprobantes.valores.PeriodoFiscal;
import com.peluware.sri.comprobantes.guiaremision.Destinatario;
import com.peluware.sri.comprobantes.guiaremision.DetalleGuiaRemision;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemision;
import com.peluware.sri.comprobantes.guiaremision.Transportista;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompra;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.notacredito.DetalleNotaCredito;
import com.peluware.sri.comprobantes.notacredito.NotaCredito;
import com.peluware.sri.comprobantes.notadebito.NotaDebito;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import com.peluware.sri.comprobantes.valores.ContribuyenteEspecial;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;

import java.time.LocalDate;

/**
 * Datos ficticios compartidos por los tests.
 *
 * <p>El RUC es el del XML de ejemplo del SRI. La razón social del comprador es la que el
 * numeral 5.6 permite usar en el ambiente de pruebas; la del emisor es inventada.
 *
 * <p>Los builders que devuelve llevan la identidad y lo obligatorio de cada tipo ya puesto,
 * salvo el código numérico en las variantes que lo dicen. Un tipo de comprobante nuevo añade
 * aquí su builder y entra solo en {@code ComprobanteContratoTest}.
 */
public final class DatosDePrueba {

    public static final Ruc RUC = new Ruc("0000000000001");
    public static final String RAZON_SOCIAL = "DISTRIBUIDORA FICTICIA S.A.";
    public static final String DIRECCION_MATRIZ = "AMAZONAS Y ROCA";
    public static final String CEDULA_COMPRADOR = "1710034065";

    public static final LocalDate FECHA_EMISION = LocalDate.of(2026, 10, 5);
    public static final Ambiente AMBIENTE = Ambiente.PRUEBAS;
    public static final TipoComprobante TIPO = TipoComprobante.FACTURA;
    public static final Serie SERIE = Serie.of(1, 1);

    /** Establecimiento de las pruebas, sin dirección, para que el comprobante mínimo no lleve bloques opcionales. */
    public static final Establecimiento ESTABLECIMIENTO = Establecimiento.of(1, 1);

    /** El mismo establecimiento con su dirección, para los comprobantes con todo lo opcional. */
    public static final Establecimiento ESTABLECIMIENTO_COMPLETO = ESTABLECIMIENTO.conDireccion("AMAZONAS Y ROCA");
    public static final Secuencial SECUENCIAL = Secuencial.of(1);
    public static final CodigoNumerico CODIGO_NUMERICO = CodigoNumerico.of(1);

    /** Clave de acceso que corresponde a los campos anteriores. */
    public static final String CLAVE_ACCESO = "0510202601000000000000110010010000000010000000114";

    /** Emisor con todos los campos opcionales de la Tabla 11. */
    public static Emisor emisorCompleto() {
        return Emisor.builder(RUC, RAZON_SOCIAL, DIRECCION_MATRIZ)
            .nombreComercial("LA TIENDA")
            .contribuyenteEspecial(new ContribuyenteEspecial("5368"))
            .obligadoContabilidad(true)
            .build();
    }

    /** Documento que modifican las notas de crédito y de débito de prueba. */
    public static final LocalDate FECHA_DOC_SUSTENTO = LocalDate.of(2026, 10, 3);

    private DatosDePrueba() {
    }

    public static Emisor emisor() {
        return Emisor.of(RUC, RAZON_SOCIAL, DIRECCION_MATRIZ);
    }

    public static Comprador comprador() {
        return Comprador.of(TipoIdentificacion.CEDULA, CEDULA_COMPRADOR, Comprador.RAZON_SOCIAL_PRUEBAS);
    }

    public static DocumentoModificado documentoModificado() {
        return new DocumentoModificado(TipoComprobante.FACTURA, Serie.of(1, 1), Secuencial.of(45), FECHA_DOC_SUSTENTO);
    }

    /** Factura completa a la que solo falta construir. */
    public static Factura.Builder factura() {
        return facturaBase().codigoNumerico(CODIGO_NUMERICO).detalle(detalleFactura());
    }

    /** Igual que {@link #factura()}, dejando el código numérico por indicar. */
    public static Factura.Builder facturaSinCodigoNumerico() {
        return facturaBase().detalle(detalleFactura());
    }

    /** Igual que {@link #factura()}, dejando los detalles por añadir. */
    public static Factura.Builder facturaSinDetalles() {
        return facturaBase().codigoNumerico(CODIGO_NUMERICO);
    }

    private static Factura.Builder facturaBase() {
        return Factura.builder()
            .emisor(emisor())
            .comprador(comprador())
            .fechaEmision(FECHA_EMISION)
            .ambiente(AMBIENTE)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(SECUENCIAL);
    }

    /** Proveedor de las pruebas, la contraparte de una liquidación de compra. */
    public static Proveedor proveedor() {
        return Proveedor.of(TipoIdentificacion.RUC, "1790012345001", "AGRICOLA LA ESPERANZA");
    }

    /** Liquidación de compra válida con un detalle. */
    public static LiquidacionCompra.Builder liquidacionCompra() {
        return liquidacionCompraBase().codigoNumerico(CODIGO_NUMERICO).detalle(detalleLiquidacionCompra());
    }

    /** Igual que {@link #liquidacionCompra()}, dejando el código numérico por indicar. */
    public static LiquidacionCompra.Builder liquidacionCompraSinCodigoNumerico() {
        return liquidacionCompraBase().detalle(detalleLiquidacionCompra());
    }

    /** Igual que {@link #liquidacionCompra()}, dejando los detalles por añadir. */
    public static LiquidacionCompra.Builder liquidacionCompraSinDetalles() {
        return liquidacionCompraBase().codigoNumerico(CODIGO_NUMERICO);
    }

    private static LiquidacionCompra.Builder liquidacionCompraBase() {
        return LiquidacionCompra.builder()
            .emisor(emisor())
            .proveedor(proveedor())
            .fechaEmision(FECHA_EMISION)
            .ambiente(AMBIENTE)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(SECUENCIAL);
    }

    /** Detalle de liquidación de compra de 100,00 con IVA del 15 %. */
    public static DetalleComprobanteVenta detalleLiquidacionCompra() {
        return DetalleComprobanteVenta.builder()
            .descripcion("Quintal de maíz")
            .cantidad(1)
            .precioUnitario("100.00")
            .iva(TarifaIva.QUINCE)
            .build();
    }

    /** Nota de crédito completa a la que solo falta construir. */
    public static NotaCredito.Builder notaCredito() {
        return notaCreditoSinCodigoNumerico().codigoNumerico(CODIGO_NUMERICO);
    }

    /** Igual que {@link #notaCredito()}, dejando el código numérico por indicar. */
    public static NotaCredito.Builder notaCreditoSinCodigoNumerico() {
        return notaCreditoSinDetalles().detalle(detalleNotaCredito());
    }

    /** Igual que {@link #notaCredito()}, dejando los detalles por añadir. */
    public static NotaCredito.Builder notaCreditoSinDetalles() {
        return NotaCredito.builder()
            .emisor(emisor())
            .comprador(comprador())
            .fechaEmision(FECHA_EMISION)
            .ambiente(AMBIENTE)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(SECUENCIAL)
            .documentoModificado(documentoModificado())
            .motivo("DEVOLUCIÓN");
    }

    /** Nota de débito completa a la que solo falta construir. */
    public static NotaDebito.Builder notaDebito() {
        return notaDebitoSinCodigoNumerico().codigoNumerico(CODIGO_NUMERICO);
    }

    /** Igual que {@link #notaDebito()}, dejando el código numérico por indicar. */
    public static NotaDebito.Builder notaDebitoSinCodigoNumerico() {
        return NotaDebito.builder()
            .emisor(emisor())
            .comprador(comprador())
            .fechaEmision(FECHA_EMISION)
            .ambiente(AMBIENTE)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(SECUENCIAL)
            .documentoModificado(documentoModificado())
            .totalSinImpuestos(Dinero.of("50.00"))
            .iva(TarifaIva.QUINCE)
            .motivo("INTERESES POR MORA", Dinero.of("50.00"));
    }

    /** Línea de factura de cien dólares con IVA. */
    public static DetalleComprobanteVenta detalleFactura() {
        return DetalleComprobanteVenta.builder()
            .descripcion("Consulta veterinaria")
            .cantidad(1)
            .precioUnitario("100.00")
            .iva(TarifaIva.QUINCE)
            .build();
    }

    /** Línea de nota de crédito de cien dólares con IVA. */
    public static DetalleNotaCredito detalleNotaCredito() {
        return DetalleNotaCredito.builder()
            .descripcion("Consulta veterinaria")
            .cantidad(1)
            .precioUnitario("100.00")
            .iva(TarifaIva.QUINCE)
            .build();
    }

    /** Transportista de las pruebas. */
    public static Transportista transportista() {
        return new Transportista(TipoIdentificacion.RUC, "1790012345001", "TRANSPORTES DEL PACIFICO S.A.");
    }

    /** Destinatario de las pruebas, con un detalle. */
    public static Destinatario destinatario() {
        return Destinatario.builder()
            .identificacion(CEDULA_COMPRADOR)
            .razonSocial(Comprador.RAZON_SOCIAL_PRUEBAS)
            .direccion("AV. 6 DE DICIEMBRE")
            .motivoTraslado("ENTREGA DE MERCADERIA VENDIDA")
            .detalle(detalleGuiaRemision())
            .build();
    }

    /** Detalle de guía de remisión, que no lleva importes. */
    public static DetalleGuiaRemision detalleGuiaRemision() {
        return DetalleGuiaRemision.builder()
            .descripcion("Alimento para perro 15kg")
            .cantidad(2)
            .build();
    }

    /** Guía de remisión válida con un destinatario. */
    public static GuiaRemision.Builder guiaRemision() {
        return guiaRemisionSinCodigoNumerico().codigoNumerico(CODIGO_NUMERICO);
    }

    /** Igual que {@link #guiaRemision()}, dejando el código numérico por indicar. */
    public static GuiaRemision.Builder guiaRemisionSinCodigoNumerico() {
        return guiaRemisionSinDestinatarios().destinatario(destinatario());
    }

    /** Igual que {@link #guiaRemision()}, dejando los destinatarios por añadir. */
    public static GuiaRemision.Builder guiaRemisionSinDestinatarios() {
        return GuiaRemision.builder()
            .emisor(emisor())
            .fechaEmision(FECHA_EMISION)
            .ambiente(AMBIENTE)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(SECUENCIAL)
            .direccionPartida("AMAZONAS Y ROCA")
            .transportista(transportista())
            .transporte(FECHA_EMISION, FECHA_EMISION.plusDays(1))
            .placa("PBA1234");
    }

    /** Sujeto retenido de las pruebas. */
    public static SujetoRetenido sujetoRetenido() {
        return SujetoRetenido.of(TipoIdentificacion.RUC, "1790012345001", "PROVEEDOR DEL PACIFICO S.A.");
    }

    /** Documento sustento con un impuesto y una retención. */
    public static DocumentoSustento documentoSustentoRetencion() {
        return DocumentoSustento.builder()
            .codigoSustento("01")
            .documento(TipoComprobante.FACTURA, SERIE, Secuencial.of(45), FECHA_EMISION.minusDays(2))
            .totales(Dinero.of("100.00"), Dinero.of("115.00"))
            .impuesto(Impuesto.of(CodigoImpuesto.IVA, TarifaIva.QUINCE.codigo(), TarifaIva.QUINCE.tarifa(), Dinero.of("100.00")))
            .retencion(Retencion.de(CodigoImpuestoRetencion.RENTA, "312", Dinero.of("100.00"), "1.75"))
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("115.00")))
            .build();
    }

    /** Comprobante de retención válido con un documento sustento. */
    public static ComprobanteRetencion.Builder comprobanteRetencion() {
        return comprobanteRetencionSinCodigoNumerico().codigoNumerico(CODIGO_NUMERICO);
    }

    /** Igual que {@link #comprobanteRetencion()}, dejando el código numérico por indicar. */
    public static ComprobanteRetencion.Builder comprobanteRetencionSinCodigoNumerico() {
        return comprobanteRetencionSinDocumentos().documentoSustento(documentoSustentoRetencion());
    }

    /** Igual que {@link #comprobanteRetencion()}, dejando los documentos por añadir. */
    public static ComprobanteRetencion.Builder comprobanteRetencionSinDocumentos() {
        return ComprobanteRetencion.builder()
            .emisor(emisor())
            .fechaEmision(FECHA_EMISION)
            .ambiente(AMBIENTE)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(SECUENCIAL)
            .sujetoRetenido(sujetoRetenido())
            .periodoFiscal(PeriodoFiscal.de(FECHA_EMISION));
    }
}
