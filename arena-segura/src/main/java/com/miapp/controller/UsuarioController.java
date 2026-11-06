package com.miapp.controller;

import com.miapp.model.UsuarioDAO;
import com.miapp.model.VictoriaDAO;
import java.util.Collections;
import java.util.List;
import com.miapp.model.Usuario;

/**
 * Controlador de Usuarios (MVC - Controller): autenticacion, sesion y CRUD de usuarios.
 *
 * REGLAS DE NEGOCIO:
 * 1. Control de Intentos Fallidos: Limite maximo de 3 intentos fallidos consecutivos.
 *    Al tercer fallo consecutivo, la sesion queda bloqueada impidiendo nuevos accesos.
 * 2. Estado de Sesion: Mantiene la referencia al usuario actualmente autenticado.
 * 3. Solo el ADMIN autenticado puede listar, crear, cambiar rol/clave/nombre o eliminar usuarios.
 * 4. Un admin no puede eliminarse a si mismo ni degradar/eliminar al ultimo ADMIN.
 * 5. Borrado seguro: no se elimina un usuario con victorias asociadas (evita error de FOREIGN KEY).
 * 6. El auto-registro publico SIEMPRE crea rol JUGADOR.
 */
public class UsuarioController {

    private static final int MAX_INTENTOS_FALLIDOS = 3;

    private final UsuarioDAO usuarioDAO;
    private final VictoriaDAO victoriaDAO;
    private Usuario usuarioAutenticado;
    private int intentosFallidos;
    private boolean bloqueado;

    public UsuarioController() {
        this(new UsuarioDAO(), new VictoriaDAO());
    }

    public UsuarioController(UsuarioDAO usuarioDAO, VictoriaDAO victoriaDAO) {
        this.usuarioDAO = usuarioDAO;
        this.victoriaDAO = victoriaDAO;
        this.intentosFallidos = 0;
        this.bloqueado = false;
    }

    /**
     * Procesa el intento de inicio de sesion aplicando las reglas de seguridad.
     *
     * @param usuario Nombre de usuario
     * @param clave   Contrasena ingresada
     * @return true si la autenticacion fue exitosa, false si fallo o esta bloqueado
     */
    public boolean login(String usuario, String clave) {
        if (bloqueado) {
            return false;
        }

        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            registrarFallo();
            return false;
        }

        Usuario jugador = usuarioDAO.autenticar(usuario.trim(), clave);
        if (jugador != null) {
            this.usuarioAutenticado = jugador;
            this.intentosFallidos = 0;
            return true;
        } else {
            registrarFallo();
            return false;
        }
    }

    private void registrarFallo() {
        this.intentosFallidos++;
        if (this.intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
            this.bloqueado = true;
        }
    }

    public void logout() {
        this.usuarioAutenticado = null;
    }

    /**
     * Auto-registro publico: SIEMPRE crea un usuario con rol JUGADOR.
     * Crear administradores solo es posible desde UsuarioController (requiere sesion ADMIN).
     */
    public ResultadoOperacion registrarNuevoUsuario(String usuario, String clave) {
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            return new ResultadoOperacion(false, "Usuario y contrasena son obligatorios.");
        }
        if (clave.length() < 6) {
            return new ResultadoOperacion(false, "La contrasena debe tener al menos 6 caracteres.");
        }
        if (usuarioDAO.obtenerPorUsuario(usuario.trim()) != null) {
            return new ResultadoOperacion(false, "El usuario '" + usuario.trim() + "' ya existe.");
        }
        boolean ok = usuarioDAO.registrar(new Usuario(usuario.trim(), clave, Usuario.ROL_JUGADOR));
        return ok ? new ResultadoOperacion(true, "Usuario registrado exitosamente.")
                  : new ResultadoOperacion(false, "No se pudo registrar el jugador (error de base de datos).");
    }


    public boolean estaAutenticado() {
        return usuarioAutenticado != null;
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public int getIntentosRestantes() {
        return Math.max(0, MAX_INTENTOS_FALLIDOS - intentosFallidos);
    }

    public void resetBloqueo() {
        this.bloqueado = false;
        this.intentosFallidos = 0;
    }

    // ===================== CRUD DE USUARIOS (solo ADMIN) =====================

    private boolean esAdmin() {
        return usuarioAutenticado != null && usuarioAutenticado.esAdmin();
    }

    private static final String DENEGADO = "ACCESO DENEGADO: Solo los administradores gestionan jugadores.";

    public List<Usuario> listarTodos() {
        return esAdmin() ? usuarioDAO.listarTodos() : Collections.emptyList();
    }

    public ResultadoOperacion crear(String usuario, String clave, String rol) {
        if (!esAdmin()) return new ResultadoOperacion(false, DENEGADO);
        if (usuario == null || usuario.isBlank() || clave == null || clave.length() < 6) {
            return new ResultadoOperacion(false, "Usuario obligatorio y contrasena de al menos 6 caracteres.");
        }
        if (usuarioDAO.obtenerPorUsuario(usuario.trim()) != null) {
            return new ResultadoOperacion(false, "El usuario '" + usuario.trim() + "' ya existe.");
        }
        boolean ok = usuarioDAO.registrar(new Usuario(usuario.trim(), clave, rol));
        return ok ? new ResultadoOperacion(true, "Usuario '" + usuario.trim() + "' creado con rol " + rol + ".")
                  : new ResultadoOperacion(false, "No se pudo crear el jugador.");
    }

    public ResultadoOperacion cambiarRol(int id, String nuevoRol) {
        if (!esAdmin()) return new ResultadoOperacion(false, DENEGADO);
        Usuario objetivo = usuarioDAO.obtenerPorId(id);
        if (objetivo == null) return new ResultadoOperacion(false, "No existe un jugador con ID " + id + ".");
        if (objetivo.esAdmin() && !Usuario.ROL_ADMIN.equalsIgnoreCase(nuevoRol) && usuarioDAO.contarAdmins() <= 1) {
            return new ResultadoOperacion(false, "No se puede degradar al unico ADMIN del sistema.");
        }
        boolean ok = usuarioDAO.cambiarRol(id, nuevoRol);
        return ok ? new ResultadoOperacion(true, "Rol de '" + objetivo.getUsuario() + "' actualizado a " + nuevoRol + ".")
                  : new ResultadoOperacion(false, "No se pudo cambiar el rol.");
    }

    public ResultadoOperacion cambiarClave(int id, String nuevaClave) {
        if (!esAdmin()) return new ResultadoOperacion(false, DENEGADO);
        if (nuevaClave == null || nuevaClave.length() < 6) {
            return new ResultadoOperacion(false, "La contrasena debe tener al menos 6 caracteres.");
        }
        if (usuarioDAO.obtenerPorId(id) == null) return new ResultadoOperacion(false, "No existe un jugador con ID " + id + ".");
        return usuarioDAO.actualizarClave(id, nuevaClave)
                ? new ResultadoOperacion(true, "Contrasena actualizada.")
                : new ResultadoOperacion(false, "No se pudo actualizar la contrasena.");
    }

    public ResultadoOperacion eliminarSeguro(int id) {
        if (!esAdmin()) return new ResultadoOperacion(false, DENEGADO);
        Usuario objetivo = usuarioDAO.obtenerPorId(id);
        if (objetivo == null) return new ResultadoOperacion(false, "No existe un jugador con ID " + id + ".");
        if (objetivo.getId() == usuarioAutenticado.getId()) {
            return new ResultadoOperacion(false, "No puedes eliminar tu propia cuenta mientras tienes la sesion activa.");
        }
        if (objetivo.esAdmin() && usuarioDAO.contarAdmins() <= 1) {
            return new ResultadoOperacion(false, "No se puede eliminar al unico ADMIN del sistema.");
        }
        int victorias = victoriaDAO.contarPorJugadorId(id);
        if (victorias > 0) {
            return new ResultadoOperacion(false, String.format(
                    "BORRADO DETENIDO: '%s' tiene %d victoria(s) en el ranking. Eliminarlo borraria su historial.",
                    objetivo.getUsuario(), victorias));
        }
        return usuarioDAO.eliminar(id)
                ? new ResultadoOperacion(true, "Usuario '" + objetivo.getUsuario() + "' eliminado.")
                : new ResultadoOperacion(false, "No se pudo eliminar el jugador.");
    }


    public ResultadoOperacion cambiarNombre(int id, String nuevoUsuario) {
        if (!esAdmin()) return new ResultadoOperacion(false, DENEGADO);
        if (nuevoUsuario == null || nuevoUsuario.isBlank()) {
            return new ResultadoOperacion(false, "El nombre de usuario es obligatorio.");
        }
        if (usuarioDAO.obtenerPorId(id) == null) return new ResultadoOperacion(false, "No existe un usuario con ID " + id + ".");
        if (usuarioDAO.obtenerPorUsuario(nuevoUsuario.trim()) != null) {
            return new ResultadoOperacion(false, "El usuario '" + nuevoUsuario.trim() + "' ya existe.");
        }
        return usuarioDAO.actualizarNombre(id, nuevoUsuario.trim())
                ? new ResultadoOperacion(true, "Nombre de usuario actualizado.")
                : new ResultadoOperacion(false, "No se pudo actualizar el nombre.");
    }

    public record ResultadoOperacion(boolean exitoso, String mensaje) {}
}
