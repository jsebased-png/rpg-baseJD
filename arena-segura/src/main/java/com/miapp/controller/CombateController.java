package com.miapp.controller;

import com.miapp.model.MonstruoDAO;
import com.miapp.model.VictoriaDAO;
import com.miapp.model.Usuario;
import com.miapp.model.Monstruo;
import com.miapp.model.MonstruoDAO;
import com.miapp.model.VictoriaDAO;
import com.miapp.model.rpg.core.Arena;
import com.miapp.model.rpg.core.RegistroCombate;
import com.miapp.model.rpg.entities.Enemigo;
import com.miapp.model.rpg.entities.Guerrero;
import com.miapp.model.rpg.entities.Mago;
import com.miapp.model.rpg.entities.Personaje;

import java.util.List;
import java.util.Random;
/**
 * Controlador de Combate (MVC - Controller).
 *
 * Usa el motor del proyecto base RPG (Arena, Guerrero, Mago, Enemigo): el monstruo se lee de la BD
 * (el Bestiario es la tabla `monstruos`) y se convierte en un Enemigo; el jugador elige Guerrero o Mago.
 *
 * REGLA DE NEGOCIO ESTRICTA (AUDIO DEL PROFESOR):
 * - Solo se guarda la victoria (jugador + monstruo + fecha/hora) si el JUGADOR GANA el combate.
 * - Si pierde (o hay empate por limite de rondas), NO se almacena nada en la base de datos.
 */
public class CombateController {

    public static final int CLASE_GUERRERO = 1;
    public static final int CLASE_MAGO = 2;

    /** La defensa del monstruo se deriva de su ataque (la tabla `monstruos` no la guarda). */
    private static final double FACTOR_DEFENSA = 1.0;

    private final MonstruoDAO monstruoDAO;
    private final VictoriaDAO victoriaDAO;
    private final UsuarioController usuarioController;
    private final Random azar;

    public CombateController(UsuarioController usuarioController) {
        this(new MonstruoDAO(), new VictoriaDAO(), usuarioController, new Random());
    }

    public CombateController(MonstruoDAO monstruoDAO, VictoriaDAO victoriaDAO, UsuarioController usuarioController) {
        this(monstruoDAO, victoriaDAO, usuarioController, new Random());
    }

    public CombateController(MonstruoDAO monstruoDAO, VictoriaDAO victoriaDAO,
                             UsuarioController usuarioController, Random azar) {
        this.monstruoDAO = monstruoDAO;
        this.victoriaDAO = victoriaDAO;
        this.usuarioController = usuarioController;
        this.azar = azar;
    }

    /** Combate con el Guerrero por defecto. */
    public ResultadoCombate simularCombate(int monstruoId) {
        return simularCombate(monstruoId, CLASE_GUERRERO);
    }

    /**
     * Ejecuta el duelo entre el heroe del jugador autenticado y el monstruo elegido.
     *
     * @param monstruoId  id del monstruo en la BD
     * @param claseHeroe  CLASE_GUERRERO o CLASE_MAGO
     */
    public ResultadoCombate simularCombate(int monstruoId, int claseHeroe) {
        Usuario jugador = usuarioController.getUsuarioAutenticado();
        if (jugador == null) {
            return new ResultadoCombate(false, List.of("Error: Debe iniciar sesion para combatir."), false);
        }
        Monstruo monstruo = monstruoDAO.obtenerPorId(monstruoId);
        if (monstruo == null) {
            return new ResultadoCombate(false, List.of("Error: Monstruo no encontrado."), false);
        }

        Personaje heroe = (claseHeroe == CLASE_MAGO) ? new Mago(jugador.getUsuario()) : new Guerrero(jugador.getUsuario());
        Enemigo rival = new Enemigo(monstruo.getNombre(), monstruo.getVida(), monstruo.getAtaque(),
                (int) Math.round(monstruo.getAtaque() * FACTOR_DEFENSA));

        RegistroCombate registro = new RegistroCombate(false);
        registro.agregar("=== INICIO DE COMBATE: %s (%s) vs %s ===", heroe.getNombre(), heroe.getTipo(), rival.getNombre());
        registro.agregar("Monstruo Rival: HP=%d | ATK=%d", monstruo.getVida(), monstruo.getAtaque());

        new Arena(registro, azar).duelo(heroe, rival);
        heroe.detenerEfectos();
        rival.detenerEfectos();

        boolean jugadorGano = heroe.estaVivo() && !rival.estaVivo();
        boolean victoriaPersistida = false;

        if (jugadorGano) {
            registro.agregar("%n¡VICTORIA TRIUNFAL! %s ha derrotado a %s.", jugador.getUsuario(), monstruo.getNombre());
            // REGLA CRITICA: solo se persiste si el jugador gano
            victoriaPersistida = victoriaDAO.registrar(jugador.getId(), monstruo.getId());
            registro.agregar(victoriaPersistida
                    ? "[PERSISTENCIA BD] Victoria registrada exitosamente en la base de datos."
                    : "[PERSISTENCIA BD] Error al registrar la victoria en la base de datos.");
        } else {
            registro.agregar("%nDERROTA: %s no logro vencer a %s.", jugador.getUsuario(), monstruo.getNombre());
            // REGLA CRITICA: si el jugador pierde, no se almacena nada
            registro.agregar("[PERSISTENCIA BD] El jugador no resulto victorioso; NO se almacena ningun registro en la BD.");
        }

        List<String> lineas = registro.getTexto().lines().toList();
        return new ResultadoCombate(jugadorGano, lineas, victoriaPersistida);
    }

    /** DTO para retornar el desenlace del combate a la capa View. */
    public record ResultadoCombate(boolean victoria, List<String> registro, boolean guardadaEnBD) {}
}
