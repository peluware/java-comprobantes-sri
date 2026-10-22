package com.peluware.sri.comprobantes;

import org.jspecify.annotations.Nullable;

/**
 * Configuración inválida o incompleta de un componente de la librería.
 */
public final class ConfiguracionException extends ComprobanteException {

    public ConfiguracionException(String detalle) {
        super(detalle);
    }

    public ConfiguracionException(String detalle, @Nullable Throwable causa) {
        super(detalle, causa);
    }

    @Override
    public String mensajeUsuario() {
        return "El sistema de facturación electrónica no está configurado correctamente. Contacte al administrador.";
    }
}
