package com.miapp.view;

import com.miapp.controller.CombateController;
import com.miapp.controller.MonstruoController;
import com.miapp.model.Monstruo;

import java.util.List;

/**
 * Vista para la interaccion del Combate en la Arena (MVC - View).
 * Renderiza la seleccion de oponente, la cronica de golpes y el estado de persistencia.
 */
public class CombateUI {

    private final CombateController combateController;
    private final MonstruoController monstruoController;

    public CombateUI(CombateController combateController, MonstruoController monstruoController) {
        this.combateController = combateController;
        this.monstruoController = monstruoController;
    }

    public void iniciarEnfrentamiento() {
        System.out.println("\n========================================================");
        System.out.println("               ARENA DE COMBATE - ENFRENTAMIENTO        ");
        System.out.println("========================================================");

        List<Monstruo> rivales = monstruoController.listarTodos();
        if (rivales.isEmpty()) {
            System.out.println("No hay monstruos disponibles en el Bestiario para combatir.");
            ConsolaUtil.pausar();
            return;
        }

        System.out.println("Selecciona a tu contrincante:");
        for (Monstruo m : rivales) {
            System.out.printf("  [%d] %-20s (HP: %3d | ATK: %2d)%n",
                    m.getId(), m.getNombre(), m.getVida(), m.getAtaque());
        }

        int idRival = ConsolaUtil.leerEntero("\nIngresa el ID del rival (0 para cancelar): ", 0, 999999);
        if (idRival == 0) {
            System.out.println("Combate cancelado. Regresando al menu...");
            return;
        }

        int clase = ConsolaUtil.leerEntero("Elige tu clase (1. Guerrero, 2. Mago): ", 1, 2);

        System.out.println("\n¡Los tambores de guerra retumban en la Arena!");
        var resultado = combateController.simularCombate(idRival, clase);

        System.out.println("\n--- CRONICA DEL ENFRENTAMIENTO ---");
        for (String linea : resultado.registro()) {
            System.out.println(linea);
        }

        System.out.println("--------------------------------------------------------");
        if (resultado.victoria()) {
            System.out.println("[RESULTADO] ¡FELICITACIONES! Has ganado el duelo.");
            if (resultado.guardadaEnBD()) {
                System.out.println("[BD] Victoria almacenada con exito en tu historial y sumada al ranking.");
            }
        } else {
            System.out.println("[RESULTADO] HAS SIDO DERROTADO.");
            System.out.println("[BD] Recordatorio: Al haber perdido, la base de datos NO guarda este combate.");
        }
        System.out.println("========================================================");
        ConsolaUtil.pausar();
    }
}

