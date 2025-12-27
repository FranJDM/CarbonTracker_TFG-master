package Modelos;

/**
 * Representa una entidad de Empresa dentro del sistema.
 * <p>
 * Esta clase almacena la información fundamental de una empresa (nombre, sector)
 * y proporciona funcionalidad auxiliar para gestionar cálculos temporales de
 * emisiones de CO2, facilitando la generación de rankings y reportes.
 */
public class Empresa {

    /**
     * Identificador único (ID) de la empresa en la base de datos.
     */
    private Long id;

    /**
     * Nombre oficial o comercial de la empresa.
     */
    private String nombreEmpresa;

    /**
     * Sector industrial o comercial al que pertenece la empresa.
     */
    private String sector;

    /**
     * Campo auxiliar para almacenar el total de emisiones de CO2 calculadas.
     * <p>
     * <b>Nota:</b> Este valor no se persiste directamente como atributo fijo,
     * sino que se utiliza para operaciones en memoria (como ordenar listas
     * por huella de carbono). Su valor por defecto es 0.0.
     */

    private double auxiliarAlmacenC02 = 0.0;



    /**
     * Constructor para crear una nueva instancia de Empresa sin ID.
     * <p>
     * Útil para registrar nuevas empresas antes de ser guardadas en la base de datos.
     *
     * @param nombreEmpresa El nombre de la empresa.
     * @param sector El sector al que pertenece.
     */
    public Empresa(String nombreEmpresa, String sector) {
        this.nombreEmpresa = nombreEmpresa;
        this.sector = sector;
    }

    /**
     * Constructor completo para instanciar una Empresa existente.
     *
     * @param id El identificador único de la empresa.
     * @param nombreEmpresa El nombre de la empresa.
     * @param sector El sector al que pertenece.
     */
    public Empresa(Long id, String nombreEmpresa, String sector) {
        this.id = id;
        this.nombreEmpresa = nombreEmpresa;
        this.sector = sector;
    }

    // Getters y setters

    /**
     * Obtiene el ID de la empresa.
     * @return El identificador único (Long).
     */
    public Long getId() { return id; }

    /**
     * Establece el ID de la empresa.
     * @param id El nuevo identificador.
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Obtiene el nombre de la empresa.
     * @return El nombre de la empresa (String).
     */
    public String getNombreEmpresa() { return nombreEmpresa; }

    /**
     * Establece el nombre de la empresa.
     * @param nombreEmpresa El nuevo nombre.
     */
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

    /**
     * Obtiene el sector de la empresa.
     * @return El sector (String).
     */
    public String getSector() { return sector; }

    /**
     * Establece el sector de la empresa.
     * @param sector El nuevo sector.
     */
    public void setSector(String sector) { this.sector = sector; }

    /**
     * Obtiene el valor auxiliar almacenado de las emisiones totales de CO2.
     * @return La cantidad de emisiones calculadas (double).
     */
    public double getAuxiliarAlmacenC02() { return auxiliarAlmacenC02; }

    /**
     * Un valor auxiliar para el total de emisiones de CO2.
     * <p>
     * Se usa este método cuando se calculan las emisiones en -tiempo de ejecución-
     * y se necesita asociar el resultado al objeto empresa temporalmente.
     *
     * @param totalCo2e El total de emisiones a almacenar.
     */
    public void setAuxiliarAlmacenC02(double totalCo2e) { this.auxiliarAlmacenC02 = totalCo2e; }

    /**
     * Devuelve una representación en cadena de la empresa para la interfaz gráfica.
     * <p>
     * La salida varía según la necesidad:
     * <ul>
     * <li>Si {@code auxiliarAlmacenC02 > 0}: Muestra "Nombre (XX.XX kgCO2e)".</li>
     * <li>Si no hay emisiones calculadas: Muestra "Nombre (Sector)".</li>
     * </ul>
     *
     * @return String formateado para listas o combos.
     */
    @Override
    public String toString() {
        if (auxiliarAlmacenC02 > 0) {
            return String.format("%s (%.2f kgCO2e)", nombreEmpresa, auxiliarAlmacenC02);
        }
        return nombreEmpresa + " (" + sector + ")";
    }
}