package com.peluware.sri.comprobantes.it;

import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.retencion.CodigoImpuestoRetencion;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencion;
import com.peluware.sri.comprobantes.retencion.DocumentoSustento;
import com.peluware.sri.comprobantes.retencion.Retencion;
import com.peluware.sri.comprobantes.retencion.SujetoRetenido;
import com.peluware.sri.comprobantes.valores.PeriodoFiscal;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.guiaremision.Destinatario;
import com.peluware.sri.comprobantes.guiaremision.DetalleGuiaRemision;
import com.peluware.sri.comprobantes.guiaremision.SustentoTraslado;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemision;
import com.peluware.sri.comprobantes.guiaremision.Transportista;
import org.jspecify.annotations.Nullable;
import com.peluware.sri.comprobantes.liquidacioncompra.Proveedor;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompra;
import com.peluware.sri.comprobantes.EstrategiaCodigoNumerico;
import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Sri;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.notacredito.DetalleNotaCredito;
import com.peluware.sri.comprobantes.notacredito.NotaCredito;
import com.peluware.sri.comprobantes.notadebito.NotaDebito;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.Establecimiento;

import java.time.LocalDate;

/**
 * Comprobantes que emiten las pruebas de integración, con el emisor y el secuencial que
 * {@link Emision} deriva de los datos del operador.
 *
 * <p>El comprador es el que el numeral 5.6 permite usar en el ambiente de pruebas.
 */
final class ComprobantesDePrueba {

    static final Establecimiento ESTABLECIMIENTO = Establecimiento.of(1, 1, "AMAZONAS Y ROCA");

    private ComprobantesDePrueba() {
    }

    static Comprador comprador() {
        return Comprador.of(TipoIdentificacion.CEDULA, "1710034065", Comprador.RAZON_SOCIAL_PRUEBAS);
    }

    /**
     * Factura de 70,50 con dos detalles, uno con IVA y otro con tarifa cero.
     *
     */
    static Factura factura() {
        return Factura.builder()
            .emisor(Emision.emisor())
            .comprador(comprador())
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(Emision.secuencial())
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(Emision.SECRETO_DE_PRUEBA))
            .detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal("SKU-001")
                .descripcion("Alimento para perro 15kg")
                .unidadMedida("SACO")
                .cantidad(2)
                .precioUnitario("25.00")
                .descuento(Dinero.of("5.00"))
                .iva(TarifaIva.QUINCE)
                .build())
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Consulta veterinaria")
                .cantidad(1)
                .precioUnitario("18.75")
                .iva(TarifaIva.CERO)
                .build())
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("70.50")))
            .campoAdicional("Origen", "comprobantes-it")
            .build();
    }

    /**
     * Liquidación de compra de 115,00 a un proveedor que no puede emitir comprobantes.
     *
     */
    static LiquidacionCompra liquidacionCompra() {
        return LiquidacionCompra.builder()
            .emisor(Emision.emisor())
            .proveedor(Proveedor.of(TipoIdentificacion.CEDULA, "1710034065", "AGRICOLA LA ESPERANZA"))
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(Emision.secuencial())
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(Emision.SECRETO_DE_PRUEBA))
            .detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal("MAIZ-01")
                .descripcion("Quintal de maíz")
                .unidadMedida("QUINTAL")
                .cantidad(2)
                .precioUnitario("50.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("115.00")))
            .campoAdicional("Origen", "comprobantes-it")
            .build();
    }

    /**
     * Nota de crédito que devuelve uno de los sacos de la factura indicada.
     */
    static NotaCredito notaCredito(DocumentoModificado modificado) {
        return NotaCredito.builder()
            .emisor(Emision.emisor())
            .comprador(comprador())
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(Emision.secuencial())
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(Emision.SECRETO_DE_PRUEBA))
            .documentoModificado(modificado)
            .motivo("DEVOLUCION DE UN SACO")
            .detalle(DetalleNotaCredito.builder()
                .codigoInterno("SKU-001")
                .descripcion("Alimento para perro 15kg")
                .cantidad(1)
                .precioUnitario("25.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .campoAdicional("Origen", "comprobantes-it")
            .build();
    }

    /**
     * Nota de débito que carga intereses sobre la factura indicada.
     */
    static NotaDebito notaDebito(DocumentoModificado modificado) {
        return NotaDebito.builder()
            .emisor(Emision.emisor())
            .comprador(comprador())
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(Emision.secuencial())
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(Emision.SECRETO_DE_PRUEBA))
            .documentoModificado(modificado)
            .totalSinImpuestos(Dinero.of("10.00"))
            .iva(TarifaIva.QUINCE)
            .motivo("INTERESES POR MORA", Dinero.of("10.00"))
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("11.50")))
            .campoAdicional("Origen", "comprobantes-it")
            .build();
    }

    /**
     * Guía de remisión que traslada mercadería a un destinatario, sustentada en la factura que
     * se le indique.
     *
     */
    static GuiaRemision guiaRemision(@Nullable SustentoTraslado sustento) {
        return GuiaRemision.builder()
            .emisor(Emision.emisor())
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(Emision.secuencial())
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(Emision.SECRETO_DE_PRUEBA))
            .direccionPartida("AMAZONAS Y ROCA")
            .transportista(new Transportista(TipoIdentificacion.RUC, "1790012345001", "TRANSPORTES DEL PACIFICO S.A."))
            .transporte(LocalDate.now(Sri.ZONA), LocalDate.now(Sri.ZONA).plusDays(1))
            .placa("PBA1234")
            .destinatario(Destinatario.builder()
                .identificacion("1710034065")
                .razonSocial(Comprador.RAZON_SOCIAL_PRUEBAS)
                .direccion("AV. 6 DE DICIEMBRE Y PATRIA")
                .motivoTraslado("ENTREGA DE MERCADERIA VENDIDA")
                .ruta("QUITO - IBARRA")
                .sustentoTraslado(sustento)
                .detalle(DetalleGuiaRemision.builder()
                    .codigoInterno("SKU-001")
                    .descripcion("Alimento para perro 15kg")
                    .cantidad(2)
                    .build())
                .build())
            .campoAdicional("Origen", "comprobantes-it")
            .build();
    }

    /**
     * Comprobante de retención sobre la factura que se le indique, con retención de renta e IVA.
     *
     */
    static ComprobanteRetencion comprobanteRetencion(TipoComprobante tipoSustento, Serie serieSustento, Secuencial secuencialSustento, LocalDate fechaSustento, String numeroAutorizacion) {
        return ComprobanteRetencion.builder()
            .emisor(Emision.emisor())
            .fechaEmisionHoy()
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(ESTABLECIMIENTO)
            .secuencial(Emision.secuencial())
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(Emision.SECRETO_DE_PRUEBA))
            .sujetoRetenido(SujetoRetenido.of(TipoIdentificacion.CEDULA, "1710034065", Comprador.RAZON_SOCIAL_PRUEBAS))
            .periodoFiscal(PeriodoFiscal.de(LocalDate.now(Sri.ZONA)))
            .documentoSustento(DocumentoSustento.builder()
                .codigoSustento("01")
                .documento(tipoSustento, serieSustento, secuencialSustento, fechaSustento)
                .numeroAutorizacion(numeroAutorizacion)
                .totales(Dinero.of("63.75"), Dinero.of("70.50"))
                .impuesto(Impuesto.of(CodigoImpuesto.IVA, TarifaIva.QUINCE.codigo(), TarifaIva.QUINCE.tarifa(), Dinero.of("45.00")))
                .retencion(Retencion.de(CodigoImpuestoRetencion.RENTA, "312", Dinero.of("63.75"), "2.00"))
                .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("70.50")))
                .build())
            .campoAdicional("Origen", "comprobantes-it")
            .build();
    }
}
