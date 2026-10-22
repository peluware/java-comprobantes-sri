package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoEmision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaveAccesoTest {

    private static final String CLAVE = DatosDePrueba.CLAVE_ACCESO;

    @Nested
    @DisplayName("generación")
    class Generacion {

        @Test
        @DisplayName("compone la clave del XML de ejemplo del SRI")
        void componeLaClaveDeEjemplo() {
            assertEquals(CLAVE, ClaveAcceso.generar(DatosDePrueba.FECHA_EMISION, DatosDePrueba.TIPO, DatosDePrueba.RUC, DatosDePrueba.AMBIENTE, DatosDePrueba.SERIE, DatosDePrueba.SECUENCIAL, DatosDePrueba.CODIGO_NUMERICO, TipoEmision.NORMAL));
        }

        @Test
        @DisplayName("la clave de cada tipo de comprobante lleva su propio código")
        void cadaTipoLlevaSuCodigo() {
            for (var tipo : TipoComprobante.values()) {
                var clave = ClaveAcceso.generar(DatosDePrueba.FECHA_EMISION, tipo, DatosDePrueba.RUC, DatosDePrueba.AMBIENTE, DatosDePrueba.SERIE, DatosDePrueba.SECUENCIAL, DatosDePrueba.CODIGO_NUMERICO, TipoEmision.NORMAL);

                assertEquals(tipo.codigo(), clave.substring(8, 10), tipo + " -> " + clave);
                assertTrue(ClaveAcceso.esValido(clave), clave);
            }
        }
    }

    @Nested
    @DisplayName("validación")
    class Validaciones {

        @Test
        @DisplayName("detecta un dígito cambiado al transcribir")
        void detectaUnDigitoCambiado() {
            var alterada = CLAVE.substring(0, 30) + (CLAVE.charAt(30) == '0' ? '1' : '0') + CLAVE.substring(31);

            assertFalse(ClaveAcceso.esValido(alterada), alterada);
        }

        @Test
        @DisplayName("detecta dos dígitos transpuestos al transcribir")
        void detectaTransposicion() {
            var transpuesta = CLAVE.charAt(1) + "" + CLAVE.charAt(0) + CLAVE.substring(2);

            assertFalse(ClaveAcceso.esValido(transpuesta), transpuesta);
        }

        @Test
        @DisplayName("toda clave mal formada se rechaza señalando el campo claveAcceso")
        void senalaElCampo() {
            assertTrue(ClaveAcceso.validar(CLAVE).esValida());

            assertEquals("claveAcceso", invalido(null).campo());
            assertEquals("claveAcceso", invalido(CLAVE.substring(0, ClaveAcceso.LONGITUD_SIN_DV)).campo());
            assertEquals("claveAcceso", invalido("O" + CLAVE.substring(1)).campo());
            assertEquals("claveAcceso", invalido(CLAVE.substring(0, ClaveAcceso.LONGITUD_SIN_DV) + "0").campo());
        }

        @Test
        @DisplayName("exigir conserva el defecto que originó el rechazo")
        void exigirConservaElDefecto() {
            var alterada = CLAVE.substring(0, ClaveAcceso.LONGITUD_SIN_DV) + "0";

            var e = assertThrows(ValidacionException.class, () -> ClaveAcceso.exigir(alterada));

            assertEquals(ClaveAcceso.validar(alterada), e.invalida());
        }

        @Test
        @DisplayName("exigir devuelve la clave intacta")
        void exigirDevuelveLaClave() {
            assertEquals(CLAVE, ClaveAcceso.exigir(CLAVE));
        }

        @Test
        @DisplayName("isValid no lanza nunca, tampoco con null")
        void isValidNoLanza() {
            assertTrue(ClaveAcceso.esValido(CLAVE));
            assertFalse(ClaveAcceso.esValido(null));
        }

        private Validacion.CampoInvalido invalido(String valor) {
            return assertInstanceOf(Validacion.CampoInvalido.class, ClaveAcceso.validar(valor));
        }
    }
}
