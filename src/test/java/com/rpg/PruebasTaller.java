package com.rpg;

import com.rpg.concurrency.AutoGuardadoDaemon;
import com.rpg.concurrency.EfectoContinuoTask;
import com.rpg.concurrency.GestorTurno;
import com.rpg.concurrency.TemporizadorTurnoTask;
import com.rpg.core.Arena;
import com.rpg.core.RegistroCombate;
import com.rpg.core.TorneoManager;
import com.rpg.core.TurnoCallback;
import com.rpg.entities.Arma;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.items.Inventario;
import com.rpg.items.Pocion;
import com.rpg.persistencia.EstadoPartida;
import com.rpg.persistencia.GestorPersistencia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Suite completa de pruebas automatizadas que valida cada punto estipulado
 * en el plan del README.md para los talleres de Concurrencia y Persistencia de Archivos.
 */
public class PruebasTaller {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" INICIANDO VALIDACION DEL PLAN (CONCURRENCIA + ARCHIVOS)");
        System.out.println("==================================================");

        int pasadas = 0;
        int fallidas = 0;

        List<RunnableTest> pruebas = List.of(
                new RunnableTest("Seccion 1 - Temporizador: cuenta regresiva y expiracion", PruebasTaller::testTemporizadorExpiracion),
                new RunnableTest("Seccion 1 - Temporizador: cancelacion con interrupt()", PruebasTaller::testTemporizadorCancelacion),
                new RunnableTest("Seccion 2 - EfectoContinuoTask: ejecucion y detencion limpia con interrupt()", PruebasTaller::testEfectoContinuoInterrupt),
                new RunnableTest("Seccion 3 - TorneoManager: semifinales paralelas, ciclo de vida de hilos y join()", PruebasTaller::testTorneoParaleloYEstados),
                new RunnableTest("Seccion 4 & Inv. 1 - AutoGuardadoDaemon: hilo daemon y IllegalThreadStateException", PruebasTaller::testAutoGuardadoDaemonYExcepcion),
                new RunnableTest("Seccion 6 - Condicion de carrera protegida con synchronized", PruebasTaller::testCondicionDeCarreraSincronizada),
                new RunnableTest("Investigacion 2 - ReentrantLock: recibirDanioConLock() y try-finally", PruebasTaller::testReentrantLock),
                new RunnableTest("Archivos Seccion 1 - Clase File: rutas relativas/absolutas, exists, length, mkdirs", PruebasTaller::testClaseFileYDirectorios),
                new RunnableTest("Archivos Seccion 2 & 3 - BufferedWriter (append) y BufferedReader (linea por linea)", PruebasTaller::testEscrituraYLecturaLog),
                new RunnableTest("Archivos Seccion 4 - Borrado con File.delete() y Files.deleteIfExists(Path)", PruebasTaller::testBorradoArchivos),
                new RunnableTest("Archivos Seccion 5 - Serializacion, serialVersionUID y campo transient", PruebasTaller::testSerializacionYTransient),
                new RunnableTest("Archivos Seccion 5 - Serializacion de ciclo bidireccional Personaje <-> Gremio", PruebasTaller::testSerializacionCicloGremio),
                new RunnableTest("Archivos Seccion 6 - Captura de NotSerializableException con try-with-resources", PruebasTaller::testNotSerializableException)
        );

        for (RunnableTest prueba : pruebas) {
            try {
                prueba.run();
                System.out.println("[OK] PASS: " + prueba.nombre);
                pasadas++;
            } catch (Throwable t) {
                System.err.println("[FALLO] FAIL: " + prueba.nombre + " -> " + t.getMessage());
                t.printStackTrace();
                fallidas++;
            }
        }

        System.out.println("==================================================");
        System.out.printf(" RESULTADO: %d pasadas, %d fallidas.%n", pasadas, fallidas);
        System.out.println("==================================================");

        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static record RunnableTest(String nombre, Runnable runnable) implements Runnable {
        @Override
        public void run() {
            runnable.run();
        }
    }

    // --- CONCURRENCIA: SECCION 1 ---

    public static void testTemporizadorExpiracion() {
        AtomicBoolean tiempoAgotado = new AtomicBoolean(false);
        TurnoCallback callback = () -> tiempoAgotado.set(true);

        // Duracion corta de 1 segundo para la prueba
        TemporizadorTurnoTask task = new TemporizadorTurnoTask(1, callback);
        Thread hilo = new Thread(task);
        hilo.start();

        try {
            hilo.join(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!tiempoAgotado.get()) {
            throw new AssertionError("El callback onTiempoAgotado debio ser invocado al finalizar el tiempo.");
        }
    }

    public static void testTemporizadorCancelacion() {
        AtomicBoolean tiempoAgotado = new AtomicBoolean(false);
        TurnoCallback callback = () -> tiempoAgotado.set(true);

        GestorTurno gestor = new GestorTurno(3, callback);
        gestor.iniciar();

        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {}

        // Se responde a tiempo, se cancela con interrupt()
        gestor.cancelar();

        try {
            Thread.sleep(1200);
        } catch (InterruptedException ignored) {}

        if (tiempoAgotado.get()) {
            throw new AssertionError("El temporizador no debio llamar al callback porque fue cancelado con interrupt().");
        }
    }

    // --- CONCURRENCIA: SECCION 2 ---

    public static void testEfectoContinuoInterrupt() {
        Guerrero g = new Guerrero("ThorTest");
        EstadoEfecto veneno = new EstadoEfecto("VenenoTest", TipoEfecto.DANIO_CONTINUO, 5, 5);

        EfectoContinuoTask task = new EfectoContinuoTask(g, veneno, 200);
        Thread hilo = new Thread(task, "Test-Veneno");
        hilo.start();

        try {
            Thread.sleep(450); // Deja pasar un par de ticks
        } catch (InterruptedException ignored) {}

        if (g.getVida() >= 120) {
            throw new AssertionError("El personaje debio haber recibido al menos un tick de dano del efecto continuo.");
        }

        // Interrumpir el efecto limpiamente
        hilo.interrupt();

        try {
            hilo.join(1000);
        } catch (InterruptedException ignored) {}

        if (hilo.isAlive()) {
            throw new AssertionError("El hilo de EfectoContinuoTask debio terminar limpiamente tras ser interrumpido.");
        }
    }

    // --- CONCURRENCIA: SECCION 3 ---

    public static void testTorneoParaleloYEstados() {
        RegistroCombate reg = new RegistroCombate();
        Arena arena = new Arena(reg, new Random(42));
        TorneoManager torneo = new TorneoManager(arena, reg);

        Personaje campeon = torneo.jugarTorneo();
        if (campeon == null || !campeon.estaVivo()) {
            throw new AssertionError("El torneo en paralelo con join() debe retornar un campeon vivo.");
        }
    }

    // --- CONCURRENCIA: SECCION 4 & INVESTIGACION 1 ---

    public static void testAutoGuardadoDaemonYExcepcion() {
        AtomicBoolean guardadoEjecutado = new AtomicBoolean(false);
        Thread hiloDaemon = AutoGuardadoDaemon.iniciar(() -> guardadoEjecutado.set(true), 200);

        if (!hiloDaemon.isDaemon()) {
            throw new AssertionError("El hilo de AutoGuardadoDaemon debe ser configurado con setDaemon(true).");
        }

        try {
            Thread.sleep(350);
        } catch (InterruptedException ignored) {}

        if (!guardadoEjecutado.get()) {
            throw new AssertionError("El daemon de autoguardado debio haber ejecutado la accion periodicamente.");
        }

        hiloDaemon.interrupt();

        // INVESTIGACION OBLIGATORIA 1: Demostrar que setDaemon(true) despues de start() lanza IllegalThreadStateException
        Thread hiloPrueba = new Thread(() -> {});
        hiloPrueba.start();
        boolean excepcionLanzada = false;
        try {
            hiloPrueba.setDaemon(true);
        } catch (IllegalThreadStateException e) {
            excepcionLanzada = true;
        }

        if (!excepcionLanzada) {
            throw new AssertionError("Llamar a setDaemon(true) tras start() debe lanzar IllegalThreadStateException.");
        }
    }

    // --- CONCURRENCIA: SECCION 6 ---

    public static void testCondicionDeCarreraSincronizada() {
        Guerrero g = new Guerrero("Hercules"); // 120 de vida maxima
        g.recibirDanio(40); // Establece vida en 80 para permitir tanto curaciones como danos sin topar el maximo (120)
        int vidaInicial = g.getVida(); // 80

        int numHilos = 40;
        List<Thread> hilos = new ArrayList<>();

        // 20 hilos restan 1 de vida, 20 hilos curan 1 de vida
        for (int i = 0; i < numHilos / 2; i++) {
            hilos.add(new Thread(() -> g.recibirDanio(1)));
            hilos.add(new Thread(() -> g.curar(1)));
        }

        for (Thread t : hilos) {
            t.start();
        }

        for (Thread t : hilos) {
            try {
                t.join();
            } catch (InterruptedException ignored) {}
        }

        // Si esta protegido con synchronized, 20 danos de 1 y 20 curas de 1 dejan la vida exactamente en vidaInicial (80)
        if (g.getVida() != vidaInicial) {
            throw new AssertionError("Condicion de carrera detectada: la vida resultante fue " + g.getVida() + " en vez de " + vidaInicial + ".");
        }
    }

    // --- CONCURRENCIA: INVESTIGACION 2 (ReentrantLock) ---

    public static void testReentrantLock() {
        Guerrero g = new Guerrero("AquilesLock"); // 120 vida
        g.recibirDanioConLock(30);
        if (g.getVida() != 90) {
            throw new AssertionError("recibirDanioConLock debio descontar el danio protegiendo con ReentrantLock.");
        }
    }

    // --- PERSISTENCIA: SECCION 1 ---

    public static void testClaseFileYDirectorios() {
        File dir = new File("partidas_test");
        GestorPersistencia.asegurarDirectorio(dir);

        File rel = GestorPersistencia.obtenerArchivoRelativo("partidas_test" + File.separator + "test_file.txt");
        File abs = GestorPersistencia.obtenerArchivoAbsoluto("partidas_test" + File.separator + "test_file.txt");

        GestorPersistencia.crearArchivoSiNoExiste(rel);

        if (!rel.exists() || !abs.exists()) {
            throw new AssertionError("El archivo creado con File debe existir tanto en ruta relativa como absoluta.");
        }

        String descripcion = GestorPersistencia.describirArchivo(rel);
        if (!descripcion.contains("Nombre: test_file.txt") || !descripcion.contains("Existe: true")) {
            throw new AssertionError("describirArchivo debe informar nombre y existencia con File.");
        }

        rel.delete();
        dir.delete();
    }

    // --- PERSISTENCIA: SECCION 2 & 3 ---

    public static void testEscrituraYLecturaLog() {
        File archivo = new File("partidas" + File.separator + "test_log.txt");
        archivo.delete();

        // Escritura en modo append con BufferedWriter
        GestorPersistencia.agregarLogTexto(archivo, "Linea 1: Combate iniciado");
        GestorPersistencia.agregarLogTexto(archivo, "Linea 2: Golpe critico");

        // Lectura con BufferedReader
        String contenido = GestorPersistencia.leerLogTexto(archivo);
        if (!contenido.contains("Linea 1: Combate iniciado") || !contenido.contains("Linea 2: Golpe critico")) {
            throw new AssertionError("El contenido leido con BufferedReader no coincide con el volcado con BufferedWriter.");
        }

        archivo.delete();
    }

    // --- PERSISTENCIA: SECCION 4 ---

    public static void testBorradoArchivos() {
        File archivoFile = new File("partidas" + File.separator + "borrar_file.txt");
        File archivoNio = new File("partidas" + File.separator + "borrar_nio.txt");

        GestorPersistencia.crearArchivoSiNoExiste(archivoFile);
        GestorPersistencia.crearArchivoSiNoExiste(archivoNio);

        boolean borradoFile = GestorPersistencia.borrarConFile(archivoFile);
        boolean borradoNio = GestorPersistencia.borrarConNIO(archivoNio.toPath());

        if (!borradoFile || !borradoNio) {
            throw new AssertionError("Tanto File.delete() como Files.deleteIfExists() deben retornar true al borrar.");
        }

        if (archivoFile.exists() || archivoNio.exists()) {
            throw new AssertionError("Los archivos borrados no deben existir en el sistema de archivos.");
        }
    }

    // --- PERSISTENCIA: SECCION 5 ---

    public static void testSerializacionYTransient() {
        File destino = new File("partidas" + File.separator + "test_partida.dat");
        destino.delete();

        Guerrero g = new Guerrero("Leonidas");
        g.setEnCombate(true); // Campo transient

        PerfilJugador perfil = new PerfilJugador();
        perfil.registrarVictoria();
        perfil.desbloquearLogro("Invicto");

        EstadoPartida estadoOriginal = new EstadoPartida(perfil, null, null, "Resumen");
        GestorPersistencia.guardarPartida(estadoOriginal, destino);

        EstadoPartida estadoCargado = GestorPersistencia.cargarPartida(destino);
        if (estadoCargado == null) {
            throw new AssertionError("No se pudo deserializar el EstadoPartida con ObjectInputStream.");
        }

        if (estadoCargado.getPerfil().getVictorias() != 1 || !estadoCargado.getPerfil().getLogros().contains("Invicto")) {
            throw new AssertionError("Las estadisticas serializadas no coinciden con las cargadas.");
        }

        // Verificacion de campo transient en Personaje:
        File personajeDat = new File("partidas" + File.separator + "test_personaje.dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(personajeDat))) {
            oos.writeObject(g);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(personajeDat))) {
            Guerrero gCargado = (Guerrero) ois.readObject();
            if (gCargado.isEnCombate()) {
                throw new AssertionError("El campo transient enCombate debio restablecerse a false tras la deserializacion.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            destino.delete();
            personajeDat.delete();
        }
    }

    // --- PERSISTENCIA: SECCION 5 - CICLO BIDIRECCIONAL ---

    public static void testSerializacionCicloGremio() {
        File destino = new File("partidas" + File.separator + "test_ciclo.dat");
        destino.delete();

        Gremio gremio = new Gremio("Cruzados");
        Guerrero g = new Guerrero("Lancelot");
        g.unirseAGremio(gremio);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(destino))) {
            oos.writeObject(gremio);
        } catch (IOException e) {
            throw new RuntimeException("Fallo al serializar ciclo Personaje-Gremio: " + e.getMessage(), e);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(destino))) {
            Gremio gremioCargado = (Gremio) ois.readObject();
            if (gremioCargado.getMiembros().isEmpty()) {
                throw new AssertionError("El gremio cargado debe tener sus miembros restaurados.");
            }
            Personaje miembro = gremioCargado.getMiembros().get(0);
            if (miembro.getGremio() != gremioCargado) {
                throw new AssertionError("La referencia bidireccional Personaje <-> Gremio debe restaurar la misma instancia.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Fallo al deserializar ciclo Personaje-Gremio: " + e.getMessage(), e);
        } finally {
            destino.delete();
        }
    }

    // --- PERSISTENCIA: SECCION 6 - MANEJO DE EXCEPCIONES ---

    public static void testNotSerializableException() {
        class ObjetoNoSerializable {
            @SuppressWarnings("unused")
            int dato = 42;
        }

        boolean capturada = false;
        File f = new File("partidas" + File.separator + "test_not_serializable.dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(new ObjetoNoSerializable());
        } catch (NotSerializableException e) {
            capturada = true;
        } catch (IOException ignored) {}
        finally {
            f.delete();
        }

        if (!capturada) {
            throw new AssertionError("Se debio lanzar y capturar NotSerializableException al serializar un objeto sin Serializable.");
        }
    }
}
