package com.miapp.controller;

import com.miapp.model.VictoriaDAO;
import com.miapp.model.HistorialVictoriaDTO;
import com.miapp.model.Usuario;
import com.miapp.model.RankingDTO;

import java.util.Collections;
import java.util.List;

/**
 * Controlador de Puntuaciones y Clasificacion General (MVC - Controller).
 * Coordina la generacion del ranking y el historial de enfrentamientos.
 */
public class RankingController {

    private final VictoriaDAO victoriaDAO;
    private final UsuarioController usuarioController;

    public RankingController(UsuarioController usuarioController) {
        this(new VictoriaDAO(), usuarioController);
    }

    public RankingController(VictoriaDAO victoriaDAO, UsuarioController usuarioController) {
        this.victoriaDAO = victoriaDAO;
        this.usuarioController = usuarioController;
    }

    /**
     * Consulta el Ranking General de jugadores ordenados por victorias.
     */
    public List<RankingDTO> obtenerRankingGeneral() {
        return victoriaDAO.rankingPorJugador();
    }

    /**
     * Consulta el historial de victorias del usuario autenticado en la sesion actual.
     */
    public List<HistorialVictoriaDTO> obtenerHistorialJugadorActual() {
        Usuario actual = usuarioController.getUsuarioAutenticado();
        if (actual == null) {
            return Collections.emptyList();
        }
        return victoriaDAO.victoriasDeJugador(actual.getUsuario());
    }

    /**
     * Consulta el historial de victorias de cualquier usuario especifico.
     */
    public List<HistorialVictoriaDTO> obtenerHistorialDeUsuario(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            return Collections.emptyList();
        }
        return victoriaDAO.victoriasDeJugador(usuario.trim());
    }
}

