package com.miapp.model.rpg.concurrency;

import com.miapp.model.rpg.entities.EstadoEfecto;
import com.miapp.model.rpg.entities.Personaje;

/**
 * Tarea concurrente para efectos periodicos continuos (DoT - Damage over Time o HoT - Heal over Time).
 * Corre en su propio hilo y aplica ticks de dano o curacion a intervalos regulares.
 * Se detiene limpiamente respondiendo a Thread.interrupt() o cuando el personaje muere / el efecto termina.
 */
public class EfectoContinuoTask implements Runnable {

    private final Personaje personaje;
    private final EstadoEfecto efecto;
    private final int intervaloMs;

    public EfectoContinuoTask(Personaje personaje, EstadoEfecto efecto, int intervaloMs) {
        if (personaje == null || efecto == null) {
            throw new IllegalArgumentException("Personaje y efecto no pueden ser nulos");
        }
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.personaje = personaje;
        this.efecto = efecto;
        this.intervaloMs = intervaloMs;
    }

    public EfectoContinuoTask(Personaje personaje, String nombreEfecto, int intervaloMs) {
        this(personaje, new EstadoEfecto(nombreEfecto, com.miapp.model.rpg.entities.enums.TipoEfecto.DANIO_CONTINUO, 4, 3), intervaloMs);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && personaje.estaVivo() && !efecto.haTerminado()) {
                Thread.sleep(intervaloMs);

                // Consulta de isInterrupted() tras despertar por si fue interrumpido justo despues del sleep
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                if (!personaje.estaVivo()) {
                    break;
                }

                if (efecto.esDanio()) {
                    personaje.recibirDanio(efecto.getValorPorTick());
                    System.out.printf("[Efecto en segundo plano] %s sufre %d de dano por %s (HP: %d/%d)%n",
                            personaje.getNombre(), efecto.getValorPorTick(), efecto.getNombre(),
                            personaje.getVida(), personaje.getVidaMaxima());
                } else {
                    int curado = personaje.curar(efecto.getValorPorTick());
                    System.out.printf("[Efecto en segundo plano] %s recupera %d de vida por %s (HP: %d/%d)%n",
                            personaje.getNombre(), curado, efecto.getNombre(),
                            personaje.getVida(), personaje.getVidaMaxima());
                }

                efecto.consumirTurno();
            }
        } catch (InterruptedException e) {
            // Manejo estricto de InterruptedException:
            // 1. Limpiar el estado del personaje (remover el efecto)
            // 2. Restaurar el estado de interrupcion del hilo
            // 3. Salir limpiamente del bucle con break
            personaje.quitarEfecto(efecto.getNombre());
            Thread.currentThread().interrupt();
        } finally {
            // El bloque finally asegura que el efecto se retire del personaje al finalizar la tarea
            personaje.quitarEfecto(efecto.getNombre());
        }
    }

    public String getNombreEfecto() {
        return efecto.getNombre();
    }
}

