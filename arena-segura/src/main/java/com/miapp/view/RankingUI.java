package com.miapp.view;

import com.miapp.controller.RankingController;
import com.miapp.model.HistorialVictoriaDTO;
import com.miapp.model.RankingDTO;

import java.util.List;

/**
 * Vista de Clasificacion y Estadisticas de Puntuacion (MVC - View).
 * Renderiza el Ranking General y el Historial de Victorias.
 */
public class RankingUI {

    private final RankingController rankingController;

    public RankingUI(RankingController rankingController) {
        this.rankingController = rankingController;
    }

    public void mostrarRankingGeneral() {
        System.out.println("\n========================================================");
        System.out.println("              RANKING GENERAL DE LA ARENA               ");
        System.out.println("========================================================");

        List<RankingDTO> ranking = rankingController.obtenerRankingGeneral();
        if (ranking.isEmpty()) {
            System.out.println("Aun no se registran victorias en la base de datos.");
        } else {
            System.out.printf("%-4s | %-18s | %-9s | %-10s%n", "POS", "JUGADOR", "ROL", "VICTORIAS");
            System.out.println("-----+--------------------+-----------+-----------");
            for (RankingDTO r : ranking) {
                System.out.printf("#%02d  | %-18s | %-9s | %4d vit.%n",
                        r.getPosicion(), r.getUsuario(), r.getRol(), r.getTotalVictorias());
            }
        }
        System.out.println("========================================================");
        ConsolaUtil.pausar();
    }

    public void mostrarHistorialPersonal() {
        System.out.println("\n========================================================");
        System.out.println("                 MI HISTORIAL DE VICTORIAS              ");
        System.out.println("========================================================");

        List<HistorialVictoriaDTO> historial = rankingController.obtenerHistorialJugadorActual();
        if (historial.isEmpty()) {
            System.out.println("No tienes victorias registradas todavia. ¡Entra a la Arena y gana!");
        } else {
            System.out.printf("Total victorias acumuladas: %d%n%n", historial.size());
            for (HistorialVictoriaDTO h : historial) {
                System.out.printf(" - [%s] Venciste a '%s' (HP: %d, ATK: %d)%n",
                        h.getFecha() != null ? h.getFecha().toString().replace('T', ' ') : "N/D",
                        h.getNombreMonstruo(), h.getVidaMonstruo(), h.getAtaqueMonstruo());
            }
        }
        System.out.println("========================================================");
        ConsolaUtil.pausar();
    }
}

