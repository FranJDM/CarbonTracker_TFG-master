package ct.Principal;

import DAO.ControlCSV;
import DAO.GestorBD;
import Modelos.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Clase principal de la aplicación JavaFX "Carbon Tracker".
 * <p>
 * Actúa como el controlador central de la interfaz de usuario, se encarga de:
 * <ul>
 * <li>El ciclo de vida de la aplicación (inicio, login, cierre).</li>
 * <li>La construcción dinámica de la interfaz (Sidebar, Dashboard, Tablas).</li>
 * <li>La lógica de navegación y cambio de vistas.</li>
 * <li>La gestión de permisos basada en roles (Admin, Usuario, Cliente).</li>
 * </ul>
 */
public class AppCT extends Application {

    // ==========================================
    // 1. VARIABLES Y COMPONENTES GLOBALES
    // ==========================================

    /** Instancia del Gestor de Base de Datos para todas las operaciones de persistencia. */
    private GestorBD gestorBD;

    /** Usuario autenticado en la sesión actual; determina los permisos de acceso. */
    private Usuario usuarioSesionActual;

    /** Panel central del layout que cambia dinámicamente según la opción del menú seleccionada. */
    private BorderPane contenidoPrincipal;

    /** Lista observable para sincronizar los datos de la tabla de Empresas. */
    private ObservableList<Empresa> infoEmpresa = FXCollections.observableArrayList();

    /** Lista observable para sincronizar los datos de la tabla de Emisiones. */
    private ObservableList<Emisiones> infoEmision = FXCollections.observableArrayList();

    /** Componente visual de la tabla de empresas. */
    private TableView<Empresa> tablaEmpresa = new TableView<>();

    /** Componente visual de la tabla de emisiones. */
    private TableView<Emisiones> tablaEmision = new TableView<>();

    /** * Almacena la empresa seleccionada actualmente como contexto.
     * Se utiliza para filtrar las emisiones, mostrar el dashboard específico o gestionar sedes.
     */
    private Empresa empresaObjetivo = null;

    /** Contenedor pre-construido para la vista de lista de empresas. */
    private VBox vistaListaEmpresa;

    /** Contenedor pre-construido para la vista de lista de emisiones. */
    private VBox vistaListaEmision;

    /** Etiqueta de título dinámico para la tarjeta de emisiones. */
    private Text tarjetaEmision = new Text();

    /** Campo de texto para filtrar la tabla de empresas. */
    private TextField busquedaEmpresa;

    /** Campo de texto para filtrar la tabla de emisiones. */
    private TextField busquedaEmision;

    /** Lista para los registros de auditoría (solo Administrador). */
    private ObservableList<Modelos.AuditoriaLog> infoAuditoria = FXCollections.observableArrayList();

    /** Tabla para ver los registros de auditoría. */
    private TableView<Modelos.AuditoriaLog> tablaAuditoria = new TableView<>();

    /** Coordenada X para el cálculo del arrastre de la ventana personalizada. */
    private double xOffset = 0;
    /** Coordenada Y para el cálculo del arrastre de la ventana personalizada. */
    private double yOffset = 0;
    /** Lista para la vista de usuarios (solo Administrador). */
    private ObservableList<Usuario> infoUsuarios = FXCollections.observableArrayList();
    /** Tabla para ver los registros de los usuarios. */
    private TableView<Usuario> tablaUsuarios = new TableView<>();
    /** Contenedor para la vista de la lista de los usuarios. */
    private VBox vistaListaUsuarios;

    // ==========================================
    // 2. CICLO DE VIDA (MAIN & START)
    // ==========================================

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Punto de entrada principal del ciclo de vida de la aplicación.
     * <p>
     * Se hace la siguiente secuencia para el inicio de la aplicación:
     * 1. Inicialización de BD y estructura.
     * 2. Configuración de ventana sin bordes, para lograr un efecto más profesional y de aplicación real.
     * 3. Proceso de Login-Registro.
     * 4. Lanzamiento de la interfaz principal (Root Layout, Sidebar, Contenido).
     *
     * @param principal El escenario (Contenedor principal) principal.
     */
    @Override
    public void start(Stage principal) {
        gestorBD = new GestorBD();
        gestorBD.arrancarBD();
        // Estilo de ventana sin decoración del SO para usar nuestra barra personalizada
        principal.initStyle(javafx.stage.StageStyle.UNDECORATED);

        // --- CARGA DEL ICONO ---
        try {
            // Asegúrate de que tu archivo se llama "logo.png" y está en la ruta correcta.
            String rutaIcono = "/ct/Principal/logo.png";
            java.io.InputStream imgStream = getClass().getResourceAsStream(rutaIcono);
            if (imgStream != null) {
                principal.getIcons().add(new javafx.scene.image.Image(imgStream));
            } else {
                System.err.println("❌ No se encontró el icono en: " + rutaIcono);
            }
        } catch (Exception e) {
            System.out.println("Error al cargar el icono: " + e.getMessage());
        }

        Registro registro = new Registro(gestorBD);
        try {
            registro.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("No se pudo cargar style.css para el diálogo.");
        }
        Optional<Usuario> estadoLogin = registro.showAndWait();
        if (estadoLogin.isEmpty()) {
            System.out.println("Login cancelado. Cerrando aplicación.");
            Platform.exit();
            return;
        }

        // Inicio de sesión con el rol asginado
        this.usuarioSesionActual = estadoLogin.get();
        // Lanzamiento del contenido principal de la aplicación
        BorderPane bPrincipal = new BorderPane();
        // Se añade la barra de título personalizada en la parte superior
        HBox barraTitulo = crearBarraTitulo(principal);
        bPrincipal.setTop(barraTitulo);
        // Contenedor de la aplicación (Barra lateral y contenido principal)
        BorderPane contenedorPrincipal = new BorderPane();
        bPrincipal.setCenter(contenedorPrincipal);

        // Configuración de buscadores
        busquedaEmpresa = new TextField();
        busquedaEmpresa.setPromptText("Buscar empresa o sector...");
        busquedaEmpresa.textProperty().addListener((obs, oldV, newV)
                -> cargarListaEmpresa());
        busquedaEmision = new TextField();
        busquedaEmision.setPromptText("Buscar por empresa, tipo o fecha...");
        busquedaEmision.textProperty().addListener((obs, oldV, newV)
                -> cargarListaEmision());

        // Se cargan los datos
        vistaListaEmpresa = crearVistaListaEmpresa(principal);
        vistaListaEmision = desplegarVistaListaEmisiones(principal);
        // Se despliega el panel lateral
        VBox panelLateral = lanzamientoPanLateral(principal);
        contenedorPrincipal.setLeft(panelLateral);
        // Se configura el área de contenido inicial
        contenidoPrincipal = new BorderPane();
        VBox cabecera = desplegarCabecera();
        contenidoPrincipal.setTop(cabecera);
        //Definimos cuál será el contenido por defecto cuando se abre la aplicación
        mostrarTodasEmpresas();
        contenidoPrincipal.setCenter(vistaListaEmpresa);
        contenedorPrincipal.setCenter(contenidoPrincipal);
        // Configuración final de la escena
        Scene vistaPrincipal = new Scene(bPrincipal);
        vistaPrincipal.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        principal.setScene(vistaPrincipal);
        principal.setMaximized(true);
        // Habilitar redimensionamiento manual para ventana
        AuxiliaReajusteVentana.gestorReajuste(principal);
        principal.show();
        // Carga inicial de datos en las tablas
        cargarListaEmpresa();
        cargarListaEmision();
    }

    // ==========================================
    // 3. ESTRUCTURA BASE (BARRA, SIDEBAR, CABECERA)
    // ==========================================

    /**
     * Crea una barra de título personalizada que está integrada con el diseño.
     * Reemplaza la barra nativa del sistema operativo para mantener la estética.
     * <p>
     * Incluye controles para arrastrar, minimizar, maximizar y cerrar la ventana.
     *
     * @param contenidoBarra El escenario al que pertenece la barra.
     * @return El contenedor HBox con la barra de título.
     */
    private HBox crearBarraTitulo(Stage contenidoBarra) {
        HBox barra = new HBox(10);
        barra.getStyleClass().add("barra-ventana");
        barra.setAlignment(Pos.CENTER_RIGHT);
        barra.setPadding(new Insets(5, 15, 5, 15));

        // LOGO Y TITULO
        HBox parteHeaderBarra = new HBox(10);
        parteHeaderBarra.setAlignment(Pos.CENTER_LEFT);
        FontIcon iconoMarca = new FontIcon(MaterialDesign.MDI_LEAF);
        iconoMarca.setIconColor(Color.web("#34D399"));
        Text tituloAplicacion = new Text("Carbon Tracker Pro");
        tituloAplicacion.getStyleClass().add("titulo-ventana");
        parteHeaderBarra.getChildren().addAll(iconoMarca, tituloAplicacion);
        Region espaciadorBarra = new Region();
        HBox.setHgrow(espaciadorBarra, Priority.ALWAYS);

        // CONTROLES DE VENTANA
        // Botón Minimizar
        Button btnMinimizar = new Button("", new FontIcon(MaterialDesign.MDI_MINUS));
        btnMinimizar.getStyleClass().add("boton-ventana");
        btnMinimizar.setOnAction(e -> contenidoBarra.setIconified(true));

        // Botón Maximizar/Restaurar (Toggle simple)
        Button btnMaximizar = new Button("", new FontIcon(MaterialDesign.MDI_CROP_SQUARE));
        btnMaximizar.getStyleClass().add("boton-ventana");
        btnMaximizar.setOnAction(e -> contenidoBarra.setMaximized(!contenidoBarra.isMaximized()));

        // Botón Cerrar (Cierra la aplicación completa)
        Button btnCerrar = new Button("", new FontIcon(MaterialDesign.MDI_CLOSE));
        btnCerrar.getStyleClass().addAll("boton-ventana", "cerrar-ventana");
        btnCerrar.setOnAction(e -> Platform.exit());

        // ARRASTRE
        // Posicion del ratón por defectp
        barra.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        // Arrastre de la ventana
        barra.setOnMouseDragged(event -> {
            if (!contenidoBarra.isMaximized()) {
                contenidoBarra.setX(event.getScreenX() - xOffset);
                contenidoBarra.setY(event.getScreenY() - yOffset);
            }
        });
        barra.getChildren().addAll(parteHeaderBarra, espaciadorBarra, btnMinimizar, btnMaximizar, btnCerrar);
        return barra;
    }

    /**
     * Construye el panel lateral de navegación (Sidebar).
     * <p>
     * Configura los botones de navegación y aplica la lógica de seguridad
     * para ocultar opciones según el rol del usuario.
     *
     * @param vista El escenario principal (para cerrar sesión).
     * @return El contenedor VBox con el menú lateral.
     */
    private VBox lanzamientoPanLateral(Stage vista) {
        VBox panLateral = new VBox();
        panLateral.getStyleClass().add("barra-lateral");

        VBox botonesNavegacion = new VBox(15);

        // SECCIÓN EMPRESAS
        Label tituloEmpresa = new Label("EMPRESAS");
        tituloEmpresa.getStyleClass().add("nav-header");
        //LLamadas a la acción
        Button botonVerEmpresa = new Button("♻ Ver Empresas");
        botonVerEmpresa.getStyleClass().add("bton-nav");
        botonVerEmpresa.setOnAction(e -> {
            contenidoPrincipal.setCenter(vistaListaEmpresa);
            cargarListaEmpresa();
        });

        Button addEmpresa = new Button("➕ Añadir Empresa");
        addEmpresa.getStyleClass().add("bton-nav");
        addEmpresa.setOnAction(e -> ventanaRegistroEmpresa(vista, null));

        // SECCIÓN EMISIONES
        Label tituloEmision = new Label("EMISIONES");
        tituloEmision.getStyleClass().add("nav-header");
        //Llamadas a la acción
        Button botonVerEmision = new Button("♻ Ver Todas las Emisiones");
        botonVerEmision.getStyleClass().add("bton-nav");
        botonVerEmision.setOnAction(e -> {
            mostrarTodasEmisiones();
            contenidoPrincipal.setCenter(vistaListaEmision);
        });

        Button addEmision = new Button("➕ Añadir Emisión");
        addEmision.getStyleClass().add("bton-nav");
        addEmision.setOnAction(e -> ventanaRegistroEmision(vista, null));

        // SECCIÓN OPTIMIZACIÓN
        Label tituloOptimizacion = new Label("OPTIMIZACIÓN");
        tituloOptimizacion.getStyleClass().add("nav-header");
        //Llamadas a la acción
        Button btnRecomendaciones = new Button(  "\uD83D\uDCA1 Recomendaciones Clave");
        btnRecomendaciones.getStyleClass().add("bton-nav");
        btnRecomendaciones.setOnAction(e -> {
            PanelRecomendaciones moduloRecomendaciones = new PanelRecomendaciones(gestorBD);
            contenidoPrincipal.setCenter(moduloRecomendaciones);
        });

        botonesNavegacion.getChildren().addAll(tituloEmpresa, botonVerEmpresa, addEmpresa, tituloEmision, botonVerEmision, addEmision,tituloOptimizacion,
                btnRecomendaciones);

        // AUDITORIA (Solo ADMIN) ---
        if (usuarioSesionActual.getRol().getNomRol().equals("ADMINISTRADOR")) {

            Label tituloAdmin = new Label("ADMINISTRACIÓN");
            tituloAdmin.getStyleClass().add("nav-header");

            Button btnAuditoria = new Button("⛊ Auditoría / Logs");
            btnAuditoria.getStyleClass().add("bton-nav");

            btnAuditoria.setOnAction(e -> {
                contenidoPrincipal.setCenter(crearVistaAuditoria());

            });
            Button btnUsuarios = new Button("👥 Gestión Usuarios");
            btnUsuarios.getStyleClass().add("bton-nav");
            btnUsuarios.setOnAction(e -> {
                // Generamos la vista y la ponemos en el centro
                vistaListaUsuarios = crearVistaUsuarios();
                contenidoPrincipal.setCenter(vistaListaUsuarios);
            });
            botonesNavegacion.getChildren().addAll(tituloAdmin, btnAuditoria, btnUsuarios);
        }

        // APLICACIÓN ROLES
        String rol = usuarioSesionActual.getRol().getNomRol();
        if (rol.equals("CLIENTE")) {
            addEmpresa.setVisible(false);
            addEmpresa.setManaged(false);
            addEmision.setVisible(false);
            addEmision.setManaged(false);
        } else if (rol.equals("USUARIO")) {
            addEmpresa.setVisible(true);
            addEmpresa.setManaged(true);
        }

        // FUNCIÓN LOGOUT
        Region cajaEspaciado = new Region();
        VBox.setVgrow(cajaEspaciado, Priority.ALWAYS);
        //Llamada a la acción
        Button botonLogOut = new Button("➜] Cerrar Sesión");
        botonLogOut.getStyleClass().add("bton-nav");
        botonLogOut.setOnAction(e -> {
            vista.close();
            Platform.runLater(() -> start(new Stage()));
        });

        panLateral.getChildren().addAll(botonesNavegacion, cajaEspaciado, botonLogOut);
        return panLateral;
    }

    /**
     * Crea el cabecero superior (Header) con elementos decorativos y acceso a ayuda.
     * @return El contenedor VBox de la cabecera.
     */
    private VBox desplegarCabecera() {
        VBox panelCabecera = new VBox(10);
        panelCabecera.getStyleClass().add("cabecera-panel");
        panelCabecera.setAlignment(Pos.CENTER);
        // Nube de Iconos decorativos
        HBox iconos = new HBox(15);
        iconos.setAlignment(Pos.CENTER);
        FontIcon iconoHoja = new FontIcon(MaterialDesign.MDI_LEAF);
        iconoHoja.setIconSize(32);
        iconoHoja.setIconColor(Color.web("#34D399"));
        FontIcon iconoMundo = new FontIcon(MaterialDesign.MDI_EARTH);
        iconoMundo.setIconSize(48);
        iconoMundo.setIconColor(Color.web("#3B82F6"));
        FontIcon iconoPapelera = new FontIcon(MaterialDesign.MDI_RECYCLE);
        iconoPapelera.setIconSize(32);
        iconoPapelera.setIconColor(Color.web("#10B981"));
        iconos.getChildren().addAll(iconoHoja, iconoMundo, iconoPapelera);

        // Título Principal
        Text tituloCabecera = new Text("Track your Carbon FootPrint");
        tituloCabecera.getStyleClass().add("title");

        // Botón de Ayuda
        FontIcon iconoManual = new FontIcon(MaterialDesign.MDI_HELP_CIRCLE);
        iconoManual.setIconSize(24);
        iconoManual.getStyleClass().add("icono-ayuda");
        iconoManual.setOnMouseClicked(e -> {
            Stage html = (Stage) panelCabecera.getScene().getWindow();
            desplegarManual(html);
        });

        BorderPane barraCabecera = new BorderPane();
        barraCabecera.setCenter(tituloCabecera);
        barraCabecera.setRight(iconoManual);
        BorderPane.setAlignment(tituloCabecera, Pos.CENTER);
        // Tags / Etiquetas
        HBox etiquetasCabecera = new HBox(10);
        etiquetasCabecera.setAlignment(Pos.CENTER);
        etiquetasCabecera.getChildren().addAll(fncTarjetas("Sostenibilidad"), fncTarjetas("Medición"), fncTarjetas("Impacto"));
        // Composición de la cabecera
        panelCabecera.getChildren().addAll(iconos, barraCabecera, etiquetasCabecera);
        return panelCabecera;
    }

    // ==========================================
    // 4. VISTAS PRINCIPALES (PANELES)
    // ==========================================

    /**
     * Construye la vista que contiene la tabla de Empresas.
     * @param vista Escenario principal.
     * @return El contenedor VBox de la vista.
     */
    private VBox crearVistaListaEmpresa(Stage vista) {
        VBox tarjetaListaEmpresa = new VBox();
        tarjetaListaEmpresa.getStyleClass().addAll("tarjeta", "tarjeta-azul");

        // Cabecera de tarjeta
        HBox tarjetaTitulo = new HBox(10);
        tarjetaTitulo.setAlignment(Pos.CENTER);
        tarjetaTitulo.setPadding(new Insets(25, 0, 15, 0));
        FontIcon iconoCat = new FontIcon(MaterialDesign.MDI_DOMAIN);
        iconoCat.setIconSize(24);
        Text tituloCategoria = new Text("Lista de Empresas");

        tituloCategoria.getStyleClass().add("tarjeta-title");

        tarjetaTitulo.getChildren().addAll(iconoCat, tituloCategoria);
        tablaEmpresa.getStyleClass().add("table-companies");


        // Barra de herramientas
        HBox cajaBusquedaEmpresa = new HBox(10);
        cajaBusquedaEmpresa.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(busquedaEmpresa, Priority.ALWAYS);

        Button btnExportarEmpresa = new Button("Exportar Todo (CSV)", new FontIcon(MaterialDesign.MDI_DOWNLOAD));
        btnExportarEmpresa.setOnAction(e -> exportarEmpresas(vista));

        cajaBusquedaEmpresa.getChildren().addAll(busquedaEmpresa, btnExportarEmpresa);
        cajaBusquedaEmpresa.setPadding(new Insets(10, 0, 0, 0));

        // Tabla
        columnasTablaEmpresa(vista);
        tablaEmpresa.setItems(infoEmpresa);
        VBox.setVgrow(tablaEmpresa, Priority.ALWAYS);

        tarjetaListaEmpresa.getChildren().addAll(tarjetaTitulo, cajaBusquedaEmpresa, tablaEmpresa);
        VBox.setMargin(tarjetaListaEmpresa, new Insets(0, 30, 30, 30));
        return tarjetaListaEmpresa;
    }

    /**
     * Construye la vista completa de la lista de Emisiones.
     * Incluye cabecera de tarjeta, buscador y tabla.
     *
     * @param vista Escenario principal.
     * @return Contenedor VBox de la vista.
     */
    private VBox desplegarVistaListaEmisiones(Stage vista) {
        VBox vistaListaEmisiones = new VBox();
        vistaListaEmisiones.getStyleClass().addAll("tarjeta", "tarjeta-verde");
        // Cabecera de la tarjeta
        HBox cabecera = new HBox(10);
        cabecera.setAlignment(Pos.CENTER);
        cabecera.setPadding(new Insets(25, 0, 15, 0));
        FontIcon iconoLista = new FontIcon(MaterialDesign.MDI_FORMAT_LIST_BULLETED);
        iconoLista.setIconSize(24);
        tarjetaEmision.setText("Lista de Emisiones");
        tarjetaEmision.getStyleClass().add("tarjeta-title");
        cabecera.getChildren().addAll(iconoLista, tarjetaEmision);
        tablaEmision.getStyleClass().add("table-emissions");
        // Barra de herramientas (Búsqueda + Exportar)
        HBox cajaBusqueda = new HBox(10);
        cajaBusqueda.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(busquedaEmision, Priority.ALWAYS);
        Button btnExportarEmision = new Button("Exportar Vista (CSV)", new FontIcon(MaterialDesign.MDI_DOWNLOAD));
        btnExportarEmision.setOnAction(e -> exportarEmisiones(vista));

        cajaBusqueda.getChildren().addAll(busquedaEmision, btnExportarEmision);
        cajaBusqueda.setPadding(new Insets(10, 0, 0, 0));

        // Configuración de la tabla
        columnasTabEmision(vista);
        tablaEmision.setItems(infoEmision);
        VBox.setVgrow(tablaEmision, Priority.ALWAYS);

        vistaListaEmisiones.getChildren().addAll(cabecera, cajaBusqueda, tablaEmision);
        VBox.setMargin(vistaListaEmisiones, new Insets(0, 30, 30, 30));
        return vistaListaEmisiones;
    }

    /**
     * Construye la vista de administración de usuarios.
     * Incluye tabla con todos los datos de los usuarios y acciones sobre ellos
     */
    private VBox crearVistaUsuarios() {
        VBox tarjetaUsuarios = new VBox();
        tarjetaUsuarios.getStyleClass().addAll("tarjeta", "tarjeta-gris");

        // Cabecera
        HBox cabecera = new HBox(10);
        cabecera.setAlignment(Pos.CENTER);
        cabecera.setPadding(new Insets(25, 0, 15, 0));
        FontIcon iconoUsuario = new FontIcon(MaterialDesign.MDI_ACCOUNT_MULTIPLE);
        iconoUsuario.setIconSize(24);
        Text tituloPanel = new Text("Gestión de Usuarios y Permisos");
        tituloPanel.getStyleClass().add("tarjeta-title");
        cabecera.getChildren().addAll(iconoUsuario, tituloPanel);

        // Configuración de Tabla
        tablaUsuarios = new TableView<>();
        tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Columnas de Texto
        TableColumn<Usuario, String> colUser = new TableColumn<>("NOMBRE DEL USUARIO");
        colUser.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));

        TableColumn<Usuario, String> colNombre = new TableColumn<>("LOGIN DE REGISTRO");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));

        TableColumn<Usuario, String> colRol = new TableColumn<>("ROL");
        // Extraemos el nombre del rol del objeto
        colRol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getRol().getNomRol()));

        // Columna del ESTADO (ACTIVO-BLOQUEADO)
        TableColumn<Usuario, Void> colEstado = new TableColumn<>("ESTADO");
        colEstado.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void objeto, boolean vacio) {
                super.updateItem(objeto, vacio);
                if (vacio) { setGraphic(null); }
                else {
                    Usuario usuario = getTableView().getItems().get(getIndex());
                    Label etiqueta = new Label(usuario.isActivo() ? "ACTIVO" : "BLOQUEADO");
                    etiqueta.getStyleClass().removeAll("lbl-estado-activo", "lbl-estado-bloqueado");
                    etiqueta.getStyleClass().add(usuario.isActivo() ? "lbl-estado-activo" : "lbl-estado-bloqueado");setGraphic(etiqueta);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Columna ACCIONES (Editar y Bloquear)
        TableColumn<Usuario, Void> colAccion = new TableColumn<>("ACCIONES");
        colAccion.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("", new FontIcon(MaterialDesign.MDI_PENCIL));
            private final Button btnBloquear = new Button("", new FontIcon(MaterialDesign.MDI_LOCK));
            private final Button btnEliminar = new Button("", new FontIcon(MaterialDesign.MDI_BASKET));


            private final HBox panel = new HBox(5, btnEditar, btnBloquear,btnEliminar);

            {
                btnEditar.getStyleClass().addAll("action-button", "btn-edit");
                btnEditar.setTooltip(new Tooltip("Editar datos y contraseña"));

                // Estilo base del botón bloquear (se actualiza abajo)
                btnBloquear.getStyleClass().addAll("action-button");
                btnBloquear.setTooltip(new Tooltip("Bloquear usuario"));


                btnEliminar.getStyleClass().addAll("action-button", "btn-delete");
                btnEliminar.setTooltip(new Tooltip("Eliminar usuario"));

                panel.setAlignment(Pos.CENTER);

                // ACCIÓN EDITAR
                btnEditar.setOnAction(e -> ventanaGestionUsuario(tablaUsuarios.getScene().getWindow(), getTableView().getItems().get(getIndex())));

                // ACCIÓN BLOQUEAR / DESBLOQUEAR
                btnBloquear.setOnAction(e -> {
                    Usuario usuario = getTableView().getItems().get(getIndex());

                    // PROTECCIÓN: No permitir que el admin se bloquee a sí mismo
                    if(usuario.getId().equals(usuarioSesionActual.getId())) {
                        popUpError("Acción denegada", "Protección de Superusuario", "No puedes bloquear tu propia cuenta.");
                        return;
                    }


                    // Invertimos el estado y guardamos en BD
                    boolean cambioEstado = !usuario.isActivo();
                    gestorBD.bloqueoUsuario(usuario.getId(), cambioEstado);
                    cargarListaUsuarios(); // Refrescamos la tabla
                });

                btnEliminar.setOnAction(e -> {
                    Usuario usuario = getTableView().getItems().get(getIndex());

                    // Protección: No borrarse a uno mismo
                    if(usuario.getId().equals(usuarioSesionActual.getId())) {
                        popUpError("Acción denegada", "Protección de Superusuario", "No puedes eliminar tu propia cuenta mientras la usas.");
                        return;
                    }

                    // Confirmación de borrado de usuario
                    Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmacion.setTitle("Eliminar Usuario");
                    confirmacion.setHeaderText("¿Estás seguro de eliminar a '" + usuario.getNombreUsuario() + "'?");
                    confirmacion.setContentText("Esta acción es irreversible y borrará sus datos de acceso.");
                    try { confirmacion.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm()); } catch(Exception ex){}

                    Optional<ButtonType> respuesta = confirmacion.showAndWait();
                    if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {

                        // Registro de borrado
                        boolean borradoExitoso = gestorBD.borrarUsuario(usuario.getId());

                        if (borradoExitoso) {
                            popUpOk("Usuario Eliminado", "El usuario ha sido borrado del sistema.");
                            cargarListaUsuarios();
                        } else {
                            // Recogemos el error arrojado por la integridad que tiene la bd entre usuario-auditoria
                            popUpError("No se pudo eliminar",
                                    "El usuario tiene historial registrado.",
                                    "Por seguridad e integridad de datos (auditoría), no se puede borrar un usuario con actividad. \n\nSolución: Bloquéalo en su lugar.");
                        }
                    }
                });

            }

            @Override
            protected void updateItem(Void objeto, boolean vacio) {
                super.updateItem(objeto, vacio);
                if (vacio) {
                    setGraphic(null);
                } else {
                    Usuario usuario = getTableView().getItems().get(getIndex());
                    FontIcon iconoBloqueo = (FontIcon) btnBloquear.getGraphic();
                    //Limpiamos los estilos
                    btnBloquear.getStyleClass().removeAll("btn-action-bloquear", "btn-action-desbloquear");
                    // Cambiamos el icono y color según el estado
                    if (usuario.isActivo()) {
                        // Si está activo, mostramos opción de BLOQUEAR (Rojo)
                        iconoBloqueo.setIconCode(MaterialDesign.MDI_LOCK);
                        btnBloquear.getStyleClass().add("btn-action-bloquear");
                        btnBloquear.setTooltip(new Tooltip("Bloquear acceso al sistema"));
                    } else {
                        // Si está bloqueado, mostramos opción de DESBLOQUEAR (Verde)
                        iconoBloqueo.setIconCode(MaterialDesign.MDI_LOCK_OPEN);
                        btnBloquear.getStyleClass().add("btn-action-desbloquear");
                        btnBloquear.setTooltip(new Tooltip("Restaurar acceso"));
                    }
                    setGraphic(panel);
                }
            }
        });

        tablaUsuarios.getColumns().addAll(colUser, colNombre, colRol, colEstado, colAccion);
        tablaUsuarios.setItems(infoUsuarios);
        VBox.setVgrow(tablaUsuarios, Priority.ALWAYS);

        // Botón superior para Crear Usuario
        Button btnAddUsuario = new Button("Crear Nuevo Usuario", new FontIcon(MaterialDesign.MDI_ACCOUNT_PLUS));
        btnAddUsuario.getStyleClass().add("btn-add-user");
        btnAddUsuario.setOnAction(e -> ventanaGestionUsuario(tablaUsuarios.getScene().getWindow(), null));

        // Contenedor del botón con margen
        HBox cajaBoton = new HBox(btnAddUsuario);
        cajaBoton.setPadding(new Insets(0, 0, 10, 0));
        cajaBoton.setAlignment(Pos.CENTER_RIGHT);

        tarjetaUsuarios.getChildren().addAll(cabecera, cajaBoton, tablaUsuarios);
        VBox.setMargin(tarjetaUsuarios, new Insets(0, 30, 30, 30));

        // Carga inicial
        cargarListaUsuarios();
        return tarjetaUsuarios;
    }

    /**
     * Genera la vista de tabla de Auditoría.
     * @return Panel VBox con la tabla de logs.
     */
    private VBox crearVistaAuditoria() {
        VBox tarjetaAudt = new VBox();
        tarjetaAudt.getStyleClass().add("tarjeta");

        HBox cabecera = new HBox(10);
        cabecera.setAlignment(Pos.CENTER);
        cabecera.setPadding(new Insets(25, 0, 15, 0));
        FontIcon icono = new FontIcon(MaterialDesign.MDI_SHIELD);
        icono.setIconSize(24);
        Text titulo = new Text("Registro de Auditoría y Seguridad");
        titulo.getStyleClass().add("tarjeta-title");
        cabecera.getChildren().addAll(icono, titulo);
        //Despliegue de tablas de auditoria
        tablaAuditoria = new TableView<>();
        tablaAuditoria.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Modelos.AuditoriaLog, String> colFecha = new TableColumn<>("FECHA / HORA");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        //Columnas de auditoria
        TableColumn<Modelos.AuditoriaLog, String> colUsuario = new TableColumn<>("USUARIO");
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));

        TableColumn<Modelos.AuditoriaLog, String> colAccion = new TableColumn<>("ACCIÓN REGISTRADA");
        colAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));

        tablaAuditoria.getColumns().addAll(colFecha, colUsuario, colAccion);
        tablaAuditoria.setItems(infoAuditoria);
        VBox.setVgrow(tablaAuditoria, Priority.ALWAYS);

        Button btnRefrescar = new Button("Actualizar Registros", new FontIcon(MaterialDesign.MDI_REFRESH));
        btnRefrescar.setOnAction(e -> cargarAuditoria());

        tarjetaAudt.getChildren().addAll(cabecera, btnRefrescar, tablaAuditoria);
        VBox.setMargin(tarjetaAudt, new Insets(0, 30, 30, 30));

        cargarAuditoria();
        return tarjetaAudt;
    }

    /**
     * Genera el Dashboard de gráficos para la empresa seleccionada.
     * @param empresa Empresa a analizar.
     * @return Panel VBox con gráficos.
     */
    private VBox crearDashboardEmisiones(Empresa empresa) {
        VBox dashboard = new VBox(20);
        dashboard.getStyleClass().add("tarjeta-dashboard");
        dashboard.setPadding(new Insets(30));
        dashboard.setAlignment(Pos.TOP_CENTER);
        //Titulo del panel
        Text titulo = new Text("Dashboard de Emisiones: " + empresa.getNombreEmpresa());
        titulo.getStyleClass().add("titulo-dash");

        List<Map.Entry<String, Double>> datosReporte = gestorBD.getReporteEmisionesPorEmpresa(empresa.getId());

        if (datosReporte.isEmpty()) {
            VBox error = new VBox(new Text("No hay datos de emisiones registrados para esta empresa."));
            error.setAlignment(Pos.CENTER);
            return error;
        }

        HBox contenedorGraficos = new HBox(50);
        contenedorGraficos.setAlignment(Pos.CENTER);
        HBox.setHgrow(contenedorGraficos, Priority.ALWAYS);
        // Gráfico Circular
        ObservableList<PieChart.Data> grafico = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : datosReporte) {
            grafico.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        PieChart graficoCircular = new PieChart(grafico);
        graficoCircular.setTitle("Distribución de CO2e por Tipo");
        graficoCircular.setPrefSize(400, 300);
        graficoCircular.setStyle("-fx-background-color: white;");

        // Gráfico de Barras
        final CategoryAxis ejex = new CategoryAxis();
        final NumberAxis ejeY = new NumberAxis();
        final BarChart<String, Number> graficoBarras = new BarChart<>(ejex, ejeY);
        graficoBarras.setTitle("Total de CO2e por Fuente (kg)");
        graficoBarras.setLegendVisible(false);
        graficoBarras.setPrefSize(550, 300);
        graficoBarras.setStyle("-fx-background-color: white;");

        XYChart.Series<String, Number> seriesGrafico = new XYChart.Series<>();
        for (Map.Entry<String, Double> datosEntrada : datosReporte) {
            seriesGrafico.getData().add(new XYChart.Data<>(datosEntrada.getKey(), datosEntrada.getValue()));
        }
        graficoBarras.getData().add(seriesGrafico);

        contenedorGraficos.getChildren().addAll(graficoCircular, graficoBarras);
        dashboard.getChildren().addAll(titulo, contenedorGraficos);

        return dashboard;
    }

    // ==========================================
    // 5. CONFIGURACIÓN DE TABLAS (COLUMNAS)
    // ==========================================

    /**
     * Configura las columnas y la compleja celda de acciones de la tabla de Empresas.
     * @param vista Escenario principal.
     */
    private void columnasTablaEmpresa(Stage vista) {
        tablaEmpresa = new TableView<>();
        tablaEmpresa.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaEmpresa.getProperties().put("javafx.table.columnReordering", Boolean.FALSE);
        //Columnas empresa
        TableColumn<Empresa, String> columnaNombre = new TableColumn<>("NOMBRE");
        columnaNombre.setCellValueFactory(new PropertyValueFactory<>("nombreEmpresa"));
        columnaNombre.setPrefWidth(200);

        TableColumn<Empresa, String> columnaSector = new TableColumn<>("SECTOR");
        columnaSector.setCellValueFactory(new PropertyValueFactory<>("sector"));
        columnaSector.setPrefWidth(150);

        TableColumn<Empresa, Double> colCoe2 = new TableColumn<>("TOTAL CO2e (KG)");
        colCoe2.setCellValueFactory(new PropertyValueFactory<>("auxiliarAlmacenC02"));
        colCoe2.setPrefWidth(150);

        TableColumn<Empresa, Void> colAcciones = new TableColumn<>("ACCIONES");
        colAcciones.setPrefWidth(320);
        colAcciones.setId("columna-acciones-header");
        colAcciones.setCellFactory(param -> new TableCell<>() {
            // Botones de acción
            private final Button btnEditar = new Button("", new FontIcon(MaterialDesign.MDI_PENCIL));
            private final Button btnEliminar = new Button("", new FontIcon(MaterialDesign.MDI_BASKET));
            private final Button btnVerEmpresa = new Button("", new FontIcon(MaterialDesign.MDI_EYE));
            private final Button btnDashboard = new Button("", new FontIcon(MaterialDesign.MDI_CHART_BAR));
            private final Button btnSede = new Button("", new FontIcon(MaterialDesign.MDI_MAP_MARKER_PLUS));

            private final HBox panelBtns = new HBox(5, btnVerEmpresa, btnDashboard, btnSede, btnEditar, btnEliminar);

            {
                // Asignación de estilos CSS para botones circulares coloreados
                btnVerEmpresa.getStyleClass().addAll("action-button", "btn-view");
                btnDashboard.getStyleClass().addAll("action-button", "btn-dashboard");
                btnSede.getStyleClass().addAll("action-button", "btn-sede");
                btnEditar.getStyleClass().addAll("action-button", "btn-edit");
                btnEliminar.getStyleClass().addAll("action-button", "btn-delete");
                //Etiquetas de información para el usuario de cada botón
                btnVerEmpresa.setTooltip(new Tooltip("Ver Empresa"));
                btnDashboard.setTooltip(new Tooltip("Mostrar gráficos de emisiones"));
                btnSede.setTooltip(new Tooltip("Añadir Sede / Sucursal"));
                btnEditar.setTooltip(new Tooltip("Editar información de la empresa"));
                btnEliminar.setTooltip(new Tooltip("Eliminar empresa y emisiones"));


                panelBtns.setAlignment(Pos.CENTER);

                // --- DEFINICIÓN DE ACCIONES ---
                btnEliminar.setOnAction(e -> borradoEmpresa(getTableView().getItems().get(getIndex())));

                btnEditar.setOnAction(e -> ventanaRegistroEmpresa(vista, getTableView().getItems().get(getIndex())));

                btnVerEmpresa.setOnAction(e -> {
                    Empresa emp = getTableView().getItems().get(getIndex());
                    AppCT.this.empresaObjetivo = emp; // Establecer contexto
                    mostrarEmisionesPorEmpresa(emp);
                    contenidoPrincipal.setCenter(vistaListaEmision);
                });

                btnDashboard.setOnAction(e -> {
                    Empresa emp = getTableView().getItems().get(getIndex());
                    AppCT.this.empresaObjetivo = emp;
                    VBox dashboardView = crearDashboardEmisiones(emp);
                    contenidoPrincipal.setCenter(dashboardView);
                });

                btnSede.setOnAction(e -> {
                    Empresa emp = getTableView().getItems().get(getIndex());
                    mostrarGestorSedes(emp);
                });

                // --- GESTIÓN DE PERMISOS ---
                String rol = usuarioSesionActual.getRol().getNomRol();
                if (rol.equals("CLIENTE")) {
                    btnEditar.setVisible(false); btnEditar.setManaged(false);
                    btnEliminar.setVisible(false); btnEliminar.setManaged(false);
                    btnSede.setVisible(false); btnSede.setManaged(false);
                } else if (rol.equals("USUARIO")) {
                    btnEliminar.setVisible(false); btnEliminar.setManaged(false);
                }
            }

            @Override
            protected void updateItem(Void registro, boolean vacio) {
                super.updateItem(registro, vacio);
                setGraphic(vacio ? null : panelBtns);
            }
        });

        tablaEmpresa.getColumns().addAll(columnaNombre, columnaSector, colCoe2, colAcciones);
    }

    /**
     * Configura las columnas y acciones de la tabla de Emisiones.
     * @param vista Escenario principal.
     */
    private void columnasTabEmision(Stage vista) {
        tablaEmision = new TableView<>();
        tablaEmision.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // --- COLUMNAS DE DATOS ---
        TableColumn<Emisiones, String> columnaEmpresa = new TableColumn<>("EMPRESA");
        columnaEmpresa.setCellValueFactory(new PropertyValueFactory<>("nombreEmpresa"));

        TableColumn<Emisiones, String> columnaTipo = new TableColumn<>("TIPO");
        columnaTipo.setCellValueFactory(new PropertyValueFactory<>("tipoEmision"));

        TableColumn<Emisiones, Double> columnaCantidad = new TableColumn<>("CANTIDAD");
        columnaCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadEmision"));

        TableColumn<Emisiones, Double> colCO2e = new TableColumn<>("CO2E (KG)");
        colCO2e.setCellValueFactory(new PropertyValueFactory<>("co2e"));

        TableColumn<Emisiones, LocalDate> colFecha = new TableColumn<>("FECHA");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));

        // --- COLUMNA DE ACCIONES (EDITAR + BORRAR) ---
        TableColumn<Emisiones, Void> colAccion = new TableColumn<>("ACCIONES");
        colAccion.getStyleClass().add("columna-acc");
        colAccion.setCellFactory(param -> new TableCell<>() {

            // 1. Botón Editar (Estilo reutilizado de Empresa)
            private final Button btnEditar = new Button("", new FontIcon(MaterialDesign.MDI_PENCIL));
            // 2. Botón Borrar
            private final Button btnEliminar = new Button("", new FontIcon(MaterialDesign.MDI_BASKET));

            // 3. Contenedor para ponerlos juntos
            private final HBox panelBtns = new HBox(5, btnEditar, btnEliminar);

            {
                // Estilos CSS (Idénticos a la tabla Empresa)
                btnEditar.getStyleClass().addAll("action-button", "btn-edit");
                btnEditar.setTooltip(new Tooltip("Editar Emisión"));

                btnEliminar.getStyleClass().addAll("action-button", "btn-delete");
                btnEliminar.setTooltip(new Tooltip("Eliminar Registro"));

                panelBtns.setAlignment(Pos.CENTER);

                // --- LÓGICA DE LOS BOTONES ---

                // Acción Editar
                btnEditar.setOnAction(event -> {
                    Emisiones emision = getTableView().getItems().get(getIndex());
                    ventanaRegistroEmision(vista, emision);
                });

                // Acción Eliminar
                btnEliminar.setOnAction(event -> {
                    Emisiones registroEmision = getTableView().getItems().get(getIndex());
                    borradoEmision(registroEmision);
                });

                // --- PERMISOS (Ocultar si es Cliente) ---
                if (usuarioSesionActual.getRol().getNomRol().equals("CLIENTE")) {
                    btnEditar.setVisible(false);
                    btnEditar.setManaged(false);
                    btnEliminar.setVisible(false);
                    btnEliminar.setManaged(false);
                }
            }

            @Override
            protected void updateItem(Void registro, boolean vacio) {
                super.updateItem(registro, vacio);
                setGraphic(vacio ? null : panelBtns);
            }
        });

        tablaEmision.getColumns().addAll(columnaEmpresa, columnaTipo, columnaCantidad, colCO2e, colFecha, colAccion);
    }

    // ==========================================
    // 6. DIÁLOGOS Y FORMULARIOS
    // ==========================================

    /**
     * Configura el panel inciial de registro de una empresa en la base de datos
     * @param vista panel del dialogo.
     * @param empresa Entidad de Empresa.
     */
    private void ventanaRegistroEmpresa(Stage vista, Empresa empresa) {
        Dialog<Empresa> dialogos = new Dialog<>();
        dialogos.setTitle(empresa == null ? "Añadir Empresa" : "Editar Empresa");
        dialogos.initOwner(vista);
        dialogos.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        //Rejilla para ordenar los cuadros de texto del formulario
        GridPane rejilla = new GridPane();
        rejilla.setHgap(10);
        rejilla.setVgap(10);
        rejilla.setPadding(new Insets(20, 150, 10, 10));
        //Campo de nombre de la empresa
        TextField campoNombre = new TextField();
        campoNombre.setPromptText("Nombre");
        if (empresa != null) campoNombre.setText(empresa.getNombreEmpresa());
        //Campo de nombre del sector
        TextField campoSector = new TextField();
        campoSector.setPromptText("Sector");
        //Solicitamos que los campos no queden vacíos
        if (empresa != null) campoSector.setText(empresa.getSector());
        rejilla.add(new Label("Nombre:"), 0, 0);
        rejilla.add(campoNombre, 1, 0);
        rejilla.add(new Label("Sector:"), 0, 1);
        rejilla.add(campoSector, 1, 1);
        dialogos.getDialogPane().setContent(rejilla);
        //Botón de guardado de registro
        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogos.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);
        dialogos.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                if (empresa == null) {
                    return new Empresa(campoNombre.getText(), campoSector.getText());
                } else {
                    empresa.setNombreEmpresa(campoNombre.getText());
                    empresa.setSector(campoSector.getText());
                    return empresa;
                }
            }
            return null;
        });
        //Bloque de gestión de las empresas, para controlar si se está añadiendo una nueva empresa o modificando una existente
        Optional<Empresa> contenido = dialogos.showAndWait();
        contenido.ifPresent(comp -> {
            if (comp.getId() == null) {
                gestorBD.agregarEmpresa(comp);
            } else {
                gestorBD.logActualizarEmpresa(comp, usuarioSesionActual);
            }
            cargarListaEmpresa();
        });
    }

    /**
     * Configura el panel de registro o edición de una emisión.
     * @param vista panel del dialogo.
     * @param registroEmision Entidad de emisión (null para crear, objeto para editar).
     */
    private void ventanaRegistroEmision(Stage vista, Emisiones registroEmision) {
        Dialog<Emisiones> ventanaRegistro = new Dialog<>();
        ventanaRegistro.setTitle(registroEmision == null ? "Añadir Emisión" : "Editar Emisión");
        ventanaRegistro.initOwner(vista);
        try { ventanaRegistro.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm()); } catch(Exception e){}
        //Rejilla de distribución para el formulario de registro de emisión
        GridPane rejilla = new GridPane();
        rejilla.setHgap(10); rejilla.setVgap(10);
        rejilla.setPadding(new Insets(20, 150, 10, 10));
        //Combo selector de empresa para la emisión (para asegurarnos de que siempre se asigne a una entidad existente)
        ComboBox<Empresa> selectorEmpresa = new ComboBox<>();
        selectorEmpresa.setItems(FXCollections.observableArrayList(gestorBD.getTodasEmpresas()));
        //Campos de registro de la emsión
        TextField campoTipo = new TextField();
        campoTipo.setPromptText("Ej: Electricidad, Transporte");
        TextField campoCantidad = new TextField();
        campoCantidad.setPromptText("Ej: 150.5");
        TextField campoCo = new TextField();
        campoCo.setPromptText("Ej: 75.2");

        // Rellenamos el formulario con los datos de la emisión que vamos a editar
        if (registroEmision != null) {
            campoTipo.setText(registroEmision.getTipoEmision());
            campoCantidad.setText(String.valueOf(registroEmision.getCantidadEmision()));
            campoCo.setText(String.valueOf(registroEmision.getCo2e()));
            // Lo asociamos a su empresa
            for (Empresa emp : selectorEmpresa.getItems()) {
                if (emp.getId().equals(registroEmision.getIdEmpresa())) {
                    selectorEmpresa.setValue(emp);
                    break;
                }
            }
            if (!usuarioSesionActual.getRol().getNomRol().equals("ADMINISTRADOR")) {
                selectorEmpresa.setDisable(true);
            }        } else if (empresaObjetivo != null) {
            for(Empresa c : selectorEmpresa.getItems()) {
                if(c.getId().equals(empresaObjetivo.getId())) {
                    selectorEmpresa.setValue(c);
                    break;
                }
            }
        }
        //Campos del formulario
        rejilla.add(new Label("Empresa:"), 0, 0); rejilla.add(selectorEmpresa, 1, 0);
        rejilla.add(new Label("Tipo:"), 0, 1);    rejilla.add(campoTipo, 1, 1);
        rejilla.add(new Label("Cantidad:"), 0, 2); rejilla.add(campoCantidad, 1, 2);
        rejilla.add(new Label("kgCO2e:"), 0, 3);  rejilla.add(campoCo, 1, 3);
        //Despliegue del formulario
        ventanaRegistro.getDialogPane().setContent(rejilla);
        //Botones de guardado
        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ventanaRegistro.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        ventanaRegistro.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                try {
                    Empresa empresaSeleccionada = selectorEmpresa.getValue();
                    String tipo = campoTipo.getText();
                    double cantidad = Double.parseDouble(campoCantidad.getText());
                    double co2e = Double.parseDouble(campoCo.getText());
                    //Control de datos
                    if (empresaSeleccionada == null || tipo.isBlank()) {
                        throw new IllegalArgumentException("Datos incompletos");
                    }

                    // Si la acción es la de editar, mantenemos el ID y la fecha original
                    if (registroEmision != null) {
                        Emisiones emisionEditada = new Emisiones(
                                registroEmision.getId(), // ID asociado previamente a la emisión
                                tipo,
                                cantidad,
                                co2e,
                                registroEmision.getFecha().toString(), // Mantenemos la fecha de registro original
                                empresaSeleccionada.getId()
                        );
                        return emisionEditada;
                    } else {
                        // Nueva emisión para la empresa
                        return new Emisiones(tipo, cantidad, co2e, empresaSeleccionada.getId());
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> popUpError("Error", "Datos inválidos", "Revise los campos numéricos."));
                    return null;
                }
            }
            return null;
        });

        //Resultado
        Optional<Emisiones> resultado = ventanaRegistro.showAndWait();
        resultado.ifPresent(rec -> {
            if (rec.getId() != null) {
                //Si tiene ID es modificación
                gestorBD.actualizarEmision(rec, usuarioSesionActual);
            } else {
                //Si no tiene ID es nuevo registro
                gestorBD.nuevaEmision(rec);
            }
            cargarListaEmision();
            cargarListaEmpresa(); // Actualizamos los CO2e totales de las empresas, después de la modificación o adición
        });
    }

    /**
     * Diálogo modal para crear o editar un usuario.
     * Permite cambiar rol y contraseña.
     * Mantiene la ventana abierta si hay errores de validación.
     */
    private void ventanaGestionUsuario(javafx.stage.Window padre, Usuario usuarioEditado) {
        Dialog<Usuario> formulario = new Dialog<>();
        formulario.setTitle(usuarioEditado == null ? "Nuevo Usuario" : "Editar Usuario");
        formulario.initOwner(padre);
        try { formulario.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm()); } catch(Exception e){}

        GridPane rejilla = new GridPane();
        rejilla.setHgap(10); rejilla.setVgap(10);
        rejilla.setPadding(new Insets(20, 150, 10, 10));

        // Campos del formulario
        TextField txtUsuario = new TextField();
        txtUsuario.setPromptText("Login (Ej: jgarcia)");

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre y Apellidos");

        // Campo de contraseña
        PasswordField txtCon = new PasswordField();
        txtCon.setPromptText(usuarioEditado == null ? "Contraseña obligatoria" : "Dejar vacía para no cambiar");

        // Selector de Rol
        ComboBox<Rol> comboRol = new ComboBox<>();
        comboRol.setItems(FXCollections.observableArrayList(gestorBD.getTodosLosRoles()));

        // Pre-cargamos los datos asociados del usuario
        if (usuarioEditado != null) {
            txtUsuario.setText(usuarioEditado.getNombreUsuario());
            txtNombre.setText(usuarioEditado.getNombreCompleto());

            for(Rol rol : comboRol.getItems()) {
                if(rol.getId().equals(usuarioEditado.getRol().getId())) {
                    comboRol.setValue(rol);
                    break;
                }
            }

            // RESTRICCIÓN DE SEGURIDAD: No editarse el rol a uno mismo
            if (usuarioEditado.getId().equals(usuarioSesionActual.getId())) {
                comboRol.setDisable(true);
                comboRol.setTooltip(new Tooltip("No puedes cambiar tu propio rol de administrador."));
            }
        }

        rejilla.add(new Label("Usuario (Login):"), 0, 0); rejilla.add(txtUsuario, 1, 0);
        rejilla.add(new Label("Nombre Completo:"), 0, 1); rejilla.add(txtNombre, 1, 1);
        rejilla.add(new Label("Rol:"), 0, 2);    rejilla.add(comboRol, 1, 2);
        rejilla.add(new Label("Password:"), 0, 3); rejilla.add(txtCon, 1, 3);

        formulario.getDialogPane().setContent(rejilla);

        // Tipos de botones
        ButtonType btnGuardado = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        formulario.getDialogPane().getButtonTypes().addAll(btnGuardado, ButtonType.CANCEL);

        // --- VALIDACIÓN QUE IMPIDE EL CIERRE ---
        // Recuperamos el nodo del botón "Guardar"
        final Button btnGuardar = (Button) formulario.getDialogPane().lookupButton(btnGuardado);

        // Añadimos un filtro al evento: Esto se ejecuta ANTES de cerrar el diálogo
        btnGuardar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // 1. Validar campos vacíos
            if(txtUsuario.getText().trim().isEmpty() || comboRol.getValue() == null) {
                popUpError("Datos incompletos", "El usuario y el rol son obligatorios.", "Por favor, revisa el formulario.");
                event.consume(); // Evitamos que se cierre la ventana
                return;
            }

            // 2. Validar contraseña obligatoria para nuevos usuarios
            if(usuarioEditado == null && txtCon.getText().trim().isEmpty()) {
                popUpError("Falta Contraseña", "Para crear un nuevo usuario debes asignar una contraseña.", null);
                event.consume(); // Evitamos que se cierre la ventana
                return;
            }
        });

        // Resultado
        // Si pasa las validaciones se actualiza el usuario
        formulario.setResultConverter(btn -> {
            if (btn == btnGuardado) {
                return new Usuario(
                        usuarioEditado == null ? null : usuarioEditado.getId(),
                        txtUsuario.getText(),
                        txtNombre.getText(),
                        comboRol.getValue(),
                        usuarioEditado == null ? true : usuarioEditado.isActivo()
                );
            }
            //Si no, devuelve null
            return null;
        });

        // Ejecución y Procesado
        Optional<Usuario> resultado = formulario.showAndWait();
        resultado.ifPresent(u -> {
            String contrasena = txtCon.getText();

            if (usuarioEditado == null) {
                // Creamos nuevo usuario si no existe
                if (gestorBD.crearUsuario(u.getNombreUsuario(), contrasena, u.getNombreCompleto(), u.getRol())) {
                    popUpOk("Usuario Creado", "El usuario " + u.getNombreUsuario() + " ha sido registrado.");
                } else {
                    popUpError("Error", "No se pudo crear el usuario", "Es posible que el nombre de usuario ya exista.");
                    // Nota: Aquí el diálogo ya se cerró porque devolvió un resultado válido.
                    // Si quisieras controlar duplicados sin cerrar, habría que mover la llamada a BD dentro del EventFilter.
                }
            } else {
                // Actualizamos el usuario existente
                if (gestorBD.actualizarUsuarioAdmin(u, contrasena)) {
                    popUpOk("Usuario Actualizado", "Cambios guardados correctamente.");
                } else {
                    popUpError("Error", "No se pudo actualizar", null);
                }
            }
            cargarListaUsuarios();
        });
    }

    /**
     * Muestra el diálogo para la gestión de sedes (crear o editar).
     * <p>
     * Este método maneja un flujo dual: permite registrar una nueva sede o,
     * si se selecciona una del desplegable, editar sus datos existentes.
     *
     * @param empresa Empresa propietaria de las sedes.
     */
    private void mostrarGestorSedes(Empresa empresa) {
        // Configuración base del diálogo y carga de estilos CSS
        Dialog<Void> dialogoRegistroSedes = new Dialog<>();
        dialogoRegistroSedes.setTitle("Gestión de Sedes - " + empresa.getNombreEmpresa());
        dialogoRegistroSedes.setHeaderText("Crear nueva sede o editar existente");
        try { dialogoRegistroSedes.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm()); } catch(Exception e){}
        dialogoRegistroSedes.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Layout principal del formulario
        GridPane rejillaTabla = new GridPane();
        rejillaTabla.setHgap(10); rejillaTabla.setVgap(10); rejillaTabla.setPadding(new Insets(20));

        // --- SELECTOR DE MODO (CREAR vs EDITAR) ---
        // Este ComboBox actúa como interruptor: si seleccionas algo, pasas a modo edición.
        ComboBox<Sede> comboSedes = new ComboBox<>();
        comboSedes.setPromptText("Seleccionar para Editar (o dejar vacío para Nueva)");
        comboSedes.setPrefWidth(300);

        // Recuperamos las sedes actuales de la BD para llenar la lista
        List<Sede> sedesExistentes = gestorBD.getSedesPorEmpresa(empresa.getId());
        comboSedes.setItems(FXCollections.observableArrayList(sedesExistentes));

        // Campos de texto para los datos
        TextField txtCiudad = new TextField(); txtCiudad.setPromptText("Ciudad");
        TextField txtDireccion = new TextField(); txtDireccion.setPromptText("Dirección");

        // Botón de acción principal (su texto y función cambian dinámicamente)
        Button btnAccion = new Button("Crear Nueva Sede");
        btnAccion.setDefaultButton(true);

        // Al seleccionar una sede existente, rellenamos los campos y cambiamos el botón a "Guardar Modificación"
        comboSedes.setOnAction(e -> {
            Sede sedeSeleccionada = comboSedes.getValue();
            if (sedeSeleccionada != null) {
                txtCiudad.setText(sedeSeleccionada.getCiudad());
                txtDireccion.setText(sedeSeleccionada.getDireccion());
                btnAccion.setText("Guardar Modificación");
            } else {
                txtCiudad.clear(); txtDireccion.clear();
                btnAccion.setText("Crear Nueva Sede");
            }
        });

        // Botón "Limpiar/Nueva": Resetea el formulario para permitir una nueva inserción
        Button btnLimpiar = new Button("Nueva");
        btnLimpiar.setOnAction(e -> {
            comboSedes.getSelectionModel().clearSelection();
            txtCiudad.clear(); txtDireccion.clear();
            btnAccion.setText("Crear Nueva Sede");
        });

        // ACCIÓN GUARDAR
        btnAccion.setOnAction(e -> {
            String ciudad = txtCiudad.getText();
            String direccion = txtDireccion.getText();
            // Validación
            if (ciudad.isEmpty() || direccion.isEmpty()) {
                popUpError("Error", "Complete los campos", null);
                return;
            }
            Sede opcionSede = comboSedes.getValue();
            boolean opcionFinal;
            if (opcionSede == null) {
                // NUEVA ALTA
                Sede nueva = new Sede(ciudad, direccion, empresa.getId());
                opcionFinal = gestorBD.registrarSedeConAuditoria(nueva, usuarioSesionActual, empresa.getNombreEmpresa());
            } else {
                // MODIFICACIÓN
                Sede editada = new Sede(opcionSede.getId(), ciudad, direccion, empresa.getId());
                opcionFinal = gestorBD.actualizarSede(editada, usuarioSesionActual, empresa.getNombreEmpresa());
            }
            // Feedback al usuario
            if (opcionFinal) {
                popUpOk("Operación Exitosa", "Cambios guardados y auditados.");
                dialogoRegistroSedes.close();
            } else {
                popUpError("Error", "No se pudo guardar.", null);
            }
        });

        // Construcción visual de la rejilla
        rejillaTabla.add(new Label("Editar existente:"), 0, 0);
        HBox selectorCombo = new HBox(10, comboSedes, btnLimpiar);
        rejillaTabla.add(selectorCombo, 1, 0);
        rejillaTabla.add(new Separator(), 0, 1, 2, 1); // Línea separadora visual
        rejillaTabla.add(new Label("Ciudad:"), 0, 2); rejillaTabla.add(txtCiudad, 1, 2);
        rejillaTabla.add(new Label("Dirección:"), 0, 3); rejillaTabla.add(txtDireccion, 1, 3);
        rejillaTabla.add(btnAccion, 1, 4);

        dialogoRegistroSedes.getDialogPane().setContent(rejillaTabla);
        dialogoRegistroSedes.showAndWait();
    }

    // ==========================================
    // 7. CARGA DE DATOS (REFRESCO)
    // ==========================================

    /**
     * Sincroniza y actualiza la lista  de empresas mostrada en la interfaz.
     * <p>
     * Este método realiza las siguientes acciones:
     * <ol>
     * <li><b>Captura de filtro:</b> Obtiene el texto actual del campo de búsqueda ({@code busquedaEmpresa}). Si es nulo, asume una cadena vacía.</li>
     * <li><b>Consulta a BD:</b> Recupera del gestor de base de datos la lista de empresas que coinciden con el término de búsqueda.</li>
     * <li><b>Refresco de UI:</b> Limpia la lista observable ({@code infoEmpresa}) y añade los nuevos registros, lo que provoca la actualización automática de la tabla asociada.</li>
     * </ol>
     * </p>
     */
    private void cargarListaEmpresa() {
        String terminoBusqueda = (busquedaEmpresa != null) ? busquedaEmpresa.getText() : "";
        List<Empresa> empresas = gestorBD.getTodasEmpresas(terminoBusqueda);
        infoEmpresa.clear();
        infoEmpresa.addAll(empresas);
    }

    /**
     * Carga el listado de emisiones aplicando una lógica contextual (Global vs. Específica).
     * <p>
     * El comportamiento del método varía dependiendo del estado de la variable {@code empresaObjetivo}:
     * <ul>
     * <li><b>Modo Específico:</b> Si existe una empresa seleccionada ({@code empresaObjetivo != null}), se cargan únicamente las emisiones asociadas a su ID.</li>
     * <li><b>Modo Global:</b> Si no hay empresa seleccionada, se recuperan todas las emisiones registradas en el sistema.</li>
     * </ul>
     * En ambos casos, se respeta el filtro de texto introducido en {@code busquedaEmision} para refinar los resultados.
     * </p>
     */
    private void cargarListaEmision() {
        String terminoBusqueda = (busquedaEmision != null) ? busquedaEmision.getText() : "";
        List<Emisiones> emisiones;

        if (empresaObjetivo != null) {
            emisiones = gestorBD.getEmissionsByCompanyId(empresaObjetivo.getId(), terminoBusqueda);
        } else {
            emisiones = gestorBD.getTodasEmisiones(terminoBusqueda);
        }

        infoEmision.clear();
        infoEmision.addAll(emisiones);
    }

    /**
     * Refresca la lista de usuarios desde la base de datos.
     */
    private void cargarListaUsuarios() {
        infoUsuarios.clear();
        // Llama al método nuevo que creamos en GestorBD
        infoUsuarios.addAll(gestorBD.getTodosLosUsuarios());
    }

    /**
     * Carga la información asociada a las auditorias de contenido de la aplicación
     */
    private void cargarAuditoria() {
        infoAuditoria.clear();
        infoAuditoria.addAll(gestorBD.getLogsAuditoria());
    }

    /**
     * Restablece la vista de la lista de empresas a su estado inicial.
     * <p>
     * Limpia los filtros de búsqueda, resetea la selección de empresa objetivo
     * y recarga los datos actualizados desde la base de datos.
     */
    private void mostrarTodasEmpresas() {
        // Reseteamos el objetivo para salir del contexto de una empresa específica
        empresaObjetivo = null;

        // Limpiamos el campo de búsqueda si existe
        if (busquedaEmpresa != null) {
            busquedaEmpresa.setText("");
        }

        // Recargamos la tabla con todos los registros
        cargarListaEmpresa();
    }

    /**
     * Restablece la vista de la lista de emisiones a su estado inicial.
     * <p>
     * Limpia los filtros de búsqueda, resetea la selección de empresa objetivo
     * y recarga los datos actualizados desde la base de datos.
     */
    private void mostrarTodasEmisiones() {
        // Reseteamos el objetivo para salir del contexto de una relación empresa-emision específica
        empresaObjetivo = null;
        tarjetaEmision.setText("Todas las Emisiones");
        // Limpiamos el campo de búsqueda si existe
        if (busquedaEmision != null) busquedaEmision.setText("");
        // Recargamos la tabla con todos los registros
        cargarListaEmision();
    }

    /**
     * Filtrado de emisiones por una empresa
     * <p>
     * Limpia los filtros de búsqueda, resetea la selección de empresa objetivo
     * y recarga los datos actualizados desde la base de datos.
     * @param empresa entidad de Empresa
     */
    private void mostrarEmisionesPorEmpresa(Empresa empresa) {
        empresaObjetivo = empresa;
        tarjetaEmision.setText("Emisiones de " + empresa.getNombreEmpresa());
        if (busquedaEmision != null) busquedaEmision.setText("");
        cargarListaEmision();
    }

    // ==========================================
    // 8. ACCIONES DE USUARIO (BORRAR, EXPORTAR)
    // ==========================================

    /**
     * Configura la lógica de borrado de la empresa (solo disponible para el administrador)
     * @param empresa entidad sobre la que se acciona.
     */
    private void borradoEmpresa(Empresa empresa) {
        //Configuramos una alerta que da información de la acción a realizar
        Alert infoUsuario = new Alert(Alert.AlertType.CONFIRMATION);
        infoUsuario.setTitle("Confirmar Eliminación");
        infoUsuario.setHeaderText("¿Eliminar empresa '" + empresa.getNombreEmpresa() + "'?");
        infoUsuario.setContentText("TODAS sus emisiones serán borradas. Esta acción no se puede deshacer.");
        infoUsuario.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        //Se comprueba el estado de la lista después de la acción y se refresca
        Optional<ButtonType> estadoPostBorrado = infoUsuario.showAndWait();
        if (estadoPostBorrado.isPresent() && estadoPostBorrado.get() == ButtonType.OK) {
            gestorBD.borrarEmpresa(empresa.getId());
            cargarListaEmpresa();
        }
    }

    /**
     * Configura la lógica de borrado de la empresa
     * @param emision entidad sobre la que se acciona.
     */
    private void borradoEmision(Emisiones emision) {
        //Configuramos una alerta que da información de la acción a realizar
        Alert infoUsuario = new Alert(Alert.AlertType.CONFIRMATION);
        infoUsuario.setTitle("Confirmar Eliminación");
        infoUsuario.setHeaderText("¿Eliminar registro de " + emision.getTipoEmision() + "?");
        infoUsuario.setContentText("Esta acción no se puede deshacer.");
        infoUsuario.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        //Se comprueba el estado de la lista después de la acción y se refresca
        Optional<ButtonType> estadoPostBorrado = infoUsuario.showAndWait();
        if (estadoPostBorrado.isPresent() && estadoPostBorrado.get() == ButtonType.OK) {
            gestorBD.borrarEmision(emision.getId());
            cargarListaEmision();
            cargarListaEmpresa();
        }
    }

    /**
     * Majea la lógica de exportación de datos
     * @param contActual contenido que se va a recoger para la exportación de datos
     */
    private void exportarEmpresas(Stage contActual) {
        //Gestor de archivos
        FileChooser ventanaExportacion = new FileChooser();
        //Titulo de la ventana del explorador de archivos
        ventanaExportacion.setTitle("Guardar Lista de Empresas");
        //Nombre por defecto para el csv
        ventanaExportacion.setInitialFileName("empresas.csv");
        //Archivo
        File archivoCSV = ventanaExportacion.showSaveDialog(contActual);
        //Mensajes de información para el usuario en función del resultado de la exportación
        if (archivoCSV != null) {
            try {
                ControlCSV.exportarEmpresas(infoEmpresa, archivoCSV);
                popUpOk("Exportación Completa", "Empresas exportadas con éxito a:\n" + archivoCSV.getAbsolutePath());
            } catch (Exception ex) {
                popUpError("Error de Exportación", "No se pudo guardar el archivo.", ex.getMessage());
            }
        }
    }

    /**
     * Maneja la lógica de exportación de datos
     * @param contActual contenido que se va a recoger para la exportación de datos
     */
    private void exportarEmisiones(Stage contActual) {
        //Gestor de archivos
        FileChooser nombreArchivo = new FileChooser();
        //Titulo de la ventana del explorador de archivos
        nombreArchivo.setTitle("Guardar Vista de Emisiones");
        //Nombre por defecto para el csv, en esta función se añade el nombre de la empresa sobre la que se exportan las emisiones
        String cadenaEmpresaEmision = empresaObjetivo != null ? "emisiones_" + empresaObjetivo.getNombreEmpresa() : "todas_las_emisiones";
        nombreArchivo.setInitialFileName(cadenaEmpresaEmision.replaceAll("[^a-zA-Z0-9]", "") + ".csv");
        File archivoCSV = nombreArchivo.showSaveDialog(contActual);
        //Mensajes de información para el usuario en función del resultado de la exportación
        if (archivoCSV != null) {
            try {
                ControlCSV.exportarEmisiones(infoEmision, archivoCSV);
                popUpOk("Exportación Completa", "Emisiones exportadas con éxito a:\n" + archivoCSV.getAbsolutePath());
            } catch (Exception ex) {
                popUpError("Error de Exportación", "No se pudo guardar el archivo.", ex.getMessage());
            }
        }
    }

    // ==========================================
    // 9. UTILIDADES Y AYUDA
    // ==========================================

    /**
     * Maneja la lógica de despliegue del manual de usuario
     * @param html contenedor del manual
     */
    private void desplegarManual(Stage html) {
        Stage manualUsuario = new Stage();
        //Titulo de la ventana del manual
        manualUsuario.setTitle("Manual de Usuario - Carbon Tracker");
        //Ventana
        WebView vista = new WebView();
        WebEngine auxiliarVista = vista.getEngine();
        //Carga del manual en origen
        try {
            String direccion = getClass().getResource("manual.html").toExternalForm();
            auxiliarVista.load(direccion);
        } catch (Exception e) {
            auxiliarVista.loadContent("<html><body><h1>Error</h1><p>No se pudo encontrar el archivo del manual (manual.html).</p></body></html>");
        }
        //Vista principal del manual
        VBox vPrincipal = new VBox(vista);
        VBox.setVgrow(vista, Priority.ALWAYS);
        //Dimensiones por defecto
        Scene vistaManualUsuario = new Scene(vPrincipal, 900, 700);
        manualUsuario.setScene(vistaManualUsuario);
        //Lanzadores de estado y despliegue del manual
        manualUsuario.initOwner(html);
        manualUsuario.initModality(Modality.NONE);
        manualUsuario.show();
    }

    /**
     * Función auxiliar para manejar los TAGS asignados a los iconos bajo el titulo
     * <p>
     *
     * @param espacioIconos entidad de Empresa
     */
    private Label fncTarjetas(String espacioIconos) {
        Label tarjeta = new Label(espacioIconos);
        tarjeta.getStyleClass().add("tag");
        return tarjeta;
    }

    /**
     * Función auxiliar para manejar los pop up de error y poder personalizar su estilo
     * <p>
     *
     * @param titulo Titulo de la ventana
     * @param cabecera Cabecera del pop up
     * @param contenido Contenido de información arrojada al usuario
     */
    private void popUpError(String titulo, String cabecera, String contenido) {
        Alert alertaInfoUsuario = new Alert(Alert.AlertType.ERROR);
        alertaInfoUsuario.setTitle(titulo);
        alertaInfoUsuario.setHeaderText(cabecera);
        alertaInfoUsuario.setContentText(contenido);
        try {
            alertaInfoUsuario.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        } catch (Exception e) { }
        alertaInfoUsuario.showAndWait();
    }

    /**
     * Función auxiliar para manejar los pop up de exito y poder personalizar su estilo
     * <p>
     *
     * @param tituloVentana titulo del pop up
     * @param contenido Contenido arrojado al usuario
     */
    private void popUpOk(String tituloVentana, String contenido) {
        Alert alertaCompletado = new Alert(Alert.AlertType.INFORMATION);
        alertaCompletado.setTitle(tituloVentana);
        alertaCompletado.setHeaderText(null);
        alertaCompletado.setContentText(contenido);
        try {
            alertaCompletado.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        } catch (Exception e) { }
        alertaCompletado.showAndWait();
    }
}