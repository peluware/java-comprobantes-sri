package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EstrategiaCodigoNumericoTest {

    private static final byte[] SECRETO = "un-secreto-de-al-menos-16-bytes".getBytes(StandardCharsets.UTF_8);

    private static final ComprobanteId ID = new ComprobanteId(DatosDePrueba.RUC, DatosDePrueba.TIPO, DatosDePrueba.SERIE, DatosDePrueba.SECUENCIAL);

    @Test
    @DisplayName("hmac devuelve siempre el mismo código para el mismo comprobante")
    void hmacEsDeterminista() {
        var estrategia = EstrategiaCodigoNumerico.hmac(SECRETO);

        assertEquals(estrategia.generar(ID), estrategia.generar(ID));
    }

    @Test
    @DisplayName("hmac es estable entre instancias construidas con el mismo secreto")
    void hmacEsEstableEntreInstancias() {
        assertEquals(EstrategiaCodigoNumerico.hmac(SECRETO).generar(ID), EstrategiaCodigoNumerico.hmac(SECRETO.clone()).generar(ID));
    }

    @Test
    @DisplayName("hmac rellena hasta ocho dígitos cuando el valor derivado es más corto")
    void hmacSiempreOchoDigitos() {
        var estrategia = EstrategiaCodigoNumerico.hmac(SECRETO);
        for (long secuencial = 1; secuencial <= 500; secuencial++) {
            var id = new ComprobanteId(ID.ruc(), ID.tipoComprobante(), ID.serie(), Secuencial.of(secuencial));
            assertEquals(CodigoNumerico.LONGITUD, estrategia.generar(id).valor().length());
        }
    }

    @Test
    @DisplayName("el secuencial siguiente produce un código distinto")
    void secuencialSiguienteDifiere() {
        var estrategia = EstrategiaCodigoNumerico.hmac(SECRETO);
        var siguiente = new ComprobanteId(ID.ruc(), ID.tipoComprobante(), ID.serie(), Secuencial.of(2));

        assertNotEquals(estrategia.generar(ID), estrategia.generar(siguiente));
    }

    @Test
    @DisplayName("otro emisor con el mismo secuencial obtiene un código distinto")
    void otroEmisorDifiere() {
        var estrategia = EstrategiaCodigoNumerico.hmac(SECRETO);
        var otroEmisor = new ComprobanteId(new Ruc("1790012345001"), ID.tipoComprobante(), ID.serie(), ID.secuencial());

        assertNotEquals(estrategia.generar(ID), estrategia.generar(otroEmisor));
    }

    @Test
    @DisplayName("un secreto distinto produce un código distinto, lo que impide enumerar claves ajenas")
    void secretoDistintoDifiere() {
        var propio = EstrategiaCodigoNumerico.hmac(SECRETO).generar(ID);
        var ajeno = EstrategiaCodigoNumerico.hmac("un-secreto-completamente-distinto".getBytes(StandardCharsets.UTF_8)).generar(ID);

        assertNotEquals(propio, ajeno);
    }

    @Test
    @DisplayName("rechaza un secreto de menos de 16 bytes")
    void rechazaSecretoCorto() {
        byte[] quinceBytes = new byte[15];

        assertThrows(ConfiguracionException.class, () -> EstrategiaCodigoNumerico.hmac(quinceBytes));
    }

    @Test
    @DisplayName("numeral 5.10: el reenvío reproduce la misma clave de acceso")
    void elReenvioReproduceLaClave() {
        var estrategia = EstrategiaCodigoNumerico.hmac(SECRETO);

        assertEquals(construir(estrategia), construir(estrategia));
    }

    private static String construir(EstrategiaCodigoNumerico estrategia) {
        return DatosDePrueba.facturaSinCodigoNumerico()
            .codigoNumerico(estrategia)
            .detalle(DetalleComprobanteVenta.builder().descripcion("Consulta veterinaria").cantidad(1).precioUnitario("100.00").iva(TarifaIva.QUINCE).build())
            .build()
            .claveAcceso();
    }
}
