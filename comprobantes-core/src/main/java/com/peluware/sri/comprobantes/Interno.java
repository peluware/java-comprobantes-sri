package com.peluware.sri.comprobantes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lo marcado es público por obligación del lenguaje, no por contrato.
 *
 * <p>Un {@code record} público no puede tener constructor canónico privado: el JLS exige que sea
 * al menos tan accesible como el propio record. Los bloques de un comprobante se obtienen de él
 * —{@code factura.info()}— y no se construyen desde fuera.
 *
 * <p>Lo marcado puede cambiar de firma en cualquier versión, incluida una de parche, sin que
 * cuente como ruptura.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
public @interface Interno {
}
