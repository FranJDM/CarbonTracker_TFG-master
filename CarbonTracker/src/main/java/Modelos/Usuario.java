package Modelos;

/**
 * Representa a un usuario dentro del sistema.
 * * Esta clase almacena la información esencial de un usuario, incluyendo su
 * identificación, nombre de usuario, nombre completo y el rol asignado.
 */
public class Usuario {

    /**
     * Identificador único del usuario (ID).
     */
    private Long id;

    /**
     * Nombre de usuario (login) utilizado para la autenticación.
     */
    private String nombreUsuario;

    /**
     * Nombre completo real del usuario.
     */
    private String nombreCompleto;

    /**
     * El objeto Rol que define los permisos y el nivel de acceso del usuario.
     */
    private Rol rol;
    /**
     * Control de esta stado del usuario en caso de ser activo o bloqueado
     */
    private boolean activo;

    /**
     * Construye una nueva instancia de Usuario.
     *
     * @param id El identificador único para el usuario.
     * @param nombreUsuario El nombre de usuario (login).
     * @param nombreCompleto El nombre completo del usuario.
     * @param rol El rol asignado al usuario.
     * @param activo El estado del usuario, está activo o bloqueado
     */

    public Usuario(Long id, String nombreUsuario, String nombreCompleto, Rol rol,boolean activo) {
        this.id = id;
        this.nombreUsuario = nombreUsuario;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.activo = activo;
    }

    /**
     * Constructor por defecto para el Usuario, no controla si está activo o no.
     *
     * @param id El identificador único para el usuario.
     * @param nombreUsuario El nombre de usuario (login).
     * @param nombreCompleto El nombre completo del usuario.
     * @param rol El rol asignado al usuario.
     */    public Usuario(Long id, String nombreUsuario, String nombreCompleto, Rol rol) {
        this(id, nombreUsuario, nombreCompleto, rol, true);
    }

    // Getters

    /**
     * Obtiene el identificador único (ID) del usuario.
     *
     * @return El ID del usuario (Long).
     */
    public Long getId() { return id; }

    /**
     * Obtiene el nombre de usuario (login) del usuario.
     *
     * @return El nombre de usuario (String).
     */
    public String getNombreCompleto() { return nombreUsuario; }

    /**
     * Obtiene el nombre completo real del usuario.
     *
     * @return El nombre completo (String).
     */
    public String getNombreUsuario() { return nombreCompleto; }

    /**
     * Obtiene el rol asignado al usuario.
     *
     * @return El objeto Rol del usuario.
     */
    public Rol getRol() { return rol; }
    /**
     * Obtiene el estado asignado al usuario.
     *
     * @return El estado del usuario
     */
    public boolean isActivo() { return activo; }

    /**
     * Establece el estado del usuario
     */
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() { return nombreUsuario; }
}

