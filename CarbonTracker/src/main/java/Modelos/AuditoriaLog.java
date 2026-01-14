package Modelos;

/**
 * Representa un registro del histórico de operaciones que se han realizado en la aplicación (Solo disponible para el ADMIN)
 * <p>
 * Esta clase se utiliza principalmente para visualizar los datos en la tabla desde administración,
 * combinando la información de la acción realizada con el nombre del usuario responsable y la fecha
 *
 */
public class AuditoriaLog {

    /**
     * Identificador único del registro de auditoría en la base de datos.
     */
    private Long id;

    /**
     * Descripción detallada de la acción realizada (ej. "LOGIN_EXITOSO", "ALTA SEDE...").
     */
    private String accion;

    /**
     * Marca de tiempo (Fecha y Hora) en la que se registra el evento.
     * Almacenada como texto para facilitar su visualización.
     */
    private String fechaHora;

    /**
     * Nombre del usuario que realizó la acción que se registra.
     * <p>
     * Este campo no se almacena directamente en la tabla de auditoría, sino que
     * se recupera cruzando datos con la tabla de usuarios.
     */
    private String nombreUsuario;


    /**
     * Crea un nuevo objeto de registro de auditoría.
     *
     * @param id El identificador único del log.
     * @param accion La descripción de lo que ha ocurrido en ese registro.
     * @param fechaHora La fecha y hora del evento.
     * @param nombreUsuario El nombre del usuario responsable.
     */
    public AuditoriaLog(Long id, String accion, String fechaHora, String nombreUsuario) {
        this.id = id;
        this.accion = accion;
        this.fechaHora = fechaHora;
        this.nombreUsuario = nombreUsuario;
    }

    // Getters

    /**
     * Obtiene el ID del registro.
     * @return El identificador (Long).
     */
    public Long getId() { return id; }

    /**
     * Obtiene la descripción de la acción.
     * @return La acción (String).
     */
    public String getAccion() { return accion; }

    /**
     * Obtiene la fecha y hora del evento.
     * @return La marca de tiempo (String).
     */
    public String getFechaHora() { return fechaHora; }

    /**
     * Obtiene el nombre del usuario asociado.
     * @return El nombre de usuario (String).
     */
    public String getNombreUsuario() { return nombreUsuario; }
}