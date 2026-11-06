package demo;

import com.miapp.model.ConexionDB;
import com.miapp.model.Usuario;
import com.miapp.model.UsuarioDAO;

/**
 * ============================================================================
 * DEMOSTRACION EDUCATIVA: LA AMENAZA (INYECCION SQL) Y EL ESCUDO
 * ============================================================================
 *
 * RESTRICCION DE AUDITORIA:
 * Esta es la UNICA clase del proyecto donde aparece la palabra 'Statement' (sin 'Prepared'),
 * con fines pedagogicos, para evidenciar la vulnerabilidad de la concatenacion de cadenas.
 *
 * Esta clase vive en el paquete 'demo' (fuera de model), por eso no importa ninguna clase de JDBC:
 * obtiene la conexion de ConexionDB y usa inferencia de tipos (var) sobre los objetos JDBC
 * (createStatement() devuelve un Statement; executeQuery() un ResultSet).
 */
public class LoginInseguroDemo {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println(" DEMO EDUCATIVA: INYECCION SQL (VULNERABLE) VS PREPAREDSTATEMENT");
        System.out.println("===============================================================");

        // NOTA: MySQL/MariaDB exigen un espacio tras "--" para que sea comentario; por eso el payload termina en espacio.
        String usuarioMalicioso = "admin' OR 1=1 -- ";
        String claveCualquiera = "cualquierClaveErronea123";

        System.out.println("\n[1] Intentando autenticacion INSEGURA con concatenacion (Statement)...");
        System.out.println("    Payload inyectado en Usuario: [" + usuarioMalicioso + "]");
        System.out.println("    Contrasena falsa:            [" + claveCualquiera + "]");

        Usuario vulnerado = loginVulnerable(usuarioMalicioso, claveCualquiera);
        if (vulnerado != null) {
            System.out.println("\n[!] ALERTA ROJA: VULNERABILIDAD EXPLOTADA EXITOSAMENTE");
            System.out.println("    El sistema autentico al usuario SIN conocer su clave legitima!");
            System.out.println("    Sesion iniciada como: " + vulnerado.getUsuario() + " (Rol: " + vulnerado.getRol() + ")");
        } else {
            System.out.println("[-] No se pudo autenticar (BD no conectada o tabla vacia).");
        }

        System.out.println("\n---------------------------------------------------------------");
        System.out.println("[2] Intentando autenticacion SEGURA con PreparedStatement (UsuarioDAO.autenticar)...");
        Usuario seguro = new UsuarioDAO().autenticar(usuarioMalicioso, claveCualquiera);

        if (seguro != null) {
            System.out.println("[!] ERROR INESPERADO: Se autentico.");
        } else {
            System.out.println("[+] ESCUDO ACTIVO: Ataque neutralizado con exito.");
            System.out.println("    PreparedStatement trato el payload como literal de texto ('" + usuarioMalicioso + "').");
            System.out.println("    No se altero el arbol sintactico SQL. Acceso denegado.");
        }
        System.out.println("===============================================================");
    }

    /**
     * METODO VULNERABLE DIDACTICO: Statement + concatenacion directa de Strings.
     * La entrada: admin' OR 1=1 --   (con un espacio final)
     * genera:
     *   SELECT ... FROM jugadores WHERE usuario = 'admin' OR 1=1 -- ' AND clave = '...'
     *
     * El caracter ' cierra prematuramente el literal; 'OR 1=1' vuelve el WHERE una TAUTOLOGIA;
     * y '-- ' convierte el resto (la validacion de la clave) en comentario.
     */
    public static Usuario loginVulnerable(String usuario, String clave) {
        String sql = "SELECT id, usuario, clave, rol FROM jugadores WHERE usuario = '"
                + usuario + "' AND clave = '" + clave + "'";

        System.out.println("\n[SQL Concatenado Ejecutado]:\n  " + sql);

        try (var conn = ConexionDB.getConnection();
             var stmt = conn.createStatement();          // <-- Statement (sin Prepared): VULNERABLE
             var rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return new Usuario(rs.getInt("id"), rs.getString("usuario"),
                        rs.getString("clave"), rs.getString("rol"));
            }
        } catch (Exception e) {
            System.err.println("[loginVulnerable] Error SQL: " + e.getMessage());
        }
        return null;
    }

    /**
     * BENCHMARK DIDACTICO: 1000 inserciones en una tabla temporal con tres estrategias:
     * Statement concatenado, PreparedStatement y PreparedStatement en batch.
     * Mide tiempos reales (System.nanoTime) para completar la tabla del INFORME.md.
     */
    public static void benchmarkInserciones() {
        final int n = 1000;
        try (var conn = ConexionDB.getConnection(); var ddl = conn.createStatement()) {
            ddl.executeUpdate("CREATE TABLE IF NOT EXISTS bench_tmp (id INT AUTO_INCREMENT PRIMARY KEY, valor VARCHAR(50))");

            ddl.executeUpdate("TRUNCATE TABLE bench_tmp");
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                ddl.executeUpdate("INSERT INTO bench_tmp (valor) VALUES ('fila-" + i + "')");
            }
            long msStatement = (System.nanoTime() - t0) / 1_000_000;

            ddl.executeUpdate("TRUNCATE TABLE bench_tmp");
            t0 = System.nanoTime();
            try (var ps = conn.prepareStatement("INSERT INTO bench_tmp (valor) VALUES (?)")) {
                for (int i = 0; i < n; i++) {
                    ps.setString(1, "fila-" + i);
                    ps.executeUpdate();
                }
            }
            long msPrepared = (System.nanoTime() - t0) / 1_000_000;

            ddl.executeUpdate("TRUNCATE TABLE bench_tmp");
            t0 = System.nanoTime();
            try (var ps = conn.prepareStatement("INSERT INTO bench_tmp (valor) VALUES (?)")) {
                for (int i = 0; i < n; i++) {
                    ps.setString(1, "fila-" + i);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            long msBatch = (System.nanoTime() - t0) / 1_000_000;

            ddl.executeUpdate("DROP TABLE bench_tmp");
            System.out.println("\n=== 1000 INSERCIONES (tiempos medidos) ===");
            System.out.println("Statement concatenado ........ " + msStatement + " ms");
            System.out.println("PreparedStatement ............ " + msPrepared + " ms");
            System.out.println("PreparedStatement + batch .... " + msBatch + " ms");
        } catch (Exception e) {
            System.err.println("[benchmarkInserciones] Error SQL: " + e.getMessage());
        }
    }
}
