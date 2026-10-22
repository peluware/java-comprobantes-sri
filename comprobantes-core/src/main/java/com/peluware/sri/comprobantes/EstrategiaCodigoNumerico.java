package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.valores.CodigoNumerico;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Genera el código numérico de una clave de acceso.
 *
 * <p>Las implementaciones deben ser thread-safe.
 */
@FunctionalInterface
public interface EstrategiaCodigoNumerico {

    /**
     * Genera el código numérico correspondiente a un comprobante.
     *
     * @param id identidad del comprobante
     */
    CodigoNumerico generar(ComprobanteId id);

    /**
     * Deriva el código numérico como {@code HMAC-SHA256(secreto, id) mod 10^8}.
     *
     * <p>Determinista: la misma identidad produce siempre el mismo código, como exige el
     * numeral 5.10 de la Ficha Técnica v2.34 para reenviar un comprobante rechazado con su
     * clave de acceso original.
     *
     * @param secreto material de clave de al menos 16 bytes; se copia defensivamente
     * @throws ConfiguracionException si el secreto es más corto de 16 bytes
     */
    static EstrategiaCodigoNumerico hmac(byte[] secreto) {
        Objects.requireNonNull(secreto, "secreto");
        if (secreto.length < 16) {
            throw new ConfiguracionException("El secreto HMAC debe tener al menos 16 bytes, tiene " + secreto.length);
        }
        byte[] clave = secreto.clone();
        return id -> {
            Objects.requireNonNull(id, "id");
            try {
                var mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(clave, "HmacSHA256"));
                byte[] digest = mac.doFinal(id.derivationInput().getBytes(StandardCharsets.UTF_8));
                long valor = new BigInteger(1, digest)
                    .mod(BigInteger.valueOf(100_000_000L))
                    .longValueExact();
                return CodigoNumerico.of(valor);
            } catch (GeneralSecurityException e) {
                throw new ConfiguracionException("No se pudo derivar el código numérico con HmacSHA256", e);
            }
        };
    }

    /**
     * Devuelve siempre el mismo código numérico.
     */
    static EstrategiaCodigoNumerico fixed(CodigoNumerico codigo) {
        Objects.requireNonNull(codigo, "codigo");
        return id -> codigo;
    }

    /**
     * Genera un código numérico distinto en cada llamada.
     *
     * @deprecated No es reproducible: incumple el numeral 5.10 de la Ficha Técnica v2.34.
     * Use {@link #hmac(byte[])}.
     */
    @Deprecated
    static EstrategiaCodigoNumerico random(RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        return id -> CodigoNumerico.of(random.nextLong(100_000_000L));
    }
}
