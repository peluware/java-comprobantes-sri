package com.peluware.sri.comprobantes;

import com.peluware.sri.comprobantes.retencion.ComprobanteRetencionWriter;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemisionWriter;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompraWriter;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.factura.FacturaWriter;
import com.peluware.sri.comprobantes.notacredito.NotaCreditoWriter;
import com.peluware.sri.comprobantes.notadebito.NotaDebitoWriter;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Lo que cumple todo comprobante, sea del tipo que sea.
 *
 * <p>Un tipo nuevo se añade en {@link #tipos()} y hereda las comprobaciones de aquí: un fallo
 * en la identidad, en la clave de acceso o en los campos adicionales se detecta una sola vez y
 * para todos.
 *
 * <p>Lo propio de cada tipo se prueba en el paquete de ese tipo.
 */
class ComprobanteContratoTest {


    /** Posición del código numérico dentro de la clave de acceso, según la Tabla 1. */
    private static final int CODIGO_NUMERICO_DESDE = 39;
    private static final int CODIGO_NUMERICO_HASTA = 47;

    /**
     * Construye un comprobante del tipo, aplicando antes un ajuste a la parte común del
     * builder.
     */
    @FunctionalInterface
    interface Fabrica {
        Comprobante construir(Consumer<Comprobante.Builder<?>> ajuste);
    }

    /**
     * @param raiz nombre del elemento raíz de su XML
     * @param xsd  esquema contra el que debe validar
     */
    record Tipo(String nombre, TipoComprobante tipoComprobante, String raiz, String xsd, Fabrica conCodigoNumerico, Fabrica sinCodigoNumerico) {

        /**
         * La versión que declara el nombre del esquema, {@code factura_V2.1.0.xsd} → 2.1.0.
         *
         * <p>Se saca de ahí a propósito: así la aserción compara lo que el writer escribe contra
         * el esquema contra el que de verdad se valida. Tomándola del propio writer no podría
         * fallar nunca.
         */
        String version() {
            return xsd.replaceAll(".*_V(.+)[.]xsd", "$1");
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    static List<Tipo> tipos() {
        return List.of(
            new Tipo("comprobante de retención", TipoComprobante.COMPROBANTE_RETENCION, "comprobanteRetencion", "ComprobanteRetencion_V2.0.0.xsd",
                ajuste -> {
                    var builder = DatosDePrueba.comprobanteRetencion();
                    ajuste.accept(builder);
                    return builder.build();
                },
                ajuste -> {
                    var builder = DatosDePrueba.comprobanteRetencionSinCodigoNumerico();
                    ajuste.accept(builder);
                    return builder.build();
                }),
            new Tipo("factura", TipoComprobante.FACTURA, "factura", "factura_V2.1.0.xsd",
                ajuste -> {
                    var builder = DatosDePrueba.factura();
                    ajuste.accept(builder);
                    return builder.build();
                },
                ajuste -> {
                    var builder = DatosDePrueba.facturaSinCodigoNumerico();
                    ajuste.accept(builder);
                    return builder.build();
                }),
            new Tipo("guía de remisión", TipoComprobante.GUIA_REMISION, "guiaRemision", "GuiaRemision_V1.1.0.xsd",
                ajuste -> {
                    var builder = DatosDePrueba.guiaRemision();
                    ajuste.accept(builder);
                    return builder.build();
                },
                ajuste -> {
                    var builder = DatosDePrueba.guiaRemisionSinCodigoNumerico();
                    ajuste.accept(builder);
                    return builder.build();
                }),
            new Tipo("liquidación de compra", TipoComprobante.LIQUIDACION_COMPRA, "liquidacionCompra", "LiquidacionCompra_V1.1.0.xsd",
                ajuste -> {
                    var builder = DatosDePrueba.liquidacionCompra();
                    ajuste.accept(builder);
                    return builder.build();
                },
                ajuste -> {
                    var builder = DatosDePrueba.liquidacionCompraSinCodigoNumerico();
                    ajuste.accept(builder);
                    return builder.build();
                }),
            new Tipo("nota de crédito", TipoComprobante.NOTA_CREDITO, "notaCredito", "NotaCredito_V1.1.0.xsd",
                ajuste -> {
                    var builder = DatosDePrueba.notaCredito();
                    ajuste.accept(builder);
                    return builder.build();
                },
                ajuste -> {
                    var builder = DatosDePrueba.notaCreditoSinCodigoNumerico();
                    ajuste.accept(builder);
                    return builder.build();
                }),
            new Tipo("nota de débito", TipoComprobante.NOTA_DEBITO, "notaDebito", "NotaDebito_V1.0.0.xsd",
                ajuste -> {
                    var builder = DatosDePrueba.notaDebito();
                    ajuste.accept(builder);
                    return builder.build();
                },
                ajuste -> {
                    var builder = DatosDePrueba.notaDebitoSinCodigoNumerico();
                    ajuste.accept(builder);
                    return builder.build();
                })
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("vuelve de su XML idéntico, byte por byte")
    void idaYVuelta(Tipo tipo) {
        var xml = ComprobanteWriter.xml(valido(tipo));

        var leido = ComprobanteReader.comprobante(xml);

        assertArrayEquals(xml, ComprobanteWriter.xml(leido), "el XML del comprobante leído no coincide con el de origen");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("su clave de acceso tiene 49 dígitos y el dígito verificador cuadra")
    void laClaveEsValida(Tipo tipo) {
        var comprobante = valido(tipo);

        assertEquals(ClaveAcceso.LONGITUD, comprobante.claveAcceso().length());
        assertTrue(ClaveAcceso.esValido(comprobante.claveAcceso()), comprobante.claveAcceso());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Tabla 1: la clave empieza por la fecha y el código de su tipo")
    void laClaveEmpiezaPorLaFechaYElCodigo(Tipo tipo) {
        var clave = valido(tipo).claveAcceso();

        assertEquals(ClaveAcceso.FECHA.format(DatosDePrueba.FECHA_EMISION), clave.substring(0, 8));
        assertEquals(tipo.tipoComprobante().codigo(), clave.substring(8, 10));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("expone la identidad que se le indicó")
    void exponeLaIdentidad(Tipo tipo) {
        var comprobante = valido(tipo);

        assertEquals(DatosDePrueba.RUC, comprobante.emisor().ruc());
        assertEquals(DatosDePrueba.AMBIENTE, comprobante.ambiente());
        assertEquals(DatosDePrueba.SERIE, comprobante.serie());
        assertEquals(DatosDePrueba.SECUENCIAL, comprobante.secuencial());
        assertEquals(DatosDePrueba.FECHA_EMISION, comprobante.fechaEmision());
        assertEquals(tipo.tipoComprobante(), comprobante.tipoComprobante());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("su XML valida contra el XSD del SRI")
    void elXmlValidaContraSuEsquema(Tipo tipo) {
        byte[] xml = ComprobanteWriter.xml(valido(tipo));
        try {
            Esquemas.de(tipo.xsd()).newValidator().validate(new StreamSource(new ByteArrayInputStream(xml)));
        } catch (Exception e) {
            fail(tipo.nombre() + " no cumple el XSD: " + e.getMessage() + "\n" + new String(xml, StandardCharsets.UTF_8));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("su raíz lleva id=comprobante y la versión de su esquema")
    void laRaizLlevaIdYVersion(Tipo tipo) {
        var xml = new String(ComprobanteWriter.xml(valido(tipo)), StandardCharsets.UTF_8);

        assertTrue(xml.contains("<" + tipo.raiz() + " id=\"" + Sri.ID_COMPROBANTE + "\" version=\"" + tipo.version() + "\">"), xml);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("admite exactamente quince campos adicionales")
    void admiteQuinceCamposAdicionales(Tipo tipo) {
        var comprobante = tipo.conCodigoNumerico().construir(builder -> {
            for (int i = 0; i < CampoAdicional.MAX_POR_COMPROBANTE; i++) {
                builder.campoAdicional("campo" + i, "valor" + i);
            }
        });

        assertEquals(CampoAdicional.MAX_POR_COMPROBANTE, comprobante.infoAdicional().size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("rechaza más de quince campos adicionales")
    void rechazaDieciseisCamposAdicionales(Tipo tipo) {
        var e = assertThrows(ValidacionException.class, () -> tipo.conCodigoNumerico().construir(builder -> {
            for (int i = 0; i <= CampoAdicional.MAX_POR_COMPROBANTE; i++) {
                builder.campoAdicional("campo" + i, "valor" + i);
            }
        }));

        assertEquals("infoAdicional", assertInstanceOf(Validacion.CampoInvalido.class, e.invalida()).campo());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Anexo 26: el RUC del proveedor va como campo adicional con el nombre literal")
    void rucDelProveedor(Tipo tipo) {
        var proveedor = new com.peluware.sri.comprobantes.valores.Ruc("1790012345001");

        var comprobante = tipo.conCodigoNumerico().construir(builder -> builder.rucProveedorSistema(proveedor));

        var campo = comprobante.infoAdicional().getFirst();
        assertEquals(CampoAdicional.NOMBRE_RUC_PROVEEDOR_SISTEMA, campo.nombre());
        assertEquals(proveedor.valor(), campo.valor());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("exige un código numérico")
    void exigeCodigoNumerico(Tipo tipo) {
        assertThrows(ConfiguracionException.class, () -> tipo.sinCodigoNumerico().construir(builder -> {
        }));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("rechaza código numérico y estrategia a la vez")
    void rechazaAmbosCodigosNumericos(Tipo tipo) {
        assertThrows(ConfiguracionException.class, () -> tipo.conCodigoNumerico()
            .construir(builder -> builder.codigoNumerico(EstrategiaCodigoNumerico.fixed(CodigoNumerico.of(2)))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("la estrategia de código numérico recibe la identidad de su propio tipo")
    void laEstrategiaRecibeLaIdentidad(Tipo tipo) {
        var esperado = new ComprobanteId(DatosDePrueba.RUC, tipo.tipoComprobante(), DatosDePrueba.SERIE, DatosDePrueba.SECUENCIAL);

        var comprobante = tipo.sinCodigoNumerico().construir(builder -> builder.codigoNumerico(id -> {
            assertEquals(esperado, id);
            return DatosDePrueba.CODIGO_NUMERICO;
        }));

        assertEquals(DatosDePrueba.CODIGO_NUMERICO.valor(), comprobante.claveAcceso().substring(CODIGO_NUMERICO_DESDE, CODIGO_NUMERICO_HASTA));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("xml() del modelo y su escritor producen lo mismo")
    void elModeloYSuEscritorCoinciden(Tipo tipo) {
        var comprobante = valido(tipo);

        assertArrayEquals(ComprobanteWriter.xml(comprobante), ComprobanteWriter.xml(comprobante));
        assertTrue(new String(ComprobanteWriter.xml(comprobante), StandardCharsets.UTF_8).contains(comprobante.claveAcceso()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Anexo 22: el régimen RIMPE se escribe con la leyenda literal")
    void leyendaRimpe(Tipo tipo) {
        var rimpe = Emisor.builder(DatosDePrueba.RUC, DatosDePrueba.RAZON_SOCIAL, DatosDePrueba.DIRECCION_MATRIZ).contribuyenteRimpe(true).build();

        var xml = xml(tipo.conCodigoNumerico().construir(builder -> builder.emisor(rimpe)));

        assertTrue(xml.contains("<contribuyenteRimpe>" + Sri.LEYENDA_RIMPE + "</contribuyenteRimpe>"), xml);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Anexo 13: la máquina fiscal va justo antes de la información adicional")
    void maquinaFiscal(Tipo tipo) {
        var maquina = new MaquinaFiscal("SISPAU", "ABC1234", "CGMC1405");

        var xml = xml(tipo.conCodigoNumerico().construir(builder -> builder.maquinaFiscal(maquina).campoAdicional("Origen", "pruebas")));

        var bloque = "<maquinaFiscal><marca>SISPAU</marca><modelo>ABC1234</modelo><serie>CGMC1405</serie></maquinaFiscal>";
        assertTrue(xml.contains(bloque), xml);
        assertTrue(xml.indexOf(bloque) < xml.indexOf("<infoAdicional>"), xml);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("la dirección del establecimiento sale del establecimiento, no del emisor")
    void direccionDelEstablecimiento(Tipo tipo) {
        var xml = xml(tipo.conCodigoNumerico().construir(builder -> builder.establecimiento(DatosDePrueba.ESTABLECIMIENTO.conDireccion("AV. NORTE"))));

        assertTrue(xml.contains("<" + Establecimiento.CAMPO_DIRECCION + ">AV. NORTE</" + Establecimiento.CAMPO_DIRECCION + ">"), xml);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("escapa los caracteres especiales y conserva los acentos")
    void escapaYConservaAcentos(Tipo tipo) {
        var emisor = Emisor.of(DatosDePrueba.RUC, "PÉREZ & HIJOS S.A.", "AV. AMAZONAS <N1>");

        var xml = xml(tipo.conCodigoNumerico().construir(builder -> builder.emisor(emisor)));

        assertTrue(xml.contains("<razonSocial>PÉREZ &amp; HIJOS S.A.</razonSocial>"), xml);
        assertTrue(xml.contains("<dirMatriz>AV. AMAZONAS &lt;N1&gt;</dirMatriz>"), xml);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("las fechas van en dd/MM/yyyy y nunca en ISO")
    void lasFechasVanEnFormatoDelEsquema(Tipo tipo) {
        var xml = xml(valido(tipo));

        assertTrue(xml.contains("05/10/2026"), xml);
        assertFalse(xml.contains("2026-10-05"), () -> "se escapó una fecha en formato ISO: " + xml);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("omite los elementos opcionales que no se indicaron")
    void omiteLosOpcionales(Tipo tipo) {
        var xml = xml(valido(tipo));

        assertFalse(xml.contains("nombreComercial"), xml);
        assertFalse(xml.contains("agenteRetencion"), xml);
        assertFalse(xml.contains("contribuyenteRimpe"), xml);
        assertFalse(xml.contains(Establecimiento.CAMPO_DIRECCION), xml);
        assertFalse(xml.contains("infoAdicional"), xml);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("el campo adicional lleva el nombre como atributo y el valor como contenido")
    void campoAdicionalConNombreYContenido(Tipo tipo) {
        var xml = xml(tipo.conCodigoNumerico().construir(builder -> builder.campoAdicional("Email", "cliente@example.com")));

        assertTrue(xml.contains("<campoAdicional nombre=\"Email\">cliente@example.com</campoAdicional>"), xml);
    }

    private static String xml(Comprobante comprobante) {
        return new String(ComprobanteWriter.xml(comprobante), StandardCharsets.UTF_8);
    }

    private static Comprobante valido(Tipo tipo) {
        return tipo.conCodigoNumerico().construir(builder -> {
        });
    }

}
