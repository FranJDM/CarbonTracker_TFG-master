package Modelos;

/**
 * Representa un rol dentro del sistema, que define permisos y acceso.
 * <p>
 * Los roles asignados han sido "ADMINISTRADOR", "USUARIO" o "CLIENTE".
 */
public class Rol {

    /**
     * Identificador único (ID) del rol.
     */
    private Long id;

    /**
     * Nombre descriptivo del rol ADMINISTRADOR, USUARIO, CLIENTE.
     */
    private String nomRol;

    /**
     * Construye una nueva instancia de Rol.
     *
     * @param id El identificador único para el rol.
     * @param nomRol El nombre descriptivo del rol.
     */
    public Rol(Long id, String nomRol) {
        this.id = id;
        this.nomRol = nomRol;
    }

    // Getters

    /**
     * Obtiene el identificador único (ID) del rol.
     *
     * @return El ID del rol (Long).
     */
    public Long getId() { return id; }

    /**
     * Obtiene el nombre descriptivo del rol.
     *
     * @return El nombre del rol (String).
     */
    public String getNomRol() { return nomRol; }

    /**
     * Devuelve la representación en cadena (String) del rol.
     * <p>
     * Este método se sobrescribe para que, al mostrar un objeto Rol
     * en componentes de UI (como una caja de selección), se muestre
     * directamente el nombre del rol ({@code nomRol}).
     *
     * @return El nombre del rol ({@code nomRol}).
     */
    @Override
    public String toString() {
        return nomRol; // Esto es para el registro de ComboBox
    }
}