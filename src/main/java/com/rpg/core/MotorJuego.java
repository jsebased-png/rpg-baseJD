package com.rpg.core;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.GestorTurno;
import com.rpg.entities.Enemigo;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.EstadoPartida;
import com.rpg.persistencia.GestorPersistencia;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/** Coordina el flujo del juego: crear heroe, combatir, torneo, bestiario, concurrencia y persistencia. */
public class MotorJuego {

    private static final String NOMBRE_GREMIO = "Guardianes del Alba";
    private static final int CURACION_POCION_VIDA = 30;
    private static final int TIEMPO_LIMITE_TURNO_SEGUNDOS = 10;
    private static final int INTERVALO_AUTOGUARDADO_MS = 15000;

    private final Consola consola;
    private final RegistroCombate registro = new RegistroCombate();
    private final Arena arena = new Arena(registro, new Random());
    private final Bestiario bestiario = new Bestiario();
    private PerfilJugador perfil = new PerfilJugador();
    private Gremio gremio = new Gremio(NOMBRE_GREMIO);
    private final TorneoManager torneo = new TorneoManager(arena, registro);
    private String ultimoGuardado = "";

    public MotorJuego(Consola consola) {
        this.consola = consola;
        // TALLER CONCURRENCIA - Seccion 4: Iniciar hilo demonio de autoguardado en segundo plano
        AutoGuardadoDaemon.iniciar(this::guardarProgresoAutomatico, INTERVALO_AUTOGUARDADO_MS);
    }

    // ---- opciones del menu principal

    public void combateRapido() {
        registro.limpiar();
        Personaje jugador = crearJugador();
        Enemigo enemigo = elegirEnemigo();
        registro.agregar("%n=== %s vs %s ===", jugador.getNombre(), enemigo.getNombre());

        boolean huyo = combatir(jugador, enemigo);

        cerrarCombate(jugador, enemigo, huyo);
        guardarProgreso();
    }

    public void jugarTorneo() {
        registro.limpiar();
        Personaje campeon = torneo.jugarTorneo();
        registro.agregar("%nCAMPEON DEL TORNEO: %s", campeon.getNombre());
        perfil.desbloquearLogro("Campeon del Torneo");
        guardarProgreso();
    }

    public void mostrarBestiario() {
        System.out.println("\n=== BESTIARIO ===");
        System.out.print(bestiario.describir());
        System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
    }

    // ---- opciones de persistencia (TALLER ARCHIVOS)

    public synchronized boolean cargarPartida() {
        File archivoDat = GestorPersistencia.obtenerArchivoRelativo(GestorPersistencia.ARCHIVO_DAT_RELATIVO);
        EstadoPartida estado = GestorPersistencia.cargarPartida(archivoDat);
        if (estado != null) {
            this.perfil = estado.getPerfil();
            this.bestiario.setDerrotados(estado.getDerrotasBestiario());
            this.ultimoGuardado = estado.getUltimoGuardadoTexto();
            if (estado.getGremio() != null) {
                this.gremio = estado.getGremio();
            }
            System.out.println("Partida cargada exitosamente.");
            System.out.printf("Victorias: %d | Derrotas: %d | Logros: %s%n",
                    perfil.getVictorias(), perfil.getDerrotas(), perfil.getLogros());
            return true;
        }
        return false;
    }

    public void mostrarHistorialCombates() {
        System.out.println("\n=== HISTORIAL DE COMBATES GUARDADOS (LOG .TXT) ===");
        File archivoLog = GestorPersistencia.obtenerArchivoRelativo(GestorPersistencia.ARCHIVO_LOG_RELATIVO);
        String historial = GestorPersistencia.leerLogTexto(archivoLog);
        if (historial.isBlank()) {
            System.out.println("El archivo de registro esta vacio o no existe aun.");
        } else {
            System.out.print(historial);
        }
    }

    public void mostrarInfoArchivos() {
        System.out.println("\n=== INFORMACION DE ARCHIVOS (File) ===");
        File archivoRel = GestorPersistencia.obtenerArchivoRelativo(GestorPersistencia.ARCHIVO_LOG_RELATIVO);
        File archivoAbs = GestorPersistencia.obtenerArchivoAbsoluto(GestorPersistencia.ARCHIVO_DAT_RELATIVO);

        System.out.println("Archivo Registro (ruta relativa):");
        System.out.println("  " + GestorPersistencia.describirArchivo(archivoRel));
        System.out.println("Archivo Guardado (.dat, ruta absoluta):");
        System.out.println("  " + GestorPersistencia.describirArchivo(archivoAbs));
    }

    public void borrarPartida() {
        System.out.println("\n=== BORRAR PARTIDA GUARDADA ===");
        int confirmacion = consola.leerEntero("Confirmas borrar la partida guardada? (1. Si, 2. No): ", 1, 2);
        if (confirmacion == 1) {
            File archivoDat = GestorPersistencia.obtenerArchivoRelativo(GestorPersistencia.ARCHIVO_DAT_RELATIVO);
            GestorPersistencia.borrarConFile(archivoDat);

            File archivoLog = GestorPersistencia.obtenerArchivoRelativo(GestorPersistencia.ARCHIVO_LOG_RELATIVO);
            GestorPersistencia.borrarConNIO(archivoLog.toPath());
            System.out.println("Partida y registros borrados correctamente.");
        } else {
            System.out.println("Borrado cancelado.");
        }
    }

    // ---- creacion de personajes

    private Personaje crearJugador() {
        int clase = consola.leerEntero("Clase (1. Guerrero, 2. Mago): ", 1, 2);
        String nombre = consola.leerNombre("Nombre del heroe: ");
        Personaje jugador = switch (clase) {
            case 1 -> new Guerrero(nombre);
            default -> new Mago(nombre);
        };
        entregarKitInicial(jugador);
        jugador.unirseAGremio(gremio);
        System.out.printf("%s se une al gremio %s (%d miembros).%n",
                jugador.getNombre(), gremio.getNombre(), gremio.getMiembros().size());
        return jugador;
    }

    private void entregarKitInicial(Personaje jugador) {
        List<Pocion> kit = List.of(
                new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN),
                new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                        new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false),
                new Pocion("Antidoto", 0, RaridadItem.COMUN, null, true));
        try {
            for (Pocion pocion : kit) {
                jugador.getInventario().agregar(pocion);
            }
        } catch (CapacidadExcedidaException e) {
            System.out.println("No se pudo entregar todo el kit: " + e.getMessage());
        }
    }

    private Enemigo elegirEnemigo() {
        List<String> nombres = bestiario.nombres();
        System.out.println("Rivales disponibles:");
        for (int i = 0; i < nombres.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, nombres.get(i));
        }
        while (true) {
            String texto = consola.leerLinea("Rival (numero o nombre): ");
            Enemigo enemigo = resolverRival(texto, nombres);
            if (enemigo != null) {
                return enemigo;
            }
            System.out.println("No existe ese rival.");
        }
    }

    private Enemigo resolverRival(String texto, List<String> nombres) {
        try {
            int numero = Integer.parseInt(texto);
            boolean valido = numero >= 1 && numero <= nombres.size();
            return valido ? bestiario.crearEnemigo(nombres.get(numero - 1)) : null;
        } catch (NumberFormatException e) {
            return bestiario.crearEnemigo(texto);
        }
    }

    // ---- bucle de combate

    /** @return true si el jugador huyo */
    private boolean combatir(Personaje jugador, Enemigo enemigo) {
        int ronda = 1;
        while (jugador.estaVivo() && enemigo.estaVivo()) {
            registro.agregar("%n-- Ronda %d --", ronda);
            if (arena.iniciarTurno(jugador) && turnoJugador(jugador, enemigo)) {
                return true;
            }
            arena.turnoAutomatico(enemigo, jugador);
            ronda++;
        }
        return false;
    }

    /**
     * Turno del jugador. Las acciones que no gastan turno (ver estado, cancelar) vuelven a pedir.
     * @return true si el jugador decidio huir
     */
    private boolean turnoJugador(Personaje jugador, Enemigo enemigo) {
        boolean turnoGastado;
        do {
            registro.agregar(FormatoCombate.resumen(jugador));
            registro.agregar(FormatoCombate.resumen(enemigo));
            Accion accion = pedirAccion(jugador);
            if (accion == null) {
                registro.agregar("%s pierde el turno por tiempo agotado.", jugador.getNombre());
                return false;
            }
            if (accion == Accion.HUIR) {
                return true;
            }
            turnoGastado = switch (accion) {
                case ATACAR -> {
                    arena.atacar(jugador, enemigo);
                    yield true;
                }
                case ESPECIAL -> arena.intentarEspecial(jugador, enemigo);
                case POCION -> usarPocion(jugador);
                default -> {
                    mostrarEstado(jugador);
                    yield false;
                }
            };
        } while (!turnoGastado && enemigo.estaVivo());
        return false;
    }

    /**
     * TALLER CONCURRENCIA - Seccion 1:
     * Pide la accion al jugador controlada por un temporizador concurrente en segundo plano.
     * Si el jugador no ingresa una opcion antes de 10 segundos, se invoca onTiempoAgotado() y el turno se pierde.
     * Si responde a tiempo, el temporizador se cancela limpiamente con interrupt().
     */
    private Accion pedirAccion(Personaje jugador) {
        System.out.println("1. Atacar  2. Habilidad especial  3. Usar pocion  4. Ver estado  0. Huir");

        final boolean[] tiempoAgotado = {false};
        TurnoCallback callback = () -> {
            tiempoAgotado[0] = true;
            System.out.println("\n[Tiempo agotado! Pierdes el turno.]");
        };

        GestorTurno gestor = new GestorTurno(TIEMPO_LIMITE_TURNO_SEGUNDOS, callback);
        gestor.iniciar();

        int opcion = consola.leerEntero("Accion: ", 0, 4);
        gestor.cancelar();

        if (tiempoAgotado[0]) {
            return null;
        }

        return switch (opcion) {
            case 1 -> Accion.ATACAR;
            case 2 -> Accion.ESPECIAL;
            case 3 -> Accion.POCION;
            case 4 -> Accion.ESTADO;
            default -> Accion.HUIR;
        };
    }

    private boolean usarPocion(Personaje jugador) {
        Inventario<Pocion> inventario = jugador.getInventario();
        if (inventario.estaVacio()) {
            System.out.println("No tienes pociones.");
            return false;
        }
        int numero = 1;
        for (Pocion pocion : inventario) {
            System.out.printf("  %d. %s%n", numero++, pocion.getNombre());
        }
        int eleccion = consola.leerEntero("Pocion (0 = cancelar): ", 0, inventario.tamanio());
        if (eleccion == 0) {
            return false;
        }
        registro.agregar(jugador.usarPocion(inventario.quitar(eleccion - 1)));
        return true;
    }

    private void mostrarEstado(Personaje jugador) {
        System.out.printf("Habilidades: %s | Arma: %s | Defensa: %d | Pociones: %d%n",
                jugador.getHabilidades(),
                jugador.getArma() != null ? jugador.getArma().getNombre() : "ninguna",
                jugador.getDefensa(), jugador.getInventario().tamanio());
        for (EstadoEfecto efecto : jugador.getEfectos()) {
            System.out.printf("Efecto activo: %s (%d turnos)%n", efecto.getNombre(), efecto.getTurnosRestantes());
        }
    }

    // ---- resultado del combate

    private void cerrarCombate(Personaje jugador, Enemigo enemigo, boolean huyo) {
        // TALLER CONCURRENCIA - Seccion 2: Detener los hilos de efectos continuos activos
        jugador.detenerEfectos();
        enemigo.detenerEfectos();

        if (huyo) {
            registro.agregar("%s huye del combate.", jugador.getNombre());
        } else if (jugador.estaVivo()) {
            registro.agregar("%nVictoria de %s!", jugador.getNombre());
            perfil.registrarVictoria();
            bestiario.registrarEnemigoDerrotado(enemigo.getNombre());
            desbloquearLogros(jugador, enemigo);
            repartirBotin(jugador);
        } else {
            registro.agregar("%n%s ha caido ante %s.", jugador.getNombre(), enemigo.getNombre());
            perfil.registrarDerrota();
        }
    }

    private void desbloquearLogros(Personaje jugador, Enemigo enemigo) {
        boolean intacto = jugador.getVida() == jugador.getVidaMaxima();
        List<String> candidatos = List.of("Primera Victoria", "Cazador de " + enemigo.getNombre(),
                intacto ? "Intocable" : "");
        for (String logro : candidatos) {
            if (!logro.isEmpty() && perfil.desbloquearLogro(logro)) {
                registro.agregar("Logro desbloqueado: %s", logro);
            }
        }
    }

    private void repartirBotin(Personaje jugador) {
        try {
            jugador.getInventario().agregar(new Pocion("Pocion de Vida", CURACION_POCION_VIDA, RaridadItem.COMUN));
            registro.agregar("Botin: Pocion de Vida.");
        } catch (CapacidadExcedidaException e) {
            registro.agregar("Botin perdido: %s", e.getMessage());
        }
    }

    // ---- guardado

    /**
     * TALLER ARCHIVOS & CONCURRENCIA:
     * Guarda el progreso en memoria, vuelca el log a .txt con append, y serializa el estado en .dat.
     */
    public synchronized void guardarProgreso() {
        StringBuilder contenido = new StringBuilder();
        contenido.append("=== PARTIDA RPG ===").append(System.lineSeparator());
        contenido.append("Victorias: ").append(perfil.getVictorias()).append(System.lineSeparator());
        contenido.append("Derrotas: ").append(perfil.getDerrotas()).append(System.lineSeparator());
        contenido.append("Logros: ").append(String.join(", ", perfil.getLogros())).append(System.lineSeparator());
        contenido.append("Gremio ").append(gremio.getNombre()).append(": ")
                .append(gremio.getMiembros().size()).append(" miembros").append(System.lineSeparator());
        contenido.append("Enemigos derrotados: ").append(bestiario.resumenDerrotados()).append(System.lineSeparator());
        contenido.append(System.lineSeparator()).append("--- Ultimo combate ---").append(System.lineSeparator());
        contenido.append(registro.getTexto());
        ultimoGuardado = contenido.toString();

        // 1. Volcar log a .txt con BufferedWriter
        File archivoLog = GestorPersistencia.obtenerArchivoRelativo(GestorPersistencia.ARCHIVO_LOG_RELATIVO);
        try {
            registro.volcarAArchivo(archivoLog);
        } catch (IOException e) {
            System.err.println("No se pudo escribir el log de combates: " + e.getMessage());
        }

        // 2. Serializar estado de la partida a archivo binario .dat
        File archivoDat = GestorPersistencia.obtenerArchivoRelativo(GestorPersistencia.ARCHIVO_DAT_RELATIVO);
        EstadoPartida estado = new EstadoPartida(perfil, gremio, bestiario.getDerrotados(), ultimoGuardado);
        GestorPersistencia.guardarPartida(estado, archivoDat);

        System.out.println("[Progreso guardado en memoria y en disco (partidas/)]");
    }

    /**
     * Guardado automatico en segundo plano ejecutado periodicamente por AutoGuardadoDaemon.
     */
    public synchronized void guardarProgresoAutomatico() {
        File archivoDat = GestorPersistencia.obtenerArchivoRelativo(GestorPersistencia.ARCHIVO_DAT_RELATIVO);
        EstadoPartida estado = new EstadoPartida(perfil, gremio, bestiario.getDerrotados(), ultimoGuardado);
        GestorPersistencia.guardarPartida(estado, archivoDat);
    }

    public String getUltimoGuardado() {
        return ultimoGuardado;
    }
}
