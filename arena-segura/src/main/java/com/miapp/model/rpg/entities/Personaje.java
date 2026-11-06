package com.miapp.model.rpg.entities;

import com.miapp.model.rpg.concurrency.EfectoContinuoTask;
import com.miapp.model.rpg.entities.enums.EstadoPersonaje;
import com.miapp.model.rpg.entities.enums.TipoPersonaje;
import com.miapp.model.rpg.exceptions.SinManaException;
import com.miapp.model.rpg.items.Inventario;
import com.miapp.model.rpg.items.Pocion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Representa una entidad combatiente en el RPG.
 * Implementa Serializable para persistencia con ObjectOutputStream.
 * Protegida con synchronized para concurrencia segura ante multiples hilos (DoT/HoT, ataques, guardados).
 */
public abstract class Personaje implements ICombatiente, Serializable {

    private static final long serialVersionUID = 1L;

    private static final int VIDA_BASE = 100;
    private static final int ATAQUE_BASE = 10;
    private static final int DEFENSA_BASE = 5;
    private static final int CAPACIDAD_INVENTARIO = 3;
    private static final int DANIO_MINIMO = 1;
    private static final double PORCENTAJE_TOTAL = 100.0;

    private String nombre;
    private final TipoPersonaje tipo;
    private int vidaMaxima;
    private int vida;
    private final int ataqueBase;
    private final int defensaBase;
    private EstadoPersonaje estado = EstadoPersonaje.VIVO;

    private Arma arma;                  // asociacion 0..1
    private Gremio gremio;              // asociacion bidireccional 0..1
    private final Inventario<Pocion> inventario = new Inventario<>(CAPACIDAD_INVENTARIO); // composicion
    private final Set<String> habilidades = new HashSet<>();
    private final Map<String, EstadoEfecto> efectos = new LinkedHashMap<>();

    // Campo transient requerido para demostrar que no se persiste y recupera su valor por defecto
    private transient boolean enCombate;

    // Hilos de efectos continuos activos en segundo plano (no se serializan)
    private transient Map<String, Thread> hilosEfectos = new LinkedHashMap<>();

    // ReentrantLock para la investigacion obligatoria
    private final transient ReentrantLock lock = new ReentrantLock();

    protected Personaje(String nombre, TipoPersonaje tipo) {
        this(nombre, tipo, VIDA_BASE, ATAQUE_BASE, DEFENSA_BASE);
    }

    protected Personaje(String nombre, TipoPersonaje tipo, int vidaMaxima, int ataqueBase, int defensaBase) {
        validarNombre(nombre);
        if (vidaMaxima <= 0 || ataqueBase < 0 || defensaBase < 0) {
            throw new IllegalArgumentException("Estadisticas invalidas para " + nombre);
        }
        this.nombre = nombre;
        this.tipo = tipo;
        this.vidaMaxima = vidaMaxima;
        this.vida = vidaMaxima;
        this.ataqueBase = ataqueBase;
        this.defensaBase = defensaBase;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.hilosEfectos = new LinkedHashMap<>();
    }

    // ---- combate

    /**
     * Ataque especial propio de cada clase.
     * @return descripcion de lo ocurrido, para el registro de combate
     */
    public abstract String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException;

    /** Hook que se ejecuta al comenzar el turno (por ejemplo, el mago regenera mana). */
    public void alIniciarTurno() {
    }

    public int calcularDanio() {
        double bruto = ataqueBase + tipo.getBonificadorAtaque();
        if (arma != null) {
            bruto += arma.getDanioEfectivo();
        }
        return (int) Math.round(bruto);
    }

    @Override
    public void defender(int danoRecibido) {
        // Con enteros, 100 / 123 daria 0: por eso se divide con un double
        double factorMitigacion = PORCENTAJE_TOTAL / (PORCENTAJE_TOTAL + getDefensa());
        int danioFinal = (int) Math.round(danoRecibido * factorMitigacion);
        recibirDanio(Math.max(DANIO_MINIMO, danioFinal));
    }

    // ---- vida y concurrencia

    /*
     * JUSTIFICACION TECNICA: Por que AtomicInteger no basta aqui:
     * AtomicInteger.addAndGet(-danio) solo garantiza la atomicidad sobre un entero aislado.
     * En Personaje, modificar la vida involucra una invariante compuesta sobre multiples variables:
     * 1. Acotar el valor resultante en el rango [0, vidaMaxima].
     * 2. Si la vida llega a 0, actualizar atomicamente el 'estado' a EstadoPersonaje.MUERTO.
     * 3. La funcion 'estaVivo()' evalua 'estado'.
     * Por ello, synchronized es el mecanismo estandar y robusto recomendado por la Unidad 4.2.
     */

    public synchronized void recibirDanio(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        setVida(vida - cantidad);
    }

    /**
     * INVESTIGACION OBLIGATORIA: ReentrantLock
     *
     * Metodo alternativo para recibir danio usando ReentrantLock con try-finally.
     *
     * Razones para preferir ReentrantLock sobre synchronized:
     * 1. Temporizacion y no bloqueo: permite tryLock() con timeout para desistir si el recurso esta ocupado.
     * 2. Equidad (Fairness): puede garantizar orden FIFO en la adquisicion de locks.
     * 3. Flexibilidad estructural: permite abrir el lock en un metodo y cerrarlo en otro.
     * 4. Soporte de interrupcion: permite lockInterruptibly() para responder a senales de cancelacion.
     *
     * Si unlock() no estuviera dentro de un bloque finally, cualquier excepcion dejaria el lock
     * tomado permanentemente, provocando un bloqueo catastrofico (deadlock).
     */
    public void recibirDanioConLock(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("El danio no puede ser negativo");
        }
        lock.lock();
        try {
            setVida(vida - cantidad);
        } finally {
            lock.unlock(); // OBLIGATORIO en bloque finally
        }
    }

    /** @return vida realmente recuperada */
    public synchronized int curar(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La curacion no puede ser negativa");
        }
        if (!estaVivo()) {
            return 0;
        }
        int vidaAntes = vida;
        setVida(vida + cantidad);
        return vida - vidaAntes;
    }

    public synchronized void restaurarVida() {
        detenerEfectos();
        efectos.clear();
        estado = EstadoPersonaje.VIVO;
        vida = vidaMaxima;
    }

    @Override
    public synchronized boolean estaVivo() {
        return estado != EstadoPersonaje.MUERTO;
    }

    // ---- efectos DoT / HoT concurrentes

    public synchronized void aplicarEfecto(EstadoEfecto efecto) {
        EstadoEfecto copia = efecto.copia();
        efectos.put(copia.getNombre(), copia);
        if (copia.esDanio() && estado == EstadoPersonaje.VIVO) {
            estado = EstadoPersonaje.ENVENENADO;
        }
        iniciarHiloEfecto(copia);
    }

    private synchronized void iniciarHiloEfecto(EstadoEfecto efecto) {
        if (hilosEfectos == null) {
            hilosEfectos = new LinkedHashMap<>();
        }
        Thread anterior = hilosEfectos.remove(efecto.getNombre());
        if (anterior != null && anterior.isAlive()) {
            anterior.interrupt();
        }
        Thread hilo = new Thread(new EfectoContinuoTask(this, efecto, 1500),
                "Efecto-" + efecto.getNombre() + "-" + nombre);
        hilo.setDaemon(true);
        hilosEfectos.put(efecto.getNombre(), hilo);
        hilo.start();
    }

    public synchronized boolean quitarEfecto(String nombreEfecto) {
        if (hilosEfectos != null) {
            Thread hilo = hilosEfectos.remove(nombreEfecto);
            if (hilo != null && hilo.isAlive()) {
                hilo.interrupt();
            }
        }
        if (efectos.remove(nombreEfecto) == null) {
            return false;
        }
        normalizarEstado();
        return true;
    }

    public synchronized void quitarEfectosDanio() {
        for (String nombreEfecto : new ArrayList<>(efectos.keySet())) {
            EstadoEfecto eff = efectos.get(nombreEfecto);
            if (eff != null && eff.esDanio()) {
                quitarEfecto(nombreEfecto);
            }
        }
    }

    public synchronized void detenerEfectos() {
        if (hilosEfectos != null) {
            for (Thread hilo : new ArrayList<>(hilosEfectos.values())) {
                if (hilo != null && hilo.isAlive()) {
                    hilo.interrupt();
                }
            }
            hilosEfectos.clear();
        }
    }

    /**
     * En el modelo concurrente, los efectos corren en sus propios hilos (EfectoContinuoTask).
     * procesarEfectos() resume el estado actual de los efectos activos sin aplicar danio duplicado.
     * @return mensajes para el registro de combate
     */
    public synchronized List<String> procesarEfectos() {
        List<String> mensajes = new ArrayList<>();
        for (EstadoEfecto efecto : efectos.values()) {
            mensajes.add(String.format("%s tiene efecto activo: %s (%d ticks)",
                    nombre, efecto.getNombre(), efecto.getTurnosRestantes()));
        }
        normalizarEstado();
        return mensajes;
    }

    private synchronized boolean tieneEfectoDanio() {
        for (EstadoEfecto efecto : efectos.values()) {
            if (efecto.esDanio()) {
                return true;
            }
        }
        return false;
    }

    /** Si ya no queda ningun DoT, un personaje ENVENENADO vuelve a estar VIVO. */
    private synchronized void normalizarEstado() {
        if (estado == EstadoPersonaje.ENVENENADO && !tieneEfectoDanio()) {
            estado = EstadoPersonaje.VIVO;
        }
    }

    // ---- aturdimiento

    public synchronized void aturdir() {
        if (estaVivo()) {
            estado = EstadoPersonaje.ATURDIDO;
        }
    }

    /** @return true si estaba aturdido (y por tanto pierde este turno) */
    public synchronized boolean consumirAturdimiento() {
        boolean estabaAturdido = estado == EstadoPersonaje.ATURDIDO;
        if (estabaAturdido) {
            estado = tieneEfectoDanio() ? EstadoPersonaje.ENVENENADO : EstadoPersonaje.VIVO;
        }
        return estabaAturdido;
    }

    // ---- inventario, arma, gremio, habilidades

    public synchronized String usarPocion(Pocion pocion) {
        int curado = curar(pocion.getCuracionReal());
        if (pocion.getEfecto() != null) {
            aplicarEfecto(pocion.getEfecto());
        }
        if (pocion.isPurifica()) {
            quitarEfectosDanio();
        }
        return String.format("%s usa %s (+%d de vida)", nombre, pocion.getNombre(), curado);
    }

    public synchronized void equiparArma(Arma nuevaArma) {
        this.arma = nuevaArma;
    }

    public synchronized void desequiparArma() {
        this.arma = null;
    }

    public synchronized void unirseAGremio(Gremio nuevoGremio) {
        if (nuevoGremio == null || nuevoGremio == gremio) {
            return;
        }
        if (gremio != null) {
            gremio.removerMiembro(this);
        }
        gremio = nuevoGremio;
        nuevoGremio.agregarMiembro(this);
    }

    public synchronized void abandonarGremio() {
        Gremio anterior = gremio;
        if (anterior == null) {
            return;
        }
        gremio = null;
        anterior.removerMiembro(this);
    }

    /** @return false si la habilidad ya estaba aprendida (el Set rechaza duplicados) */
    public synchronized boolean aprenderHabilidad(String habilidad) {
        return habilidades.add(habilidad);
    }

    // ---- getters y setters validados

    public synchronized void setVida(int nuevaVida) {
        vida = Math.max(0, Math.min(nuevaVida, vidaMaxima));
        if (vida == 0) {
            estado = EstadoPersonaje.MUERTO;
            detenerEfectos();
        }
    }

    public synchronized void setNombre(String nombre) {
        validarNombre(nombre);
        this.nombre = nombre;
    }

    public synchronized void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima <= 0) {
            throw new IllegalArgumentException("La vida maxima debe ser positiva");
        }
        this.vidaMaxima = vidaMaxima;
        this.vida = Math.min(vida, vidaMaxima);
    }

    public int getDefensa() {
        return defensaBase + tipo.getBonificadorDefensa();
    }

    public synchronized String getNombre() {
        return nombre;
    }

    public TipoPersonaje getTipo() {
        return tipo;
    }

    public synchronized int getVida() {
        return vida;
    }

    public synchronized int getVidaMaxima() {
        return vidaMaxima;
    }

    public synchronized EstadoPersonaje getEstado() {
        return estado;
    }

    public synchronized Arma getArma() {
        return arma;
    }

    public synchronized Gremio getGremio() {
        return gremio;
    }

    public Inventario<Pocion> getInventario() {
        return inventario;
    }

    public synchronized Set<String> getHabilidades() {
        return Collections.unmodifiableSet(new HashSet<>(habilidades));
    }

    public synchronized Collection<EstadoEfecto> getEfectos() {
        return Collections.unmodifiableCollection(new ArrayList<>(efectos.values()));
    }

    public boolean isEnCombate() {
        return enCombate;
    }

    public void setEnCombate(boolean enCombate) {
        this.enCombate = enCombate;
    }
}
