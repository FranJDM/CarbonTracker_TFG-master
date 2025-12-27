package ct.Principal;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Clase de utilidad diseñada para habilitar el redimensionamiento manual de ventanas
 * que tienen el estilo {@code StageStyle.UNDECORATED}.
 * <p>
 * Al eliminar los bordes nativos del sistema operativo, se pierde la capacidad de
 * estirar la ventana desde los bordes. Esta clase restaura esa funcionalidad
 * detectando la posición del ratón en los bordes de la escena.
 */
public class AuxiliaReajusteVentana {

    /**
     * Inicializa y activa la lógica de redimensionamiento para un escenario (Contenedor de la ventana principal).
     * <p>
     *  Añade los disparadores (listeners) de eventos de ratón necesarios a la escena e integra
     *  los eventos a todos los hijos para asegurar que el redimensionamiento
     *  funcione incluso si el ratón está sobre un componente de la interfaz (aunque no debería de hacerse).
     *
     * @param contenedorPrincipal El escenario del contendor principal al que se aplicará la funcionalidad.
     */
    public static void gestorReajuste(Stage contenedorPrincipal) {
        eventoReajustar eventoReajuste = new eventoReajustar(contenedorPrincipal);
        contenedorPrincipal.getScene().addEventHandler(MouseEvent.MOUSE_MOVED, eventoReajuste);
        contenedorPrincipal.getScene().addEventHandler(MouseEvent.MOUSE_PRESSED, eventoReajuste);
        contenedorPrincipal.getScene().addEventHandler(MouseEvent.MOUSE_DRAGGED, eventoReajuste);
        contenedorPrincipal.getScene().addEventHandler(MouseEvent.MOUSE_EXITED, eventoReajuste);
        contenedorPrincipal.getScene().addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, eventoReajuste);


        ObservableList<Node> eventoAnidado = contenedorPrincipal.getScene().getRoot().getChildrenUnmodifiable();
        for (Node anidado : eventoAnidado) {
            reajustesVentana(anidado, eventoReajuste);
        }
    }

    /**
     * Método auxiliar para asegurar que los eventos de redimensionamiento
     * también funcionen en los nodos hijo de la interfaz.
     * Lo que quiere decir esto, es que el redimensionamiento funcionará en cualquier
     * pantalla de las creadas (distintas categorías)
     * <p>
     * Esto evita que componentes como botones o paneles bloqueen la detección
     * del borde de la ventana.
     *
     * @param ptoReajuste El nodo (elemento) actual al que se le añade el evento.
     * @param evento El evento que maneja el redimensionamiento.
     */
    public static void reajustesVentana(Node ptoReajuste, EventHandler<MouseEvent> evento) {
        ptoReajuste.addEventHandler(MouseEvent.MOUSE_MOVED, evento);
        ptoReajuste.addEventHandler(MouseEvent.MOUSE_PRESSED, evento);
        ptoReajuste.addEventHandler(MouseEvent.MOUSE_DRAGGED, evento);
        ptoReajuste.addEventHandler(MouseEvent.MOUSE_EXITED, evento);
        ptoReajuste.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, evento);
        if (ptoReajuste instanceof Parent) {
            Parent contPrincipal = (Parent) ptoReajuste;
            ObservableList<Node> contenedorAnidado = contPrincipal.getChildrenUnmodifiable();
            for (Node anidado : contenedorAnidado) {
                reajustesVentana(anidado, evento);
            }
        }
    }

    /**
     * Clase encargada de procesar los eventos del ratón y calcular
     * el redimensionamiento de la ventana.
     */
    static class eventoReajustar implements EventHandler<MouseEvent> {

        /** Referencia al escenario que se va a redimensionar. */
        private Stage contenedorVista;

        /** Almacena el tipo de cursor actual (flecha, redimensionar N, S, E, O, etc.). */
        private Cursor eventoRaton = Cursor.DEFAULT;

        /** Define el grosor en píxeles del "borde invisible" sensible al ratón. */
        private int bordeAncla = 4;

        /** Coordenada X inicial al presionar el ratón. */
        private double ejeX = 0;
        /** Coordenada Y inicial al presionar el ratón. */
        private double ejeY = 0;

        /**
         * Constructor para el manejador de eventos.
         * @param contenedorVista La vista que se va a controlar.
         */
        public eventoReajustar(Stage contenedorVista) {
            this.contenedorVista = contenedorVista;
        }

        /**
         * Esta clase vale para alternar el aspecto del cursor a la hora de redimensionar la ventana.
         * <p>
         * Lógica:
         * <ul>
         * <li><b>MOUSE_MOVED:</b> Detecta si el ratón está en un borde y cambia el icono del cursor.</li>
         * <li><b>MOUSE_PRESSED:</b> Guarda la posición inicial para calcular el desplazamiento.</li>
         * <li><b>MOUSE_DRAGGED:</b> Calcula el nuevo tamaño y posición de la ventana basándose en el movimiento.</li>
         * </ul>
         *
         * @param eventoRaton El evento de ratón capturado.
         */
        @Override
        public void handle(MouseEvent eventoRaton) {
            EventType<? extends MouseEvent> tipoEventoRaton = eventoRaton.getEventType();
            Scene contenedor = contenedorVista.getScene();

            double eventoRatonX = eventoRaton.getSceneX();
            double eventoRatonY = eventoRaton.getSceneY();
            double anchoContenedor = contenedor.getWidth();
            double alturaContenedor = contenedor.getHeight();

            if (MouseEvent.MOUSE_MOVED.equals(tipoEventoRaton)) {
                // --- ESQUINAS ---
                if (eventoRatonX < bordeAncla && eventoRatonY < bordeAncla) {
                    this.eventoRaton = Cursor.NW_RESIZE;
                } else if (eventoRatonX < bordeAncla && eventoRatonY > alturaContenedor - bordeAncla) {
                    this.eventoRaton = Cursor.SW_RESIZE;
                } else if (eventoRatonX > anchoContenedor - bordeAncla && eventoRatonY < bordeAncla) {
                    this.eventoRaton = Cursor.NE_RESIZE;
                } else if (eventoRatonX > anchoContenedor - bordeAncla && eventoRatonY > alturaContenedor - bordeAncla) {
                    this.eventoRaton = Cursor.SE_RESIZE;
                } else if (eventoRatonX < bordeAncla) {
                    this.eventoRaton = Cursor.W_RESIZE;
                } else if (eventoRatonX > anchoContenedor - bordeAncla) {
                    this.eventoRaton = Cursor.E_RESIZE;
                } else if (eventoRatonY < bordeAncla) {
                    this.eventoRaton = Cursor.N_RESIZE;
                } else if (eventoRatonY > alturaContenedor - bordeAncla) {
                    this.eventoRaton = Cursor.S_RESIZE;
                } else {
                    this.eventoRaton = Cursor.DEFAULT;
                }
                contenedor.setCursor(this.eventoRaton);
            }
            // --- BORDES LATERALES Y SUPERIORES ---
            // -----------------------------------------------------------------
            // RESTAURACIÓN DEL CURSOR
            // -----------------------------------------------------------------
            else if (MouseEvent.MOUSE_EXITED.equals(tipoEventoRaton) || MouseEvent.MOUSE_EXITED_TARGET.equals(tipoEventoRaton)) {
                contenedor.setCursor(Cursor.DEFAULT);
            } else if (MouseEvent.MOUSE_PRESSED.equals(tipoEventoRaton)) {
                ejeX = contenedorVista.getWidth() - eventoRatonX;
                ejeY = contenedorVista.getHeight() - eventoRatonY;
                // -----------------------------------------------------------------
                // EJECUCIÓN DEL REDIMENSIONADO
                // -----------------------------------------------------------------
            } else if (MouseEvent.MOUSE_DRAGGED.equals(tipoEventoRaton)) {
                if (!Cursor.DEFAULT.equals(this.eventoRaton)) {
                    if (!Cursor.W_RESIZE.equals(this.eventoRaton) && !Cursor.E_RESIZE.equals(this.eventoRaton)) {
                        double alturaMin = contenedorVista.getMinHeight() > (bordeAncla * 2) ? contenedorVista.getMinHeight() : (bordeAncla * 2);
                        if (Cursor.NW_RESIZE.equals(this.eventoRaton) || Cursor.N_RESIZE.equals(this.eventoRaton) || Cursor.NE_RESIZE.equals(this.eventoRaton)) {
                            if (contenedorVista.getHeight() > alturaMin || eventoRatonY < 0) {
                                contenedorVista.setHeight(contenedorVista.getY() - eventoRaton.getScreenY() + contenedorVista.getHeight());
                                contenedorVista.setY(eventoRaton.getScreenY());
                            }
                        } else {
                            if (contenedorVista.getHeight() > alturaMin || eventoRatonY + ejeY - contenedorVista.getHeight() > 0) {
                                contenedorVista.setHeight(eventoRatonY + ejeY);
                            }
                        }
                    }

                    if (!Cursor.N_RESIZE.equals(this.eventoRaton) && !Cursor.S_RESIZE.equals(this.eventoRaton)) {
                        double anchoMin = contenedorVista.getMinWidth() > (bordeAncla * 2) ? contenedorVista.getMinWidth() : (bordeAncla * 2);
                        if (Cursor.NW_RESIZE.equals(this.eventoRaton) || Cursor.W_RESIZE.equals(this.eventoRaton) || Cursor.SW_RESIZE.equals(this.eventoRaton)) {
                            if (contenedorVista.getWidth() > anchoMin || eventoRatonX < 0) {
                                contenedorVista.setWidth(contenedorVista.getX() - eventoRaton.getScreenX() + contenedorVista.getWidth());
                                contenedorVista.setX(eventoRaton.getScreenX());
                            }
                        } else {
                            if (contenedorVista.getWidth() > anchoMin || eventoRatonX + ejeX - contenedorVista.getWidth() > 0) {
                                contenedorVista.setWidth(eventoRatonX + ejeX);
                            }
                        }
                    }
                }
            }
        }
    }
}