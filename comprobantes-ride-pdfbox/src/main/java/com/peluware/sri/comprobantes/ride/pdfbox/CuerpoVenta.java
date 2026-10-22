package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.ComprobanteVenta;
import com.peluware.sri.comprobantes.Contraparte;
import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;
import static com.peluware.sri.comprobantes.ride.pdfbox.Receptor.*;

/**
 * El cuerpo del RIDE de un comprobante de venta: el recuadro de la contraparte, la tabla de
 * detalles y el cierre con los totales, la información adicional y las formas de pago.
 *
 * <p>Anexo 2, páginas 60 y 65. La factura y la liquidación de compra dibujan lo mismo salvo en
 * dos sitios, y esta clase arma lo demás: sus subtipos no pueden saltarse ni el recuadro ni las
 * columnas.
 *
 * <p>Los dos sitios que varían son huecos con valor por omisión, porque solo la factura los
 * declara. La liquidación de compra simplemente no los sobreescribe.
 *
 * @param <C> comprobante de venta que dibuja
 */
abstract sealed class CuerpoVenta<C extends ComprobanteVenta> extends Cuerpo<C> permits CuerpoFactura, CuerpoLiquidacionCompra {

    protected CuerpoVenta(C comprobante, PDFont normal, PDFont negrita, EstiloRide estilo) {
        super(comprobante, normal, negrita, estilo);
    }

    /**
     * La otra parte: el comprador de la factura o el proveedor de la liquidación.
     */
    protected abstract Contraparte contraparte();

    /**
     * La guía de remisión que acompaña a la fecha; por omisión ninguna, porque solo la factura la
     * declara.
     */
    protected @Nullable String guiaRemision() {
        return null;
    }

    /**
     * La propina, que va entre los totales y el importe; por omisión ninguna, porque el esquema
     * de la liquidación de compra no la declara.
     */
    protected @Nullable Dinero propina() {
        return null;
    }

    @Override
    protected final Maquetacion armarMaquetacion() {
        return new ReceptorConTabla(estilo, pares(), Tabla.de(columnas(estilo.formatoAdicionales()), comprobante.detalles(), normal, negrita, estilo, ANCHO_UTIL));
    }

    @Override
    protected final Cierre.Contenido armarContenido() {
        return new Cierre.Contenido(totales(), Cierre.adicionales(comprobante.infoAdicional(), normal, negrita, estilo), comprobante.info().pagos());
    }

    /**
     * Las columnas del detalle, iguales en los dos esquemas.
     *
     * <p>La unidad de medida acompaña a la cantidad: el Anexo 2 no le da columna propia, pero el
     * esquema la admite.
     */
    private static List<Columna<DetalleComprobanteVenta>> columnas(FormatoAdicionales formato) {
        return ColumnasDetalle.de(formato,
            linea -> Formatos.texto(linea.codigoPrincipal()),
            linea -> Formatos.texto(linea.codigoAuxiliar()),
            linea -> Formatos.cantidad(linea.cantidad().valor()) + (linea.unidadMedida() == null ? "" : " " + linea.unidadMedida())
        );
    }

    /**
     * El recuadro de la contraparte. La guía de remisión, cuando la hay, acompaña a la fecha.
     */
    private Pares pares() {
        var contraparte = contraparte();
        var guiaRemision = guiaRemision();
        var fecha = Formatos.FECHA.format(comprobante.info().fechaEmision());

        var constructor = Pares.constructor(normal, negrita, estilo)
            .fila(izquierda(), anchoIzquierda(), "Razón Social / Nombres y Apellidos:", contraparte.razonSocial(), mitad(), anchoDerecha(), "Identificación:", contraparte.identificacion());

        if (guiaRemision == null) {
            constructor.fila(izquierda(), anchoCompleto(), "Fecha de Emisión:", fecha);
        } else {
            constructor.fila(izquierda(), anchoIzquierda(), "Fecha de Emisión:", fecha, mitad(), anchoDerecha(), "Guía Remisión:", guiaRemision);
        }
        return constructor.fila(izquierda(), anchoCompleto(), "Dirección:", contraparte.direccion()).build();
    }

    /**
     * Las filas de la tabla de totales. Solo la factura declara propina.
     */
    private List<Cierre.Fila> totales() {
        var totales = comprobante.info().totales();
        var propina = propina();
        var filas = Cierre.totales(totales.totalConImpuestos(), totales.totalSinImpuestos()).descuento(totales.totalDescuento());

        if (propina != null) {
            filas.propina(propina);
        }
        return filas.total("VALOR TOTAL", totales.importeTotal());
    }
}
