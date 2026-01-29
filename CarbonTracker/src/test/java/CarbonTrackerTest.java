

import DAO.GestorBD;
import Modelos.*;
import org.junit.jupiter.api.*;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Fran
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CarbonTrackerTest {

    private static GestorBD gestorBD;

    @BeforeAll
    static void setup() {
        gestorBD = new GestorBD();

        // Borramos la BD vieja para empezar limpios
        File dbFile = new File("carbon_tracker.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
        gestorBD.arrancarBD();
    }

    // ==========================================
    // BATERÍA DE PRUEBAS DE UNIDAD (10 Pruebas)
    // ==========================================

    @Test
    @Order(1)
    @DisplayName("PU-01: Modelo Empresa - Constructor y Getters")
    void testModeloEmpresa() {
        Empresa emp = new Empresa("Tesla", "Automoción");
        assertAll("Propiedades de Empresa",
                () -> assertEquals("Tesla", emp.getNombreEmpresa()),
                () -> assertEquals("Automoción", emp.getSector())
        );
    }

    @Test
    @Order(2)
    @DisplayName("PU-02: Modelo Empresa - toString Formato Estándar")
    void testEmpresaToString() {
        Empresa emp = new Empresa("Tesla", "Automoción");
        // Verifica que el formato sea "Nombre (Sector)"
        assertEquals("Tesla (Automoción)", emp.toString());
    }

    @Test
    @Order(3)
    @DisplayName("PU-03: Modelo Empresa - toString con Emisiones")
    void testEmpresaToStringConCO2() {
        Empresa emp = new Empresa("Tesla", "Automoción");
        emp.setAuxiliarAlmacenC02(150.55);

        String resultado = emp.toString();

        boolean formatoConPunto = resultado.contains("150.55 kgCO2e");
        boolean formatoConComa = resultado.contains("150,55 kgCO2e");

        assertTrue(formatoConPunto || formatoConComa,
                "El toString falló. Se esperaba '150.55' o '150,55'. Recibido: " + resultado);
    }

    @Test
    @Order(4)
    @DisplayName("PU-04: Modelo Rol - Verificación")
    void testModeloRol() {
        Rol rol = new Rol(1L, "ADMINISTRADOR");
        assertEquals("ADMINISTRADOR", rol.getNomRol());
    }

    @Test
    @Order(5)
    @DisplayName("PU-05: Modelo Emisiones - Fecha Automática")
    void testModeloEmisionesFecha() {
        // Al crear una nueva emisión sin ID, la fecha debe ser HOY
        Emisiones em = new Emisiones("Luz", 100, 50, 1L);
        assertNotNull(em.getFecha());
        assertEquals(LocalDate.now(), em.getFecha());
    }

    @Test
    @Order(6)
    @DisplayName("PU-06: Modelo Sede - Constructor")
    void testModeloSede() {
        Sede sede = new Sede("Madrid", "Gran Vía", 1L);
        assertEquals("Madrid", sede.getCiudad());
        assertEquals(1L, sede.getIdEmpresa());
    }

    @Test
    @Order(7)
    @DisplayName("PU-07: GestorBD - Inicialización")
    void testArranqueBD() {
        // Verifica que el archivo existe tras el setup
        File dbFile = new File("carbon_tracker.db");
        assertTrue(dbFile.exists());
    }

    @Test
    @Order(8)
    @DisplayName("PU-08: GestorBD - Crear Usuario")
    void testCrearUsuario() {
        Rol rolUser = new Rol(2L, "USUARIO");

        // Intentamos crear un usuario nuevo.
        // Añadimos 'null' al final porque en este test no hay un administrador logueado actuando como autor.
        boolean resultado = gestorBD.crearUsuario("testUser", "1234", "Test User", rolUser, null);

        assertTrue(resultado, "El usuario debería crearse correctamente");
    }
    @Test
    @Order(9)
    @DisplayName("PU-09: GestorBD - Login Correcto")
    void testLoginCorrecto() {
        // Probamos loguearnos con el usuario creado en PU-08
        Usuario user = gestorBD.login("testUser", "1234");
        assertNotNull(user, "El login debería devolver un usuario");
        assertEquals("Test User", user.getNombreUsuario());
    }
    @Test
    @Order(10)
    @DisplayName("PU-10: GestorBD - Login Incorrecto")
    void testLoginIncorrecto() {
        Usuario user = gestorBD.login("testUser", "contraseñaFalsa");
        assertNull(user, "El login debería fallar y devolver null");
    }

    // ==========================================
    // PRUEBA DE INTEGRACIÓN
    // ==========================================

    @Test
    @Order(11)
    @DisplayName("PI-01: Integración - Flujo Sede y Auditoría")
    void testIntegracionSedeAuditoria() {
        // 1. Preparar Datos: Admin y Empresa
        Usuario admin = gestorBD.login("admin", "admin"); // El admin se crea en arrancarBD
        assertNotNull(admin, "Debe existir el admin por defecto");

        Empresa empresa = new Empresa("Integracion Corp", "Test");
        gestorBD.agregarEmpresa(empresa); // Esto genera ID
        assertNotNull(empresa.getId(), "La empresa debe tener ID tras insertarse");

        // 2. Acción: Crear Sede usando el método transaccional
        Sede nuevaSede = new Sede("Bilbao", "Guggenheim", empresa.getId());
        boolean exito = gestorBD.registrarSedeConAuditoria(nuevaSede, admin, empresa.getNombreEmpresa());

        // 3. Verificación 1: Operación exitosa
        assertTrue(exito, "La transacción debe completarse");

        // 4. Verificación 2: Sede existe en BD
        List<Sede> sedes = gestorBD.getSedesPorEmpresa(empresa.getId());
        boolean sedeEncontrada = sedes.stream().anyMatch(s -> s.getCiudad().equals("Bilbao"));
        assertTrue(sedeEncontrada, "La sede debe estar en la base de datos");

        // 5. Verificación 3: Auditoría existe (Integración completa)
        List<AuditoriaLog> logs = gestorBD.getLogsAuditoria();
        boolean logEncontrado = logs.stream()
                .anyMatch(log -> log.getAccion().contains("ALTA SEDE") &&
                        log.getAccion().contains("Integracion Corp") &&
                        log.getNombreUsuario().equals("admin"));

        assertTrue(logEncontrado, "Debe existir un registro de auditoría vinculando al usuario admin con la acción");
    }


    // ==========================================
    // PRUEBAS EXTRA. POST-V3
    // ==========================================

    @Test
    @Order(12)
    @DisplayName("PU-11: GestorBD - Evitar Duplicidad Empresas")
    void testDuplicidadEmpresa() {
        // 1. Creamos e insertamos la primera empresa
        Empresa emp1 = new Empresa("EmpresaUnica", "Tecnología");
        gestorBD.agregarEmpresa(emp1);

        // 2. Intentamos insertar otra con el MISMO nombre
        // El método agregarEmpresa devuelve NULL si ya existe
        Empresa emp2 = new Empresa("EmpresaUnica", "Agro");
        Empresa resultado = gestorBD.agregarEmpresa(emp2);

        assertNull(resultado, "El método debería devolver null al intentar registrar una empresa existente");
    }

    @Test
    @Order(13)
    @DisplayName("PU-12: GestorBD - Evitar Duplicidad Usuarios")
    void testDuplicidadUsuario() {
        Rol rolUser = new Rol(2L, "USUARIO");

        // 1. Creamos el usuario original
        gestorBD.crearUsuario("userDuplicado", "1234", "Original", rolUser, null);

        // 2. Intentamos crear otro con el MISMO username ("userDuplicado")
        // Tu método devuelve FALSE porque salta el SQLException (UNIQUE constraint)
        boolean resultado = gestorBD.crearUsuario("userDuplicado", "9999", "Copia", rolUser, null);

        assertFalse(resultado, "El método debería devolver false al saltar la restricción UNIQUE de usuario");
    }

    @Test
    @Order(14)
    @DisplayName("PU-13: GestorBD - Evitar Duplicidad Sedes")
    void testDuplicidadSede() {
        // Preparación: Login como admin y crear empresa
        Usuario admin = gestorBD.login("admin", "admin");
        Empresa empresa = new Empresa("SedeCorp", "Logística");
        gestorBD.agregarEmpresa(empresa);

        // 1. Registramos la primera sede en 'Valencia'
        Sede sede1 = new Sede("Valencia", "Calle Colón", empresa.getId());
        gestorBD.registrarSedeConAuditoria(sede1, admin, empresa.getNombreEmpresa());

        // 2. Intentamos registrar OTRA sede en 'Valencia' para la MISMA empresa
        Sede sede2 = new Sede("Valencia", "Calle Diferente", empresa.getId());

        // Esto debe devolver error de transaccion
        boolean resultado = gestorBD.registrarSedeConAuditoria(sede2, admin, empresa.getNombreEmpresa());

        assertFalse(resultado, "No se debería permitir duplicar ciudad para la misma empresa");
    }
}