package Modelos;

import java.time.LocalDate;

/**
 * Representa un registro individual de emisiones que luego son asociados a la empresa.
 * <p>
 * Esta clase vincula una cantidad de emisión y su cálculo equivalente en CO2
 * a una empresa específica en una fecha determinada.
 */
public class Emisiones {

    /**
     * Identificador único (ID) del registro de emisión.
     */
    private Long id;

    /**
     * Tipo o fuente de la emisión (ej. "Electricidad", "Combustible", "Transporte").
     */
    private String tipoEmision;

    /**
     * Cantidad bruta del recurso consumido (ej. kWh, litros, km).
     */
    private double cantidadEmision;

    /**
     * Cantidad calculada de CO2 equivalente (Huella de Carbono) en kgCO2e.
     */
    private double co2e;

    /**
     * Fecha en la que se realizó el registro de la emisión.
     */
    private LocalDate fecha;

    /**
     * Identificador (FK) de la empresa a la que pertenece esta emisión.
     */
    private Long idEmpresa; // Solo guardamos el ID de la empresa

    /**
     * Nombre de la empresa asociada.
     * <p>
     * Este campo es auxiliar y sirve para facilitar la visualización en tablas/listas
     * sin tener que realizar consultas adicionales a la base de datos.
     */
    private String nombreEmpresa;


    /**
     * Constructor para crear un nuevo registro de emisión (sin persistir).
     * <p>
     * <b>Aclaracion:</b> Asigna automáticamente la fecha actual ({@code LocalDate.now()})
     * al momento de la creación.
     *
     * @param tipoEmision El tipo de emisión (ej. "Electricidad").
     * @param cantidadEmision La cantidad consumida.
     * @param co2e El cálculo resultante en kgCO2e.
     * @param idEmpresa El ID de la empresa responsable.
     */
    public Emisiones(String tipoEmision, double cantidadEmision, double co2e, Long idEmpresa) {
        this.tipoEmision = tipoEmision;
        this.cantidadEmision = cantidadEmision;
        this.co2e = co2e;
        this.idEmpresa = idEmpresa;
        this.fecha = LocalDate.now();
    }

    /**
     * Constructor para crear una instancia de un registro recuperado de la base de datos.
     * <p>
     * Parsea la fecha almacenada como texto en la BD a un objeto {@code LocalDate}.
     *
     * @param id El identificador único del registro.
     * @param tipoEmision El tipo de emisión.
     * @param cantidadEmision La cantidad emitida.
     * @param co2e El cálculo de CO2.
     * @param fecha La fecha en formato String (se parseará a LocalDate).
     * @param idEmpresa El ID de la empresa asociada.
     */
    public Emisiones(Long id, String tipoEmision, double cantidadEmision, double co2e, String fecha, Long idEmpresa) {
        this.id = id;
        this.tipoEmision = tipoEmision;
        this.cantidadEmision = cantidadEmision;
        this.co2e = co2e;
        this.fecha = LocalDate.parse(fecha);
        this.idEmpresa = idEmpresa;
    }

    // Getters y Setters

    /**
     * Obtiene el identificador único del registro.
     * @return El ID (Long).
     */
    public Long getId() { return id; }

    /**
     * Obtiene el tipo de fuente de la emisión.
     * @return El tipo de emisión (String).
     */
    public String getTipoEmision() { return tipoEmision; }

    /**
     * Obtiene la cantidad bruta consumida.
     * @return La cantidad (double).
     */
    public double getCantidadEmision() { return cantidadEmision; }

    /**
     * Obtiene el valor calculado de CO2 equivalente.
     * @return El CO2e (double).
     */
    public double getCo2e() { return co2e; }

    /**
     * Obtiene la fecha del registro.
     * @return La fecha (LocalDate).
     */
    public LocalDate getFecha() { return fecha; }

    /**
     * Obtiene el ID de la empresa asociada.
     * @return El ID de la empresa (Long).
     */
    public Long getIdEmpresa() { return idEmpresa; }

    /**
     * Obtiene el nombre de la empresa asociada (campo auxiliar).
     * @return El nombre de la empresa (String).
     */
    public String getNombreEmpresa() { return nombreEmpresa; }

    /**
     * Establece el nombre de la empresa asociada.
     * <p>
     * Útil para rellenar datos en interfaces visuales tras recuperar el ID.
     *
     * @param nombreEmpresa El nombre de la empresa.
     */
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }
}