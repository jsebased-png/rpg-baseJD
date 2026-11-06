package com.miapp.view;

import com.miapp.controller.UsuarioController;

/**
 * Vista de Autenticacion e Inicio de Sesion (MVC - View).
 * Responsabilidad exclusiva: captura de credenciales y renderizado de mensajes de estado.
 */
public class LoginUI {

    private final UsuarioController usuarioController;

    public LoginUI(UsuarioController usuarioController) {
        this.usuarioController = usuarioController;
    }

    /**
     * Muestra la interfaz de Login interactiva.
     * Si la cuenta queda bloqueada (3 intentos fallidos), alerta al usuario y detiene el flujo.
     *
     * @return true si el usuario se autentico correctamente, false si cancelo o se bloqueo.
     */
    public boolean mostrarLogin() {
        System.out.println("\n==============================================");
        System.out.println("          ARENA SEGURA - INICIAR SESION       ");
        System.out.println("==============================================");

        if (usuarioController.isBloqueado()) {
            System.out.println("[!] ACCESO BLOQUEADO: Se supero el limite de 3 intentos fallidos.");
            System.out.println("    Por motivos de seguridad, la sesion actual esta suspendida.");
            return false;
        }

        while (!usuarioController.isBloqueado()) {
            System.out.printf("Intentos restantes: %d / 3%n", usuarioController.getIntentosRestantes());
            String usuario = ConsolaUtil.leerTexto("Usuario (0 para cancelar): ");
            if ("0".equals(usuario)) {
                return false;
            }

            String clave = ConsolaUtil.leerTexto("Contrasena: ");

            boolean autenticado = usuarioController.login(usuario, clave);
            if (autenticado) {
                System.out.printf("%n[+] ¡Bienvenido, %s! Rol activo: [%s]%n",
                        usuarioController.getUsuarioAutenticado().getUsuario(),
                        usuarioController.getUsuarioAutenticado().getRol());
                return true;
            } else {
                if (usuarioController.isBloqueado()) {
                    System.out.println("\n[!] ALERTA DE SEGURIDAD: 3 intentos fallidos consecutivos.");
                    System.out.println("    La sesion ha sido BLOQUEADA temporalmente.");
                    return false;
                } else {
                    System.out.println("[-] Credenciales invalidas. Intentalo de nuevo.\n");
                }
            }
        }
        return false;
    }

    public void mostrarRegistro() {
        System.out.println("\n==============================================");
        System.out.println("       ARENA SEGURA - REGISTRO DE JUGADOR     ");
        System.out.println("==============================================");
        String usuario = ConsolaUtil.leerTexto("Nuevo nombre de usuario: ");
        String clave = ConsolaUtil.leerTexto("Contrasena: ");
        var res = usuarioController.registrarNuevoUsuario(usuario, clave);
        System.out.println((res.exitoso() ? "[+] " : "[-] ") + res.mensaje());
        ConsolaUtil.pausar();
    }
}

