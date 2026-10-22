package com.peluware.sri.comprobantes.ride.pdfbox;

import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;
import com.google.zxing.oned.Code128Reader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * El oráculo es el lector de ZXing, que es independiente de nuestro codificador: si un patrón de
 * la tabla o el dígito de verificación estuvieran mal, no decodificaría.
 */
class Code128Test {

    private static final String CLAVE = "2010202601179001234500110010020000001304070511711";

    @Test
    @DisplayName("la clave de acceso se vuelve a leer tal cual")
    void idaYVuelta() throws NotFoundException, FormatException, ChecksumException {
        assertEquals(CLAVE, leer(Code128.modulos(CLAVE)));
    }

    @Test
    @DisplayName("un número par de dígitos también, sin el cambio de juego del sobrante")
    void numeroParDeDigitos() {
        var par = CLAVE.substring(1);

        assertAll(
            () -> assertEquals(48, par.length()),
            () -> assertEquals(par, leer(Code128.modulos(par)))
        );
    }

    @Test
    @DisplayName("los cuarenta y nueve dígitos caben en veintinueve símbolos")
    void anchoDelSimbolo() {
        int modulos = Code128.modulos(CLAVE).length;

        assertEquals(2 * Code128.ZONA_MUDA + 28 * 11 + 13, modulos, "arranque, veinticuatro pares, cambio a B, el dígito suelto, la verificación y la parada");
    }

    @Test
    @DisplayName("las zonas mudas van en blanco a ambos lados")
    void zonasMudas() {
        var modulos = Code128.modulos(CLAVE);
        int encendidos = 0;
        for (int i = 0; i < Code128.ZONA_MUDA; i++) {
            if (modulos[i] || modulos[modulos.length - 1 - i]) {
                encendidos++;
            }
        }

        assertEquals(0, encendidos, "las zonas mudas de los dos lados van en blanco");
    }

    @Test
    @DisplayName("lo que no sea un dígito se rechaza")
    void soloDigitos() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> Code128.modulos("2010A0260117")),
            () -> assertThrows(IllegalArgumentException.class, () -> Code128.modulos(""))
        );
    }

    /**
     * Pasa los módulos al lector de ZXing como una fila de píxeles.
     */
    private static String leer(boolean[] modulos) throws NotFoundException, FormatException, ChecksumException {
        var fila = new BitArray(modulos.length);
        for (int i = 0; i < modulos.length; i++) {
            if (modulos[i]) {
                fila.set(i);
            }
        }
        return new Code128Reader().decodeRow(0, fila, null).getText();
    }
}
