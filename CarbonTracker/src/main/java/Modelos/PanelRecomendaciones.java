package Modelos;

import DAO.GestorBD;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Componente de la sección "Optimización y Recomendaciones".
 * <p>
 * Su función principal es analizar los datos de emisiones de una empresa seleccionada,
 * identificar los puntos críticos y generar un plan de acción genérico y exportable.
 */
public class PanelRecomendaciones extends VBox {

    /** Instancia del gestor de datos para realizar consultas. */
    private final GestorBD gestorBD;

    /** Contenedor que se encarga de actualizar la información para el reporte generado. */
    private final VBox contenedorResultados;

    // Variables para la exportación del archivo con los consejos
    private String nombreEmpresaActual = "";
    private String textoResumen = "";
    private String[] consejosActuales = new String[]{};

    /**
     * Inicializa el panel de recomendaciones con su estructura base y el selector de empresas.
     *
     * @param gestorBD El gestor de base de datos inicializado.
     */
    public PanelRecomendaciones(GestorBD gestorBD) {
        this.gestorBD = gestorBD;

        // Configuración del estilo base del panel (Tarjeta)
        this.getStyleClass().add("tarjeta-dashboard");
        this.setPadding(new Insets(30));
        this.setSpacing(20);
        this.setAlignment(Pos.TOP_LEFT);

        // Cabecera
        HBox cabecera = new HBox(20);
        cabecera.setAlignment(Pos.CENTER);
        cabecera.setPadding(new Insets(25, 0, 15, 0));
        Text tituloSeccion = new Text("Centro de Optimización de Huella");
        tituloSeccion.getStyleClass().add("titulo-dash");

        ComboBox<Empresa> selectorEmpresa = new ComboBox<>();
        selectorEmpresa.setPromptText("Seleccione una Empresa para analizar...");
        selectorEmpresa.setPrefWidth(300);
        selectorEmpresa.setItems(FXCollections.observableArrayList(gestorBD.getTodasEmpresas()));

        // Listener
        selectorEmpresa.setOnAction(e -> {
            Empresa seleccionada = selectorEmpresa.getValue();
            if (seleccionada != null) {
                generarAnalisis(seleccionada);
            }
        });

        cabecera.getChildren().addAll(tituloSeccion, selectorEmpresa);

        // Resultados
        contenedorResultados = new VBox(15);
        contenedorResultados.setAlignment(Pos.TOP_LEFT);

        // Estado inicial (cuando no hay selección)
        mostrarMensajeInicio();

        this.getChildren().addAll(cabecera, new Separator(), contenedorResultados);
    }

    /**
     * Muestra un mensaje indicando al usuario que debe seleccionar una empresa.
     */
    private void mostrarMensajeInicio() {
        contenedorResultados.getChildren().clear();

        VBox mensajeInicio = new VBox(10);
        mensajeInicio.setAlignment(Pos.CENTER);
        mensajeInicio.setPadding(new Insets(50));
        //Hardcodeado de estilos para la librería ikonly
        FontIcon iconoLuz = new FontIcon(MaterialDesign.MDI_LIGHTBULB);
        iconoLuz.setIconSize(48);
        iconoLuz.getStyleClass().add("msg-icono-defecto");

        Text instruccion = new Text("Seleccione una empresa arriba para generar su plan de acción.");
        instruccion.getStyleClass().add("msg-defecto");
        mensajeInicio.getChildren().addAll(iconoLuz, instruccion);
        contenedorResultados.getChildren().add(mensajeInicio);
    }

    /**
     * Clase principal para la generación del informe de optimización.
     * <p>
     * Recupera los datos, calcula la métrica de emisión más alta y construye la interfaz visual.
     *
     * @param empresa La empresa sobre la que realizar el análisis.
     */
    private void generarAnalisis(Empresa empresa) {
        contenedorResultados.getChildren().clear();
        this.nombreEmpresaActual = empresa.getNombreEmpresa();

        // 1. Se recopilan los datos
        List<Map.Entry<String, Double>> reporte = gestorBD.getReporteEmisionesPorEmpresa(empresa.getId());

        if (reporte.isEmpty()) {
            Label sinDatos = new Label("⚠ No hay registros de emisiones para " + empresa.getNombreEmpresa() + ".");
            sinDatos.getStyleClass().add("msg-vacio");
            contenedorResultados.getChildren().add(sinDatos);
            return;
        }

        // 2. Se calcula el impacto y se obtiene el tipo de emisión dominante
        Map.Entry<String, Double> mayorFuente = reporte.stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String tipoDominante = mayorFuente.getKey();
        double totalDominante = mayorFuente.getValue();

        // Se preparan los datos con formato para la exportación
        this.textoResumen = "Área crítica: " + tipoDominante + " (" + String.format("%.2f", totalDominante) + " kg CO2e)";

        // 3. Se configura la barra de herramientas (Exportar/Imprimir)
        HBox barraHerramientas = new HBox(10);
        barraHerramientas.setAlignment(Pos.CENTER_RIGHT);
        //Nube de configuración de botones
        //Imprimir
        Button btnImprimir = new Button("Imprimir", new FontIcon(MaterialDesign.MDI_PRINTER));
        btnImprimir.setOnAction(e -> imprimirReporte(contenedorResultados));
        //Descargar
        Button btnDescargar = new Button("Descargar .TXT", new FontIcon(MaterialDesign.MDI_DOWNLOAD));
        btnDescargar.setOnAction(e -> descargarReporteTxt());
        //Espaciador de botones
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        //Título personalizado para que se muestre la empresa de la que se va a hacer el reporte
        Text subtituloPersonalizado = new Text("Análisis para: " + empresa.getNombreEmpresa());
        subtituloPersonalizado.getStyleClass().add("subtitulo-reporte");
        //Configuración de la barra de herramientas con todos los elementos
        barraHerramientas.getChildren().addAll(subtituloPersonalizado, espaciador, btnImprimir, btnDescargar);
        // 4. Se configura la estructura del cuerpo del reporte
        Text resumenInforme = new Text("El área crítica detectada es " + tipoDominante + " con un total de " + String.format("%.2f", totalDominante) + " kg CO2e.");
        resumenInforme.getStyleClass().add("msg-reporte");
        // Caja contenedora de recomendaciones
        VBox cajaRecomendaciones = new VBox(10);
        cajaRecomendaciones.getStyleClass().add("cabecera-recomendacion");
        //Etiqueta del plan de acción
        Label etiquetaRecomendaciones = new Label("PLAN DE ACCIÓN RECOMENDADO:");
        etiquetaRecomendaciones.getStyleClass().add("cabecera-recomendacion");

        cajaRecomendaciones.getChildren().add(etiquetaRecomendaciones);

        // Configuración de la lista de consejos por emisión
        this.consejosActuales = obtenerConsejos(tipoDominante);

        for (String consejo : consejosActuales) {
            HBox objConsejo = new HBox(10);

            FontIcon iconoConsejo = new FontIcon(MaterialDesign.MDI_CHECK_CIRCLE);
            iconoConsejo.getStyleClass().add("icono-exito");

            Text txtConsejo = new Text(consejo);
            txtConsejo.getStyleClass().add("texto-recomendacion");

            objConsejo.getChildren().addAll(iconoConsejo, txtConsejo);
            cajaRecomendaciones.getChildren().add(objConsejo);
        }

        contenedorResultados.getChildren().addAll(barraHerramientas, resumenInforme, cajaRecomendaciones);
    }

    // Métodos para la exportación en los diferentes formatos

    /**
     * Lanza el diálogo de impresión del sistema operativo para el panel actual.
     * @param seleccionImpresa Nodo raíz a imprimir.
     */
    private void imprimirReporte(Node seleccionImpresa) {
        PrinterJob impresionReporte = PrinterJob.createPrinterJob();
        if (impresionReporte != null && impresionReporte.showPrintDialog(this.getScene().getWindow())) {
            boolean correcto = impresionReporte.printPage(seleccionImpresa);
            if (correcto) {
                impresionReporte.endJob();
            }
        }
    }

    /**
     * Genera y guarda un archivo .txt con el contenido del análisis.
     */
    private void descargarReporteTxt() {
        FileChooser selectorTxt = new FileChooser();
        selectorTxt.setTitle("Guardar Plan de Acción");
        selectorTxt.setInitialFileName("Plan_Accion_" + nombreEmpresaActual.replaceAll(" ", "_") + ".txt");
        selectorTxt.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos de Texto", "*.txt"));

        File archivo = selectorTxt.showSaveDialog(this.getScene().getWindow());

        if (archivo != null) {
            try (FileWriter fw = new FileWriter(archivo)) {
                fw.write("========================================\n");
                fw.write("   CARBON TRACKER - PLAN DE ACCIÓN\n");
                fw.write("========================================\n\n");
                fw.write("Empresa: " + nombreEmpresaActual + "\n");
                fw.write("Fecha: " + LocalDate.now() + "\n\n");
                fw.write("--- ANÁLISIS ---\n");
                fw.write(textoResumen + "\n\n");
                fw.write("--- RECOMENDACIONES ---\n");
                for (String consejo : consejosActuales) {
                    fw.write("[x] " + consejo + "\n");
                }
                fw.write("\n\nGenerado por Carbon Tracker App");

                mostrarAlerta("Éxito", "El archivo se ha guardado correctamente.");
            } catch (IOException ex) {
                mostrarAlerta("Error", "No se pudo guardar el archivo: " + ex.getMessage());
            }
        }
    }

    /**
     * Configuración personalizada para mostrar alertas al usuario.
     */
    private void mostrarAlerta(String titulo, String contenido) {
        Alert popUp = new Alert(Alert.AlertType.INFORMATION);
        popUp.setTitle(titulo);
        popUp.setHeaderText(null);
        popUp.setContentText(contenido);
        try {
            Stage ventana = (Stage) popUp.getDialogPane().getScene().getWindow();
            popUp.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) { /* Ignorar */ }
        popUp.showAndWait();
    }

    /**
     * Retorna sugerencias basadas en la categoría de emisión.
     * Este contenido es reciclable y exportable. Estos consejos son genéricos y limitados
     * la opción más lógica sería insertar un token de alguna IA para poder darlos de forma
     * personalizada.
     */
    private String[] obtenerConsejos(String tipoRecomendado) {
        return switch (tipoRecomendado.toUpperCase()) {
            case "ELECTRICIDAD" -> new String[]{
                    "Realizar una auditoría energética (ISO 50001).",
                    "Instalar sensores de movimiento e iluminación LED.",
                    "Contratar proveedores de energía 100% renovable.",
                    "Programar el apagado automático de maquinaria en stand-by."
            };
            case "TRANSPORTE" -> new String[]{
                    "Optimizar rutas de distribución mediante software GPS.",
                    "Renovar la flota hacia vehículos eléctricos o híbridos.",
                    "Fomentar el teletrabajo para reducir desplazamientos in itinere.",
                    "Revisar la presión de neumáticos mensualmente (ahorro 5-10%)."
            };
            case "COMBUSTIÓN", "CALEFACCIÓN" -> new String[]{
                    "Sustituir calderas antiguas por bombas de calor aerotérmicas.",
                    "Mejorar el aislamiento térmico de las instalaciones.",
                    "Instalar termostatos inteligentes y zonificados."
            };
            case "RESIDUOS" -> new String[]{
                    "Implementar política de 'Residuo Cero' en oficinas.",
                    "Negociar envases retornables con proveedores.",
                    "Separar y valorizar subproductos industriales."
            };
            default ->
                    new String[]{"Contacte con un consultor ambiental para un análisis detallado de esta categoría."};
        };
    }
}