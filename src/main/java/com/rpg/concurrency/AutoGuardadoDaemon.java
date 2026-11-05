package com.rpg.concurrency;

/**
 * Hilo demonio (Daemon Thread) que realiza guardados automaticos del estado del juego en segundo plano.
 *
 * INVESTIGACION OBLIGATORIA:
 * 1. Diferencia entre Hilo de Usuario (User Thread) e Hilo Daemon:
 *    - Hilo de usuario: Son los hilos principales de la aplicacion. La JVM permanece viva mientras
 *      exista al menos un hilo de usuario en ejecucion, impidiendo el cierre del programa.
 *    - Hilo daemon: Son hilos de servicio o soporte en segundo plano (como el Garbage Collector de Java).
 *      No impiden que la JVM termine. Cuando el ultimo hilo de usuario finaliza (por ejemplo, al salir del
 *      menu en Main), la JVM termina de inmediato sin esperar a que los hilos daemon completen su bucle.
 * 2. Momento para configurar el hilo como daemon:
 *    - Debe llamarse a {@code thread.setDaemon(true)} OBLIGATORIAMENTE antes de llamar a {@code thread.start()}.
 *    - Si se llama {@code setDaemon(true)} despues de haber iniciado el hilo con {@code start()},
 *      la JVM lanza una {@link IllegalThreadStateException}.
 */
public class AutoGuardadoDaemon implements Runnable {

    private final Runnable accionGuardado;
    private final int intervaloMs;

    public AutoGuardadoDaemon(Runnable accionGuardado, int intervaloMs) {
        if (accionGuardado == null) {
            throw new IllegalArgumentException("La accion de guardado no puede ser nula");
        }
        if (intervaloMs <= 0) {
            throw new IllegalArgumentException("El intervalo debe ser positivo");
        }
        this.accionGuardado = accionGuardado;
        this.intervaloMs = intervaloMs;
    }

    /**
     * Fabrica y arranca un hilo daemon configurado correctamente antes de start().
     */
    public static Thread iniciar(Runnable accionGuardado, int intervaloMs) {
        AutoGuardadoDaemon tarea = new AutoGuardadoDaemon(accionGuardado, intervaloMs);
        Thread hilo = new Thread(tarea, "AutoGuardadoDaemon");
        // OBLIGATORIO: setDaemon(true) antes de start()
        hilo.setDaemon(true);
        hilo.start();
        return hilo;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
                accionGuardado.run();
            } catch (InterruptedException e) {
                // Si el daemon es interrumpido, restablecemos la bandera y terminamos
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

