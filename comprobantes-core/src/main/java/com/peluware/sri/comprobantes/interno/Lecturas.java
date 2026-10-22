package com.peluware.sri.comprobantes.interno;

import com.peluware.sri.comprobantes.interno.xml.Dom;
import com.peluware.sri.comprobantes.CampoAdicional;
import com.peluware.sri.comprobantes.ClaveAcceso;
import com.peluware.sri.comprobantes.Comprobante;
import com.peluware.sri.comprobantes.DetalleAdicional;
import com.peluware.sri.comprobantes.DocumentoModificado;
import com.peluware.sri.comprobantes.Emisor;
import com.peluware.sri.comprobantes.Establecimiento;
import com.peluware.sri.comprobantes.EstrategiaCodigoNumerico;
import com.peluware.sri.comprobantes.Impuesto;
import com.peluware.sri.comprobantes.LecturaXmlException;
import com.peluware.sri.comprobantes.MaquinaFiscal;
import com.peluware.sri.comprobantes.Pago;
import com.peluware.sri.comprobantes.Plazo;
import com.peluware.sri.comprobantes.catalogo.Ambiente;
import com.peluware.sri.comprobantes.TotalImpuesto;
import com.peluware.sri.comprobantes.catalogo.CodigoImpuesto;
import com.peluware.sri.comprobantes.catalogo.FormaPago;
import com.peluware.sri.comprobantes.catalogo.TipoComprobante;
import com.peluware.sri.comprobantes.catalogo.TipoEmision;
import com.peluware.sri.comprobantes.valores.CodigoNumerico;
import com.peluware.sri.comprobantes.valores.ContribuyenteEspecial;
import com.peluware.sri.comprobantes.valores.Dinero;
import com.peluware.sri.comprobantes.valores.Ruc;
import com.peluware.sri.comprobantes.valores.Secuencial;
import com.peluware.sri.comprobantes.valores.Serie;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.peluware.sri.comprobantes.interno.Bloques.FECHA;
import static com.peluware.sri.comprobantes.interno.Bloques.SI;
import static com.peluware.sri.comprobantes.interno.Lecturas.atributoExigido;
import static com.peluware.sri.comprobantes.interno.xml.Dom.childrenIn;
import static com.peluware.sri.comprobantes.interno.xml.Dom.child;
import static com.peluware.sri.comprobantes.interno.Lecturas.hijoExigido;
import static com.peluware.sri.comprobantes.interno.xml.Dom.text;
import static com.peluware.sri.comprobantes.interno.Lecturas.textoExigido;

/**
 * Lee del XML los bloques que los seis comprobantes comparten.
 *
 * <p>Es el espejo de {@link Bloques}: lo que allí se escribe, aquí se recupera.
 *
 * <p>Los importes se toman tal como vienen. El archivo manda.
 */
public final class Lecturas {

    private Lecturas() {
    }

    /**
     * Rellena en el builder lo que declara {@code <infoTributaria>}, más lo que el bloque
     * específico del comprobante añade sobre el emisor y el establecimiento.
     *
     * <p>El código numérico se saca de la clave de acceso, de modo que el builder la vuelve a
     * generar idéntica a la del archivo.
     */
    public static <B extends Comprobante.Builder<B>> B identidad(B builder, Element raiz, Element info) {
        var tributaria = hijoExigido(raiz, "infoTributaria");
        var clave = textoExigido(tributaria, "claveAcceso");

        if (clave.length() != ClaveAcceso.LONGITUD) {
            throw new LecturaXmlException("La clave de acceso tiene " + clave.length() + " dígitos y el esquema exige " + ClaveAcceso.LONGITUD);
        }

        return builder
            .fechaEmision(fecha(ClaveAcceso.fecha(clave), ClaveAcceso.CAMPO, ClaveAcceso.FECHA))
            .emisor(emisor(tributaria, info))
            .ambiente(Ambiente.of(textoExigido(tributaria, "ambiente")))
            .tipoEmision(TipoEmision.of(textoExigido(tributaria, "tipoEmision")))
            .establecimiento(Establecimiento.of(
                entero(textoExigido(tributaria, Serie.CAMPO_ESTABLECIMIENTO), Serie.CAMPO_ESTABLECIMIENTO),
                entero(textoExigido(tributaria, Serie.CAMPO_PUNTO_EMISION), Serie.CAMPO_PUNTO_EMISION),
                text(info, Establecimiento.CAMPO_DIRECCION)))
            .secuencial(Secuencial.of(entero(textoExigido(tributaria, "secuencial"), "secuencial")))
            .codigoNumerico(EstrategiaCodigoNumerico.fixed(new CodigoNumerico(ClaveAcceso.codigoNumerico(clave))));
    }

    /**
     * El emisor, que se arma con datos de {@code <infoTributaria>} y del bloque específico del
     * comprobante.
     */
    public static Emisor emisor(Element tributaria, Element info) {
        var especial = text(info, ContribuyenteEspecial.CAMPO);

        return Emisor.builder(
                new Ruc(textoExigido(tributaria, "ruc")),
                textoExigido(tributaria, "razonSocial"),
                textoExigido(tributaria, Emisor.CAMPO_DIRECCION_MATRIZ))
            .nombreComercial(text(tributaria, "nombreComercial"))
            .agenteRetencion(text(tributaria, "agenteRetencion"))
            .contribuyenteRimpe(child(tributaria, "contribuyenteRimpe") != null)
            .contribuyenteEspecial(especial == null ? null : new ContribuyenteEspecial(especial))
            .obligadoContabilidad(SI.equals(text(info, "obligadoContabilidad")))
            .build();
    }

    /**
     * Las formas de pago que lleva el bloque {@code <pagos>}; vacío si no viene.
     */
    public static List<Pago> pagos(Element info) {
        var pagos = new ArrayList<Pago>();
        for (var pago : childrenIn(info, "pagos", "pago")) {
            var plazo = text(pago, "plazo");
            var total = Dinero.of(textoExigido(pago, "total"));
            var formaPago = FormaPago.of(textoExigido(pago, "formaPago"));

            pagos.add(plazo == null
                ? new Pago(formaPago, total, null)
                : new Pago(formaPago, total, new Plazo(new BigDecimal(plazo), textoExigido(pago, "unidadTiempo"))));
        }
        return List.copyOf(pagos);
    }

    /**
     * El primer hijo directo con ese nombre.
     *
     * @throws LecturaXmlException si el esquema lo exige y no viene
     */
    public static Element hijoExigido(Element padre, String nombre) {
        var hijo = Dom.child(padre, nombre);
        if (hijo == null) {
            throw new LecturaXmlException("Falta el elemento <" + nombre + "> dentro de <" + Dom.name(padre) + ">");
        }
        return hijo;
    }

    /**
     * Texto del primer hijo directo con ese nombre.
     *
     * @throws LecturaXmlException si el esquema lo exige y no viene
     */
    public static String textoExigido(Element padre, String nombre) {
        return Dom.text(hijoExigido(padre, nombre));
    }

    /**
     * Valor de un atributo.
     *
     * @throws LecturaXmlException si el esquema lo exige y no viene
     */
    public static String atributoExigido(Element elemento, String nombre) {
        var valor = Dom.attribute(elemento, nombre);
        if (valor == null) {
            throw new LecturaXmlException("Falta el atributo " + nombre + " en <" + Dom.name(elemento) + ">");
        }
        return valor;
    }

    /**
     * El bloque {@code <totalConImpuestos>}, que la factura, la liquidación de compra y la nota
     * de crédito declaran igual. La tarifa es opcional: la nota de crédito no la trae.
     */
    public static List<TotalImpuesto> totalConImpuestos(Element info) {
        var impuestos = new ArrayList<TotalImpuesto>();
        for (var total : childrenIn(info, "totalConImpuestos", "totalImpuesto")) {
            var tarifa = text(total, "tarifa");
            impuestos.add(new TotalImpuesto(
                CodigoImpuesto.of(textoExigido(total, "codigo")),
                textoExigido(total, "codigoPorcentaje"),
                tarifa == null ? null : new BigDecimal(tarifa),
                Dinero.of(textoExigido(total, "baseImponible")),
                Dinero.of(textoExigido(total, "valor"))
            ));
        }
        return List.copyOf(impuestos);
    }

    /**
     * Los impuestos que lleva el bloque {@code <impuestos>}, con su valor tal como viene.
     */
    public static List<Impuesto> impuestos(Element contenedor) {
        var impuestos = new ArrayList<Impuesto>();
        for (var impuesto : childrenIn(contenedor, "impuestos", "impuesto")) {
            impuestos.add(new Impuesto(
                CodigoImpuesto.of(textoExigido(impuesto, "codigo")),
                textoExigido(impuesto, "codigoPorcentaje"),
                new BigDecimal(textoExigido(impuesto, "tarifa")),
                Dinero.of(textoExigido(impuesto, "baseImponible")),
                Dinero.of(textoExigido(impuesto, "valor"))));
        }
        return List.copyOf(impuestos);
    }

    /**
     * Los datos adicionales de una línea; vacío si no vienen.
     */
    public static List<DetalleAdicional> detallesAdicionales(Element detalle) {
        var adicionales = new ArrayList<DetalleAdicional>();
        for (var adicional : childrenIn(detalle, "detallesAdicionales", "detAdicional")) {
            adicionales.add(new DetalleAdicional(atributoExigido(adicional, "nombre"), atributoExigido(adicional, "valor")));
        }
        return List.copyOf(adicionales);
    }

    /**
     * Los campos del bloque {@code <infoAdicional>}; vacío si no viene.
     */
    public static List<CampoAdicional> infoAdicional(Element raiz) {
        var campos = new ArrayList<CampoAdicional>();
        for (var campo : childrenIn(raiz, "infoAdicional", "campoAdicional")) {
            campos.add(new CampoAdicional(atributoExigido(campo, "nombre"), Dom.text(campo)));
        }
        return List.copyOf(campos);
    }

    /**
     * La máquina fiscal, o {@code null} si el comprobante no la lleva.
     */
    public static @Nullable MaquinaFiscal maquinaFiscal(Element raiz) {
        var maquina = child(raiz, "maquinaFiscal");
        return maquina == null ? null : new MaquinaFiscal(
            textoExigido(maquina, "marca"),
            textoExigido(maquina, "modelo"),
            textoExigido(maquina, "serie")
        );
    }

    /**
     * El documento que el comprobante modifica, cuyo número viene formateado como
     * {@code 000-000-000000000}.
     */
    public static DocumentoModificado documentoModificado(Element info, String campoCodigo, String campoNumero, String campoFecha) {
        var numero = textoExigido(info, campoNumero);
        var partes = numero.split(Serie.SEPARADOR, -1);
        if (partes.length != 3) {
            throw new LecturaXmlException("El campo " + campoNumero + " no tiene el formato 000-000-000000000: " + numero);
        }

        return new DocumentoModificado(
            TipoComprobante.of(textoExigido(info, campoCodigo)),
            Serie.of(entero(partes[0], campoNumero), entero(partes[1], campoNumero)),
            Secuencial.of(entero(partes[2], campoNumero)),
            fecha(textoExigido(info, campoFecha), campoFecha)
        );
    }

    /**
     * Una fecha en el formato {@code dd/MM/uuuu} del esquema.
     */
    public static LocalDate fecha(String valor, String campo) {
        return fecha(valor, campo, FECHA);
    }

    private static LocalDate fecha(String valor, String campo, DateTimeFormatter formato) {
        try {
            return LocalDate.parse(valor, formato);
        } catch (DateTimeParseException e) {
            throw new LecturaXmlException("El campo " + campo + " no trae una fecha legible: " + valor, e);
        }
    }

    private static int entero(String valor, String campo) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new LecturaXmlException("El campo " + campo + " no es un número: " + valor, e);
        }
    }
}
