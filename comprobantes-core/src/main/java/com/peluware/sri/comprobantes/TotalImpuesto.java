package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.interno.Decimales;
import com.peluware.sri.comprobantes.interno.Digitos;
import com.peluware.sri.comprobantes.valores.Dinero;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Impuesto totalizado del comprobante, correspondiente al bloque {@code <totalImpuesto>}.
 *
 * @param codigo           impuesto de la Tabla 16
 * @param codigoPorcentaje código de tarifa dentro de ese impuesto
 * @param tarifa           porcentaje común a los detalles agrupados; {@code null} si difiere
 * @param baseImponible    suma de las bases de los detalles
 * @param valor            suma de los valores de los detalles
 */
public record TotalImpuesto(CodigoImpuesto codigo, String codigoPorcentaje, @Nullable BigDecimal tarifa, Dinero baseImponible, Dinero valor) {

    public TotalImpuesto {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(baseImponible, "baseImponible");
        Objects.requireNonNull(valor, "valor");
        Digitos.validar(codigoPorcentaje, 1, Impuesto.CODIGO_PORCENTAJE_LONGITUD_MAXIMA, "codigoPorcentaje").lanzarSiInvalida();
        if (tarifa != null) {
            Decimales.validar(tarifa, Impuesto.TARIFA_DECIMALES, Impuesto.TARIFA_DIGITOS_TOTALES, "tarifa").lanzarSiInvalida();
            tarifa = Decimales.normalizar(tarifa, Impuesto.TARIFA_DECIMALES);
        }
    }

    /**
     * Agrupa los impuestos de los detalles por impuesto y código de porcentaje.
     *
     * <p>Las bases y los valores son la suma de los de cada detalle; no se recalculan sobre la
     * base agrupada. El ICE específico y el IRBPNR tienen tarifa por unidad y no por porcentaje.
     *
     * <p>El resultado se ordena por impuesto y, dentro de cada uno, por código de porcentaje
     * ascendente.
     */
    public static List<TotalImpuesto> agrupar(List<? extends Detalle> detalles) {
        var grupos = new LinkedHashMap<Clave, Acumulador>();
        for (var detalle : detalles) {
            for (var impuesto : detalle.impuestos()) {
                grupos.computeIfAbsent(new Clave(impuesto.codigo(), impuesto.codigoPorcentaje()), clave -> new Acumulador(impuesto.tarifa())).sumar(impuesto);
            }
        }
        var totales = new ArrayList<TotalImpuesto>(grupos.size());
        grupos.forEach((clave, acumulador) -> totales.add(acumulador.total(clave)));
        totales.sort(Comparator.comparing((TotalImpuesto total) -> total.codigo().codigo()).thenComparing(total -> Integer.parseInt(total.codigoPorcentaje())));
        return List.copyOf(totales);
    }

    private record Clave(CodigoImpuesto codigo, String codigoPorcentaje) {
    }

    private static final class Acumulador {

        private final BigDecimal tarifa;
        private boolean tarifaComun = true;
        private Dinero baseImponible = Dinero.CERO;
        private Dinero valor = Dinero.CERO;

        private Acumulador(BigDecimal tarifa) {
            this.tarifa = tarifa;
        }

        private void sumar(Impuesto impuesto) {
            tarifaComun = tarifaComun && tarifa.compareTo(impuesto.tarifa()) == 0;
            baseImponible = baseImponible.mas(impuesto.baseImponible());
            valor = valor.mas(impuesto.valor());
        }

        private TotalImpuesto total(Clave clave) {
            return new TotalImpuesto(clave.codigo(), clave.codigoPorcentaje(), tarifaComun ? tarifa : null, baseImponible, valor);
        }
    }
}
