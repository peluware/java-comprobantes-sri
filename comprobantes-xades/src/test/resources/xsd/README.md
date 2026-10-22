# Esquemas del SRI, copia para este módulo

Son los mismos que `comprobantes-core/src/test/resources/xsd`, pero este módulo necesita su propia
copia: los recursos del test-jar de core viven en el paquete `com.peluware.sri.comprobantes`, que
el jar principal ya declara, y JPMS no deja que un paquete viva en dos módulos. Desde fuera de core
son invisibles.

Solo están los dos que hacen falta aquí: el de la factura y el de la firma que este importa.
