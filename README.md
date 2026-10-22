# Comprobantes Electrónicos SRI

Emite comprobantes electrónicos del SRI de Ecuador en el esquema **offline**: construye el XML,
lo firma con XAdES-BES, lo envía, consulta su autorización y comprueba si sigue valiendo.

Java 21. Sin dependencias en runtime más allá del JDK, salvo el módulo del RIDE, que usa Apache PDFBox.

> **Estado:** `0.1.0-SNAPSHOT`, todavía sin publicar en Maven Central. Probada contra el ambiente
> de certificación, no contra producción. El RIDE cubre los seis comprobantes.

## Una factura

```java
var emisor = Emisor.builder(new Ruc("1790012345001"), "PELUWARE S.A.", "AMAZONAS Y ROCA")
    .obligadoContabilidad(true)
    .build();

var factura = Factura.builder()
    .emisor(emisor)
    .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
    .fechaEmisionHoy()
    .ambiente(Ambiente.PRUEBAS)
    .establecimiento(Establecimiento.of(1, 1, "AMAZONAS Y ROCA"))
    .secuencial(Secuencial.of(130))
    .codigoNumerico(EstrategiaCodigoNumerico.hmac(secreto))
    .detalle(DetalleComprobanteVenta.builder()
        .codigoPrincipal("SKU-001")
        .descripcion("Alimento para perro 15kg")
        .unidadMedida("SACO")
        .cantidad(2)
        .precioUnitario("25.00")
        .descuento(Dinero.of("5.00"))
        .iva(TarifaIva.QUINCE)
        .build())
    .pago(Pago.of(FormaPago.SIN_UTILIZACION_SISTEMA_FINANCIERO, Dinero.of("51.75")))
    .build();
```

El builder calcula la clave de acceso y los totales: 50,00 menos 5,00 de descuento, más 15 % de
IVA, 51,75. `ComprobanteWriter.xml(factura)` devuelve el XML sin firmar, listo para firmar.

## Firmarla

```java
var firmador = new FirmadorXadesBes();

try (InputStream p12 = Files.newInputStream(Path.of("certificado.p12"))) {
    var origenClave = new OrigenClavePkcs12(p12, password);

    ComprobanteFirmado firmado = firmador.firmar(origenClave, factura);

    // O una tanda, abriendo el certificado una sola vez:
    List<ComprobanteFirmado> tanda = firmador.firmar(origenClave, List.of(factura, otra, otraMas));
}
```

El constructor consume el flujo del `.p12` y lo cierra; la contraseña no se retiene.

## Enviarla

El esquema offline es asíncrono: el envío solo acusa recibo. La autorización se consulta aparte,
y el SRI dispone de hasta 24 horas para resolver.

```java
var cliente = ClienteSri.of(Ambiente.PRUEBAS);

switch (cliente.enviar(firmado.xml())) {
    case Recibida()                                  -> encolarConsulta(factura.claveAcceso());
    case Devuelta(var rechazados)                    -> corregir(rechazados);
    case RespuestaRecepcion.Desconocida(var e, var c) -> registrar(e, c);
}
```

Y más tarde, con la clave de acceso:

```java
switch (cliente.consultarAutorizacion(claveAcceso)) {
    case Autorizado a                                  -> archivar(a.comprobanteEnBytes());
    case NoAutorizado n                                -> rechazar(n.errores());
    case EnProceso(var pendiente)                      -> reintentarMasTarde(pendiente);
    case RespuestaAutorizacion.Desconocida(var e, var m) -> registrar(e, m);
}
```

Las respuestas son jerarquías selladas de records, así que el `switch` es exhaustivo sin
`default`: si el SRI añadiera un estado y esta librería lo modelara, tu código dejaría de
compilar en vez de caer en una rama silenciosa.

El cliente no reintenta ni espera. La política de reintentos queda en tu aplicación.

## Saber si un comprobante sigue valiendo

La autorización es un hecho y no cambia. El **estado** sí: un comprobante autorizado pasa a
anulado si su anulación se completa, y eso ocurre sin que el emisor intervenga. Para eso hay un
tercer servicio, la consulta de validez:

```java
import static com.peluware.sri.comprobantes.client.RespuestaValidez.*;

var validez = cliente.consultarValidez(claveAcceso);

switch (validez) {
    case Autorizado(var c)        -> ok();
    case PendienteDeAnular(var c) -> avisarAlReceptor(c);   // sigue valiendo, de momento
    case Anulado(var c)           -> darDeBaja(c);
    case NoAutorizado(var c)      -> rechazar(c);
    case NoConsultable(var clave, var mensajes) -> reintentar(clave, mensajes);
    case Desconocida(var estado, var mensajes)  -> registrar(estado, mensajes);
}
```

O, si solo te importa el sí o el no, `validez.tieneValidez()`, que es cierto para `Autorizado`
y para `PendienteDeAnular`.

Ojo con los nombres: `RespuestaValidez` y `RespuestaAutorizacion` comparten `Autorizado`,
`NoAutorizado` y `Desconocida`, porque el SRI usa las mismas palabras para los dos servicios. En
una clase que consulte ambos hay que cualificar uno de los dos juegos.

Fíjate también en que `NoConsultable` **no** significa que el comprobante no valga: significa que
la consulta no se pudo resolver, porque la clave no consta en el SRI o porque su fecha de emisión
quedó fuera del rango que el servicio admite. Confundir las dos cosas es el error fácil aquí, y
por eso son casos distintos y no un `boolean`.

Este servicio no devuelve el XML del comprobante; para archivarlo sigue siendo la autorización.

Y esta librería no consulta por su cuenta: no guarda qué emitiste ni cuándo, así que la cadencia
es tuya. Un criterio razonable es consultar hasta que cierre la ventana de anulación del mes de
emisión, no indefinidamente.

## Imprimirla

El RIDE es la representación impresa del comprobante, y tiene validez tributaria y jurídica. Pero
el documento sigue siendo el XML: el RIDE es lo que ve la persona.

```java
var generador = new RidePdfBox();

try (var salida = Files.newOutputStream(Path.of("factura.pdf"))) {
    generador.escribir(factura, fechaAutorizacion, salida);
}
```

Los seis comprobantes se dibujan según su modelo del Anexo 2. Sin la fecha de autorización sale
igual, con ese campo en blanco, que es lo que sirve para previsualizar antes de enviar:

```java
generador.escribir(factura, salida);
```

El generador es inmutable y se puede compartir entre hilos.

### El estilo

`EstiloRide` no cambia **qué** datos salen —eso lo fija el Anexo 2— sino cómo se ven.

```java
var estilo = EstiloRide.builder()
    .logo(Files.readAllBytes(Path.of("logo.png")))
    .paleta(Paleta.deMarca(0x1F4E79))
    .familia(FamiliaTipografica.HELVETICA)
    .codigoBarras(true)
    .build();

var generador = new RidePdfBox(estilo);
```

| Parámetro | Qué hace | Admite |
|---|---|---|
| `logo` | Imagen sobre la caja del emisor | JPEG, PNG, TIFF, BMP o GIF |
| `altoLogo` | Alto del logo, en puntos | 20 a 120 |
| `alineacionLogo` | Dónde se apoya dentro de su banda | `Alineacion` |
| `codigoBarras` | Code 128 con la clave de acceso | opcional por el numeral 9.20 |
| `paleta` | Colores de líneas, rótulos, texto y cabeceras | `Paleta` |
| `familia` | Una de las familias base-14 | `FamiliaTipografica` |
| `tipografia` | Fuentes propias, incrustadas en el documento | `Tipografia` |
| `escala` | Tamaño de todo el documento | 0,8 a 1,4 |
| `interlineado` | Aire entre renglones | 0,8 a 2 |
| `aireEntreBloques` | Separación entre recuadros | 0,5 a 3 |
| `enfasis` | Qué va en negrita: el rótulo, el valor o nada | `Enfasis` |
| `rotulos` | Las mayúsculas del Anexo 2, o frase | `Rotulos` |
| `contorno` | Caja, regla o nada alrededor de cada bloque | `Contorno` |
| `divisiones` | Líneas internas de la tabla de detalles | `Divisiones` |
| `formatoAdicionales` | Cómo se listan los datos adicionales de cada línea | `FormatoAdicionales` |
| `grosorLineas` | Grosor del trazo, en puntos | 0,25 a 3 |
| `radioBordes` | Redondeo de las cajas, en puntos | 0 a 8 |
| `marcoPagina` | Filete alrededor de la página | `MarcoPagina` |
| `pie` | Texto y logo al pie de cada página | `PieRide` |

Tres cosas ocurren solas. Una columna que ninguna línea llena desaparece, y su ancho se reparte
entre las demás. Un valor que no cabe a lo ancho pasa al renglón siguiente y su recuadro crece, en
vez de recortarse. Y cuando el detalle no cabe en una hoja el RIDE se reparte en varias, repitiendo
la cabecera y numerando las páginas.

## Leerla

El camino inverso: de un XML a los objetos. Sirve para el comprobante que te llega de un
proveedor, y para releer lo que archivaste.

```java
var comprobante = ComprobanteReader.comprobante(entrada);
```

Mira el elemento raíz y devuelve el tipo que toque, así que el `switch` que venga después es
exhaustivo:

```java
switch (ComprobanteReader.comprobante(entrada)) {
    case Factura f               -> registrarCompra(f);
    case NotaCredito n           -> aplicarDevolucion(n);
    case ComprobanteRetencion r  -> acreditarRetencion(r);
    case NotaDebito n            -> registrarRecargo(n);
    case LiquidacionCompra l     -> registrarLiquidacion(l);
    case GuiaRemision g          -> registrarTraslado(g);
}
```

Si ya sabes qué es, cada tipo tiene el suyo, y cada uno expone su única instancia:
`FacturaReader.INSTANCIA`, `NotaCreditoReader.INSTANCIA`, `NotaDebitoReader.INSTANCIA`,
`LiquidacionCompraReader.INSTANCIA`, `GuiaRemisionReader.INSTANCIA` y
`ComprobanteRetencionReader.INSTANCIA`. Su `read` toma un `InputStream` —el de una respuesta
HTTP, el de un archivo— o un `byte[]`.

Acepta el comprobante firmado tal cual: la firma va dentro del mismo documento y se ignora. El que
devuelve el SRI al autorizar ya viene desenvuelto en `comprobante()`.

### Lo que el archivo dice es lo que vale

La lectura **no recalcula nada**. Si el XML declara un total que no cuadra con sus líneas, lo que
obtienes es el total del archivo. Un comprobante que el SRI autorizó manda sobre la aritmética de
esta librería, y quien recibe la factura de un proveedor quiere los números del proveedor.

Una salvedad: los importes pasan tal cual, pero las reglas de coherencia del modelo siguen
corriendo al construirlo. En concreto, si el comprobante declara formas de pago, tienen que sumar
el importe total; si no, la lectura falla con `ValidacionException`. Esa regla no sale del XSD, es
de esta librería.

Por eso la vuelta es exacta: para los seis comprobantes, el XML de lo leído es **idéntico byte por
byte** al que entró. Es lo que comprueba `ComprobanteContratoTest`.

Dos cosas que la lectura no hace, a propósito:

| | |
|---|---|
| Verificar la firma | Quien adjudica es el SRI, en la segunda de sus dos puertas. |
| Validar contra el XSD | Los esquemas no se publican en el jar. |

## Una nota de crédito que modifica a esa factura

```java
var notaCredito = NotaCredito.builder()
    .emisor(emisor)
    .comprador(Comprador.of(TipoIdentificacion.CEDULA, "1710034065", "JUAN PEREZ"))
    .fechaEmisionHoy()
    .ambiente(Ambiente.PRUEBAS)
    .establecimiento(Establecimiento.of(1, 1, "AMAZONAS Y ROCA"))
    .secuencial(Secuencial.of(7))
    .codigoNumerico(EstrategiaCodigoNumerico.hmac(secreto))
    .documentoModificado(new DocumentoModificado(
        TipoComprobante.FACTURA,
        factura.serie(),
        factura.secuencial(),
        factura.fechaEmision()))
    .motivo("Devolución del saco de alimento")
    .detalle(DetalleNotaCredito.builder()
        .codigoInterno("SKU-001")
        .descripcion("Alimento para perro 15kg")
        .cantidad(1)
        .precioUnitario("25.00")
        .iva(TarifaIva.QUINCE)
        .build())
    .build();
```

El `numDocModificado` sale tipado de la serie y el secuencial de la factura original, no de
texto libre.

## Los seis comprobantes

| Comprobante | Clase | Esquema |
|---|---|---|
| Factura | `factura.Factura` | 2.1.0 |
| Nota de crédito | `notacredito.NotaCredito` | 1.1.0 |
| Nota de débito | `notadebito.NotaDebito` | 1.0.0 |
| Liquidación de compra | `liquidacioncompra.LiquidacionCompra` | 1.1.0 |
| Guía de remisión | `guiaremision.GuiaRemision` | 1.1.0 |
| Comprobante de retención | `retencion.ComprobanteRetencion` | 2.0.0 |

Todos se construyen con un builder, todos derivan su clave de acceso de sus propios datos y
todos salen por `ComprobanteWriter.xml(comprobante)`, que despacha sobre el tipo. Si lo tienes
en la mano, su writer también escribe directo: `FacturaWriter.INSTANCIA.toByteArray(factura)`.

## Instalar

```xml
<dependency>
  <groupId>com.peluware.sri</groupId>
  <artifactId>comprobantes-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>com.peluware.sri</groupId>
  <artifactId>comprobantes-xades</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>com.peluware.sri</groupId>
  <artifactId>comprobantes-client</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>com.peluware.sri</groupId>
  <artifactId>comprobantes-ride-pdfbox</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

| Artefacto | Contenido |
|---|---|
| `comprobantes-core` | Modelo, catálogos, clave de acceso y escritura del XML. |
| `comprobantes-xades` | Firma XAdES-BES sobre el proveedor de XML-DSig del JDK. |
| `comprobantes-client` | Clientes de recepción, autorización y consulta de validez. |
| `comprobantes-ride` | Contrato de generación del RIDE y bloques comunes del Anexo 2. |
| `comprobantes-ride-pdfbox` | Genera el RIDE en PDF. Única con dependencia: Apache PDFBox. |

Cada módulo declara su `module-info`, así que funciona igual en el module path y en el classpath.

## Reenviar un comprobante rechazado

El numeral 5.10 de la Ficha Técnica obliga a reenviarlo con la misma clave de acceso.
`EstrategiaCodigoNumerico.hmac(secreto)` deriva el código numérico de la identidad del
comprobante: el mismo RUC, tipo, serie y secuencial producen siempre la misma clave.

```java
.codigoNumerico(EstrategiaCodigoNumerico.hmac(secreto))
```

Guarda ese secreto como cualquier otra credencial. `EstrategiaCodigoNumerico.random(...)` está
`@Deprecated` por no cumplir el numeral.

## Anulación

No hay forma de anular un comprobante desde esta librería, ni desde ninguna: el SRI no expone un
servicio que lo haga. Se tramita en línea, en el portal institucional, y la Resolución
NAC-DGERCGC25-00000014 la restringe a los comprobantes «emitidos con errores o aquellos cuya
operación, motivo de su emisión, no se haya producido».

Un comprobante autorizado es inmutable, como un asiento contable ya registrado: no se edita, se
corrige con otro documento que lo referencia. El Art. 50 del Reglamento obliga a conservar siete
años incluso los comprobantes anulados, en original y con sus copias. Que el cliente no pague no
es causal de nada: la operación se produjo, y eso es cobranza, no facturación.

Lo que sí te afecta:

- Una `Factura` o una `LiquidacionCompra` se revierte con una `NotaCredito`, hasta doce meses
  después de la emisión del comprobante afectado. Las facturas a consumidor final no, en ningún
  plazo y por ninguna vía.
- Una `NotaCredito`, una `NotaDebito`, una `GuiaRemision` o un `ComprobanteRetencion` solo se
  anulan en línea. Las tres primeras, salvo la guía, necesitan que el receptor acepte dentro de
  cinco días hábiles.
- El estado de un comprobante ya autorizado puede cambiar sin que intervengas, y como emisor
  estás obligado a informar al receptor de ese cambio. Para detectarlo está
  `consultarValidez(...)`.
- Los comprobantes se transmiten al SRI en el momento de su emisión. No hay ventana para
  acumularlos.

## Errores

Todas las excepciones heredan de `ComprobanteException`, que lleva el detalle técnico en
`getMessage()` y un texto apto para pantalla en `mensajeUsuario()`.

```java
try {
    var firmado = firmador.firmar(origenClave, factura);
} catch (FirmaException e) {
    switch (e) {
        case FirmaException.CertificadoCaducado c -> avisarRenovacion(c.caducidad());
        case FirmaException.VariasClaves v        -> pedirAlias(v.alias());
        default                                   -> registrar(e);
    }
}
```

Un dato mal formado llega como `ValidacionException` con un `Validacion.CampoInvalido` que nombra
el campo tal como lo nombra el XSD, o un `Validacion.ReglaInvalida` cuando falla una regla que
abarca varios campos.

<details>
<summary><b>Lo que valida, en detalle</b></summary>

Lo que impone el XSD, en el momento de construir: longitudes, patrones, decimales, dígitos
totales y cardinalidades. Un comprobante que se construye es un comprobante que cumple su
esquema.

Además:

- El dígito verificador de la clave de acceso, por módulo 11 con factor ponderado 2
  (numeral 5.2).
- La correspondencia entre los detalles y los totales: el subtotal es la suma de los precios
  totales sin impuesto, y los impuestos se agrupan por impuesto y código de porcentaje.
- La cascada de bases imponibles de la Tabla 21: el ICE se calcula sobre el precio total sin
  impuesto, y el IVA sobre ese precio más el ICE.
- Que las formas de pago, si se declaran, sumen el importe total.

Lo que el SRI resuelve contra sus catálogos y su padrón no se replica aquí: si el RUC existe, si
el código de retención está vigente, si el secuencial ya se usó. El SRI tiene dos puertas —
recepción valida la estructura, autorización valida los catálogos — y esta librería solo puede
anticipar la primera.

Los catálogos que el esquema no acota quedan abiertos: `FormaPago` y `TarifaIva` traen como
constantes lo que enumeran las Tablas 24 y 17, y admiten construir un valor que el SRI añada sin
esperar una versión nueva de la librería.

Tampoco se asignan secuenciales ni se mantiene estado: la unicidad por RUC, tipo, establecimiento
y punto de emisión es de quien llama.

</details>

<details>
<summary><b>Campos y servicios que no están</b></summary>

Campos opcionales de los esquemas que no se escriben: `valorDevolucionIva`, `precioSinSubsidio`,
`tipoNegociable`, `reembolsos`, `dividendos`, `compraCajBanano` y los tres `total*Reembolso`.

La consulta de factura comercial negociable (`ConsultaFactura`, cuarto servicio de la ficha) no
está. Solo aplica a las facturas con `tipoNegociable`, que tampoco se escribe.

El envío en lote (`RecepcionComprobantesOffline` con `<lote>`) no está, y no es un olvido: el
servicio de certificación no lo atiende. El mismo comprobante firmado se autoriza enviándolo solo
y es invisible dentro de un lote. Desde agosto de 2025 la normativa además exige transmitir cada
comprobante en el momento de emitirlo, así que el lote tampoco tendría a quién servir.

</details>

## Pruebas

```bash
mvn test
```

Las pruebas de integración (`*IT`) emiten contra el ambiente de certificación con un certificado
real. Maven no las recoge por su nombre, así que hay que pedirlas, y piden el certificado, su
clave y el RUC por teclado al arrancar:

```bash
mvn test -Dtest='*IT' -pl comprobantes-it -am "-Dsurefire.failIfNoSpecifiedTests=false"
```

El `failIfNoSpecifiedTests` hace falta porque `-am` también construye los otros módulos, donde
ese patrón no encuentra nada. Para una sola, sustituye `'*IT'` por su nombre.

Cada prueba se autoabastece: las que necesitan un comprobante previo lo emiten y lo autorizan
ellas mismas, y el secuencial sale de un contador por ejecución, así que las siete pueden
lanzarse de un tirón sin chocar de clave de acceso. Dejan los XML de cada recorrido en
`comprobantes-it/target`, incluidos los sobres SOAP crudos de la consulta de validez.

## Licencia

Apache 2.0.
