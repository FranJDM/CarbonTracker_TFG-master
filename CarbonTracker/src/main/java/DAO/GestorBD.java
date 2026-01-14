package DAO;

import Modelos.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Clase de acceso a datos (DAO) encargada de gestionar la persistencia de la aplicación.
 * <p>
 * Administra la conexión con la base de datos SQLite y realiza todas las operaciones
 * 'CRUD' (Crear, Leer, Actualizar, Borrar) para Usuarios, Empresas y Emisiones.
 * También gestiona la seguridad básica mediante hash de contraseñas.
 */
public class GestorBD {

    // ==========================================
    // 1. CONFIGURACIÓN Y CONEXIÓN
    // ==========================================

    /**
     * Cadena de conexión JDBC para la base de datos SQLite local.
     */
    private static final String URL_BASEDATOS = "jdbc:sqlite:carbon_tracker.db";

    /**
     * Establece y configura la conexión con la base de datos.
     * <p>
     * Es importante activar explícitamente las claves foráneas (Foreign Keys)
     * en SQLite mediante el "PRAGMA foreign_keys = ON" para asegurar
     * la integridad.
     *
     * @return Objeto Connection activo o null si falla la conexión.
     */
    private Connection establecerConexion() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(URL_BASEDATOS);
            try (Statement st = conexion.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException e) {
            System.out.println("Error conectando BD: " + e.getMessage());
        }
        return conexion;
    }

    // ==========================================
    // 2. INICIALIZACIÓN Y ESQUEMA
    // ==========================================

    /**
     * Método auxiliar para actualizar la estructura de la tabla usuarios
     * si la base de datos ya existía previamente sin el campo 'activo' (Base de datos vacía por defecto).
     */
    private void actualizarEsquemaUsuarios(Statement stmt) {
        try {
            // Intentamos añadir la columna. Si ya existe, SQLite lanzará una excepción .
            stmt.execute("ALTER TABLE usuario ADD COLUMN activo INTEGER DEFAULT 1;");
            System.out.println("Esquema actualizado: Columna 'activo' añadida.");
        } catch (SQLException e) {
            // La columna  ya existe
        }
    }

    /**
     * Inicializa la estructura de la base de datos (Tablas y datos por defecto).
     * <p>
     * Crea las tablas 'empresa', 'registro_emisiones', 'rol' y 'usuario' si no existen.
     * Inserta los roles básicos (ADMINISTRADOR, USUARIO, CLIENTE) y un usuario
     * administrador por defecto (admin/admin).
     */
    public void arrancarBD() {
        String filtro = "CREATE TABLE IF NOT EXISTS filtro (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "criterio_busqueda TEXT, " +
                "ordenamiento TEXT, " + // AÑADIDO: Para guardar "Nombre Ascendente"
                "contexto TEXT, " +
                "fecha_hora TEXT DEFAULT (datetime('now', 'localtime')), " +
                "id_usuario INTEGER NOT NULL, " +
                "FOREIGN KEY (id_usuario) REFERENCES usuario(id));";
        // Tabla Empresa
        String empresa = "CREATE TABLE IF NOT EXISTS empresa (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " nombre TEXT NOT NULL,\n"
                + " sector TEXT NOT NULL\n"
                + ");";

        // Tabla Emisiones
        String emision = "CREATE TABLE IF NOT EXISTS registro_emisiones (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " tipo TEXT NOT NULL,\n"
                + " cantidad REAL NOT NULL,\n"
                + " co2e REAL NOT NULL,\n"
                + " fecha TEXT NOT NULL,\n"
                + " id_empresa INTEGER NOT NULL,\n"
                + " FOREIGN KEY (id_empresa) REFERENCES empresa (id) ON DELETE CASCADE\n"
                + ");";

        // Tabla Rol
        String rol = "CREATE TABLE IF NOT EXISTS rol (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " nombre_rol TEXT NOT NULL UNIQUE\n"
                + ");";

        // Tabla Usuario
        String usuario = "CREATE TABLE IF NOT EXISTS usuario (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " nombre_usuario TEXT NOT NULL UNIQUE,\n"
                + " hash_contrasena TEXT NOT NULL,\n"
                + " nombre_completo TEXT NOT NULL,\n"
                + " id_rol INTEGER NOT NULL,\n"
                + " FOREIGN KEY (id_rol) REFERENCES rol (id)\n"
                + ");";
        String sede = "CREATE TABLE IF NOT EXISTS sede (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ciudad TEXT NOT NULL, " +
                "pais TEXT DEFAULT 'España', " +
                "direccion TEXT, " +
                "id_empresa INTEGER NOT NULL, " +
                "FOREIGN KEY (id_empresa) REFERENCES empresa(id) ON DELETE CASCADE);";

        String auditoria = "CREATE TABLE IF NOT EXISTS auditoria (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "accion TEXT NOT NULL, " +
                "fecha_hora TEXT NOT NULL, " +
                "id_usuario INTEGER NOT NULL, " +
                "FOREIGN KEY (id_usuario) REFERENCES usuario(id));";

        //Conexion y lanzamientos
        try (Connection conexion = establecerConexion();
             Statement st = conexion.createStatement()) {
            st.execute(filtro);
            st.execute(empresa);
            st.execute(emision);
            st.execute(rol);
            st.execute(usuario);
            st.execute(sede);
            st.execute(auditoria);
            actualizarEsquemaUsuarios(st);
            // Se rellenan los roles
            st.execute("INSERT OR IGNORE INTO rol(id, nombre_rol) VALUES (1, 'ADMINISTRADOR');");
            st.execute("INSERT OR IGNORE INTO rol(id, nombre_rol) VALUES (2, 'USUARIO');");
            st.execute("INSERT OR IGNORE INTO rol(id, nombre_rol) VALUES (3, 'CLIENTE');");

            // Se crea el usuario admin por defecto para poder testear los valores genéricos.
            //Este usuario se puede sacar y/o cambiar de credenciales para que sea más seguro
            String loginAdmin = truncarPass("admin");
            String insertCredencialesAdmin = "INSERT OR IGNORE INTO usuario(id, nombre_usuario, hash_contrasena, nombre_completo, id_rol) "
                    + "VALUES (1, 'admin', '" + loginAdmin + "', 'Administrador', 1);";
            st.execute(insertCredencialesAdmin);

        } catch (SQLException e) {
            System.out.println("Error inicializando BD: " + e.getMessage());
        }
    }

    // ==========================================
    // 3. SEGURIDAD Y AUTENTICACIÓN
    // ==========================================

    /**
     * Genera un hash de tipo 'SHA-256' a partir de una contraseña en texto plano.
     *
     * @param contrasena La contraseña original.
     * @return Representación hexadecimal del hash SHA-256.
     */
    private String truncarPass(String contrasena) {
        try {
            MessageDigest procesar = MessageDigest.getInstance("SHA-256");
            byte[] hash = procesar.digest(contrasena.getBytes(StandardCharsets.UTF_8));
            StringBuilder semiPass = new StringBuilder();
            for (byte h : hash) {
                String hexadecimal = Integer.toHexString(0xff & h);
                if (hexadecimal.length() == 1) semiPass.append('0');
                semiPass.append(hexadecimal);
            }
            return semiPass.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Valída las credenciales de un usuario en la base de datos.
     *
     * @param nomUsuario El nombre de usuario (el registrado en el login).
     * @param pass La contraseña proporcionada por el usuario (se hasheará para comparar con el texto guardado).
     * @return El objeto Usuario completo si las credenciales son válidas, -null en caso contrario-.
     */
    public Usuario login(String nomUsuario, String pass) {
        String passTruncada = truncarPass(pass);

        // Control de usuario, controla si está activo o no
        String consultaLogin = "SELECT u.id, u.nombre_usuario, u.nombre_completo, u.activo, r.id as id_rol, r.nombre_rol "
                + "FROM usuario u "
                + "JOIN rol r ON u.id_rol = r.id "
                + "WHERE u.nombre_usuario = ? AND u.hash_contrasena = ?";

        try (Connection conexion = establecerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(consultaLogin)) {

            pstmt.setString(1, nomUsuario);
            pstmt.setString(2, passTruncada);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Comprobación de estado
                boolean esActivo = rs.getInt("activo") != 0;

                if (!esActivo) {
                    // Controlamos que está bloqueado para lanzar el mensaje personalizado
                    //Usamos SecurityException que es la función adecuada para este tipo de excepciones
                    throw new SecurityException("ESTÁS BLOQUEADO: Tu cuenta se encuentra bloqueada. No puedes acceder.");
                }

                Rol rol = new Rol(rs.getLong("id_rol"), rs.getString("nombre_rol"));
                return new Usuario(
                        rs.getLong("id"),
                        rs.getString("nombre_usuario"),
                        rs.getString("nombre_completo"),
                        rol,
                        esActivo
                );
            } else {
                return null; // Usuario no encontrado o contraseña incorrecta
            }
        } catch (SQLException e) {
            System.out.println("Error en login: " + e.getMessage());
            return null;
        }
    }

    // ==========================================
    // 4. GESTIÓN DE USUARIOS
    // ==========================================

    /**
     * Registra un nuevo usuario en la base de datos.
     *
     * @param nomUsuario Nombre de usuario.
     * @param pass Contraseña (se almacenará pasada por el hash).
     * @param nomCompleto Nombre real del usuario.
     * @param rol El rol asignado al usuario.
     * @return true si la inserción en la bd fue exitosa, false si hubo error (Por ejemplo: usuario duplicado).
     */
    public boolean crearUsuario(String nomUsuario, String pass, String nomCompleto, Rol rol, Usuario autor) {
        String passHasheado = truncarPass(pass);
        String insertUsuario = "INSERT INTO usuario(nombre_usuario, hash_contrasena, nombre_completo, id_rol) VALUES(?,?,?,?)";
        String insertLog = "INSERT INTO auditoria (accion, fecha_hora, id_usuario) VALUES (?, datetime('now', 'localtime'), ?)";

        Connection conn = null;
        try {
            conn = establecerConexion();
            conn.setAutoCommit(false); // Inicio transacción

            try (PreparedStatement ps = conn.prepareStatement(insertUsuario)) {
                ps.setString(1, nomUsuario);
                ps.setString(2, passHasheado);
                ps.setString(3, nomCompleto);
                ps.setLong(4, rol.getId());
                ps.executeUpdate();
            }

            // Si hay un autor (es decir, lo crea un admin y no es autorregistro), guardamos log
            if (autor != null) {
                try (PreparedStatement psLog = conn.prepareStatement(insertLog)) {
                    psLog.setString(1, "ALTA USUARIO | Nuevo: " + nomUsuario + " (" + rol.getNomRol() + ")");
                    psLog.setLong(2, autor.getId());
                    psLog.executeUpdate();
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            System.out.println("Error al crear usuario: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {}
        }
    }

    /**
     * Recupera la lista de roles disponibles para el registro de usuarios.
     * Se excluye el rol 'ADMINISTRADOR' por seguridad (El sistema siempre tiene que tener un Administrador activo).
     *
     * @return Lista de objetos Rol.
     */
    public List<Rol> getRoles() {
        List<Rol> roles = new ArrayList<>();
        String sql = "SELECT * FROM rol WHERE nombre_rol != 'ADMINISTRADOR'";

        try (Connection conn = establecerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                roles.add(new Rol(rs.getLong("id"), rs.getString("nombre_rol")));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return roles;
    }

    /**
     * Recupera TODOS los usuarios del sistema, incluyendo su estado (Activo/Bloqueado) y Rol.
     */
    public List<Usuario> getTodosLosUsuarios() {
        //Lista de usuarios y consulta
        List<Usuario> lista = new ArrayList<>();
        String listaUsuarios = "SELECT u.*, r.nombre_rol FROM usuario u JOIN rol r ON u.id_rol = r.id";

        try (Connection conexion = establecerConexion();
             Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(listaUsuarios)) {

            while (rs.next()) {
                // Traducimos el estado boolean para 1-0 que es lo que entiende SQLite
                boolean isActivo = rs.getInt("activo") != 0;
                //Añadimos los usuarios con sus datos
                lista.add(new Usuario(
                        rs.getLong("id"),
                        rs.getString("nombre_usuario"),
                        rs.getString("nombre_completo"),
                        new Rol(rs.getLong("id_rol"), rs.getString("nombre_rol")),
                        isActivo
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error recopilando usuarios: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Actualiza los datos de un usuario desde el panel de administración.
     * Permite cambiar nombre, rol, estado y contraseña.
     *
     * @param usuario El objeto usuario con los datos nuevos (excepto la pass).
     * @param nuevaContrasena La nueva contraseña en texto plano. Si es null o vacía, no se cambia la contraseña.
     * @return true si la actualización fue exitosa.
     */
    public boolean actualizarUsuarioAdmin(Usuario usuario, String nuevaContrasena, Usuario autor) {
        boolean cambiarCon = nuevaContrasena != null && !nuevaContrasena.isBlank();
        String modUsuario = "UPDATE usuario SET nombre_usuario=?, nombre_completo=?, id_rol=?, activo=? "
                + (cambiarCon ? ", hash_contrasena=? " : "")
                + "WHERE id=?";
        String insertLog = "INSERT INTO auditoria (accion, fecha_hora, id_usuario) VALUES (?, datetime('now', 'localtime'), ?)";

        Connection conexion = null;
        try {
            conexion = establecerConexion();
            conexion.setAutoCommit(false);

            try (PreparedStatement ps = conexion.prepareStatement(modUsuario)) {
                ps.setString(1, usuario.getNombreUsuario());
                ps.setString(2, usuario.getNombreCompleto());
                ps.setLong(3, usuario.getRol().getId());
                ps.setInt(4, usuario.isActivo() ? 1 : 0);
                int indiceHasheo = 5;
                if (cambiarCon) {
                    ps.setString(indiceHasheo++, truncarPass(nuevaContrasena));
                }
                ps.setLong(indiceHasheo, usuario.getId());
                ps.executeUpdate();
            }

            if (autor != null) {
                try (PreparedStatement psLog = conexion.prepareStatement(insertLog)) {
                    psLog.setString(1, "MODIFICACIÓN USUARIO | ID: " + usuario.getNombreUsuario());
                    psLog.setLong(2, autor.getId());
                    psLog.executeUpdate();
                }
            }

            conexion.commit();
            return true;

        } catch (SQLException e) {
            System.out.println("Error actualizando usuario: " + e.getMessage());
            if (conexion != null) try { conexion.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conexion != null) try { conexion.setAutoCommit(true); conexion.close(); } catch (SQLException ex) {}
        }
    }

    /**
     * Método  para Bloquear/Desbloquear un usuario sin editar el resto de datos.
     */
    public void bloqueoUsuario(Long idUsuario, boolean nuevoEstado) {
        String actBloqueo = "UPDATE usuario SET activo = ? WHERE id = ?";
        try (Connection conexion = establecerConexion();
             PreparedStatement ps = conexion.prepareStatement(actBloqueo)) {
            ps.setInt(1, nuevoEstado ? 1 : 0);
            ps.setLong(2, idUsuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al cambiar de estado: " + e.getMessage());
        }
    }

    /**
     * Recupera TODOS los roles disponibles (incluyendo ADMINISTRADOR).
     * Función para que el administrador asigne su mismo rol a otro usuario
     *
     */
    public List<Rol> getTodosLosRoles() {
        List<Rol> roles = new ArrayList<>();
        String consultaRol = "SELECT * FROM rol";

        try (Connection conexion = establecerConexion();
             Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(consultaRol)) {
            while (rs.next()) {
                roles.add(new Rol(rs.getLong("id"), rs.getString("nombre_rol")));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return roles;
    }

    /**
     * Elimina  un usuario de la base de datos.
     * @param idUsuario ID del usuario a borrar.
     * @return true si se borró, false si falló (por ejemplo, si tiene registros vinculados que impiden borrarlo).
     */
    public boolean borrarUsuario(Long idUsuario, Usuario autor) {
        String selectNombre = "SELECT nombre_usuario FROM usuario WHERE id = ?";
        String borradoUsuario = "DELETE FROM usuario WHERE id = ?";
        String insertLog = "INSERT INTO auditoria (accion, fecha_hora, id_usuario) VALUES (?, datetime('now', 'localtime'), ?)";

        Connection conexion = null;
        try {
            conexion = establecerConexion();
            conexion.setAutoCommit(false);

            // Guardamos el nombre
            String nombreBorrado = "Desconocido";
            try (PreparedStatement psSel = conexion.prepareStatement(selectNombre)) {
                psSel.setLong(1, idUsuario);
                ResultSet rs = psSel.executeQuery();
                if (rs.next()) nombreBorrado = rs.getString("nombre_usuario");
            }

            // Borramos el registro si es necesario
            try (PreparedStatement ps = conexion.prepareStatement(borradoUsuario)) {
                ps.setLong(1, idUsuario);
                int filas = ps.executeUpdate();
                if (filas == 0) throw new SQLException("No se borró ningún registro");
            }

            // Mostramos el Log
            if (autor != null) {
                try (PreparedStatement psLog = conexion.prepareStatement(insertLog)) {
                    psLog.setString(1, "BAJA USUARIO | Eliminado: " + nombreBorrado);
                    psLog.setLong(2, autor.getId());
                    psLog.executeUpdate();
                }
            }

            conexion.commit();
            return true;

        } catch (SQLException e) {
            System.out.println("Error borrando usuario (FK restriction): " + e.getMessage());
            if (conexion != null) try { conexion.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conexion != null) try { conexion.setAutoCommit(true); conexion.close(); } catch (SQLException ex) {}
        }
    }

    // ==========================================
    // 5. GESTIÓN DE EMPRESAS
    // ==========================================

    /**
     * Inserta una nueva empresa en la base de datos.
     *
     * @param empresa El objeto Empresa que se va a guardar (sin ID).
     * @return El objeto Empresa que se ha creado, con el ID generado asignado, o devolviendo null si falla.
     */
    public Empresa agregarEmpresa(Empresa empresa) {
        String yaExiste = "SELECT id FROM empresa WHERE LOWER(nombre) = LOWER(?)";
        String insertEmpresa = "INSERT INTO empresa(nombre, sector) VALUES(?,?)";

        try (Connection conexion = establecerConexion()) {

            // Comprobamos si existe la empresa que se intenta añadir
            try (PreparedStatement psCheck = conexion.prepareStatement(yaExiste)) {
                psCheck.setString(1, empresa.getNombreEmpresa());
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) {
                    System.out.println("Error: Empresa ya existente.");
                    return null;
                }
            }

            // Si no hay duplicados, insertamos el registro
            try (PreparedStatement ps = conexion.prepareStatement(insertEmpresa)) {
                ps.setString(1, empresa.getNombreEmpresa());
                ps.setString(2, empresa.getSector());
                ps.executeUpdate();
            }


            try (Statement st = conexion.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    empresa.setId(rs.getLong(1));
                }
            }
            return empresa;

        } catch (SQLException e) {
            System.out.println("Error al agregar empresa: " + e.getMessage());
            return null;
        }
    }

    /**
     * Actualiza los datos (nombre y sector) de una empresa existente si es necesario.
     *
     * @param empresa El objeto Empresa con los datos modificados.
     */
    public void logActualizarEmpresa(Empresa empresa) {
        String updateInfoEmpresa = "UPDATE empresa SET nombre = ?, sector = ? WHERE id = ?";
        try (Connection conexion = establecerConexion();
             PreparedStatement ps = conexion.prepareStatement(updateInfoEmpresa)) {
            ps.setString(1, empresa.getNombreEmpresa());
            ps.setString(2, empresa.getSector());
            ps.setLong(3, empresa.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Actualiza los datos de una empresa en la base de datos y registra automáticamente la acción en la auditoría.
     *
     * El método realiza dos operaciones:
     * <ol>
     * <li><b>UPDATE:</b> Modifica el nombre y el sector de la empresa basándose en su ID.</li>
     * <li><b>AUDITORÍA:</b> Si se proporciona un usuario activo, inserta un registro en la tabla de auditoría detallando la modificación.</li>
     * </ol>
     *
     *
     * @param empresa       El objeto {@link Empresa} que contiene los nuevos datos (nombre, sector) y el ID de la empresa a modificar.
     * @param usuarioActivo El objeto {@link Usuario} responsable de la acción. Si es {@code null}, la actualización se realiza pero no se genera registro de auditoría.
     */
    public void logActualizarEmpresa(Empresa empresa, Usuario usuarioActivo) {
        String updateEmpresa = "UPDATE empresa SET nombre = ?, sector = ? WHERE id = ?";
        String insertAuditoriaEmpresa = "INSERT INTO auditoria (accion, fecha_hora, id_usuario) VALUES (?, datetime('now', 'localtime'), ?)";

        try (Connection conexion = establecerConexion()) {
            try (PreparedStatement pstmt = conexion.prepareStatement(updateEmpresa)) {
                pstmt.setString(1, empresa.getNombreEmpresa());
                pstmt.setString(2, empresa.getSector());
                pstmt.setLong(3, empresa.getId());
                pstmt.executeUpdate();
            }

            if (usuarioActivo != null) {
                try (PreparedStatement ps = conexion.prepareStatement(insertAuditoriaEmpresa)) {
                    String msgLog = "MODIFICACIÓN EMPRESA | Nombre: " + empresa.getNombreEmpresa();
                    ps.setString(1, msgLog);
                    ps.setLong(2, usuarioActivo.getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Elimina una empresa de la base de datos.
     * Nota: Por razones de diseño, como solo el administrador tiene acceso a esta función
     * se considera lógico que también se borren sus emisiones asociadas.
     *
     * @param idEmpresa El ID de la empresa a eliminar.
     */
    public void borrarEmpresa(Long idEmpresa) {
        String borradoEmpresa = "DELETE FROM empresa WHERE id = ?";
        try (Connection conexion = establecerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(borradoEmpresa)) {
            pstmt.setLong(1, idEmpresa);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Recupera empresas filtradas por nombre o sector y calcula su huella total de CO2.
     *
     * @param terminoBusqueda Texto para filtrar por nombre o sector.
     * @return Lista de empresas con el campo auxiliar de CO2 calculado.
     */
    public List<Empresa> getTodasEmpresas(String terminoBusqueda) {
        String consultaEmpresas = "SELECT c.id, c.nombre, c.sector, COALESCE(SUM(e.co2e), 0) as total_co2e "
                + "FROM empresa c "
                + "LEFT JOIN registro_emisiones e ON c.id = e.id_empresa "
                + "WHERE (c.nombre LIKE ? OR c.sector LIKE ?) "
                + "GROUP BY c.id, c.nombre, c.sector";

        List<Empresa> empresas = new ArrayList<>();
        try (Connection conexion = establecerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(consultaEmpresas)) {
            String patronBusqueda = "%" + terminoBusqueda + "%";
            pstmt.setString(1, patronBusqueda);
            pstmt.setString(2, patronBusqueda);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Empresa empresa = new Empresa(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("sector")
                );
                empresa.setAuxiliarAlmacenC02(rs.getDouble("total_co2e"));
                empresas.add(empresa);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return empresas;
    }

    /**
     * Recupera todas las empresas ordenadas por nombre, sin calcular las emisiones.
     * Diseñado para listados simples o combos.
     *
     * @return Lista de todas las empresas.
     */
    public List<Empresa> getTodasEmpresas() {
        String consulta = "SELECT id, nombre, sector FROM empresa ORDER BY nombre ASC";
        List<Empresa> empresas = new ArrayList<>();
        try (Connection conexion = establecerConexion();
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(consulta)) {
            while (rs.next()) {
                empresas.add(new Empresa(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getString("sector")
                ));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return empresas;
    }

    // ==========================================
    // 6. GESTIÓN DE EMISIONES
    // ==========================================

    /**
     * Registra una nueva emisión para una empresa.
     *
     * @param emisiones Objeto con los datos de la emisión.
     * @return El objeto creado con su ID, o el original si falla la recuperación.
     */
    public Emisiones nuevaEmision(Emisiones emisiones) {

        String insertEmision = "INSERT INTO registro_emisiones(tipo, cantidad, co2e, fecha, id_empresa) VALUES(?,?,?,?,?)";

        try (Connection conexion = establecerConexion();
             PreparedStatement ps = conexion.prepareStatement(insertEmision)) {

            ps.setString(1, emisiones.getTipoEmision());
            ps.setDouble(2, emisiones.getCantidadEmision());
            ps.setDouble(3, emisiones.getCo2e());
            ps.setString(4, emisiones.getFecha().toString());
            ps.setLong(5, emisiones.getIdEmpresa());
            ps.executeUpdate();

            try (Statement stmt = conexion.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return getEmisionesPorId(rs.getLong(1));
                }
            }
            return emisiones;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Actualiza una emisión existente y guarda un registro en la auditoría.
     * Utiliza una -transacción- para asegurar la integridad de los datos.
     *
     * @param emision Objeto Emision con los nuevos datos (cantidad, co2, fecha, etc).
     * @param usuarioActual El usuario que realiza la modificación.
     * @return true si se actualizó correctamente.
     */
    public boolean actualizarEmision(Emisiones emision, Usuario usuarioActual) {
        //Se actualizan los datos de la emisión y se plasma en el control de auditoria
        String updateEmision = "UPDATE registro_emisiones SET tipo = ?, cantidad = ?, co2e = ?, fecha = ?, id_empresa = ? WHERE id = ?";
        String insertAuditoria = "INSERT INTO auditoria (accion, fecha_hora, id_usuario) VALUES (?, datetime('now', 'localtime'), ?)";
        //Conexión
        Connection conexion = null;
        try {
            conexion = establecerConexion();
            conexion.setAutoCommit(false);

            try (PreparedStatement ps = conexion.prepareStatement(updateEmision)) {
                ps.setString(1, emision.getTipoEmision());
                ps.setDouble(2, emision.getCantidadEmision());
                ps.setDouble(3, emision.getCo2e());
                ps.setString(4, emision.getFecha().toString());
                ps.setLong(5, emision.getIdEmpresa());
                ps.setLong(6, emision.getId()); // ID actual

                int filasModi = ps.executeUpdate();
                if(filasModi == 0) {
                    conexion.rollback();
                    return false;
                }
            }

            if (usuarioActual != null) {
                try (PreparedStatement ps = conexion.prepareStatement(insertAuditoria)) {
                    //Interceptamos el número, por si fuera necesario formatearlo y evitar la vista exponencial
                    String co2Fmt = String.format("%.2f", emision.getCo2e());
                    String msgLog = "MODIFICACIÓN EMISIÓN | ID: " + emision.getId() + " | Nuevo CO2e: " + co2Fmt;
                    ps.setString(1, msgLog);
                    ps.setLong(2, usuarioActual.getId());
                    ps.executeUpdate();
                }
            }

            conexion.commit();
            return true;

        } catch (SQLException e) {
            System.out.println("Error actualizando emisión: " + e.getMessage());
            if (conexion != null) try { conexion.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conexion != null) try { conexion.setAutoCommit(true); conexion.close(); } catch (SQLException ex) {}
        }
    }

    /**
     * Elimina un registro de emisión específico.
     *
     * @param idEmision ID de la emisión a borrar.
     */
    public void borrarEmision(Long idEmision) {
        String eliminarEmision = "DELETE FROM registro_emisiones WHERE id = ?";
        try (Connection conexion = establecerConexion();
             PreparedStatement ps = conexion.prepareStatement(eliminarEmision)) {
            ps.setLong(1, idEmision);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Muestra todas las emisiones registradas, filtrando por nombre de empresa, tipo o fecha.
     *
     * @param terminoBusqueda Término para filtrar.
     * @return Lista de emisiones coincidentes.
     */
    public List<Emisiones> getTodasEmisiones(String terminoBusqueda) {
        String consulta = "SELECT e.*, c.nombre as nombreEmpresa "
                + "FROM registro_emisiones e "
                + "JOIN empresa c ON e.id_empresa = c.id "
                + "WHERE (c.nombre LIKE ? OR e.tipo LIKE ? OR e.fecha LIKE ?) ";
        return getEmisionConsulta(consulta, terminoBusqueda);
    }

    /**
     * Muestra las emisiones asociadas a una empresa específica.
     *
     * @param idEmpresa ID de la empresa.
     * @param terminoBusqueda Filtro extra por tipo o fecha.
     * @return Lista de emisiones de esa empresa.
     */
    public List<Emisiones> getEmissionsByCompanyId(Long idEmpresa, String terminoBusqueda) {
        String consulta = "SELECT e.*, c.nombre as nombreEmpresa "
                + "FROM registro_emisiones e "
                + "JOIN empresa c ON e.id_empresa = c.id "
                + "WHERE e.id_empresa = ? "
                + "AND (c.nombre LIKE ? OR e.tipo LIKE ? OR e.fecha LIKE ?)";
        return getEmisionConsulta(consulta, terminoBusqueda, idEmpresa);
    }

    /**
     * Muestra una emisión específica filtrado por su ID.
     *
     * @param id El ID de la emisión.
     * @return El objeto de tipo Emision que se haya encontrado o bien null.
     */
    private Emisiones getEmisionesPorId(long id) {
        String consultaEmisionId = "SELECT * FROM registro_emisiones WHERE id = ?";
        try (Connection conexion = establecerConexion();
             PreparedStatement ps = conexion.prepareStatement(consultaEmisionId)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Emisiones(
                        rs.getLong("id"),
                        rs.getString("tipo"),
                        rs.getDouble("cantidad"),
                        rs.getDouble("co2e"),
                        rs.getString("fecha"),
                        rs.getLong("id_empresa")
                );
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    /**
     * Mapea un registro de emisiones cruzas por búsqueda de empresa
     * @param idEmpresa El ID de la empresa
     * @return el report de emision filtrado de la empresa seleccionada.
     */
    public List<Map.Entry<String, Double>> getReporteEmisionesPorEmpresa(Long idEmpresa) {
        String consultaEmisionEmpresa = "SELECT tipo, SUM(co2e) AS total_co2e " +
                "FROM registro_emisiones " +
                "WHERE id_empresa = ? " +
                "GROUP BY tipo " +
                "ORDER BY total_co2e DESC";

        List<Map.Entry<String, Double>> reporte = new ArrayList<>();

        try (Connection conexion = establecerConexion();
             PreparedStatement ps = conexion.prepareStatement(consultaEmisionEmpresa)) {
            // El ID de la empresa
            ps.setLong(1, idEmpresa);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tipo = rs.getString("tipo");
                    double totalCo2e = rs.getDouble("total_co2e");
                    reporte.add(new AbstractMap.SimpleEntry<>(tipo, totalCo2e));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al generar el reporte de emisiones: " + e.getMessage());
        }

        return reporte;
    }

    /**
     * Método auxiliar  para ejecutar consultas de emisiones y mapear los resultados que muestre.
     *
     */
    private List<Emisiones> getEmisionConsulta(String consulta, String terminoBusqueda, Object... parametro) {
        List<Emisiones> emisiones = new ArrayList<>();
        try (Connection conexion = establecerConexion();
             PreparedStatement ps = conexion.prepareStatement(consulta)) {
            int indice = 1;
            for (Object parametros : parametro) {
                ps.setObject(indice++, parametros);
            }
            String patronBusqueda = "%" + terminoBusqueda + "%";
            ps.setString(indice++, patronBusqueda);
            ps.setString(indice++, patronBusqueda);
            ps.setString(indice++, patronBusqueda);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Emisiones emision = new Emisiones(
                        rs.getLong("id"),
                        rs.getString("tipo"),
                        rs.getDouble("cantidad"),
                        rs.getDouble("co2e"),
                        rs.getString("fecha"),
                        rs.getLong("id_empresa")
                );
                emision.setNombreEmpresa(rs.getString("nombreEmpresa"));
                emisiones.add(emision);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return emisiones;
    }

    // ==========================================
    // 7. GESTIÓN DE SEDES Y AUDITORÍA
    // ==========================================

    /**
     * Registra una nueva sede en el sistema y genera automáticamente una entrada detallada en el registro de auditoría.
     *
     * Este método implementa una <b>transacción</b> para garantizar la integridad de los datos.
     * El flujo de ejecución es:
     * <ol>
     * <li><b>INSERT Sede:</b> Se guarda la nueva sede en la base de datos (estableciendo 'España' como país por defecto).</li>
     * <li><b>INSERT Auditoría:</b> Se registra la acción asociándola al usuario activo, incluyendo el nombre de la empresa y la ubicación en el mensaje.</li>
     * <li><b>COMMIT:</b> Si ambas operaciones tienen éxito, se confirman los cambios permanentemente.</li>
     * </ol>
     * En caso de cualquier error SQL durante el proceso, se ejecuta un <b>rollback</b> para deshacer cualquier cambio parcial.
     *
     *
     * @param sede          Objeto {@link Sede} que contiene los datos de ubicación (ciudad, dirección) y el ID de la empresa asociada.
     * @param usuarioActivo El objeto {@link Usuario} que ejecuta la operación (requerido para el registro de auditoría).
     * @param nombreEmpresa El nombre de la empresa a la que se asocia la sede (utilizado para construir el mensaje descriptivo del log).
     * @return {@code true} si la transacción se completó exitosamente; {@code false} si ocurrió un error y se realizó un rollback.
     */
    public boolean registrarSedeConAuditoria(Sede sede, Usuario usuarioActivo, String nombreEmpresa) {
        String insertSede = "INSERT INTO sede (ciudad, pais, direccion, id_empresa) VALUES (?, 'España', ?, ?)";
        String insertLogAuditoria = "INSERT INTO auditoria (accion, fecha_hora, id_usuario) VALUES (?, datetime('now', 'localtime'), ?)";

        Connection conexion = null;
        try {
            conexion = establecerConexion();
            conexion.setAutoCommit(false);

            // Insercción de la sede
            try (PreparedStatement pstSede = conexion.prepareStatement(insertSede)) {
                pstSede.setString(1, sede.getCiudad());
                pstSede.setString(2, sede.getDireccion());
                pstSede.setLong(3, sede.getIdEmpresa());
                pstSede.executeUpdate();
            }

            // Insercción del log de la auditoria
            try (PreparedStatement ps = conexion.prepareStatement(insertLogAuditoria)) {
                String msgLog = "ALTA SEDE | Para: " + nombreEmpresa + " | Ubicación: " + sede.getCiudad();

                ps.setString(1, msgLog);
                ps.setLong(2, usuarioActivo.getId());
                ps.executeUpdate();
            }
            //Se guarda
            conexion.commit();
            return true;

        } catch (SQLException e) {
            System.out.println("Error transacción: " + e.getMessage());
            if (conexion != null) try { conexion.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conexion != null) try { conexion.setAutoCommit(true); conexion.close(); } catch (SQLException ex) {}
        }
    }

    /**
     * Actualiza la información de una sede existente y registra la operación en la tabla de auditoría.
     *
     * Este método utiliza una <b>transacción</b> de base de datos. Se realizan dos operaciones:
     * <ol>
     * <li>UPDATE: Actualiza la ciudad y dirección de la sede.</li>
     * <li>INSERT: Crea un registro en la tabla de auditoría indicando qué usuario realizó la acción.</li>
     * </ol>
     * Si alguna de las dos operaciones falla, se realiza un <i>rollback</i> y no se guardan los cambios.
     *
     *
     * @param sede          El objeto {@link Sede} con los datos actualizados (ciudad, dirección) y su ID correspondiente.
     * @param usuario       El objeto {@link Usuario} que está realizando la modificación (necesario para el log de auditoría).
     * @param nombreEmpresa El nombre de la empresa a la que pertenece la sede (utilizado para construir el mensaje del log).
     * @return {@code true} si la actualización y el registro de auditoría fueron exitosos; {@code false} si ocurrió algún error SQL.
     */
    public boolean actualizarSede(Sede sede, Usuario usuario, String nombreEmpresa) {
        String updateSede = "UPDATE sede SET ciudad = ?, direccion = ? WHERE id = ?";
        String insertLogAudtioria = "INSERT INTO auditoria (accion, fecha_hora, id_usuario) VALUES (?, datetime('now', 'localtime'), ?)";
        Connection conexion = null;
        try {
            conexion = establecerConexion();
            conexion.setAutoCommit(false);

            try (PreparedStatement psActualizar = conexion.prepareStatement(updateSede)) {
                psActualizar.setString(1, sede.getCiudad());
                psActualizar.setString(2, sede.getDireccion());
                psActualizar.setLong(3, sede.getId()); // Usamos el ID para el WHERE
                psActualizar.executeUpdate();
            }
            try (PreparedStatement psModificar = conexion.prepareStatement(insertLogAudtioria)) {

                String msgLog = "MODIFICACIÓN SEDE | Empresa: " + nombreEmpresa + " | Nueva Ubicación: " + sede.getCiudad();
                psModificar.setString(1, msgLog);
                psModificar.setLong(2, usuario.getId());
                psModificar.executeUpdate();
            }

            conexion.commit();
            return true;
        } catch (SQLException e) {
            if (conexion != null) try { conexion.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            if (conexion != null) try { conexion.setAutoCommit(true); conexion.close(); } catch (SQLException ex) {}
        }
    }


    /**
     * Elimina una una sede existente y registra la operación en la tabla de auditoría.
     *
     * Este método utiliza una <b>transacción</b> de base de datos. Se realizan dos operaciones:
     * <ol>
     * <li>UPDATE: Elimina el registro existente de una sede.</li>
     * </ol>
     * Si alguna de las dos operaciones falla, se realiza un <i>rollback</i> y no se guardan los cambios.
     *
     * @param idSede        El ID de registro con el que se asocia la sede
     * @param ciudadSede    El nombre de la ciudad a la que está asociada la sede
     * @param usuario       El objeto {@link Usuario} que está realizando la modificación (necesario para el log de auditoría).
     * @param nombreEmpresa El nombre de la empresa a la que pertenece la sede (utilizado para construir el mensaje del log).
     * @return {@code true} si la actualización y el registro de auditoría fueron exitosos; {@code false} si ocurrió algún error SQL.
     */
    public boolean borrarSede(Long idSede, String ciudadSede, Usuario usuario, String nombreEmpresa) {
        String borrarSede = "DELETE FROM sede WHERE id = ?";
        String insertLog = "INSERT INTO auditoria (accion, fecha_hora, id_usuario) VALUES (?, datetime('now', 'localtime'), ?)";

        Connection conexion = null;
        try {
            conexion = establecerConexion();
            conexion.setAutoCommit(false);

            try (PreparedStatement ps = conexion.prepareStatement(borrarSede)) {
                ps.setLong(1, idSede);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conexion.prepareStatement(insertLog)) {
                String msgLog = "BAJA SEDE | Empresa: " + nombreEmpresa + " | Ciudad: " + ciudadSede;
                ps.setString(1, msgLog);
                ps.setLong(2, usuario.getId());
                ps.executeUpdate();
            }

            conexion.commit();
            return true;
        } catch (SQLException e) {
            if (conexion != null) try { conexion.rollback(); } catch (SQLException ex) {}
            System.out.println("Error borrando sede: " + e.getMessage());
            return false;
        } finally {
            if (conexion != null) try { conexion.setAutoCommit(true); conexion.close(); } catch (SQLException ex) {}
        }
    }
    /**
     * Obtiene una lista de todas las sedes asociadas a una empresa específica.
     * <p>
     * Realiza una consulta a la base de datos filtrando por el ID de la empresa objetivo.
     * En caso de error de conexión o SQL, se captura la excepción y se retorna una lista vacía.
     * </p>
     *
     * @param idEmpresa El ID de la empresa de la cual se quieren buscar las sedes.
     * @return Una lista de objetos {@link Sede}. Retorna una lista vacía si no se encuentran resultados o si ocurre un error.
     */
    public List<Sede> getSedesPorEmpresa(Long idEmpresa) {
        List<Sede> lista = new ArrayList<>();
        String consultaSedeEmpresa = "SELECT * FROM sede WHERE id_empresa = ?";

        try (Connection conexion = establecerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(consultaSedeEmpresa)) {
            pstmt.setLong(1, idEmpresa);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                lista.add(new Sede(
                        rs.getLong("id"),
                        rs.getString("ciudad"),
                        rs.getString("direccion"),
                        rs.getLong("id_empresa")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al listar sedes: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Recupera el historial completo de registros de auditoría almacenados en la base de datos.
     * <p>
     * Esta consulta utiliza una sentencia <b>JOIN</b> para combinar la tabla de auditoría con la tabla de usuarios,
     * permitiendo obtener el <i>nombre de usuario</i> legible en lugar de solo su ID.
     * </p>
     * <p>
     * Los resultados se ordenan cronológicamente de forma descendente (<b>ORDER BY fecha_hora DESC</b>),
     * mostrando primero los eventos más recientes.
     * </p>
     *
     * @return Una lista de objetos {@link AuditoriaLog}. Si ocurre un error SQL, devuelve una lista vacía o parcialmente llena.
     */
    public List<AuditoriaLog> getLogsAuditoria() {
        String consultaLog = "SELECT a.id, a.accion, a.fecha_hora, u.nombre_usuario " +
                "FROM auditoria a " +
                "JOIN usuario u ON a.id_usuario = u.id " +
                "ORDER BY a.fecha_hora DESC"; // Lo más reciente primero

        List<AuditoriaLog> msgLogs = new ArrayList<>();

        try (Connection conexion = establecerConexion();
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(consultaLog)) {

            while (rs.next()) {
                msgLogs.add(new AuditoriaLog(
                        rs.getLong("id"),
                        rs.getString("accion"),
                        rs.getString("fecha_hora"),
                        rs.getString("nombre_usuario")
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error recuperando auditoría: " + e.getMessage());
        }
        return msgLogs;
    }

    // ==========================================
    // 8. DATOS DE PRUEBA
    // ==========================================

    /**
     * Inserta datos de prueba para facilitar la corrección y el testeo.
     * Crea: Usuarios de cada rol, 4 empresas, emisiones y sedes.
     */
    public void cargarDatosDemo() {
        System.out.println("--- Iniciando Carga de Datos Demo ---");
        List<Rol> roles = getRoles();
        Rol rolAdmin = roles.stream().filter(r -> r.getNomRol().equalsIgnoreCase("ADMINISTRADOR")).findFirst().orElse(null);
        Rol rolUsuario = roles.stream().filter(r -> r.getNomRol().equalsIgnoreCase("USUARIO")).findFirst().orElse(null);
        Rol rolCliente = roles.stream().filter(r -> r.getNomRol().equalsIgnoreCase("CLIENTE")).findFirst().orElse(null);

        // MODIFICADO: Añadido 'null' como 5º parámetro (autor) porque no hay usuario logueado
        if(rolAdmin != null) crearUsuario("admin", "admin", "Profesor Admin", rolAdmin, null);
        if(rolUsuario != null) crearUsuario("empleado", "1234", "Empleado Test", rolUsuario, null);
        if(rolCliente != null) crearUsuario("cliente", "1234", "Cliente Visita", rolCliente, null);

        String[][] datosEmpresas = {{"TechSolar Solutions", "Energía"}, {"Logística Rápida S.L.", "Transporte"}, {"AgroCultivos Bio", "Agricultura"}, {"Construcciones Norte", "Construcción"}};
        for (String[] datos : datosEmpresas) {
            String nombre = datos[0];
            if (getTodasEmpresas(nombre).isEmpty()) {
                Empresa nuevaEmp = new Empresa(nombre, datos[1]);
                agregarEmpresa(nuevaEmp);
                List<Empresa> recuperada = getTodasEmpresas(nombre);
                if (!recuperada.isEmpty()) {
                    Empresa empConId = recuperada.get(0);
                    Long id = empConId.getId();
                    nuevaEmision(new Emisiones("Consumo Eléctrico", 500.0, 120.5, id));
                    nuevaEmision(new Emisiones("Flota Vehículos", 1000.0, 2500.0, id));
                    nuevaEmision(new Emisiones("Generadores Diesel", 200.0, 800.0, id));
                    nuevaEmision(new Emisiones("Residuos Industriales", 50.0, 100.0, id));
                    registrarSedeConAuditoria(new Sede("Madrid", "C/ Principal 1", id), new Usuario(1L, "admin", "admin", rolAdmin), nombre);
                    registrarSedeConAuditoria(new Sede("Barcelona", "Av. Diagonal 200", id), new Usuario(1L, "admin", "admin", rolAdmin), nombre);
                }
            }
        }
        System.out.println("--- Datos Demo Cargados ---");
    }


    // ----------------------------------------
    // Método auxiliar de filtro para crear el registro en la BD
    // ----------------------------------------

    public void registrarFiltro(String criterio, String orden, String contexto, Usuario usuario) {
        String sql = "INSERT INTO filtro (criterio_busqueda, ordenamiento, contexto, id_usuario) VALUES (?, ?, ?, ?)";
        try (Connection conn = establecerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (criterio == null || criterio.isBlank()) ? "Todo" : criterio);
            ps.setString(2, orden);
            ps.setString(3, contexto);
            ps.setLong(4, usuario.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error log filtro: " + e.getMessage());
        }
    }

    /**
     * Recupera el historial de filtros usados en los informes.
     * Reutilizamos la clase AuditoriaLog para mostrar los datos en la tabla (hack rápido y limpio).
     */

    public ObservableList<Modelos.AuditoriaLog> getHistorialFiltros() {
        ObservableList<Modelos.AuditoriaLog> lista = FXCollections.observableArrayList();

        String sql = "SELECT f.fecha_hora, u.nombre_usuario, f.contexto, f.criterio_busqueda, f.ordenamiento " +
                "FROM filtro f " +
                "JOIN usuario u ON f.id_usuario = u.id " +
                "ORDER BY f.id DESC";

        try (Connection conn = establecerConexion();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String fecha = rs.getString("fecha_hora");
                String usuario = rs.getString("nombre_usuario");

                String contexto = rs.getString("contexto");
                String criterio = rs.getString("criterio_busqueda");
                String orden = rs.getString("ordenamiento");

                // Montamos el "query" de los parámetros de filtrado
                String detalle = String.format("[%s] Buscó: '%s' | Orden: %s", contexto, criterio, orden);
                lista.add(new Modelos.AuditoriaLog(0L, detalle, fecha, usuario));
            }
        } catch (SQLException e) {
            System.out.println("Error leyendo filtros: " + e.getMessage());
        }
        return lista;
    }}