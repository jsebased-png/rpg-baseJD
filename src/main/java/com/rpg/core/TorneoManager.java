package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/**
 * Torneo de 4 gladiadores: dos semifinales en paralelo coordinadas con join(), y una final.
 */
public class TorneoManager {

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /**
     * TALLER CONCURRENCIA - Seccion 3:
     * Ejecuta las dos semifinales en paralelo usando dos hilos independientes.
     * El hilo principal sincroniza con join() y solo consulta los ganadores tras la culminacion de ambos.
     * Inspecciona y muestra Thread.getState() para evidenciar el ciclo de vida de los hilos.
     * @return el campeon del torneo
     */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();

        final Personaje[] finalistas = new Personaje[2];

        // Creacion de hilos para cada semifinal con sus respectivos Runnables
        Thread hiloSemi1 = new Thread(() -> {
            finalistas[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1));
        }, "Hilo-Semifinal-1");

        Thread hiloSemi2 = new Thread(() -> {
            finalistas[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3));
        }, "Hilo-Semifinal-2");

        // Ciclo de vida: Estado NEW (creado, sin invocar start)
        registro.agregar("[Torneo] Estado Semi 1 antes de start: %s", hiloSemi1.getState());
        registro.agregar("[Torneo] Estado Semi 2 antes de start: %s", hiloSemi2.getState());

        // Iniciar ejecucion concurrente
        hiloSemi1.start();
        hiloSemi2.start();

        // Ciclo de vida: Estado durante ejecucion (RUNNABLE o TIMED_WAITING)
        registro.agregar("[Torneo] Estado Semi 1 tras start: %s", hiloSemi1.getState());
        registro.agregar("[Torneo] Estado Semi 2 tras start: %s", hiloSemi2.getState());

        // El hilo principal espera a que ambas semifinales culminen con join()
        try {
            hiloSemi1.join();
            hiloSemi2.join();
        } catch (InterruptedException e) {
            registro.agregar("Error: El torneo fue interrumpido: %s", e.getMessage());
            Thread.currentThread().interrupt();
        }

        // Ciclo de vida: Estado TERMINATED (finalizado tras join)
        registro.agregar("[Torneo] Estado Semi 1 tras join: %s", hiloSemi1.getState());
        registro.agregar("[Torneo] Estado Semi 2 tras join: %s", hiloSemi2.getState());

        // Consultar los ganadores solo despues de que ambos join hayan retornado
        Personaje finalista1 = finalistas[0];
        Personaje finalista2 = finalistas[1];

        if (finalista1 == null || finalista2 == null) {
            registro.agregar("Error: No se pudieron obtener los finalistas del torneo.");
            return gladiadores.get(0);
        }

        return simularFinal(finalista1, finalista2);
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
