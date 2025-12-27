package Modelos;

/**
 * Entidad que designamos para asociar a una empresa con un lugar físico.
 * <p>
 * Esta clase almacena la información de ubicación (ciudad y dirección)
 * y mantiene el vínculo con la empresa en cuestión mediante su ID.
 */
public class Sede {

    /**
     * Identificador único (ID) de la sede en la base de datos.
     * <p>
     * Este campo es crucial para realizar operaciones de actualización (editar)
     * o eliminación sobre un registro específico.
     */
    private Long id;

    /**
     * Nombre de la ciudad donde se encuentra la sede.
     */
    private String ciudad;

    /**
     * Dirección física específica de la sede (Calle, número, polígono, etc.).
     */
    private String direccion;

    /**
     * Identificador (Fk) de la empresa a la que pertenece esta sede.
     */
    private Long idEmpresa;

    /**
     * Constructor completo para usar como instancia de una sede existente.
     * <p>
     * Utilizado generalmente al recuperar datos de la base de datos donde el ID ya existe.
     *
     * @param id El identificador único de la sede.
     * @param ciudad La ciudad de ubicación.
     * @param direccion La dirección física.
     * @param idEmpresa El ID de la empresa propietaria.
     */
    public Sede(Long id, String ciudad, String direccion, Long idEmpresa) {
        this.id = id;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.idEmpresa = idEmpresa;
    }

    /**
     * Constructor para CREAR una nueva sede (sin ID).
     * <p>
     * Este constructor lo usamos para registrar nuevas sedes en la base de datos.
     * El ID será generado automáticamente por la BD.
     *
     * @param ciudad La ciudad de ubicación.
     * @param direccion La dirección física.
     * @param idEmpresa El ID de la empresa propietaria.
     */
    public Sede(String ciudad, String direccion, Long idEmpresa) {
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.idEmpresa = idEmpresa;
    }


    /**
     * Obtiene el identificador único de la sede.
     * @return El ID (Long).
     */
    public Long getId() { return id; }

    /**
     * Obtiene la ciudad de la sede.
     * @return La ciudad (String).
     */
    public String getCiudad() { return ciudad; }

    /**
     * Obtiene la dirección de la sede.
     * @return La dirección (String).
     */
    public String getDireccion() { return direccion; }

    /**
     * Obtiene el ID de la empresa asociada.
     * @return El ID de la empresa (Long).
     */
    public Long getIdEmpresa() { return idEmpresa; }

    /**
     * Devuelve una representación en texto de la sede para la interfaz de usuario.
     * <p>
     * Formato: "Ciudad (Dirección)".
     * Este método es utilizado por componentes visuales como el ComboBox para mostrar
     * la información de manera legible al usuario.
     *
     * @return String formateado descriptivo de la sede.
     */
    @Override
    public String toString() {
        return ciudad + " (" + direccion + ")";
    }
}