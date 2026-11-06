package com.miapp.controller;

import com.miapp.model.MonstruoDAO;
import com.miapp.model.VictoriaDAO;
import com.miapp.model.Usuario;
import com.miapp.model.Monstruo;

import java.util.Collections;
import java.util.List;

/**
 * Controlador para la administracion y consulta del Bestiario (MVC - Controller).
 *
 * REGLAS DE NEGOCIO:
 * 1. Control de Permisos por Rol:
 *    Unicamente los usuarios con rol 'ADMIN' tienen autorizacion para crear, editar o eliminar monstruos.
 * 2. Control de Borrado Seguro (Integridad Referencial Preventiva):
 *    Antes de ejecutar el borrado de un monstruo, el controlador consulta a VictoriaDAO
 *    si existen victorias historicas asociadas. Si existen, detiene la operacion y retorna
 *    un mensaje descriptivo amigable, impidiendo que la BD lance una excepcion de Foreign Key.
 */
public class MonstruoController {

    private final MonstruoDAO monstruoDAO;
    private final VictoriaDAO victoriaDAO;
    private final UsuarioController usuarioController;

    public MonstruoController(UsuarioController usuarioController) {
        this(new MonstruoDAO(), new VictoriaDAO(), usuarioController);
    }

    public MonstruoController(MonstruoDAO monstruoDAO, VictoriaDAO victoriaDAO, UsuarioController usuarioController) {
        this.monstruoDAO = monstruoDAO;
        this.victoriaDAO = victoriaDAO;
        this.usuarioController = usuarioController;
    }

    /**
     * Valida permisos de administrador para operaciones de modificacion.
     */
    private boolean esOperacionPermitida() {
        Usuario actual = usuarioController.getUsuarioAutenticado();
        return actual != null && actual.esAdmin();
    }

    /**
     * Crea un nuevo monstruo en el catalogo (Solo ADMIN).
     */
    public ResultadoOperacion crearMonstruo(String nombre, int vida, int ataque) {
        if (!esOperacionPermitida()) {
            return new ResultadoOperacion(false, "ACCESO DENEGADO: Solo los administradores pueden crear monstruos.");
        }
        try {
            Monstruo existente = monstruoDAO.buscarPorNombre(nombre);
            if (existente != null) {
                return new ResultadoOperacion(false, "Ya existe un monstruo registrado con el nombre '" + nombre + "'.");
            }

            Monstruo nuevo = new Monstruo(nombre, vida, ataque);
            boolean exito = monstruoDAO.crear(nuevo);
            if (exito) {
                return new ResultadoOperacion(true, "Monstruo '" + nombre + "' creado exitosamente con ID " + nuevo.getId() + ".");
            } else {
                return new ResultadoOperacion(false, "Error al registrar el monstruo en la base de datos.");
            }
        } catch (IllegalArgumentException e) {
            return new ResultadoOperacion(false, "Datos invalidos: " + e.getMessage());
        }
    }

    /**
     * Actualiza un monstruo existente (Solo ADMIN).
     */
    public ResultadoOperacion actualizarMonstruo(int id, String nombre, int vida, int ataque) {
        if (!esOperacionPermitida()) {
            return new ResultadoOperacion(false, "ACCESO DENEGADO: Solo los administradores pueden editar monstruos.");
        }
        Monstruo existente = monstruoDAO.obtenerPorId(id);
        if (existente == null) {
            return new ResultadoOperacion(false, "No existe ningun monstruo con el ID " + id + ".");
        }
        try {
            existente.setNombre(nombre);
            existente.setVida(vida);
            existente.setAtaque(ataque);

            boolean exito = monstruoDAO.actualizar(existente);
            return exito
                    ? new ResultadoOperacion(true, "Monstruo ID " + id + " actualizado correctamente.")
                    : new ResultadoOperacion(false, "No se pudo actualizar el monstruo.");
        } catch (IllegalArgumentException e) {
            return new ResultadoOperacion(false, "Datos invalidos: " + e.getMessage());
        }
    }

    /**
     * BORRADO SEGURO:
     * Verifica proactivamente si el monstruo tiene victorias asociadas antes de borrar.
     * Si tiene victorias, aborta la operacion e informa al usuario amigablemente.
     */
    public ResultadoOperacion eliminarMonstruoSeguro(int id) {
        if (!esOperacionPermitida()) {
            return new ResultadoOperacion(false, "ACCESO DENEGADO: Solo los administradores pueden eliminar monstruos.");
        }

        Monstruo monstruo = monstruoDAO.obtenerPorId(id);
        if (monstruo == null) {
            return new ResultadoOperacion(false, "El monstruo con ID " + id + " no existe.");
        }

        // Consulta preventiva a la tabla de victorias
        int victoriasAsociadas = victoriaDAO.contarPorMonstruoId(id);
        if (victoriasAsociadas > 0) {
            return new ResultadoOperacion(false, String.format(
                    "BORRADO DETENIDO: El monstruo '%s' posee %d victoria(s) asociada(s) en el historial de combates. " +
                    "No se puede eliminar para no corromper la integridad referencial del ranking.",
                    monstruo.getNombre(), victoriasAsociadas));
        }

        boolean exito = monstruoDAO.eliminar(id);
        return exito
                ? new ResultadoOperacion(true, "Monstruo '" + monstruo.getNombre() + "' eliminado satisfactoriamente.")
                : new ResultadoOperacion(false, "No se pudo eliminar el monstruo de la base de datos.");
    }

    public List<Monstruo> listarTodos() {
        return monstruoDAO.listarTodos();
    }

    public Monstruo buscarPorNombre(String nombre) {
        return monstruoDAO.buscarPorNombre(nombre);
    }

    public List<Monstruo> listarPorVidaMinima(int vidaMinima) {
        if (vidaMinima <= 0) {
            return Collections.emptyList();
        }
        return monstruoDAO.listarPorVidaMinima(vidaMinima);
    }

    /**
     * Registro utilitario para transferir el resultado de operaciones del controlador a la vista.
     */
    public record ResultadoOperacion(boolean exitoso, String mensaje) {}
}

