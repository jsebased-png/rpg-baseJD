package com.miapp.model;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para la persistencia de Victorias y consulta de Rankings.
 * Aplica reglas estrictas de JDBC: PreparedStatement exclusivo, try-with-resources
 * y retorno exclusivo de POJOs / colecciones de entidades.
 */
public class VictoriaDAO {

    /**
     * Registra una nueva victoria en la base de datos con fecha/hora actual.
     * REGLA DE NEGOCIO: Este metodo solo debe ser invocado cuando el Usuario gana.
     * Si pierde, el Controller omite esta invocacion.
     *
     * @param jugadorId  Identificador del jugador ganador
     * @param monstruoId Identificador del monstruo derrotado
     * @return true si se registro la victoria con exito
     */
    public boolean registrar(int jugadorId, int monstruoId) {
        String sql = "INSERT INTO victorias (jugador_id, monstruo_id, fecha) VALUES (?, ?, NOW())";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, jugadorId);
            ps.setInt(2, monstruoId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[VictoriaDAO.registrar] Error SQL: " + e.getMessage());
        }
        return false;
    }

    /**
     * Genera el Ranking General consolidado mediante una consulta SQL con JOIN y GROUP BY,
     * ordenado descendentemente por el conteo de victorias de cada jugador.
     *
     * @return Lista de RankingDTO con la clasificacion general
     */
    public List<RankingDTO> rankingPorJugador() {
        List<RankingDTO> ranking = new ArrayList<>();
        String sql = """
            SELECT j.usuario, j.rol, COUNT(v.id) AS total_victorias
            FROM jugadores j
            JOIN victorias v ON j.id = v.jugador_id
            GROUP BY j.id, j.usuario, j.rol
            ORDER BY total_victorias DESC, j.usuario ASC
        """;

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int posicion = 1;
            while (rs.next()) {
                ranking.add(new RankingDTO(
                        posicion++,
                        rs.getString("usuario"),
                        rs.getString("rol"),
                        rs.getInt("total_victorias")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[VictoriaDAO.rankingPorJugador] Error SQL: " + e.getMessage());
        }
        return ranking;
    }

    /**
     * Consulta el historial cronologico detallado de victorias de un jugador especifico.
     *
     * @param usuario Nombre de usuario a consultar
     * @return Lista de enfrentamientos ganados con datos del monstruo y marca temporal
     */
    public List<HistorialVictoriaDTO> victoriasDeJugador(String usuario) {
        List<HistorialVictoriaDTO> historial = new ArrayList<>();
        String sql = """
            SELECT v.id, j.usuario, m.nombre AS nombre_monstruo, m.vida, m.ataque, v.fecha
            FROM victorias v
            JOIN jugadores j ON v.jugador_id = j.id
            JOIN monstruos m ON v.monstruo_id = m.id
            WHERE LOWER(j.usuario) = LOWER(?)
            ORDER BY v.fecha DESC
        """;

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario != null ? usuario.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha");
                    LocalDateTime fechaHora = ts != null ? ts.toLocalDateTime() : null;

                    historial.add(new HistorialVictoriaDTO(
                            rs.getInt("id"),
                            rs.getString("usuario"),
                            rs.getString("nombre_monstruo"),
                            rs.getInt("vida"),
                            rs.getInt("ataque"),
                            fechaHora
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[VictoriaDAO.victoriasDeJugador] Error SQL: " + e.getMessage());
        }
        return historial;
    }

    /**
     * Cuenta cuantas victorias historicas estan asociadas a un monstruo especifico.
     * Es utilizado por el Controller para la regla de BORRADO SEGURO, evitando
     * que la base de datos falle con violacion de llave foranea (FK constraint).
     *
     * @param monstruoId ID del monstruo a consultar
     * @return Cantidad de registros en la tabla 'victorias' que referencian este monstruo
     */
    public int contarPorMonstruoId(int monstruoId) {
        String sql = "SELECT COUNT(*) FROM victorias WHERE monstruo_id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, monstruoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[VictoriaDAO.contarPorMonstruoId] Error SQL: " + e.getMessage());
        }
        return 0;
    }

    /** Cuenta las victorias de un jugador (regla de borrado seguro de jugadores). */
    public int contarPorJugadorId(int jugadorId) {
        String sql = "SELECT COUNT(*) FROM victorias WHERE jugador_id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jugadorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[VictoriaDAO.contarPorJugadorId] Error SQL: " + e.getMessage());
        }
        return 0;
    }

    /** Lista todas las victorias como POJOs Victoria (mas recientes primero). */
    public List<Victoria> listarTodas() {
        List<Victoria> lista = new ArrayList<>();
        String sql = "SELECT id, jugador_id, monstruo_id, fecha FROM victorias ORDER BY fecha DESC";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha");
                lista.add(new Victoria(rs.getInt("id"), rs.getInt("jugador_id"),
                        rs.getInt("monstruo_id"), ts != null ? ts.toLocalDateTime() : null));
            }
        } catch (SQLException e) {
            System.err.println("[VictoriaDAO.listarTodas] Error SQL: " + e.getMessage());
        }
        return lista;
    }
}
