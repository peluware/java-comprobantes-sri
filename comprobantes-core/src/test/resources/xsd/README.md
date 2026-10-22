# Esquemas del SRI

Única copia de los esquemas en el repositorio. Los consumen los tests de este módulo y, a
través del `test-jar` de `comprobantes-core`, los de `comprobantes-xades` y
`comprobantes-it`. No se publican en el jar principal.

Copia literal de los paquetes `XML y XSD …` que publica el SRI en
<https://www.sri.gob.ec/facturacion-electronica>, sección «Información técnica y guías» →
«Esquema actual», actualizados a febrero de 2022. Cada paquete trae el esquema de cada versión
y un XML de ejemplo por versión, que están en `ejemplos/`.

## Los seis tipos de comprobante

| Tipo | Código | Esquemas | Raíz | Bloque propio |
|---|---|---|---|---|
| Factura | 01 | `factura_V1.0.0` … `V2.1.0` | `<factura>` | `infoFactura` |
| Liquidación de compra | 03 | `LiquidacionCompra_V1.0.0`, `V1.1.0` | `<liquidacionCompra>` | `infoLiquidacionCompra` |
| Nota de crédito | 04 | `NotaCredito_V1.0.0`, `V1.1.0` | `<notaCredito>` | `infoNotaCredito` |
| Nota de débito | 05 | `NotaDebito_V1.0.0` | `<notaDebito>` | `infoNotaDebito` |
| Guía de remisión | 06 | `GuiaRemision_V1.0.0`, `V1.1.0` | `<guiaRemision>` | `infoGuiaRemision` |
| Comprobante de retención | 07 | `ComprobanteRetencion_V1.0.0`, `V2.0.0` | `<comprobanteRetencion>` | `infoCompRetencion` |

Los seis comparten `infoTributaria` e `infoAdicional`, idénticos. Lo que cambia es el bloque
propio de cada uno.

La librería escribe, por ahora, `factura_V2.1.0`, `NotaCredito_V1.1.0` y `NotaDebito_V1.0.0`.
Las versiones anteriores están para poder contrastar qué cambió entre ellas.

## Lo que no cuadra en los materiales del SRI

`ejemplos/factura_V2.0.0.xml` **no valida contra `factura_V2.0.0.xsd`**: trae
`<incoTermFactura></incoTermFactura>` vacío contra un tipo con `minLength` 1. El SRI lo corrigió
en el ejemplo de la 2.1.0. Los otros doce ejemplos sí validan; `EsquemasDelSriTest` lo comprueba
y deja ese defecto fijado aparte.

## `xmldsig-core-schema.xsd`

Esquema de firma XML del W3C, necesario porque todos los esquemas del SRI lo importan:

```xml
<xsd:import namespace="http://www.w3.org/2000/09/xmldsig#"
            schemaLocation="xmldsig-core-schema.xsd"/>
```

Descargado de
<https://www.w3.org/TR/2002/REC-xmldsig-core-20020212/xmldsig-core-schema.xsd>.

**Única modificación:** se eliminó la declaración `DOCTYPE`, que apuntaba a
`http://www.w3.org/2001/XMLSchema.dtd` por red. Su subconjunto interno declaraba tres
entidades (`dsig`, `p`, `s`) y un `ATTLIST` para `xmlns:ds`; ninguna de las entidades se
usa en el cuerpo del esquema, y el elemento `<schema>` ya declara `xmlns:ds`
explícitamente, así que el contenido es equivalente. Sin esa eliminación, el parser
intenta descargar el DTD en cada test.

El `schemaLocation` del import es relativo, así que basta con que los esquemas estén en
este mismo directorio: la resolución la hace el parser y nunca sale a la red.
