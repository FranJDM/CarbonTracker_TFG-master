package DAO;

import Modelos.Emisiones;
import Modelos.Empresa;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
/**
 * Clase para la gestión y exportación de datos a formato CSV.
 * <p>
 * Esta clase facilita la generación de informes permitiendo guardar
 * listados de empresas y registros de emisiones en archivos de texto.
 */
public class ControlCSV {

    // Formateador de fecha para que salga en español en el CSV (dd/MM/yyyy)
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /**
     * Genera un archivo CSV con el listado completo de empresas proporcionado.
     * <p>
     * El archivo generado tendrá la estructura de columnas:
     * {@code ID, Nombre, Sector}.
     *
     * @param empresas La lista de objetos {@link Empresa} a exportar.
     * @param archivo El archivo de destino (File) donde se escribirán los datos.
     * @throws IOException Si ocurre un error de entrada/salida al escribir el archivo.
     */
    public static void exportarEmpresas(List<Empresa> empresas, File archivo) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivo))) {
            // Escribir cabecera
            bw.write("Nombre;Sector\n");
            // Escribir datos
            for (Empresa empresa : empresas) {
                bw.write(String.format("\"%s\";\"%s\"\n",
                        auxiliarComillas(empresa.getNombreEmpresa()),
                        auxiliarComillas(empresa.getSector())
                ));
            }
        }
    }
    /**
     * Genera un archivo CSV con el historial de emisiones, no se indicará la empresa, porque se filtran por TODAS
     * o por Empresa seleccionada. Posibilidad de ampliar en el futuro la función para que se muestre a qué Empresa pertenencen.
     * <p>
     * El archivo generado tendrá la siguiente estructura de columnas:
     * {@code ID, Tipo, Cantidad, CO2e, Fecha}.
     *
     * @param emisiones La lista de objetos {@link Emisiones} a exportar.
     * @param archivoEmisiones El archivo de destino (File) donde se escribirán los datos.
     * @throws IOException Si ocurre un error de entrada/salida al escribir el archivo.
     */
    public static void exportarEmisiones(List<Emisiones> emisiones, File archivoEmisiones) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(archivoEmisiones))) {
            // Cabecera con Nombre de Empresa
            bw.write("Empresa;Tipo;Cantidad;CO2e (kg);Fecha\n");

            for (Emisiones registro : emisiones) {

                //Formato fecha
                String fechaFormateada;
                try {
                    //Fecha base
                    String fechaRaw = registro.getFecha().toString();

                    //Controlamos separadores
                    if (fechaRaw.contains(" ")) {
                        fechaRaw = fechaRaw.split(" ")[0];
                    }

                    // Parseamos y pasamos a formato esp
                    LocalDate fechaObj = LocalDate.parse(fechaRaw);
                    fechaFormateada = fechaObj.format(FORMATO_FECHA);

                } catch (Exception e) {
                    // Controlamos la excepcion e imprimimos la base por si hubiera algún fallo no quedarnos sin dato
                    fechaFormateada = registro.getFecha().toString();
                }
                // Escribimos la línea usando el NOMBRE DE LA EMPRESA
                bw.write(String.format("\"%s\";\"%s\";%.2f;%.2f;\"%s\"\n",
                        auxiliarComillas(registro.getNombreEmpresa()), // Nombre real
                        auxiliarComillas(registro.getTipoEmision()),
                        registro.getCantidadEmision(),
                        registro.getCo2e(),
                        fechaFormateada
                ));
            }
        }
    }

    /**
     * Método auxiliar para 'limpiar' cadenas de texto antes de escribirlas en el CSV.
     * <p>
     * Este método se asegura de que las comillas dobles dentro del texto
     * se 'escapen' correctamente (reemplazándolas por comillas dobles dobles),
     * así evitamos errores de formato al abrir el archivo en Excel u otros visores.
     *
     * @param cadenaOriginal La cadena de texto original.
     * @return La cadena saneada y segura para incluir en un campo CSV entrecomillado.
     */
    private static String auxiliarComillas(String cadenaOriginal) {
        if (cadenaOriginal == null) return "";
        return cadenaOriginal.replace("\"", "\"\"");
    }
}