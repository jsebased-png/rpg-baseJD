package com.miapp.model;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para la entidad Monstruo.
 * Administra el catalogo del Bestiario en la base de datos utilizando
 * exclusivamente PreparedStatement y try-with-resources.
 */
public class MonstruoDAO {

    private static final int GENERAR_CLAVES_PRIMARIAS = 1;

    /**
     * Inserta un nuevo monstruo en el catalogo.
     */
    public boolean crear(Monstruo monstruo) {
        String sql = "INSERT INTO monstruos (nombre, vida, ataque) VALUES (?, ?, ?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, GENERAR_CLAVES_PRIMARIAS)) {

            ps.setString(1, monstruo.getNombre());
            ps.setInt(2, monstruo.getVida());
            ps.setInt(3, monstruo.getAtaque());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        monstruo.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[MonstruoDAO.crear] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtiene un monstruo por su ID primario.
     */
    public Monstruo obtenerPorId(int id) {
        String sql = "SELECT id, nombre, vida, ataque FROM monstruos WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearMonstruo(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[MonstruoDAO.obtenerPorId] Error SQL: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca un monstruo por su nombre exacto o ignorando mayusculas.
     */
    public Monstruo buscarPorNombre(String nombre) {
        String sql = "SELECT id, nombre, vida, ataque FROM monstruos WHERE LOWER(nombre) = LOWER(?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nombre != null ? nombre.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearMonstruo(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[MonstruoDAO.buscarPorNombre] Error SQL: " + e.getMessage());
        }
        return null;
    }

    /**
     * Actualiza las estadisticas o nombre de un monstruo existente.
     */
    public boolean actualizar(Monstruo monstruo) {
        String sql = "UPDATE monstruos SET nombre = ?, vida = ?, ataque = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, monstruo.getNombre());
            ps.setInt(2, monstruo.getVida());
            ps.setInt(3, monstruo.getAtaque());
            ps.setInt(4, monstruo.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MonstruoDAO.actualizar] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /**
     * Elimina un monstruo por su ID.
     */
    public boolean eliminar(int id) {
        String sql = "DELETE FROM monstruos WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MonstruoDAO.eliminar] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lista todos los monstruos registrados en el Bestiario.
     */
    public List<Monstruo> listarTodos() {
        List<Monstruo> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, vida, ataque FROM monstruos ORDER BY id ASC";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearMonstruo(rs));
            }
        } catch (SQLException e) {
            System.err.println("[MonstruoDAO.listarTodos] Error SQL: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Lista monstruos cuya vida sea mayor o igual a un umbral minimo.
     *
     * @param vidaMinima Umbral de puntos de vida
     * @return Lista de monstruos que cumplen el filtro
     */
    public List<Monstruo> listarPorVidaMinima(int vidaMinima) {
        List<Monstruo> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, vida, ataque FROM monstruos WHERE vida >= ? ORDER BY vida ASC";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, vidaMinima);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMonstruo(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[MonstruoDAO.listarPorVidaMinima] Error SQL: " + e.getMessage());
        }
        return lista;
    }

    private Monstruo mapearMonstruo(ResultSet rs) throws SQLException {
        return new Monstruo(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getInt("vida"),
                rs.getInt("ataque")
        );
    }
}

