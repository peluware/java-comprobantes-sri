package com.peluware.sri.comprobantes.ride.pdfbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Codificación Code 128 de una cadena de dígitos, para el código de barras de la clave de acceso.
 *
 * <p>Ficha Técnica v2.34, numeral 9.20: el código de barras es opcional y lo que lleva es la clave
 * de acceso. El 9.21 remite a una guía de GS1 sobre dónde imprimirlo; no exige los identificadores
 * de aplicación de GS1-128, y los emisores del mercado imprimen Code 128 a secas.
 *
 * <p>La simbología no lleva corrección de errores: cada símbolo son once módulos repartidos en
 * tres barras y tres espacios, y el único control es un dígito de verificación.
 */
final class Code128 {

    /**
     * Módulos en blanco a cada lado del símbolo, sin los cuales un lector no lo reconoce.
     */
    static final int ZONA_MUDA = 10;

    private static final int CODE_B = 100;
    private static final int START_C = 105;
    private static final int STOP = 106;

    /**
     * Ancho de las tres barras y los tres espacios de cada símbolo, en módulos. El de parada
     * lleva una barra más.
     */
    private static final String[] PATRONES = {
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
        "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
        "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
        "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
        "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
        "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
        "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
        "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
        "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
    };

    private Code128() {
    }

    /**
     * Módulos del símbolo, de izquierda a derecha, con las zonas mudas incluidas.
     *
     * <p>Los dígitos se codifican de dos en dos en el juego C. Si sobra uno, como en los cuarenta
     * y nueve de la clave de acceso, el último va suelto en el juego B.
     *
     * @param digitos cadena no vacía de dígitos decimales
     * @return un elemento por módulo; {@code true} es barra
     * @throws IllegalArgumentException si la cadena está vacía o trae algo que no sea un dígito
     */
    static boolean[] modulos(String digitos) {
        var simbolos = simbolos(digitos);

        int ancho = 2 * ZONA_MUDA;
        for (int simbolo : simbolos) {
            ancho += modulosDe(simbolo);
        }

        var modulos = new boolean[ancho];
        int x = ZONA_MUDA;
        for (int simbolo : simbolos) {
            boolean barra = true;
            for (char anchoElemento : PATRONES[simbolo].toCharArray()) {
                int elemento = anchoElemento - '0';
                if (barra) {
                    for (int i = x; i < x + elemento; i++) {
                        modulos[i] = true;
                    }
                }
                x += elemento;
                barra = !barra;
            }
        }
        return modulos;
    }

    /**
     * Símbolos del mensaje: arranque, datos, dígito de verificación y parada.
     */
    private static List<Integer> simbolos(String digitos) {
        if (digitos.isEmpty()) {
            throw new IllegalArgumentException("No hay nada que codificar");
        }

        var simbolos = new ArrayList<Integer>();
        simbolos.add(START_C);

        int i = 0;
        for (; i + 1 < digitos.length(); i += 2) {
            simbolos.add(10 * digito(digitos, i) + digito(digitos, i + 1));
        }
        if (i < digitos.length()) {
            simbolos.add(CODE_B);
            simbolos.add(digito(digitos, i) + 16);
        }

        simbolos.add(verificacion(simbolos));
        simbolos.add(STOP);
        return simbolos;
    }

    /**
     * Dígito de verificación: el valor del arranque más cada símbolo por su posición, módulo 103.
     */
    private static int verificacion(List<Integer> simbolos) {
        int suma = simbolos.getFirst();
        for (int posicion = 1; posicion < simbolos.size(); posicion++) {
            suma += posicion * simbolos.get(posicion);
        }
        return suma % 103;
    }

    private static int modulosDe(int simbolo) {
        int modulos = 0;
        for (char ancho : PATRONES[simbolo].toCharArray()) {
            modulos += ancho - '0';
        }
        return modulos;
    }

    private static int digito(String digitos, int posicion) {
        char caracter = digitos.charAt(posicion);
        if (caracter < '0' || caracter > '9') {
            throw new IllegalArgumentException("Solo se codifican dígitos, se encontró «" + caracter + "» en la posición " + posicion);
        }
        return caracter - '0';
    }
}
