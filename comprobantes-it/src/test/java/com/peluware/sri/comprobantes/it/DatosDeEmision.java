package com.peluware.sri.comprobantes.it;

import com.peluware.sri.comprobantes.valores.Ruc;
import org.jspecify.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Datos que la prueba de emisión necesita del operador: certificado, su clave y el RUC.
 *
 * <p>Se piden por teclado al arrancar, una sola vez por ejecución: con un diálogo cuando hay
 * pantalla y por consola cuando no. Nada se guarda en el entorno, en disco ni en el registro.
 *
 * <p>Cada campo admite un valor por omisión desde el entorno ({@code SRI_P12_PATH},
 * {@code SRI_P12_PASSWORD} y {@code SRI_RUC}) para poder automatizar la ejecución.
 *
 * @param p12   ruta del archivo de firma electrónica
 * @param clave contraseña del archivo
 * @param ruc   RUC del emisor, que debe ser el del titular del certificado
 */
record DatosDeEmision(Path p12, char[] clave, Ruc ruc) {

    private static @Nullable DatosDeEmision pedidos;

    /**
     * @throws IllegalStateException si no hay forma de pedirlos o el operador cancela
     */
    static synchronized DatosDeEmision pedir() {
        if (pedidos == null) {
            pedidos = delEntorno().orElseGet(() -> GraphicsEnvironment.isHeadless() ? porConsola() : porDialogo());
        }
        return pedidos;
    }

    /**
     * Datos tomados del entorno, si están los tres. Evita preguntar en ejecuciones automáticas.
     */
    private static Optional<DatosDeEmision> delEntorno() {
        var ruc = entorno("SRI_RUC");
        var ruta = entorno("SRI_P12_PATH");
        var clave = entorno("SRI_P12_PASSWORD");
        if (ruc.isBlank() || ruta.isBlank() || clave.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new DatosDeEmision(ruta(ruta), clave.toCharArray(), new Ruc(ruc.strip())));
    }

    private static DatosDeEmision porDialogo() {
        var ruc = new JTextField(entorno("SRI_RUC"), 24);
        var ruta = new JTextField(entorno("SRI_P12_PATH"), 24);
        var clave = new JPasswordField(entorno("SRI_P12_PASSWORD"), 24);

        var panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("RUC del emisor"));
        panel.add(ruc);
        panel.add(new JLabel("Ruta del .p12"));
        panel.add(ruta);
        panel.add(new JLabel("Clave del .p12"));
        panel.add(clave);

        var marco = alFrente();
        try {
            while (true) {
                var respuesta = JOptionPane.showConfirmDialog(marco, panel, "Emisión contra el ambiente de pruebas", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (respuesta != JOptionPane.OK_OPTION) {
                    throw new IllegalStateException("Se canceló la introducción de los datos");
                }
                var reparo = reparo(ruta.getText(), clave.getPassword(), ruc.getText());
                if (reparo == null) {
                    return new DatosDeEmision(ruta(ruta.getText()), exigir(clave.getPassword()), new Ruc(exigir("RUC", ruc.getText())));
                }
                JOptionPane.showMessageDialog(marco, reparo, "Revise los datos", JOptionPane.WARNING_MESSAGE);
            }
        } finally {
            marco.dispose();
        }
    }

    /**
     * Lo que impide usar los datos escritos, o {@code null} si sirven.
     *
     * <p>Se comprueba antes de arrancar porque una ruta mal escrita, de otro modo, no se
     * descubre hasta el momento de firmar.
     */
    private static @Nullable String reparo(String ruta, char @Nullable [] clave, String ruc) {
        try {
            var p12 = ruta(ruta);
            if (!Files.isReadable(p12)) {
                return "No se puede leer el certificado en:\n" + p12;
            }
            exigir(clave);
            new Ruc(exigir("RUC", ruc));
            return null;
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    private static DatosDeEmision porConsola() {
        var consola = System.console();
        if (consola == null) {
            throw new IllegalStateException("Sin consola ni pantalla para pedir los datos de emisión");
        }
        var ruc = consola.readLine("RUC del emisor: ");
        var ruta = consola.readLine("Ruta del .p12: ");
        var clave = consola.readPassword("Clave del .p12: ");
        return new DatosDeEmision(ruta(ruta), exigir(clave), new Ruc(exigir("RUC", ruc)));
    }

    private static Path ruta(@Nullable String valor) {
        return Path.of(exigir("ruta del .p12", valor).replace("\"", ""));
    }

    private static String exigir(String campo, @Nullable String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("No se introdujo " + campo);
        }
        return valor.strip();
    }

    private static char[] exigir(char @Nullable [] clave) {
        if (clave == null || clave.length == 0) {
            throw new IllegalStateException("No se introdujo la clave del .p12");
        }
        return clave;
    }

    private static String entorno(String nombre) {
        var valor = System.getenv(nombre);
        return valor == null ? "" : valor;
    }

    /**
     * Ventana invisible que hace de padre del diálogo para que salga encima de cualquier ventana.
     *
     * <p>Un diálogo sin padre lanzado desde un proceso de consola queda detrás de las ventanas
     * del escritorio y no aparece en la barra de tareas.
     */
    private static JFrame alFrente() {
        var marco = new JFrame();
        marco.setUndecorated(true);
        marco.setAlwaysOnTop(true);
        marco.setLocationRelativeTo(null);
        marco.setVisible(true);
        marco.toFront();
        marco.requestFocus();
        return marco;
    }
}
