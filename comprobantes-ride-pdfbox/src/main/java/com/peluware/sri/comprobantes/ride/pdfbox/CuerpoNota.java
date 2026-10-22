package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.ComprobanteNota;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.TotalImpuesto;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static com.peluware.sri.comprobantes.ride.pdfbox.Receptor.*;

/**
 * El cuerpo del RIDE de una nota que corrige un comprobante: el recuadro del comprador con el
 * documento que se modifica, una tabla y el cierre.
 *
 * <p>Anexo 2, páginas 61 y 62. Las cuatro filas del recuadro son idénticas en las dos notas: son
 * {@code InfoNota} dibujado. Lo que sigue no se parece, y está bien que no: la nota de crédito
 * justifica la corrección detallando lo que vuelve, con líneas de producto, y la de débito
 * listando razones con su importe.
 *
 * @param <C> nota que dibuja
 */
abstract sealed class CuerpoNota<C extends ComprobanteNota> extends Cuerpo<C> permits CuerpoNotaCredito, CuerpoNotaDebito {

    /**
     * Lo que cierra la nota, que las dos declaran con la misma forma y distinto origen.
     */
    record Cifras(List<TotalImpuesto> impuestos, Dinero totalSinImpuestos, Dinero valorTotal) {
    }

    protected CuerpoNota(C comprobante, PDFont normal, PDFont negrita, EstiloRide estilo) {
        super(comprobante, normal, negrita, estilo);
    }

    /**
     * La tabla con que cada nota justifica la corrección.
     */
    protected abstract Tabla tabla();

    /**
     * Los importes que cierran la nota.
     */
    protected abstract Cifras cifras();

    /**
     * La razón de la modificación, que va al pie del recuadro; por omisión ninguna, porque solo
     * la nota de crédito la declara.
     */
    protected @Nullable String motivo() {
        return null;
    }

    /**
     * Las formas de pago; por omisión ninguna, porque una nota de crédito no se paga, se aplica.
     */
    protected List<Pago> pagos() {
        return List.of();
    }

    @Override
    protected final Maquetacion armarMaquetacion() {
        return new ReceptorConTabla(estilo, pares(), tabla());
    }

    @Override
    protected final Cierre.Contenido armarContenido() {
        var cifras = cifras();
        var totales = Cierre.totales(cifras.impuestos(), cifras.totalSinImpuestos()).total("VALOR TOTAL", cifras.valorTotal());
        return new Cierre.Contenido(totales, Cierre.adicionales(comprobante.infoAdicional(), normal, negrita, estilo), pagos());
    }

    /**
     * El recuadro del comprador con el comprobante que se modifica.
     */
    private Pares pares() {
        var info = comprobante.info();
        var comprador = info.comprador();
        var modificado = info.documentoModificado();
        var motivo = motivo();

        var constructor = Pares.constructor(normal, negrita, estilo)
            .fila(izquierda(), anchoIzquierda(), "Razón Social / Nombres y Apellidos:", comprador.razonSocial(), mitad(), anchoDerecha(), "Identificación:", comprador.identificacion())
            .fila(izquierda(), anchoIzquierda(), "Fecha de Emisión:", Formatos.FECHA.format(info.fechaEmision()), mitad(), anchoDerecha(), "Dirección:", comprador.direccion())
            .fila(izquierda(), anchoCompleto(), "Comprobante que se modifica:", Formatos.titulo(modificado.tipoComprobante()) + "  " + modificado.numero())
            .fila(izquierda(), anchoCompleto(), "Fecha Emisión (Comprobante a modificar):", Formatos.FECHA.format(modificado.fechaEmision()));

        if (motivo != null) {
            constructor.fila(izquierda(), anchoCompleto(), "Razón de Modificación:", motivo);
        }
        return constructor.build();
    }
}
