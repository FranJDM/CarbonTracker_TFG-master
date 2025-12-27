package ct.Principal;

import DAO.GestorBD;
import Modelos.Rol;
import Modelos.Usuario;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Diálogo ({@code Dialog<Usuario>}) para manejar la autenticación y el registro de usuarios.
 * <p>
 * Este diálogo alterna entre dos vistas (Login y Registro) y devuelve el objeto {@link Usuario}
 * si el login es exitoso.
 */
public class Registro extends Dialog<Usuario> {

    /**
     * Instancia al gestor de la base de datos para realizar operaciones de login y registro.
     */
    private GestorBD gestorBD;

    /**
     * Contenedor principal para la vista de inicio de sesión.
     */
    private VBox panelLogin;
    /**
     * Contenedor principal para la vista de creación de cuenta.
     */
    private VBox panelRegistro;

    /** Campo de texto para el nombre de usuario en el login. */
    private TextField campoUsuario = new TextField();
    /** Campo de texto para la contraseña en el login. */
    private PasswordField campoContrasena = new PasswordField();
    /** Etiqueta para mostrar mensajes de error/éxito del login. */
    private Label msgInfo = new Label();

    /** Campo de texto para el nombre de usuario en el registro. */
    private TextField campoRegistroUsuario = new TextField();
    /** Campo de texto para la contraseña en el registro. */
    private PasswordField campoRegistroContrasena = new PasswordField();
    /** Campo de texto para el nombre completo del usuario. */
    private TextField campoNombreCompleto = new TextField();
    /** ComboBox para seleccionar el {@link Rol} durante el registro. */
    private ComboBox<Rol> comboRoles = new ComboBox<>();
    /** Etiqueta para mostrar mensajes de error/éxito del registro. */
    private Label msgErrorRegistro = new Label();

    /**
     * Constructor del diálogo de Registro/Login.
     * <p>
     * Inicializa las vistas y configura el diálogo para comenzar en la pantalla de Login.
     *
     * @param gestorBD La instancia del GestorBD necesaria para la autenticación.
     */
    public Registro(GestorBD gestorBD) {
        this.gestorBD = gestorBD;
        setTitle("Bienvenido a Carbon Tracker");
        // Se incializan las ventanas de login y registro
        panelLogin = incioPanelLogin();
        panelRegistro = inicioPanelRegistro();
        // Primero se lanza el login
        getDialogPane().setContent(panelLogin);
        setHeaderText(null);
        setGraphic(null);
        // Botón de cancelar para poder volver al login
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        try {
            // Obtenemos la ventana del diálogo
            Stage ventana = (Stage) this.getDialogPane().getScene().getWindow();

            // Le asignamos el mismo icono que a la app principal
            ventana.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/ct/Principal/logo.png")));
        } catch (Exception e) {
            System.out.println("Icono login no cargado: " + e.getMessage());
        }
    }

    /**
     * Crea el panel de Login (Iniciar Sesión).
     *
     * @return El contenedor configurado con los elementos de Login.
     */
    private VBox incioPanelLogin() {
        VBox panelDeLogin = new VBox(40);
        //Padding HardCodeado
        panelDeLogin.setPadding(new Insets(20));
        panelDeLogin.setPrefWidth(350);
        //Alineamiento HardCodeado
        panelDeLogin.setAlignment(Pos.CENTER);
        Text tituloVentana = new Text("Iniciar Sesión");
        //Añadimos aquí los estilos para no mezclarlos con los de la pantalla principal
        tituloVentana.setStyle("-fx-font-size: 20px; -fx-fill: -fx-text-fill;");
        //Ponemos los campo ejemplo para que el usuario sepa el tipo de dato esperado
        campoUsuario.setPromptText("Nombre de usuario");
        campoContrasena.setPromptText("Contraseña");
        //Añadimos aquí los estilos para no mezclarlos con los de la pantalla principal
        msgInfo.setStyle("-fx-text-fill: #E53E3E;"); // Color rojo para error
        //Configuramos el botón del panel
        Button botonPanelOk = new Button("Entrar");
        botonPanelOk.setDefaultButton(true);
        botonPanelOk.setMaxWidth(Double.MAX_VALUE); // Botón ancho
        botonPanelOk.setOnAction(e -> manejarLogin());
        // Enlace para cambiar a la vista de registro
        Hyperlink redireccionRegistro = new Hyperlink("¿No tienes cuenta? Regístrate");
        redireccionRegistro.setStyle("-fx-text-fill: #3ba136;");
        redireccionRegistro.setOnAction(e -> {
            getDialogPane().setContent(panelRegistro);
            // Esto fuerza a la ventana a estirarse para que quepa el panel de registro (420px)
            getDialogPane().getScene().getWindow().sizeToScene();
        });

        //Panel con todos los elementos
        panelDeLogin.getChildren().addAll(tituloVentana, campoUsuario, campoContrasena, botonPanelOk, msgInfo, redireccionRegistro);
        // Separador visual
        Separator separador = new Separator();
        separador.setPadding(new Insets(10, 0, 5, 0));
        //Link para cargar la DEMO
        Hyperlink llamadaDemo = new Hyperlink("🛠 Cargar Datos de Prueba");
        llamadaDemo.setStyle("-fx-text-fill: #E53E3E;");
        llamadaDemo.setOnAction(e -> {
            // Ejecutamos la carga
            gestorBD.cargarDatosDemo();
            // Feedback de usuarios
            Alert alerta = new Alert(Alert.AlertType.INFORMATION);
            alerta.setTitle("Modo Test");
            alerta.setHeaderText("Datos de prueba cargados correctamente");
            alerta.setContentText("Credenciales creadas:\nUsuario: admin / Pass: admin\nUsuario: empleado / Pass: 1234\nUsuario: cliente / Pass: 1234");
            try { alerta.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm()); } catch(Exception ex){}
            alerta.showAndWait();
            //Relleno de datos automático
            campoUsuario.setText("admin");
            campoContrasena.setText("admin");
        });

        VBox contenedorLink = new VBox(separador, llamadaDemo);
        contenedorLink.setAlignment(Pos.CENTER);
        panelDeLogin.getChildren().addAll(contenedorLink);
        return panelDeLogin;
    }

    /**
     * Inicializa y configura el panel de la interfaz gráfica (UI) para el Registro de Nuevos Usuarios.
     * <p>
     * Este método construye un contenedor ({@link VBox}) que organiza los elementos del formulario:
     * <ul>
     * <li><b>Campos de entrada:</b> Configura los textos de ayuda (<i>prompt text</i>) para el nombre de usuario, contraseña y nombre completo.</li>
     * <li><b>Selector de Rol:</b> Inicializa un {@link ComboBox} cargando dinámicamente los roles disponibles desde la base de datos ({@code gestorBD}).</li>
     * <li><b>Navegación:</b> Define los botones de "Registrarse" y "Volver", así como un hipervínculo, gestionando la lógica para cambiar la vista de vuelta al panel de Login y limpiar mensajes de error previos.</li>
     * </ul>
     *
     * </p>
     *
     * @return El contenedor {@link VBox} completamente configurado y funcional, listo para ser establecido como contenido del diálogo.
     */
    private VBox inicioPanelRegistro() {
        VBox ventanaRegistro = new VBox(15);
        ventanaRegistro.setPadding(new Insets(20));
        ventanaRegistro.setPrefWidth(420);
        ventanaRegistro.setPrefHeight(500);

        ventanaRegistro.setAlignment(Pos.CENTER_LEFT);
        //Título de la ventana
        Text tituloVentana = new Text("Crear Cuenta");
        //Añadimos aquí los estilos para no mezclarlos con los de la pantalla principal
        tituloVentana.setStyle("-fx-font-size: 20px; -fx-fill: -fx-text-fill;");
        //Ponemos los campo ejemplo para que el usuario sepa el tipo de dato esperado
        campoRegistroUsuario.setPromptText("Nombre de usuario");
        campoRegistroContrasena.setPromptText("Contraseña");
        campoNombreCompleto.setPromptText("Nombre completo");
        // Rellenamos el ComboBox con los roles recuperados de la BD
        comboRoles.setItems(FXCollections.observableArrayList(gestorBD.getRoles()));
        comboRoles.setPromptText("Selecciona un rol");
        comboRoles.setMaxWidth(Double.MAX_VALUE);
        //Añadimos aquí los estilos para no mezclarlos con los de la pantalla principal
        msgErrorRegistro.setStyle("-fx-text-fill: #E53E3E;");
        msgErrorRegistro.setWrapText(true); // Para que el texto de error no se corte

        // NUBE DE CONFIGURACIÓN DE BOTONOES
        //---------------------------------//
        //Botón de registro
        Button botonRegistro = new Button("Registrarse");
        botonRegistro.setDefaultButton(true);
        botonRegistro.setOnAction(e -> manejarRegistro());
        //Botón de volver al panel de login
        Button botonVolver = new Button("Volver");
        botonVolver.setCancelButton(true); // También si se pulsa Esc

        botonVolver.setOnAction(e -> {
            // Limpiamos los campos del login
            msgInfo.setText("");
            msgErrorRegistro.setText("");
            getDialogPane().setContent(panelLogin);
            getDialogPane().getScene().getWindow().sizeToScene();
        });
        // Contenedor para alinear los botones en la misma fila
        HBox celdaBoton = new HBox(10, botonVolver, botonRegistro);
        celdaBoton.setAlignment(Pos.CENTER_RIGHT);
        // Redirección para cambiar a la vista de login
        Hyperlink redireccionLogin = new Hyperlink("¿Ya tienes cuenta? Inicia sesión");
        redireccionLogin.setOnAction(e -> {
            msgInfo.setText("");
            getDialogPane().setContent(panelLogin);
            getDialogPane().getScene().getWindow().sizeToScene();
        });

        ventanaRegistro.getChildren().addAll(
                tituloVentana,
                new Label("Nombre Completo:"), campoNombreCompleto,
                new Label("Nombre de Usuario:"), campoRegistroUsuario,
                new Label("Contraseña:"), campoRegistroContrasena,
                new Label("Rol:"), comboRoles,
                celdaBoton,
                msgErrorRegistro,
                redireccionLogin
        );
        return ventanaRegistro;
    }

    /**
     * Ejecuta la lógica de control para el proceso de inicio de sesión (Login).
     * <p>
     * Este método se dispara al pulsar el botón de confirmar y sigue el siguiente flujo:
     * <ol>
     * <li><b>Validación de entrada:</b> Verifica que los campos de usuario y contraseña no estén vacíos. Si lo están, muestra una advertencia y detiene el proceso.</li>
     * <li><b>Autenticación:</b> Consulta al {@code gestorBD} para verificar las credenciales proporcionadas.</li>
     * <li><b>Resolución:</b>
     * <ul>
     * <li>Si es <b>exitoso</b>: Asigna el objeto {@link Usuario} recuperado como resultado del diálogo ({@code setResult}) y cierra la ventana.</li>
     * <li>Si <b>falla</b>: Mantiene la ventana abierta y muestra un mensaje de error ("Nombre de usuario o contraseña incorrectos") en la etiqueta informativa.</li>
     * </ul>
     * </li>
     * </ol>
     * </p>
     */
    private void manejarLogin() {
        String usuario = campoUsuario.getText();
        String pass = campoContrasena.getText();

        // Validación básica
        if (usuario.isEmpty() || pass.isEmpty()) {
            msgInfo.setText("Usuario y contraseña no pueden estar vacíos.");
            return;
        }

        try {
            // Intentamos hacer login
            Usuario usuarioRegistrado = gestorBD.login(usuario, pass);

            if (usuarioRegistrado != null) {
                // CASO 1: LOGIN CORRECTO
                setResult(usuarioRegistrado);
                close();
            } else {
                // CASO 2: CREDENCIALES INCORRECTAS (Usuario no existe o pass mal)
                msgInfo.setText("Nombre de usuario o contraseña incorrectos.");
                msgInfo.setStyle("-fx-text-fill: #E53E3E;"); // Hardcoded Rojo
            }

        } catch (SecurityException e) {
            // CASO 3: USUARIO BLOQUEADO (Capturamos la excepción desde GestorBD)
            msgInfo.setText(""); // Limpiamos
            campoContrasena.clear(); // Limpiamos la pass

            // Mostramos el PopUp específico para el bloqueo
            popUpInfoError(
                    "Acceso No Permitido",
                    "Cuenta Bloqueada",
                    e.getMessage() //
            );
        }
    }

    /**
     * Gestiona el proceso completo de registro de un nuevo usuario en el sistema.
     * <p>
     * Este método actúa como controlador del formulario de registro, ejecutando los siguientes pasos:
     * <ol>
     * <li><b>Recopilación y Validación:</b> Extrae los datos de la interfaz y verifica que todos los campos (usuario, contraseña, nombre, rol) estén informados.</li>
     * <li><b>Persistencia:</b> Invoca a {@code gestorBD.crearUsuario()} para intentar insertar el nuevo registro en la base de datos.</li>
     * <li><b>Gestión de Respuesta:</b>
     * <ul>
     * <li>En caso de <b>éxito</b>: Limpia el formulario, muestra un mensaje de confirmación (verde) y redirige automáticamente al usuario al panel de inicio de sesión.</li>
     * <li>En caso de <b>error</b> (ej. nombre de usuario duplicado): Muestra un mensaje de alerta en la misma pantalla para permitir la corrección.</li>
     * </ul>
     * </li>
     * </ol>
     * </p>
     */
    private void manejarRegistro() {
        String usuario = campoRegistroUsuario.getText();
        String pass = campoRegistroContrasena.getText();
        String nomCompleto = campoNombreCompleto.getText();
        Rol rol = comboRoles.getValue();

        // Bucle para comprobar que no queda ningún campo vacío o incorrecto.
        if (usuario.isEmpty() || pass.isEmpty() || nomCompleto.isEmpty() || rol == null) {
            msgErrorRegistro.setText("Todos los campos son obligatorios.");
            return;
        }

        boolean registroCorrecto = gestorBD.crearUsuario(usuario, pass, nomCompleto, rol);

        if (registroCorrecto) {
            // Éxito en la creación
            msgErrorRegistro.setText("");
            msgInfo.setStyle("-fx-text-fill: #34D399;");
            msgInfo.setText("¡Registro exitoso! Por favor, inicia sesión.");
            // Limpiamos los campos y cambiamos al login
            campoRegistroUsuario.clear();
            campoRegistroContrasena.clear();
            campoNombreCompleto.clear();
            comboRoles.getSelectionModel().clearSelection();

            getDialogPane().setContent(panelLogin);
            if (getDialogPane().getScene() != null) {
                getDialogPane().getScene().getWindow().sizeToScene();
            }

        } else {
            // Controlamos el registro si ya existe el usuario
            msgErrorRegistro.setText("Error al crear. El nombre de usuario ya existe.");
        }}

    /**
     * Muestra una alerta de error modal sobre el diálogo actual.
     */
    private void popUpInfoError(String titulo, String cabecera, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(cabecera);
        alert.setContentText(contenido);
        try { alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm()); } catch(Exception e){}
        // Hacemos que la alerta sea hija de la ventana de Registro
        alert.initOwner(this.getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }
}