# 🌍 Carbon Footprint Tracker

**Carbon Tracker** es una aplicación de escritorio moderna desarrollada en **Java** y **JavaFX** diseñada para gestionar, calcular y monitorizar la huella de carbono de diferentes empresas.

La aplicación permite llevar un registro detallado de las emisiones (electricidad, transporte, residuos, etc.), calcular el equivalente de CO2 (kgCO2e) y gestionar el acceso mediante un sistema de roles y usuarios seguro.

![Estado](https://img.shields.io/badge/Estado-Terminado-success)
![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![Base de Datos](https://img.shields.io/badge/SQLite-Incrustada-lightgrey)

## ✨ Características Principales

* **📊 Gestión de Empresas:** Registro, edición y eliminación de empresas con cálculo automático del total de emisiones acumuladas.
* **🏭 Rastreo de Emisiones:** Registro detallado por tipo (transporte, energía, etc.), cantidad y fecha.
* **🔐 Seguridad y Roles (RBAC):** Sistema de login y registro con contraseñas hasheadas (SHA-256).
    * **ADMIN:** Control total (Crear/Editar/Borrar empresas y emisiones).
    * **USER:** Gestión de emisiones y edición de datos básicos.
    * **CLIENT:** Acceso de solo lectura y reportes.
* **🔍 Filtrado y Búsqueda:** Barras de búsqueda en tiempo real para filtrar por nombre, sector, tipo de emisión o fecha.
* **📂 Exportación de Datos:** Capacidad para exportar listados de empresas y registros de emisiones a formato **CSV**.
* **📖 Ayuda Integrada:** Manual de usuario accesible desde la propia aplicación.
* **🎨 Interfaz Moderna:** UI limpia construida con JavaFX, CSS personalizado e iconos vectoriales (Ikonli).

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Java 21
* **Framework UI:** JavaFX (con FXML y CSS)
* **Gestión de Dependencias:** Maven
* **Base de Datos:** SQLite (vía JDBC)
* **Librerías Adicionales:**
    * `sqlite-jdbc`: Conector de base de datos.
    * `ikonli`: Paquete de iconos Material Design.

## 🚀 Instalación y Ejecución

### Prerrequisitos
* Tener instalado **Java JDK 17** o superior (recomendado JDK 21).
* Tener **Maven** instalado (o usar el wrapper de tu IDE).

### Pasos para ejecutar

1.  **Clonar el repositorio:**
    ```bash
    git clone [https://github.com/TU_USUARIO/CarbonTracker.git](https://github.com/TU_USUARIO/CarbonTracker.git)
    cd CarbonTracker
    ```

2.  **Compilar y Ejecutar:**
    La aplicación utiliza el plugin de Maven para JavaFX para gestionar las rutas de módulos correctamente. Ejecuta el siguiente comando en la raíz del proyecto:
    ```bash
    mvn clean javafx:run
    ```

> **Nota:** La primera vez que se ejecute, la aplicación creará automáticamente la base de datos `carbon_tracker.db` e insertará los datos iniciales y el usuario administrador.

## 👤 Usuarios y Roles por Defecto

Para acceder por primera vez, utiliza las credenciales de administrador generadas automáticamente:

| Usuario | Contraseña | Rol | Permisos |
| :--- | :--- | :--- | :--- |
| **admin** | `admin` | **ADMIN** | Acceso total. |
| *(Registro)*| *(Registro)* | **USER** | Puede añadir emisiones, pero no crear empresas. |
| *(Registro)*| *(Registro)* | **CLIENT** | Solo puede ver datos y exportar CSV. |

*Puedes registrar nuevos usuarios (USER o CLIENT) desde la pantalla de login.*

## 📂 Estructura del Proyecto

El proyecto sigue una arquitectura separada por capas para facilitar el mantenimiento:

* **`src/main/java/ct/carbontracker`**:
    * `App.java`: Controlador principal, gestión de escenas y lógica de UI.
    * `LoginDialog.java`: Gestión de autenticación y registro.
* **`src/main/java/DAO`**:
    * `DataBaseManager.java`: Capa de acceso a datos (CRUD, conexión SQLite, inicialización).
    * `CsvExporter.java`: Utilidad para la generación de reportes.
* **`src/main/java/Modelos`**:
    * Clases POJO (`Company`, `EmissionRecord`, `Usuario`, `Rol`).
* **`src/main/resources`**:
    * `style.css`: Estilos de la interfaz.
    * `manual.html`: Archivo de ayuda.

## 📸 Capturas de Pantalla

*(Opcional: Añade aquí imágenes de tu aplicación funcionando)*

1.  **Pantalla de Login**
2.  **Dashboard Principal**
3.  **Listado de Emisiones**

## 📄 Licencia

Este proyecto está distribuido bajo la licencia MIT. Eres libre de usarlo y modificarlo.

---
Hecho con ☕ y JavaFX.
