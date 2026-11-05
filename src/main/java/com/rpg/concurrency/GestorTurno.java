package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

/**
 * Administra el ciclo de vida del hilo temporizador de cada turno.
 * Lanza un hilo nuevo para cada turno y permite cancelarlo mediante interrupt().
 */
public class GestorTurno {

    private final int segundos;
    private final TurnoCallback callback;
    private Thread hiloTemporizador;

    public GestorTurno(int segundos, TurnoCallback callback) {
        this.segundos = segundos;
        this.callback = callback;
    }

    /**
     * Inicia un hilo nuevo para el conteo regresivo del turno.
     * Nunca se reutiliza un hilo existente (no se llama start() dos veces sobre el mismo Thread).
     */
    public void iniciar() {
        cancelar();
        TemporizadorTurnoTask tarea = new TemporizadorTurnoTask(segundos, callback);
        hiloTemporizador = new Thread(tarea, "TemporizadorTurno");
        hiloTemporizador.start();
    }

    /**
     * Cancela el temporizador enviando una senal de interrupcion al hilo.
     * No utiliza banderas booleanas manuales, apoyandose en interrupt() e InterruptedException.
     */
    public void cancelar() {
        if (hiloTemporizador != null && hiloTemporizador.isAlive()) {
            hiloTemporizador.interrupt();
        }
    }

    public boolean estaActivo() {
        return hiloTemporizador != null && hiloTemporizador.isAlive();
    }
}

