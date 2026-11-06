package com.miapp.view;

import com.miapp.controller.UsuarioController;
import com.miapp.model.Usuario;

/**
 * Pantalla de gestion de usuarios (CRUD) para el ADMIN (MVC - View).
 * Solo captura datos y renderiza; las reglas de negocio viven en UsuarioController.
 */
public class UsuarioUI {

    private final UsuarioController usuarioController;

    public UsuarioUI(UsuarioController usuarioController) {
        this.usuarioController = usuarioController;
    }

    public void mostrarGestion() {
        System.out.println("\n========================================================");
        System.out.println("              [ADMIN] GESTION DE USUARIOS               ");
        System.out.println("========================================================");
        for (Usuario u : usuarioController.listarTodos()) {
            System.out.printf("  [%d] %-15s | Rol: %-7s%n", u.getId(), u.getUsuario(), u.getRol());
        }

        System.out.println("\n1. Crear usuario  2. Cambiar rol  3. Cambiar clave  4. Cambiar nombre  5. Eliminar  0. Cancelar");
        int sub = ConsolaUtil.leerEntero("Opcion: ", 0, 5);
        UsuarioController.ResultadoOperacion res = null;
        switch (sub) {
            case 1 -> {
                String usuario = ConsolaUtil.leerTexto("Usuario: ");
                String clave = ConsolaUtil.leerTexto("Contrasena (min. 6): ");
                int rolOpt = ConsolaUtil.leerEntero("Rol (1. JUGADOR, 2. ADMIN): ", 1, 2);
                res = usuarioController.crear(usuario, clave, rolOpt == 2 ? Usuario.ROL_ADMIN : Usuario.ROL_JUGADOR);
            }
            case 2 -> {
                int id = ConsolaUtil.leerEntero("ID del usuario: ", 1, 999999);
                int rolOpt = ConsolaUtil.leerEntero("Nuevo rol (1. JUGADOR, 2. ADMIN): ", 1, 2);
                res = usuarioController.cambiarRol(id, rolOpt == 2 ? Usuario.ROL_ADMIN : Usuario.ROL_JUGADOR);
            }
            case 3 -> {
                int id = ConsolaUtil.leerEntero("ID del usuario: ", 1, 999999);
                String clave = ConsolaUtil.leerTexto("Nueva contrasena (min. 6): ");
                res = usuarioController.cambiarClave(id, clave);
            }
            case 4 -> {
                int id = ConsolaUtil.leerEntero("ID del usuario: ", 1, 999999);
                String nombre = ConsolaUtil.leerTexto("Nuevo nombre de usuario: ");
                res = usuarioController.cambiarNombre(id, nombre);
            }
            case 5 -> {
                int id = ConsolaUtil.leerEntero("ID del usuario a eliminar: ", 1, 999999);
                res = usuarioController.eliminarSeguro(id);
            }
            default -> { }
        }
        if (res != null) {
            System.out.println((res.exitoso() ? "[+] " : "[-] ") + res.mensaje());
        }
        ConsolaUtil.pausar();
    }
}
