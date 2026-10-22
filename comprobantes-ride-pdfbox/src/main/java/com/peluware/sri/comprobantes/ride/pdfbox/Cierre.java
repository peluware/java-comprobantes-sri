package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.CampoAdicional;
import com.peluware.sri.comprobantes.TotalImpuesto;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import org.apache.pdfbox.pdmodel.font.PDFont;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.valores.Dinero;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.ANCHO_UTIL;
import static com.peluware.sri.comprobantes.ride.pdfbox.Hoja.MARGEN;

/**
 * El bloque que cierra el cuerpo: los totales a la derecha, la información adicional y las formas
 * de pago a la izquierda.
 *
 * <p>El Anexo 2 lo dibuja igual en la factura, la nota de crédito, la nota de débito y la
 * liquidación de compra. Lo que cambia entre ellos es qué filas de total trae cada uno y si lleva
 * formas de pago: la nota de crédito no las lleva, y pasa la lista vacía.
 *
 * <p>La guía de remisión y el comprobante de retención no tienen cierre.
 */
final class Cierre {

    private static final float ANCHO_TOTALES = 190;

    /** Alto de un campo del bloque de información adicional. */
    private static final float ALTO_CAMPO = 10;

    /** Distancia del rótulo al valor en el bloque de información adicional. */
    private static final float SANGRIA_CAMPO = 96;

    private final EstiloRide estilo;

    Cierre(EstiloRide estilo) {
        this.estilo = Objects.requireNonNull(estilo, "estilo");
    }

    /**
     * Una fila de la tabla de totales.
     */
    record Fila(String rotulo, Dinero valor) {
    }

    /**
     * Arma las filas de la tabla de totales en el orden del Anexo 2: los subtotales por tarifa, el
     * descuento, los impuestos, la propina y el importe que cierra.
     *
     * <p>El descuento y la propina solo salen si no son cero.
     */
    static Totales totales(List<TotalImpuesto> impuestos, Dinero totalSinImpuestos) {
        return new Totales(impuestos, totalSinImpuestos);
    }

    /**
     * Recoge lo que cada comprobante lleva en su tabla de totales. El orden en que se dibujan no
     * depende del orden en que se indiquen.
     *
     * <p>No es thread-safe.
     */
    static final class Totales {

        private final List<TotalImpuesto> impuestos;
        private final Dinero totalSinImpuestos;
        private Dinero descuento = Dinero.CERO;
        private Dinero propina = Dinero.CERO;

        private Totales(List<TotalImpuesto> impuestos, Dinero totalSinImpuestos) {
            this.impuestos = List.copyOf(impuestos);
            this.totalSinImpuestos = Objects.requireNonNull(totalSinImpuestos, "totalSinImpuestos");
        }

        Totales descuento(Dinero descuento) {
            this.descuento = Objects.requireNonNull(descuento, "descuento");
            return this;
        }

        Totales propina(Dinero propina) {
            this.propina = Objects.requireNonNull(propina, "propina");
            return this;
        }

        /**
         * Cierra la tabla con el importe final y devuelve las filas.
         */
        List<Fila> total(String rotulo, Dinero valor) {
            var filas = new ArrayList<Fila>();
            subtotales(filas, impuestos, totalSinImpuestos);
            if (!descuento.esCero()) {
                filas.add(new Fila("TOTAL DESCUENTO", descuento));
            }
            impuestos(filas, impuestos);
            if (!propina.esCero()) {
                filas.add(new Fila("PROPINA", propina));
            }
            filas.add(new Fila(rotulo, valor));
            return List.copyOf(filas);
        }
    }

    /**
     * Lo que el bloque de cierre lleva: los totales, la información adicional y las formas de
     * pago. Se mide y se dibuja a partir del mismo valor.
     *
     * @param totales     filas de la tabla de totales; vacía cuando el comprobante no lleva
     * @param adicionales campos del bloque de información adicional
     * @param pagos       formas de pago; vacía cuando el comprobante no las lleva
     */
    record Contenido(List<Fila> totales, Pares adicionales, List<Pago> pagos) {

        Contenido {
            totales = List.copyOf(totales);
            pagos = List.copyOf(pagos);
        }

        /**
         * Cierre con información adicional y nada más.
         */
        static Contenido soloAdicionales(Pares adicionales) {
            return new Contenido(List.of(), adicionales, List.of());
        }
    }

    /**
     * Mide los campos del bloque de información adicional. Sus valores quedan alineados en
     * columna y pasan al renglón siguiente cuando no caben.
     */
    static Pares adicionales(List<CampoAdicional> campos, PDFont normal, PDFont negrita, EstiloRide estilo) {
        float ancho = ANCHO_UTIL - ANCHO_TOTALES - 12;
        var constructor = Pares.constructor(normal, negrita, estilo, estilo.tamChico(), ALTO_CAMPO);
        for (var campo : campos) {
            constructor.filaAlineada(MARGEN + 4, SANGRIA_CAMPO, ancho - 8, campo.nombre() + ":", campo.valor());
        }
        return constructor.build();
    }

    /**
     * Alto que pide el bloque.
     */
    float alto(Contenido contenido) {
        float derecha = estilo.espacio(11) * contenido.totales().size();

        float izquierda = 0;
        if (contenido.adicionales().renglones() > 0) {
            izquierda += altoAdicionales(contenido.adicionales()) + estilo.hueco();
        }
        if (!contenido.pagos().isEmpty()) {
            izquierda += estilo.espacio(11) * (contenido.pagos().size() + 1);
        }
        return Math.max(derecha, izquierda);
    }

    /**
     * Alto del bloque de información adicional: su título más los campos medidos.
     */
    private float altoAdicionales(Pares adicionales) {
        return estilo.espacio(4 + ALTO_CAMPO) + adicionales.alto();
    }

    /**
     * Dibuja el bloque.
     */
    void dibujar(Pluma pluma, Contenido contenido, float arriba) throws IOException {
        dibujar(pluma, contenido.totales(), contenido.adicionales(), contenido.pagos(), arriba);
    }

    /**
     * Alto que pide el bloque: el mayor de sus dos columnas.
     */
    private void dibujar(Pluma pluma, List<Fila> totales, Pares adicionales, List<Pago> pagos, float arriba) throws IOException {
        float anchoTotales = ANCHO_TOTALES;
        float xTotales = MARGEN + ANCHO_UTIL - anchoTotales;
        float altoFila = estilo.espacio(11);
        if (!totales.isEmpty()) {
            float alto = altoFila * totales.size();
            float abajo = arriba - alto;

            pluma.bloque(xTotales, abajo, anchoTotales, alto);
            pluma.lienzo().recortarA(xTotales, abajo, anchoTotales, alto, estilo.radio());
            float y = arriba;
            for (var fila : totales) {
                y -= altoFila;
                if (fila == totales.getLast()) {
                    valorTotal(pluma, fila, xTotales, y, anchoTotales, altoFila);
                } else {
                    pluma.rotulo(estilo.tamChico(), xTotales + 4, y + estilo.espacio(3), fila.rotulo());
                    pluma.lienzo().textoDerecha(pluma.fuenteValor(), estilo.tamChico(), xTotales + anchoTotales - 4, y + estilo.espacio(3), fila.valor().toString());
                }
                if (y > abajo) {
                    pluma.divisionHorizontal(xTotales, y, xTotales + anchoTotales);
                }
            }
            pluma.lienzo().finRecorte();
        }

        float anchoIzquierda = ANCHO_UTIL - anchoTotales - 12;
        float yIzquierda = arriba;
        
        if (adicionales.renglones() > 0) {
            float altoAdicional = altoAdicionales(adicionales);
            pluma.bloque(MARGEN, yIzquierda - altoAdicional, anchoIzquierda, altoAdicional);
            pluma.lienzo().texto(pluma.lienzo().negrita(), estilo.tamTexto(), MARGEN + 4, yIzquierda - estilo.espacio(ALTO_CAMPO), pluma.comoRotulo("Información Adicional"));
            adicionales.dibujar(pluma, yIzquierda - estilo.espacio(ALTO_CAMPO));
            yIzquierda -= altoAdicional + estilo.hueco();
        }
        pagos(pluma, pagos, MARGEN, yIzquierda, anchoIzquierda);
    }

    /**
     * Última fila de los totales, la del importe a pagar.
     *
     * <p>Va en negrita y un punto más grande que el resto, y sobre su propio fondo cuando la
     * paleta lo trae. Es el dato que primero se busca y el Anexo 2 no fija cómo se escribe, solo
     * que esté.
     */
    void valorTotal(Pluma pluma, Fila fila, float x, float y, float ancho, float alto) throws IOException {
        var fondo = estilo.paleta().fondoTotal();
        if (fondo != null) {
            pluma.lienzo().relleno(x, y, ancho, alto, fondo, estilo.paleta().textoTotal());
        } else {
            pluma.lienzo().colorRelleno(estilo.paleta().textoTotal());
        }
        pluma.lienzo().texto(pluma.lienzo().negrita(), estilo.tamChico() + 1, x + 4, y + estilo.espacio(3), pluma.comoRotulo(fila.rotulo()));
        pluma.lienzo().textoDerecha(pluma.lienzo().negrita(), estilo.tamChico() + 1, x + ancho - 4, y + estilo.espacio(3), fila.valor().toString());
        pluma.lienzo().colorRelleno(estilo.paleta().texto());
    }

    /**
     * Tabla de formas de pago, bajo la información adicional.
     *
     * <p>No se dibuja cuando el comprobante no las detalla: el esquema las admite vacías.
     */
    void pagos(Pluma pluma, List<Pago> pagos, float x, float arriba, float ancho) throws IOException {
        
        if (pagos.isEmpty()) {
            return;
        }
        boolean conPlazo = pagos.stream().anyMatch(pago -> pago.plazo() != null);

        float altoFila = estilo.espacio(11);
        float alto = altoFila * (pagos.size() + 1);
        float abajo = arriba - alto;
        float anchoValor = 70;
        float anchoPlazo = conPlazo ? 80 : 0;

        pluma.bloque(x, abajo, ancho, alto);
        pluma.lienzo().recortarA(x, abajo, ancho, alto, estilo.radio());
        pluma.rayaDeCabecera(x, arriba - altoFila, x + ancho);
        pluma.divisionVertical(x + ancho - anchoValor - anchoPlazo, abajo, arriba);
        if (conPlazo) {
            pluma.divisionVertical(x + ancho - anchoValor, abajo, arriba);
        }

        pluma.rotulo(estilo.tamChico(), x + 4, arriba - estilo.espacio(8), "Forma de pago");
        float x1 = x + ancho - anchoValor - anchoPlazo + 4;
        if (conPlazo) {
            pluma.rotulo(estilo.tamChico(), x1, arriba - estilo.espacio(8), "Plazo");
        }
        pluma.rotulo(estilo.tamChico(), x + ancho - anchoValor + 4, arriba - estilo.espacio(8), "Valor");

        float y = arriba - altoFila;
        for (var pago : pagos) {
            y -= altoFila;
            pluma.lienzo().textoRecortado(pluma.fuenteValor(), estilo.tamChico(), x + 4, y + estilo.espacio(3), ancho - anchoValor - anchoPlazo - 8, Formatos.formaPago(pago.formaPago().codigo()));
            var plazo = pago.plazo();
            if (plazo != null) {
                pluma.lienzo().textoRecortado(pluma.fuenteValor(), estilo.tamChico(), x1, y + estilo.espacio(3), anchoPlazo - 8, Formatos.cantidad(plazo.valor()) + " " + plazo.unidadTiempo());
            }
            pluma.lienzo().textoDerecha(pluma.fuenteValor(), estilo.tamChico(), x + ancho - 4, y + estilo.espacio(3), pago.total().toString());
        }
        pluma.lienzo().finRecorte();
    }

    /**
     * Subtotales por tarifa y subtotal sin impuestos.
     *
     * <p>El Anexo 2 los abre igual en los cuatro comprobantes que llevan totales, y los dos datos
     * salen de la lista de impuestos y del total sin impuestos, que todos declaran.
     */
    private static void subtotales(List<Fila> filas, List<TotalImpuesto> impuestos, Dinero totalSinImpuestos) {
        for (var impuesto : impuestos) {
            if (impuesto.codigo() == CodigoImpuesto.IVA) {
                filas.add(new Fila("SUBTOTAL" + Formatos.porcentaje(impuesto), impuesto.baseImponible()));
            }
        }
        filas.add(new Fila("SUBTOTAL SIN IMPUESTOS", totalSinImpuestos));
    }

    /**
     * Una fila por cada impuesto que haya dado valor. Los que salen en cero no se imprimen.
     */
    private static void impuestos(List<Fila> filas, List<TotalImpuesto> impuestos) {
        for (var impuesto : impuestos) {
            if (!impuesto.valor().esCero()) {
                filas.add(new Fila(Formatos.rotuloImpuesto(impuesto), impuesto.valor()));
            }
        }
    }
}
