package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import org.jspecify.annotations.Nullable;

/**
 * La otra parte de un comprobante de venta: el {@link Comprador} de una factura o el proveedor de
 * una liquidación de compra.
 *
 * <p>Describe cómo se leen sus datos, y nada más. Los dos siguen siendo tipos distintos y no
 * intercambiables —son los dos extremos opuestos de la misma transacción, y que el compilador no
 * deje confundirlos es deliberado—; lo que comparten es que se declaran con los mismos cuatro
 * campos.
 *
 * <p>Quien solo necesite leerlos, como el RIDE para su recuadro, puede pedir esto en vez del tipo
 * concreto.
 */
public interface Contraparte {

    /**
     * Tipo de identificación de la Tabla 6.
     */
    TipoIdentificacion tipoIdentificacion();

    /**
     * Número de identificación.
     */
    String identificacion();

    /**
     * Nombre o razón social.
     */
    String razonSocial();

    /**
     * Dirección; {@code null} si no se declara.
     */
    @Nullable String direccion();
}
