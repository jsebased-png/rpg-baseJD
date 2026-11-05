package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

/**
 * Tarea de cuenta regresiva para el turno del jugador.
 * Ejecuta un segundo por iteracion y, si no es interrumpida antes de llegar a cero,
 * notifica al callback que el tiempo se ha agotado.
 */
public class TemporizadorTurnoTask implements Runnable {

    private final int segundos;
    private final TurnoCallback callback;

    public TemporizadorTurnoTask(int segundos, TurnoCallback callback) {
        if (segundos <= 0) {
            throw new IllegalArgumentException("La duracion debe ser mayor a 0 segundos");
        }
        if (callback == null) {
            throw new IllegalArgumentException("El callback no puede ser nulo");
        }
        this.segundos = segundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            for (int i = segundos; i > 0; i--) {
                Thread.sleep(1000);
            }
            callback.onTiempoAgotado();
        } catch (InterruptedException e) {
            // El hilo fue interrumpido porque el jugador respondio a tiempo o el combate finalizo.
            // Se restaura el estado de interrupcion y se sale limpiamente sin llamar al callback.
            Thread.currentThread().interrupt();
        }
    }
}

