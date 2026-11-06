package com.miapp.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ============================================================================
 * GESTOR DE CONEXION JDBC - PROYECTO "ARENA SEGURA"
 * ============================================================================
 *
 * RESPUESTAS TEORICAS OBLIGATORIAS:
 *
 * 1. Que es JDBC (Java Database Connectivity)?
 *    Es una API estandar de Java (parte de java.sql y javax.sql) que define una
 *    capa de abstraccion uniforme orientada a interfaces (Connection, PreparedStatement,
 *    ResultSet, etc.). Permite a las aplicaciones Java interactuar con cualquier
 *    sistema de gestion de bases de datos relacionales (RDBMS) sin acoplar el codigo
 *    a un motor especifico, siempre que exista un driver que implemente dicha API.
 *
 * 2. Que hace DriverManager?
 *    Es la clase utilitaria central de la infraestructura JDBC que actua como factoria
 *    y mediador de conexiones. Se encarga de:
 *      a) Administrar la lista de controladores (Drivers) JDBC registrados en la JVM
 *         (historicamente mediante Class.forName() o modernamente por SPI ServiceLoader
 *         escaneando META-INF/services/java.sql.Driver dentro de los JARs del classpath).
 *      b) Inspeccionar las URLs de conexion suministradas (e.g., jdbc:mysql://...).
 *      c) Localizar el driver que reconozca dicho prefijo y delegar en el la creacion
 *         de un objeto fisico java.sql.Connection autenticado.
 *
 * 3. Que pasa exactamente si falta el driver?
 *    - Si se intenta cargar manualmente la clase con:
 *      Class.forName("com.mysql.cj.jdbc.Driver");
 *      y el JAR (mysql-connector-j) no se encuentra en el classpath, la JVM lanza inmediatamente:
 *      java.lang.ClassNotFoundException: com.mysql.cj.jdbc.Driver
 *
 *    - En JDBC 4.0+ (carga automatica via ServiceLoader), si el JAR del driver falta
 *      en el classpath al momento de invocar DriverManager.getConnection(url, user, pass),
 *      DriverManager no encuentra ningun Driver registrado que soporte la URL "jdbc:mysql:...",
 *      arrojando exactamente la siguiente excepcion SQL:
 *      java.sql.SQLException: No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db...
 * ============================================================================
 */
public class ConexionDB {

    private static final String ARCHIVO_CONFIG = "config.txt";

    private static String url;
    private static String user;
    private static String password;

    /**
     * Carga config.txt (carpeta desde la que se ejecuta la app). Si no existe o esta
     * incompleto, falla con un mensaje claro en lugar de usar credenciales por defecto.
     */
    private static synchronized void cargarConfiguracion() throws SQLException {
        if (url != null) {
            return;
        }
        Path ruta = Path.of(ARCHIVO_CONFIG).toAbsolutePath();
        if (!Files.exists(ruta)) {
            throw new SQLException("No se encontro " + ruta + ". Copia config.example.txt como config.txt "
                    + "y ajusta db.url, db.user y db.password (ejecuta la app desde la carpeta del proyecto).");
        }
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(ruta.toFile())) {
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("No se pudo leer " + ruta + ": " + e.getMessage(), e);
        }
        String u = props.getProperty("db.url");
        String us = props.getProperty("db.user");
        String pw = props.getProperty("db.password");
        if (u == null || u.isBlank() || us == null || pw == null) {
            throw new SQLException("config.txt incompleto: se requieren db.url, db.user y db.password.");
        }
        url = u.trim();
        user = us.trim();
        password = pw;
    }

    /**
     * Obtiene una nueva conexion. El invocador debe cerrarla con try-with-resources.
     *
     * @throws SQLException si falta la configuracion, el driver, o falla la conexion.
     */
    public static Connection getConnection() throws SQLException {
        cargarConfiguracion();
        return DriverManager.getConnection(url, user, password);
    }

    /** Sobrescribe la configuracion en tiempo de ejecucion (pruebas). */
    public static synchronized void configurar(String nuevaUrl, String nuevoUser, String nuevoPassword) {
        url = nuevaUrl;
        user = nuevoUser;
        password = nuevoPassword;
    }
}
