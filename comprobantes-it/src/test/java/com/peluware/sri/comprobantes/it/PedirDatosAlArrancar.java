package com.peluware.sri.comprobantes.it;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Pide los datos de emisión una sola vez, antes de que arranque cualquier prueba.
 *
 * <p>JUnit abre la sesión del lanzador antes de descubrir y ejecutar los tests, así que lo que
 * tarde el operador en escribir sus credenciales no se le atribuye a ninguna clase. Antes se
 * pedían en el {@code @BeforeAll} de la primera prueba que corriera, y su tiempo salía en el
 * informe: la factura aparecía tardando 33 segundos cuando el recorrido contra el SRI son dos.
 *
 * <p>Se registra con el fichero
 * {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.
 *
 * <p>Si falta algún dato, el fallo salta aquí y ninguna prueba llega a ejecutarse, que es lo
 * que se quiere: sin certificado no hay nada que medir.
 */
public final class PedirDatosAlArrancar implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        DatosDeEmision.pedir();
    }
}
