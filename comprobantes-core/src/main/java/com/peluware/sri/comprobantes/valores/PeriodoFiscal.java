package com.peluware.sri.comprobantes.valores;

import com.peluware.sri.comprobantes.Validacion;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;

/**
 * Periodo fiscal de un comprobante de retención, en formato {@code MM/AAAA}.
 *
 * <p>El esquema lo restringe al patrón {@code (0[1-9]|1[012])/20[0-9][0-9]}: mes de dos dígitos
 * con cero a la izquierda, año entre 2000 y 2099.
 *
 * @param valor el periodo tal como viaja en el XML
 */
public record PeriodoFiscal(String valor) {

    public static final String CAMPO = "periodoFiscal";

    private static final Pattern FORMATO = Pattern.compile("(0[1-9]|1[012])/20[0-9][0-9]");

    public PeriodoFiscal {
        validar(valor).lanzarSiInvalida();
    }

    /**
     * Periodo del mes y año indicados.
     */
    public static PeriodoFiscal of(int mes, int anio) {
        return new PeriodoFiscal("%02d/%d".formatted(mes, anio));
    }

    /**
     * Periodo al que pertenece la fecha indicada.
     */
    public static PeriodoFiscal de(LocalDate fecha) {
        return of(fecha.getMonthValue(), fecha.getYear());
    }

    /**
     * Periodo al que pertenece el mes indicado.
     */
    public static PeriodoFiscal de(YearMonth mes) {
        return of(mes.getMonthValue(), mes.getYear());
    }

    /**
     * Valida una cadena como periodo fiscal sin construirlo.
     */
    public static Validacion validar(@Nullable String valor) {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            return new Validacion.CampoInvalido(CAMPO, CAMPO + " debe tener el formato MM/AAAA entre 2000 y 2099: " + valor);
        }
        return new Validacion.Valida();
    }

    /**
     * Indica si la cadena es un periodo fiscal válido.
     */
    public static boolean esValido(@Nullable String valor) {
        return validar(valor).esValida();
    }

    @Override
    public String toString() {
        return valor;
    }
}
