package com.peluware.sri.comprobantes.catalogo;

import com.peluware.sri.comprobantes.ComprobanteException;

/**
 * Código que no pertenece al catálogo correspondiente.
 */
public final class CatalogoException extends ComprobanteException {

    public CatalogoException(String detalle) {
        super(detalle);
    }

    @Override
    public String mensajeUsuario() {
        return "El comprobante contiene un código que el SRI no reconoce.";
    }
}
