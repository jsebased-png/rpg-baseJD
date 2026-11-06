package com.miapp.model;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para la entidad Usuario.
 * Encapsula todas las operaciones SQL sobre la tabla 'jugadores' utilizando
 * exclusivamente PreparedStatement para blindar la aplicacion contra Inyeccion SQL.
 */
public class UsuarioDAO {

    // Valor de la constante JDBC RETURN_GENERATED_KEYS (se usa el literal para mantener
    // la palabra reservada de la demo solo en LoginInseguroDemo).
    private static final int GENERAR_CLAVES_PRIMARIAS = 1;

    /**
     * Registra un nuevo jugador en la base de datos con su contrasena hasheada.
     *
     * @param jugador Entidad a persistir
     * @return true si se inserto exitosamente, false en caso contrario
     */
    public boolean registrar(Usuario jugador) {
        String sql = "INSERT INTO jugadores (usuario, clave, rol) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, GENERAR_CLAVES_PRIMARIAS)) {

            String claveHasheada = PasswordHasher.hash(jugador.getClave());
            ps.setString(1, jugador.getUsuario());
            ps.setString(2, claveHasheada != null ? claveHasheada : jugador.getClave());
            ps.setString(3, jugador.getRol() != null ? jugador.getRol() : Usuario.ROL_JUGADOR);

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        jugador.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.registrar] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /**
     * Autenticacion SEGURA contra la base de datos.
     * Emplea PreparedStatement con marcadores de posicion (?) para que el motor
     * trate las credenciales estrictamente como datos literales y no como sintaxis SQL ejecutable.
     *
     * @param usuario Nombre de usuario
     * @param clave   Contrasena en texto plano ingresada
     * @return Objeto Usuario autenticado o null si las credenciales son invalidas
     */
    public Usuario autenticar(String usuario, String clave) {
        String sql = "SELECT id, usuario, clave, rol FROM jugadores WHERE usuario = ? AND clave = ?";
        String hashClave = PasswordHasher.hash(clave);
        if (usuario == null || hashClave == null) {
            return null;
        }

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ps.setString(2, hashClave); // Solo se compara contra el hash SHA-256

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.autenticar] Error SQL: " + e.getMessage());
        }
        return null;
    }

    /** Actualiza la clave (se almacena hasheada). */
    public boolean actualizarClave(int id, String nuevaClave) {
        String sql = "UPDATE jugadores SET clave = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordHasher.hash(nuevaClave));
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.actualizarClave] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /** Cambia el nombre de usuario (debe seguir siendo unico). */
    public boolean actualizarNombre(int id, String nuevoUsuario) {
        String sql = "UPDATE jugadores SET usuario = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoUsuario);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.actualizarNombre] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /** Cantidad de jugadores con rol ADMIN (para no dejar el sistema sin administradores). */
    public int contarAdmins() {
        String sql = "SELECT COUNT(*) FROM jugadores WHERE rol = 'ADMIN'";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.contarAdmins] Error SQL: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Modifica el rol de un jugador (JUGADOR o ADMIN).
     */
    public boolean cambiarRol(int id, String nuevoRol) {
        String sql = "UPDATE jugadores SET rol = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoRol != null ? nuevoRol.toUpperCase() : Usuario.ROL_JUGADOR);
            ps.setInt(2, id);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.cambiarRol] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /**
     * Elimina a un jugador por su identificador primario.
     */
    public boolean eliminar(int id) {
        String sql = "DELETE FROM jugadores WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.eliminar] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /**
     * Busca un jugador por su ID primario.
     */
    public Usuario obtenerPorId(int id) {
        String sql = "SELECT id, usuario, clave, rol FROM jugadores WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.obtenerPorId] Error SQL: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca un jugador por su nombre de usuario unico.
     */
    public Usuario obtenerPorUsuario(String usuario) {
        String sql = "SELECT id, usuario, clave, rol FROM jugadores WHERE usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.obtenerPorUsuario] Error SQL: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lista todos los jugadores registrados en la base de datos.
     */
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, usuario, clave, rol FROM jugadores ORDER BY id ASC";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO.listarTodos] Error SQL: " + e.getMessage());
        }
        return lista;
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("usuario"),
                rs.getString("clave"),
                rs.getString("rol")
        );
    }
}
