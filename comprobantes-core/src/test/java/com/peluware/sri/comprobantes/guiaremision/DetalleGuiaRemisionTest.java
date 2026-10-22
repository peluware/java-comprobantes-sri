package com.peluware.sri.comprobantes.guiaremision;

import com.peluware.sri.comprobantes.Linea;
import com.peluware.sri.comprobantes.Validacion;
import com.peluware.sri.comprobantes.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * La línea de la guía de remisión es la única que hereda de {@link Linea} sin pasar por
 * {@code Detalle}, así que es el camino por el que se comprueba que las reglas comunes se
 * imponen igual.
 */
class DetalleGuiaRemisionTest {

    @Test
    @DisplayName("rechaza una descripción más larga de lo que admite el esquema")
    void rechazaDescripcionDemasiadoLarga() {
        var larga = "X".repeat(Linea.DESCRIPCION_LONGITUD_MAXIMA + 1);

        assertEquals("descripcion", campoInvalido(() -> DetalleGuiaRemision.builder()
            .descripcion(larga)
            .cantidad(1)
            .build()));
    }

    @Test
    @DisplayName("rechaza más datos adicionales de los que admite el esquema")
    void rechazaDemasiadosDatosAdicionales() {
        var builder = DetalleGuiaRemision.builder()
            .descripcion("Alimento para perro")
            .cantidad(1)
            .detalleAdicional("a", "1")
            .detalleAdicional("b", "2")
            .detalleAdicional("c", "3")
            .detalleAdicional("d", "4");

        assertEquals("detallesAdicionales", campoInvalido(builder::build));
    }

    @Test
    @DisplayName("rechaza un código más largo de lo que admite el esquema")
    void rechazaCodigoDemasiadoLargo() {
        var largo = "X".repeat(Linea.CODIGO_LONGITUD_MAXIMA + 1);

        assertEquals("codigoInterno", campoInvalido(() -> DetalleGuiaRemision.builder()
            .descripcion("Alimento para perro")
            .cantidad(1)
            .codigoInterno(largo)
            .build()));
    }

    /**
     * Nombre del campo que la validación señala.
     */
    private static String campoInvalido(Executable accion) {
        var error = assertThrows(ValidacionException.class, accion);
        return assertInstanceOf(Validacion.CampoInvalido.class, error.invalida()).campo();
    }
}
