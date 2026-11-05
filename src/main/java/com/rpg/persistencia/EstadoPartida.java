package com.rpg.persistencia;

import com.rpg.entities.Gremio;
import com.rpg.entities.PerfilJugador;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsula el estado completo de una partida para su serializacion a disco (.dat).
 * Implementa Serializable con serialVersionUID.
 */
public class EstadoPartida implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PerfilJugador perfil;
    private final Gremio gremio;
    private final Map<String, Integer> derrotasBestiario;
    private final String ultimoGuardadoTexto;

    public EstadoPartida(PerfilJugador perfil, Gremio gremio, Map<String, Integer> derrotasBestiario, String ultimoGuardadoTexto) {
        this.perfil = perfil;
        this.gremio = gremio;
        this.derrotasBestiario = derrotasBestiario != null ? new LinkedHashMap<>(derrotasBestiario) : new LinkedHashMap<>();
        this.ultimoGuardadoTexto = ultimoGuardadoTexto != null ? ultimoGuardadoTexto : "";
    }

    public PerfilJugador getPerfil() {
        return perfil;
    }

    public Gremio getGremio() {
        return gremio;
    }

    public Map<String, Integer> getDerrotasBestiario() {
        return new LinkedHashMap<>(derrotasBestiario);
    }

    public String getUltimoGuardadoTexto() {
        return ultimoGuardadoTexto;
    }
}

