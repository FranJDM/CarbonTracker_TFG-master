package ct.Principal;

/**
 * Punto de entrada para la aplicación Carbon Tracker.
 * <p>
 * Esta clase actúa como un <b>lanzador independiente</b> (Launcher) que desacopla el inicio de la Máquina Virtual Java (JVM)
 * de la clase principal de la interfaz gráfica ({@link AppCT}).
 * </p>
 * <p>
 * <b>Nota de arquitectura:</b> Su existencia es fundamental para garantizar la correcta ejecución de la aplicación
 * cuando se empaqueta en un archivo JAR ejecutable (Fat JAR), evitando conflictos de carga con el sistema de módulos
 * y las dependencias de tiempo de ejecución de JavaFX.
 * </p>
 */
public class Launcher {

    /**
     * Método principal estándar que sirve como lanzador de la ejecución.
     * <p>
     * Este método no contiene lógica; su única responsabilidad es redirigir el flujo de control
     * y transferir los argumentos de línea de comandos al método main de la clase {@link AppCT},
     * iniciando así oficialmente el ciclo de vida de la aplicación gráfica.
     * </p>
     *
     * @param args Argumentos de línea de comandos recibidos al iniciar el programa (opcionales).
     */
    public static void main(String[] args) {
        AppCT.main(args);
    }
}