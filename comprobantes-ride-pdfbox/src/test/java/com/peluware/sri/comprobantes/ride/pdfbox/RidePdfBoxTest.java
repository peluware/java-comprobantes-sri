package com.peluware.sri.comprobantes.ride.pdfbox;

import com.peluware.sri.comprobantes.Comprador;
import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.DetalleComprobanteVenta;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.EstrategiaCodigoNumerico;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Plazo;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TarifaIva;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoIdentificacion;
import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.factura.Factura;
import com.peluware.sri.comprobantes.guiaremision.Destinatario;
import com.peluware.sri.comprobantes.guiaremision.DetalleGuiaRemision;
import com.peluware.sri.comprobantes.guiaremision.GuiaRemision;
import com.peluware.sri.comprobantes.guiaremision.Transportista;
import com.peluware.sri.comprobantes.notacredito.DetalleNotaCredito;
import com.peluware.sri.comprobantes.notacredito.NotaCredito;
import com.peluware.sri.comprobantes.ride.RideException;
import com.peluware.sri.comprobantes.valores.Serie;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.liquidacioncompra.LiquidacionCompra;
import com.peluware.sri.comprobantes.liquidacioncompra.Proveedor;
import com.peluware.sri.comprobantes.notadebito.NotaDebito;
import com.peluware.sri.comprobantes.retencion.CodigoImpuestoRetencion;
import com.peluware.sri.comprobantes.retencion.ComprobanteRetencion;
import com.peluware.sri.comprobantes.retencion.DocumentoSustento;
import com.peluware.sri.comprobantes.retencion.Retencion;
import com.peluware.sri.comprobantes.retencion.SujetoRetenido;
import com.peluware.sri.comprobantes.valores.PeriodoFiscal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.util.List;
import java.util.ArrayList;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RidePdfBoxTest {

    private static final byte[] SECRETO = "un-secreto-de-al-menos-dieciseis-bytes".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("el RIDE es un PDF de una página A4 con el texto del Anexo 2")
    void contenido() throws IOException {
        var factura = factura();
        var pdf = generar(new RidePdfBox(), factura);

        try (var documento = Loader.loadPDF(pdf)) {
            var texto = new PDFTextStripper().getText(documento);
            assertAll(
                () -> assertEquals(1, documento.getNumberOfPages()),
                () -> assertEquals(595, Math.round(documento.getPage(0).getMediaBox().getWidth()), "A4"),
                () -> assertTrue(texto.contains("FACTURA"), texto),
                () -> assertTrue(texto.contains("1790012345001"), "el RUC del emisor"),
                () -> assertTrue(texto.contains("PELUWARE S.A."), "la razón social"),
                () -> assertTrue(texto.contains("001-002-000000130"), "el número de documento"),
                () -> assertTrue(texto.contains(factura.claveAcceso()), "la clave de acceso, que es el número de autorización"),
                () -> assertTrue(texto.contains("JUAN PEREZ"), "el comprador"),
                () -> assertTrue(texto.contains("Alimento para perro 15kg"), "la línea de detalle"),
                () -> assertTrue(texto.contains("VALOR TOTAL"), "la tabla de totales"),
                () -> assertTrue(texto.contains("68.50"), "el importe total")
            );
        }
    }

    @Test
    @DisplayName("sin autorizar, la fecha de autorización queda en blanco")
    void sinAutorizar() throws IOException {
        var pdf = generar(new RidePdfBox(), factura());

        try (var documento = Loader.loadPDF(pdf)) {
            var texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("FECHA Y HORA DE AUTORIZACIÓN"), "el rótulo va siempre");
            assertTrue(texto.contains("—"), "y el valor queda vacío");
        }
    }

    @Test
    @DisplayName("con fecha de autorización, la imprime")
    void autorizada() throws IOException {
        var cuando = OffsetDateTime.of(2026, 10, 20, 14, 33, 36, 0, ZoneOffset.ofHours(-5));

        var salida = new ByteArrayOutputStream();
        new RidePdfBox().escribir(factura(), cuando, salida);

        try (var documento = Loader.loadPDF(salida.toByteArray())) {
            assertTrue(new PDFTextStripper().getText(documento).contains("20/10/2026 14:33:36"));
        }
    }

    @Test
    @DisplayName("el logo se encaja en su caja sin deformarse")
    void logoSinDeformar() throws IOException {
        var logo = logo("logo-apaisado.jpg");

        var pdf = generar(new RidePdfBox(EstiloRide.builder().logo(logo).build()), factura());

        try (var documento = Loader.loadPDF(pdf)) {
            var recursos = documento.getPage(0).getResources();
            var nombres = recursos.getXObjectNames().iterator();
            assertTrue(nombres.hasNext(), "el PDF debe llevar la imagen del logo");
            var imagen = (PDImageXObject) recursos.getXObject(nombres.next());
            assertAll(
                () -> assertEquals(400, imagen.getWidth(), "las dimensiones declaradas son las reales"),
                () -> assertEquals(100, imagen.getHeight())
            );
        }
    }

    @Test
    @DisplayName("un logo cuadrado también entra, dejando aire a los lados")
    void logoCuadrado() throws IOException {
        var pdf = generar(new RidePdfBox(EstiloRide.builder().logo(logo("logo-cuadrado.jpg")).build()), factura());

        try (var documento = Loader.loadPDF(pdf)) {
            assertEquals(1, documento.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("el RUC del proveedor de facturación sale en la información adicional, Anexo 26")
    void rucDelProveedorDeFacturacion() throws IOException {
        var pdf = generar(new RidePdfBox(), factura());

        try (var documento = Loader.loadPDF(pdf)) {
            var texto = new PDFTextStripper().getText(documento);
            assertAll(
                () -> assertTrue(texto.contains("RUC Proveedor"), "el rótulo que exige el Anexo 26"),
                () -> assertTrue(texto.contains("1719369561001"), "y el RUC"),
                () -> assertTrue(texto.contains("Información Adicional"), "dentro de ese bloque")
            );
        }
    }

    @Test
    @DisplayName("la guía de remisión y las formas de pago se imprimen")
    void guiaYFormasDePago() throws IOException {
        var pdf = generar(new RidePdfBox(), factura());

        try (var documento = Loader.loadPDF(pdf)) {
            var texto = new PDFTextStripper().getText(documento);
            assertAll(
                () -> assertTrue(texto.contains("Guía Remisión"), texto),
                () -> assertTrue(texto.contains("001-002-000000045"), "el número de la guía"),
                () -> assertTrue(texto.contains("Forma de pago"), "la tabla de pagos"),
                () -> assertTrue(texto.contains("SIN UTILIZACION DEL SISTEMA FINANCIERO"), "con su denominación de la Tabla 24")
            );
        }
    }

    @Test
    @DisplayName("el llamador puede decidir cómo se embebe el logo")
    void logoConFabricaPropia() throws IOException {
        var jpg = logo("logo-apaisado.jpg");
        var veces = new java.util.concurrent.atomic.AtomicInteger();

        LogoRide propia = documento -> {
            veces.incrementAndGet();
            return JPEGFactory.createFromStream(documento, new ByteArrayInputStream(jpg));
        };

        var pdf = generar(new RidePdfBox(EstiloRide.builder().logo(propia).build()), factura());

        assertEquals(1, veces.get(), "la fábrica se invoca una vez por documento");
        try (var documento = Loader.loadPDF(pdf)) {
            var recursos = documento.getPage(0).getResources();
            var nombre = recursos.getXObjectNames().iterator().next();
            var imagen = (PDImageXObject) recursos.getXObject(nombre);
            assertEquals(400, imagen.getWidth());
        }
    }

    @Test
    @DisplayName("una imagen de un documento ya cerrado no se cuela: el logo saldría vacío")
    void logoDeOtroDocumento() throws IOException {
        PDImageXObject ajena;
        try (var otro = new PDDocument()) {
            ajena = PDImageXObject.createFromByteArray(otro, logo("logo-apaisado.jpg"), "logo");
        }

        var conAjena = generar(new RidePdfBox(EstiloRide.builder().logo(documento -> ajena).build()), factura());
        var conPropia = generar(new RidePdfBox(EstiloRide.builder().logo(logo("logo-apaisado.jpg")).build()), factura());

        assertTrue(conAjena.length < conPropia.length - 1000,
            "reutilizar una imagen de otro documento no lanza, pero deja el stream vacío: " + conAjena.length + " contra " + conPropia.length + " bytes");
    }


    @Test
    @DisplayName("la guia de remision lleva transportista y destinatario, y no lleva totales")
    void guiaRemision() throws IOException {
        var guia = GuiaRemision.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").build())
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(7))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .direccionPartida("AMAZONAS Y ROCA")
            .transportista(new Transportista(TipoIdentificacion.RUC, "1790012345001", "TRANSPORTES SA"))
            .transporte(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21))
            .placa("ABC1234")
            .destinatario(Destinatario.builder()
                .identificacion("1710034065")
                .razonSocial("JUAN PEREZ")
                .direccion("QUITO")
                .motivoTraslado("Venta")
                .detalle(DetalleGuiaRemision.builder().descripcion("Alimento para perro").cantidad(1).build())
                .build())
            .build();

        var texto = textoDe(generar(new RidePdfBox(), guia));

        assertAll(
            () -> assertTrue(texto.contains("TRANSPORTES SA"), "falta el transportista"),
            () -> assertTrue(texto.contains("ABC1234"), "falta la placa"),
            () -> assertTrue(texto.contains("JUAN PEREZ"), "falta el destinatario"),
            () -> assertTrue(texto.contains("Alimento para perro"), "falta el detalle"),
            () -> assertFalse(texto.contains("VALOR TOTAL"), "la guia de remision no lleva totales")
        );
    }

    @Test
    @DisplayName("la nota de debito lleva la tabla de motivos con su razon y su valor")
    void notaDebito() throws IOException {
        var nota = NotaDebito.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").build())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(9))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .documentoModificado(new DocumentoModificado(TipoComprobante.FACTURA, Serie.of(1, 2), Secuencial.of(130), LocalDate.of(2026, 10, 18)))
            .totalSinImpuestos(Dinero.of("50.00"))
            .iva(TarifaIva.QUINCE)
            .motivo("INTERESES POR MORA", Dinero.of("50.00"))
            .build();

        var texto = textoDe(generar(new RidePdfBox(), nota));

        assertAll(
            () -> assertTrue(texto.contains("INTERESES POR MORA"), "falta el motivo"),
            () -> assertTrue(texto.contains("001-002-000000130"), "falta el comprobante que modifica"),
            () -> assertTrue(texto.contains("VALOR TOTAL"), "la nota de debito si lleva totales")
        );
    }

    @Test
    @DisplayName("el comprobante de retencion abre una fila por retencion, no por documento")
    void comprobanteRetencion() throws IOException {
        var sustento = DocumentoSustento.builder()
            .codigoSustento("01")
            .documento(TipoComprobante.FACTURA, Serie.of(1, 2), Secuencial.of(45), LocalDate.of(2026, 10, 18))
            .totales(Dinero.of("100.00"), Dinero.of("115.00"))
            .impuesto(Impuesto.of(CodigoImpuesto.IVA, TarifaIva.QUINCE.codigo(), TarifaIva.QUINCE.tarifa(), Dinero.of("100.00")))
            .retencion(Retencion.de(CodigoImpuestoRetencion.RENTA, "312", Dinero.of("100.00"), "1.75"))
            .retencion(Retencion.de(CodigoImpuestoRetencion.IVA, "9", Dinero.of("15.00"), "30"))
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("115.00")))
            .build();

        var retencion = ComprobanteRetencion.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").build())
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(11))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .sujetoRetenido(SujetoRetenido.of(TipoIdentificacion.RUC, "1790012345001", "PROVEEDOR DEL PACIFICO S.A."))
            .periodoFiscal(PeriodoFiscal.de(LocalDate.of(2026, 10, 20)))
            .documentoSustento(sustento)
            .build();

        var texto = textoDe(generar(new RidePdfBox(), retencion));

        assertAll(
            () -> assertTrue(texto.contains("PROVEEDOR DEL PACIFICO S.A."), "falta el sujeto retenido"),
            () -> assertEquals(2, veces(texto, "001002000000045"), "las dos retenciones del mismo documento repiten su numero, en los 15 dígitos corridos que exige su esquema"),
            () -> assertTrue(texto.contains("RENTA"), "falta la retencion de renta"),
            () -> assertFalse(texto.contains("VALOR TOTAL"), "el comprobante de retencion no lleva totales")
        );
    }

    @Test
    @DisplayName("la liquidacion de compra lleva al proveedor y la misma tabla que la factura")
    void liquidacionCompra() throws IOException {
        var liquidacion = LiquidacionCompra.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").build())
            .proveedor(Proveedor.of(TipoIdentificacion.RUC, "1790012345001", "AGRICOLA LA ESPERANZA"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(13))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Quintal de maiz")
                .cantidad(1).precioUnitario("100.00").iva(TarifaIva.QUINCE).build())
            .build();

        var texto = textoDe(generar(new RidePdfBox(), liquidacion));

        assertAll(
            () -> assertTrue(texto.contains("AGRICOLA LA ESPERANZA"), "falta el proveedor"),
            () -> assertTrue(texto.contains("Quintal de maiz"), "falta el detalle"),
            () -> assertTrue(texto.contains("VALOR TOTAL"), "la liquidacion si lleva totales")
        );
    }

    /** Cuantas veces aparece un literal en el texto extraido. */
    private static int veces(String texto, String literal) {
        int cuenta = 0;
        for (int i = texto.indexOf(literal); i >= 0; i = texto.indexOf(literal, i + literal.length())) {
            cuenta++;
        }
        return cuenta;
    }

    @Test
    @DisplayName("la nota de crédito lleva el comprobante que modifica y no lleva formas de pago")
    void notaCredito() throws IOException {
        var nota = NotaCredito.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").obligadoContabilidad(true).build())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(47))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .documentoModificado(new DocumentoModificado(TipoComprobante.FACTURA, Serie.of(1, 2), Secuencial.of(130), LocalDate.of(2026, 10, 18)))
            .motivo("Devolución de mercadería")
            .detalle(DetalleNotaCredito.builder()
                .codigoInterno("SKU-001").codigoAdicional("ALM-15")
                .descripcion("Alimento para perro 15kg")
                .cantidad(1).precioUnitario("25.00").iva(TarifaIva.QUINCE).build())
            .build();

        var pdf = generar(new RidePdfBox(), nota);
        Files.write(Path.of("target", "ride-nota-credito.pdf"), pdf);
        var texto = textoDe(pdf);

        assertAll(
            () -> assertTrue(texto.contains("NOTA DE CRÉDITO"), "se rotula con su nombre"),
            () -> assertTrue(texto.contains("Comprobante que se modifica"), "y dice cuál modifica"),
            () -> assertTrue(texto.contains("001-002-000000130")),
            () -> assertTrue(texto.contains("18/10/2026"), "con la fecha de aquel, no la suya"),
            () -> assertTrue(texto.contains("Razón de Modificación")),
            () -> assertTrue(texto.contains("Devolución de mercadería")),
            () -> assertTrue(texto.contains("SKU-001"), "los códigos de la nota salen en las columnas de código"),
            () -> assertTrue(texto.contains("ALM-15")),
            () -> assertTrue(texto.contains("VALOR TOTAL"), "cierra con el valor de la modificación"),
            () -> assertFalse(texto.contains("Forma de pago"), "y no lleva formas de pago"),
            () -> assertEquals(1, paginasDe(pdf))
        );
    }

    @Test
    @DisplayName("la paleta, la familia y el pie cambian el documento sin tocar el formato")
    void conEstiloDeMarca() throws IOException {
        var marca = EstiloRide.builder()
            .logo(logo("logo-apaisado.jpg"))
            .paleta(Paleta.deMarca(0x1A3D7C))
            .familia(FamiliaTipografica.TIMES)
            .radioBordes(6)
            .pie(PieRide.de("Emitido con comprobantes-sri · soporte@peluware.ec", documento -> PDImageXObject.createFromByteArray(documento, logo("logo-apaisado.jpg"), "pie")).centrado())
            .build();

        var pdf = generar(new RidePdfBox(marca), factura());
        Files.write(Path.of("target", "ride-factura-marca.pdf"), pdf);

        try (var documento = Loader.loadPDF(pdf)) {
            var texto = new PDFTextStripper().getText(documento);
            assertAll(
                () -> assertEquals(1, documento.getNumberOfPages(), "el estilo no añade páginas"),
                () -> assertTrue(texto.contains("FACTURA"), "ni quita los requisitos"),
                () -> assertTrue(texto.contains("VALOR TOTAL")),
                () -> assertTrue(texto.contains("Emitido con comprobantes-sri"), "y el pie sale"),
                () -> assertEquals(2, documento.getPage(0).getResources().getXObjectNames().spliterator().estimateSize(), "dos imágenes: el logo del emisor y el del pie")
            );
        }
    }

    @Test
    @DisplayName("la escala agranda la letra sin añadir páginas ni perder requisitos")
    void escala() throws IOException {
        var grande = EstiloRide.builder().escala(1.3f).build();

        var pdf = generar(new RidePdfBox(grande), factura());
        Files.write(Path.of("target", "ride-factura-escala.pdf"), pdf);

        try (var documento = Loader.loadPDF(pdf)) {
            var texto = new PDFTextStripper().getText(documento);
            assertAll(
                () -> assertEquals(1, documento.getNumberOfPages()),
                () -> assertTrue(texto.contains("FACTURA")),
                () -> assertTrue(texto.contains("VALOR TOTAL")),
                () -> assertTrue(texto.contains("68.50"))
            );
        }
    }

    @Test
    @DisplayName("una escala fuera de rango se rechaza al construir el estilo")
    void escalaFueraDeRango() {
        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> EstiloRide.builder().escala(0.5f)),
            () -> assertThrows(IllegalArgumentException.class, () -> EstiloRide.builder().escala(2f))
        );
    }

    @Test
    @DisplayName("el pie se puede centrar")
    void pieCentrado() {
        var izquierda = new RidePdfBox(EstiloRide.builder().pie(PieRide.de("Emitido con comprobantes-sri")).build());
        var centro = new RidePdfBox(EstiloRide.builder().pie(PieRide.de("Emitido con comprobantes-sri").centrado()).build());

        assertAll(
            () -> assertTrue(posicionDelPie(izquierda) < posicionDelPie(centro), "centrado empieza más a la derecha"),
            () -> assertEquals(1, paginasDe(generar(centro, factura())))
        );
    }

    /** Coordenada x donde empieza el texto del pie, leída del contenido de la página. */
    private static float posicionDelPie(RidePdfBox generador) throws IOException {
        try (var documento = Loader.loadPDF(generar(generador, factura()))) {
            var buscador = new PDFTextStripper() {
                float x = -1;

                @Override
                protected void writeString(String texto, java.util.List<org.apache.pdfbox.text.TextPosition> posiciones) {
                    if (x < 0 && texto.contains("Emitido con")) {
                        x = posiciones.getFirst().getXDirAdj();
                    }
                }
            };
            buscador.getText(documento);
            return buscador.x;
        }
    }

    @Test
    @DisplayName("una tipografía propia se incrusta y engorda el documento")
    void tipografiaPropia() throws IOException {
        FuenteRide liberation = documento -> {
            try (var ttf = PDDocument.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf")) {
                assertNotNull(ttf, "PDFBox debería traer su fuente de respaldo");
                return PDType0Font.load(documento, ttf);
            }
        };

        var conPropia = generar(new RidePdfBox(EstiloRide.builder().tipografia(Tipografia.de(liberation, liberation)).build()), factura());
        var conBase14 = generar(new RidePdfBox(EstiloRide.porDefecto()), factura());
        Files.write(Path.of("target", "ride-factura-fuente-propia.pdf"), conPropia);

        try (var documento = Loader.loadPDF(conPropia)) {
            var texto = new PDFTextStripper().getText(documento);
            assertAll(
                () -> assertTrue(texto.contains("FACTURA"), "el documento se lee igual"),
                () -> assertTrue(texto.contains("68.50")),
                () -> assertTrue(conPropia.length > conBase14.length, "la fuente incrustada pesa: " + conPropia.length + " contra " + conBase14.length + " bytes")
            );
        }
    }

    @Test
    @DisplayName("la tipografía estándar no incrusta nada")
    void tipografiaEstandar() {
        var estilo = EstiloRide.builder().familia(FamiliaTipografica.TIMES).build();

        assertEquals(new Tipografia.Estandar(FamiliaTipografica.TIMES), estilo.tipografia());
    }

    @Test
    @DisplayName("el PDF generado queda en target para poder mirarlo")
    void dejarloEnTarget() throws IOException {
        var pdf = generar(new RidePdfBox(EstiloRide.builder().logo(logo("logo-apaisado.jpg")).build()), factura());

        var destino = Path.of("target", "ride-factura.pdf");
        Files.createDirectories(destino.getParent());
        Files.write(destino, pdf);

        assertTrue(Files.size(destino) > 1000, "un RIDE con logo no puede pesar casi nada");
    }

    private static byte[] generar(RidePdfBox generador, Comprobante comprobante) {
        var salida = new ByteArrayOutputStream();
        generador.escribir(comprobante, salida);
        return salida.toByteArray();
    }

    /**
     * Logo de prueba, como recurso: generarlo con AWT obligaría al módulo a declarar
     * { java.desktop}, que el código de producción no usa.
     */
    private static byte[] logo(String nombre) {
        try (var entrada = RidePdfBoxTest.class.getResourceAsStream("/" + nombre)) {
            assertNotNull(entrada, "falta el recurso " + nombre);
            return entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("una descripción larga se parte en renglones; ni ella ni los rótulos se abrevian")
    void textoLargoSeParte() throws IOException {
        var larga = "Alimento balanceado premium para perro adulto de raza grande, saco de quince kilos, sabor cordero y arroz";

        var pdf = generar(new RidePdfBox(), factura(larga));

        try (var documento = Loader.loadPDF(pdf)) {
            var texto = new PDFTextStripper().getText(documento);
            assertAll(
                () -> assertFalse(texto.contains("…"), "nada se abrevia, ni la descripción ni los rótulos de la tabla"),
                () -> assertTrue(texto.contains("arroz"), "la última palabra de la descripción llega al papel"),
                () -> assertTrue(texto.contains("Principal"), "y el rótulo sale entero, partido en dos renglones"),
                () -> assertEquals(1, documento.getNumberOfPages())
            );
        }
    }

    @Test
    @DisplayName("el código de barras dibujado es el de la clave, con módulos que un lector lee")
    void codigoBarras() throws IOException {
        var pdf = generar(new RidePdfBox(EstiloRide.builder().codigoBarras(true).build()), factura());
        Files.write(Path.of("target", "ride-factura-barras.pdf"), pdf);

        try (var documento = Loader.loadPDF(pdf)) {
            var clave = claveImpresa(new PDFTextStripper().getText(documento));
            var barras = barrasDe(documento);
            float modulo = barras.stream().map(rectangulo -> rectangulo[2]).min(Float::compare).orElseThrow();

            assertAll(
                () -> assertArrayEquals(sinZonasMudas(Code128.modulos(clave)), modulosDibujados(barras, modulo), "lo dibujado es el símbolo que produce el codificador"),
                () -> assertTrue(modulo >= 0.5f, "el módulo mide " + modulo + " pt; por debajo de medio punto un lector no lo coge"),
                () -> assertEquals(1, documento.getNumberOfPages(), "el código de barras no añade páginas")
            );
        }
    }

    /** La clave de acceso, cuarenta y nueve dígitos seguidos, tal como sale impresa. */
    private static String claveImpresa(String texto) {
        var buscador = java.util.regex.Pattern.compile("[0-9]{49}").matcher(texto);
        assertTrue(buscador.find(), "el RIDE debe imprimir la clave de acceso");
        return buscador.group();
    }

    /**
     * Las barras del código: el grupo más numeroso de rectángulos que comparten base y altura.
     */
    private static List<float[]> barrasDe(PDDocument documento) throws IOException {
        var rectangulos = new ArrayList<float[]>();
        var motor = new PDFStreamEngine() {
            @Override
            protected void processOperator(Operator operador, List<COSBase> operandos) throws IOException {
                if (operador.getName().equals("re") && operandos.size() == 4) {
                    var valores = new float[4];
                    for (int i = 0; i < 4; i++) {
                        valores[i] = ((COSNumber) operandos.get(i)).floatValue();
                    }
                    rectangulos.add(valores);
                }
                super.processOperator(operador, operandos);
            }
        };
        motor.processPage(documento.getPage(0));

        var porFila = new java.util.HashMap<String, List<float[]>>();
        for (var rectangulo : rectangulos) {
            porFila.computeIfAbsent(rectangulo[1] + ":" + rectangulo[3], fila -> new ArrayList<>()).add(rectangulo);
        }
        return porFila.values().stream().max(java.util.Comparator.comparingInt(List::size)).orElseThrow();
    }

    /** Reconstruye el patrón de módulos a partir de los rectángulos pintados. */
    private static boolean[] modulosDibujados(List<float[]> barras, float modulo) {
        float inicio = barras.stream().map(rectangulo -> rectangulo[0]).min(Float::compare).orElseThrow();
        float fin = barras.stream().map(rectangulo -> rectangulo[0] + rectangulo[2]).max(Float::compare).orElseThrow();

        var modulos = new boolean[Math.round((fin - inicio) / modulo)];
        for (var barra : barras) {
            int desde = Math.round((barra[0] - inicio) / modulo);
            int hasta = Math.round((barra[0] + barra[2] - inicio) / modulo);
            for (int i = desde; i < hasta; i++) {
                modulos[i] = true;
            }
        }
        return modulos;
    }

    /** El símbolo sin las zonas mudas, que son blanco y no se dibujan. */
    private static boolean[] sinZonasMudas(boolean[] modulos) {
        return java.util.Arrays.copyOfRange(modulos, Code128.ZONA_MUDA, modulos.length - Code128.ZONA_MUDA);
    }

    @Test
    @DisplayName("el interlineado baja el documento sin tocar el tamaño de la letra")
    void interlineado() {
        var apretado = new RidePdfBox(EstiloRide.builder().interlineado(0.85f).build());
        var aireado = new RidePdfBox(EstiloRide.builder().interlineado(1.5f).build());

        assertAll(
            () -> assertTrue(alturaDelTotal(aireado) > alturaDelTotal(apretado), "con más aire el total cae más abajo"),
            () -> assertEquals(tamanoDelTotal(apretado), tamanoDelTotal(aireado), 0.01f, "y la letra mide lo mismo"),
            () -> assertEquals(1, paginasDe(generar(aireado, factura())))
        );
    }

    /** Coordenada y del rótulo VALOR TOTAL, con el origen arriba. */
    private static float alturaDelTotal(RidePdfBox generador) throws IOException {
        return totalDe(generador).getYDirAdj();
    }

    /** Tamaño con el que se escribió ese rótulo. */
    private static float tamanoDelTotal(RidePdfBox generador) throws IOException {
        return totalDe(generador).getFontSizeInPt();
    }

    private static TextPosition totalDe(RidePdfBox generador) throws IOException {
        try (var documento = Loader.loadPDF(generar(generador, factura()))) {
            return buscar(documento, "VALOR TOTAL");
        }
    }

    /** La primera letra de la línea que contiene {@code texto}. */
    private static TextPosition buscar(PDDocument documento, String texto) throws IOException {
        var buscador = new PDFTextStripper() {
            @Nullable TextPosition encontrada;

            @Override
            protected void writeString(String linea, List<TextPosition> posiciones) {
                if (encontrada == null && linea.contains(texto)) {
                    encontrada = posiciones.getFirst();
                }
            }
        };
        buscador.getText(documento);
        return Objects.requireNonNull(buscador.encontrada, "el RIDE debe llevar «" + texto + "»");
    }

    @Test
    @DisplayName("el valor total se escribe más grande que los demás totales")
    void totalDestacado() throws IOException {
        try (var documento = Loader.loadPDF(generar(new RidePdfBox(), factura()))) {
            assertAll(
                () -> assertTrue(buscar(documento, "VALOR TOTAL").getFontSizeInPt() > buscar(documento, "SUBTOTAL SIN IMPUESTOS").getFontSizeInPt(), "el importe a pagar es lo que primero se busca"),
                () -> assertNull(Paleta.porDefecto().fondoTotal(), "sin color de marca va sin relleno"),
                () -> assertNotNull(Paleta.deMarca(0xC0192B).fondoTotal(), "con color de marca, sobre su propio fondo")
            );
        }
    }

    @Test
    @DisplayName("en forma de frase los rótulos dejan de gritar, pero las siglas se respetan")
    void rotulosEnFrase() throws IOException {
        var comoElAnexo = textoDe(generar(new RidePdfBox(), factura()));
        var enFrase = textoDe(generar(new RidePdfBox(EstiloRide.builder().rotulos(Rotulos.FRASE).build()), factura()));

        assertAll(
            () -> assertTrue(comoElAnexo.contains("NÚMERO DE AUTORIZACIÓN"), "por omisión, como el Anexo 2"),
            () -> assertTrue(enFrase.contains("Número de autorización"), "y en frase, con la inicial sola"),
            () -> assertFalse(enFrase.contains("NÚMERO DE AUTORIZACIÓN")),
            () -> assertTrue(enFrase.contains("Obligado a llevar contabilidad"), "incluso el que más grita"),
            () -> assertTrue(enFrase.contains("IVA 15%"), "una sigla no se baja a minúscula"),
            () -> assertFalse(enFrase.contains("Iva 15%")),
            () -> assertTrue(enFrase.contains("Valor total"), "y el total también cambia"),
            () -> assertTrue(enFrase.contains("PRUEBAS"), "los valores no se tocan, solo los rótulos")
        );
    }

    @Test
    @DisplayName("el aire entre bloques baja el documento sin tocar el interlineado")
    void aireEntreBloques() {
        var junto = new RidePdfBox(EstiloRide.builder().aireEntreBloques(0.5f).build());
        var suelto = new RidePdfBox(EstiloRide.builder().aireEntreBloques(3f).build());

        assertAll(
            () -> assertTrue(alturaDelTotal(suelto) > alturaDelTotal(junto), "con más aire el total cae más abajo"),
            () -> assertEquals(tamanoDelTotal(junto), tamanoDelTotal(suelto), 0.01f, "y la letra mide lo mismo")
        );
    }

    @Test
    @DisplayName("el marco de página se traza en todas las hojas")
    void marcoDePagina() {
        var desnudo = generar(new RidePdfBox(), facturaDe(60));
        var conFilete = generar(new RidePdfBox(EstiloRide.builder().marcoPagina(MarcoPagina.FILETE).build()), facturaDe(60));
        var conDoble = generar(new RidePdfBox(EstiloRide.builder().marcoPagina(MarcoPagina.DOBLE).build()), facturaDe(60));

        assertAll(
            () -> assertEquals(trazos(desnudo) + 1, trazos(conFilete), "un filete es un trazo más en la primera hoja"),
            () -> assertEquals(trazos(desnudo) + 2, trazos(conDoble), "y el doble, dos"),
            () -> assertEquals(trazos(desnudo, 1) + 2, trazos(conDoble, 1), "también en la segunda")
        );
    }

    @Test
    @DisplayName("el contorno y las divisiones deciden cuántas líneas se trazan")
    void contornoYDivisiones() throws IOException {
        int rejilla = trazos(generar(new RidePdfBox(), factura()));
        int soloCabecera = trazos(generar(new RidePdfBox(EstiloRide.builder().contorno(Contorno.REGLA).divisiones(Divisiones.CABECERA).build()), factura()));
        int ninguna = trazos(generar(new RidePdfBox(EstiloRide.builder().contorno(Contorno.NINGUNO).divisiones(Divisiones.NINGUNA).build()), factura()));

        assertAll(
            () -> assertTrue(rejilla > soloCabecera, "la rejilla traza más líneas que solo las reglas de cabecera"),
            () -> assertTrue(soloCabecera > 0, "con reglas se traza algo"),
            () -> assertEquals(0, ninguna, "sin contorno ni divisiones no queda ni una línea")
        );
    }

    /**
     * Cuenta los trazos de la primera página: cada {@code S} del flujo de contenido.
     */
    private static int trazos(byte[] pdf) throws IOException {
        return trazos(pdf, 0);
    }

    /** Cuenta los trazos de una página: cada {@code S} de su flujo de contenido. */
    private static int trazos(byte[] pdf, int pagina) throws IOException {
        try (var documento = Loader.loadPDF(pdf)) {
            var contador = new int[1];
            var motor = new PDFStreamEngine() {
                @Override
                protected void processOperator(Operator operador, List<COSBase> operandos) throws IOException {
                    if (operador.getName().equalsIgnoreCase("S")) {
                        contador[0]++;
                    }
                    super.processOperator(operador, operandos);
                }
            };
            motor.processPage(documento.getPage(pagina));
            return contador[0];
        }
    }

    private static Factura factura() {
        return factura("Alimento para perro 15kg");
    }

    @Test
    @DisplayName("las columnas y el plazo solo salen cuando hay algo que poner en ellos")
    void soloLoQueSeLlena() throws IOException {
        var pelado = textoDe(generar(new RidePdfBox(), factura()));

        var conTodo = Factura.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").obligadoContabilidad(true).build())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .detalle(DetalleComprobanteVenta.builder().codigoPrincipal("SKU-001").codigoAuxiliar("ALM-15")
                .descripcion("Alimento").unidadMedida("SACO").cantidad(2).precioUnitario("25.00")
                .detalleAdicional("Lote", "A-77").iva(TarifaIva.QUINCE).build())
            .pago(new Pago(FormaPago.TARJETA_CREDITO, Dinero.of("57.50"), Plazo.enDias(30)))
            .build();
        var lleno = textoDe(generar(new RidePdfBox(), conTodo));

        assertAll(
            () -> assertFalse(pelado.contains("Auxiliar"), "sin códigos auxiliares no se dibuja su columna"),
            () -> assertFalse(pelado.contains("Detalle Adicional"), "ni la de detalles adicionales"),
            () -> assertFalse(pelado.contains("Plazo"), "ni la columna de plazo si ningún pago lo trae"),
            () -> assertTrue(lleno.contains("Auxiliar"), "y cuando los hay, sí"),
            () -> assertTrue(lleno.contains("Detalle Adicional")),
            () -> assertTrue(lleno.contains("Plazo")),
            () -> assertTrue(lleno.contains("30 dias"), "el plazo sale sin ceros de relleno"),
            () -> assertTrue(lleno.contains("2 SACO"), "y la unidad de medida acompaña a la cantidad")
        );
    }

    @Test
    @DisplayName("una factura larga se reparte en páginas y no pierde nada por el borde")
    void paginacion() throws IOException {
        var pdf = generar(new RidePdfBox(EstiloRide.builder().codigoBarras(true).build()), facturaDe(60));
        Files.write(Path.of("target", "ride-factura-paginada.pdf"), pdf);

        try (var documento = Loader.loadPDF(pdf)) {
            int paginas = documento.getNumberOfPages();
            var ultima = textoDe(documento, paginas - 1);

            assertAll(
                () -> assertTrue(paginas > 1, "sesenta líneas no caben en una página"),
                () -> assertTrue(ultima.contains("VALOR TOTAL"), "los totales van en la última"),
                () -> assertTrue(ultima.contains("Forma de pago"), "y las formas de pago también"),
                () -> assertTrue(ultima.contains("SKU-0159"), "hasta la última línea del detalle"),
                () -> assertFalse(textoDe(documento, 0).contains("VALOR TOTAL"), "y no antes"),
                () -> assertTrue(textoDe(documento, 0).contains("Razón Social"), "el receptor solo en la primera"),
                () -> assertFalse(textoDe(documento, 1).contains("Razón Social")),
                () -> assertEquals(paginas, paginasCon(documento, "Principal"), "la cabecera de la tabla se repite en todas"),
                () -> assertEquals(paginas, paginasCon(documento, claveDe(factura())), "y la clave de acceso también"),
                () -> assertEquals(1, paginasCon(documento, "OBLIGADO A LLEVAR CONTABILIDAD"), "la cabecera entera no se repite"),
                () -> assertTrue(textoDe(documento, 1).contains("FACTURA No."), "las demás llevan una franja con el número"),
                () -> assertTrue(textoDe(documento, 0).contains("1 de " + paginas), "cada hoja se numera"),
                () -> assertTrue(ultima.contains(paginas + " de " + paginas))
            );
        }
    }

    @Test
    @DisplayName("una imagen que sale en varias páginas se crea una sola vez")
    void logoUnicoEntrePaginas() throws IOException {
        var conPie = EstiloRide.builder()
            .pie(PieRide.de("comprobantes-sri", documento -> PDImageXObject.createFromByteArray(documento, logo("logo-apaisado.jpg"), "pie")))
            .build();

        try (var documento = Loader.loadPDF(generar(new RidePdfBox(conPie), facturaDe(60)))) {
            assertTrue(documento.getNumberOfPages() > 1, "la prueba necesita varias páginas");
            assertEquals(imagenDe(documento, 0), imagenDe(documento, 1), "las dos páginas apuntan a la misma imagen");
        }
    }

    private static COSBase imagenDe(PDDocument documento, int pagina) throws IOException {
        var recursos = documento.getPage(pagina).getResources();
        return recursos.getXObject(recursos.getXObjectNames().iterator().next()).getCOSObject();
    }

    @Test
    @DisplayName("una sola página no lleva numeración")
    void sinNumerarCuandoCabeEnUna() throws IOException {
        try (var documento = Loader.loadPDF(generar(new RidePdfBox(), factura()))) {
            assertAll(
                () -> assertEquals(1, documento.getNumberOfPages()),
                () -> assertFalse(textoDe(documento, 0).contains("1 de 1"))
            );
        }
    }

    /** Páginas del PDF, cerrando el documento. */
    private static int paginasDe(byte[] pdf) throws IOException {
        try (var documento = Loader.loadPDF(pdf)) {
            return documento.getNumberOfPages();
        }
    }

    /** Todo el texto del PDF, cerrando el documento. */
    private static String textoDe(byte[] pdf) throws IOException {
        try (var documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    private static String textoDe(PDDocument documento, int pagina) throws IOException {
        var extractor = new PDFTextStripper();
        extractor.setStartPage(pagina + 1);
        extractor.setEndPage(pagina + 1);
        return extractor.getText(documento);
    }

    private static int paginasCon(PDDocument documento, String texto) throws IOException {
        int cuantas = 0;
        for (int i = 0; i < documento.getNumberOfPages(); i++) {
            if (textoDe(documento, i).contains(texto)) {
                cuantas++;
            }
        }
        return cuantas;
    }

    private static String claveDe(Factura factura) {
        return factura.claveAcceso();
    }

    /** La factura de siempre, con tantas líneas de detalle como se pidan. */
    private static Factura facturaDe(int lineas) {
        var builder = Factura.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").obligadoContabilidad(true).build())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2, "SUCURSAL NORTE"))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .campoAdicional("Teléfono", "02 2345678")
            .rucProveedorSistema(new Ruc("1719369561001"));

        for (int i = 0; i < lineas; i++) {
            builder.detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal(String.format("SKU-%04d", 100 + i))
                .descripcion("Alimento balanceado para perro adulto de raza grande, saco de 15 kg")
                .cantidad(1)
                .precioUnitario("10.00")
                .iva(TarifaIva.QUINCE)
                .build());
        }
        return builder.pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, builder.build().info().totales().importeTotal())).build();
    }

    @Test
    @DisplayName("un valor que no cabe a lo ancho envuelve en vez de recortarse")
    void valorLargoEnvuelve() throws IOException {
        var razonSocial = "COMERCIALIZADORA Y DISTRIBUIDORA DE PRODUCTOS AGRICOLAS DEL LITORAL ECUATORIANO";
        var observacion = "Entrega coordinada con bodega central de Guayaquil para el turno de la tarde del jueves";
        var direccionMatriz = "AV. AMAZONAS N34-451 Y AV. ATAHUALPA, EDIFICIO TORRES DEL NORTE, PISO 7, OFICINA 704";

        var factura = Factura.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", direccionMatriz).build())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", razonSocial))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Alimento para perro")
                .cantidad(1)
                .precioUnitario("25.00")
                .iva(TarifaIva.QUINCE)
                .build())
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("28.75")))
            .campoAdicional("Observación", observacion)
            .build();

        var texto = textoDe(generar(new RidePdfBox(), factura));

        assertAll(
            () -> assertFalse(texto.contains("\u2026"), "nada debería quedar recortado"),
            () -> assertTrue(texto.contains("ECUATORIANO"), "la última palabra de la razón social tiene que salir"),
            () -> assertTrue(texto.contains("jueves"), "la última palabra del campo adicional tiene que salir"),
            () -> assertTrue(texto.contains("704"), "la última palabra de la dirección del emisor tiene que salir")
        );
    }

    private static Factura factura(String descripcion) {
        return Factura.builder()
            .emisor(Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA").obligadoContabilidad(true).build())
            .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
            .fechaEmision(LocalDate.of(2026, 10, 20))
            .ambiente(Ambiente.PRUEBAS)
            .establecimiento(Establecimiento.of(1, 2, "SUCURSAL NORTE"))
            .secuencial(Secuencial.of(130))
            .codigoNumerico(EstrategiaCodigoNumerico.hmac(SECRETO))
            .detalle(DetalleComprobanteVenta.builder()
                .codigoPrincipal("SKU-001")
                .descripcion(descripcion)
                .cantidad(2)
                .precioUnitario("25.00")
                .descuento(Dinero.of("5.00"))
                .iva(TarifaIva.QUINCE)
                .build())
            .detalle(DetalleComprobanteVenta.builder()
                .descripcion("Consulta veterinaria")
                .cantidad(1)
                .precioUnitario("16.75")
                .iva(TarifaIva.CERO)
                .build())
            .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("68.50")))
            .guiaRemision("001-002-000000045")
            .campoAdicional("Teléfono", "02 2345678")
            .campoAdicional("Email", "ventas@peluware.ec")
            .rucProveedorSistema(new Ruc("1719369561001"))
            .build();
    }
}
