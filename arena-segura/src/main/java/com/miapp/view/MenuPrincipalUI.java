package com.miapp.view;

import com.miapp.controller.UsuarioController;
import com.miapp.controller.CombateController;
import com.miapp.controller.UsuarioController;
import com.miapp.controller.MonstruoController;
import com.miapp.controller.RankingController;
import com.miapp.model.Usuario;
import demo.LoginInseguroDemo;

/**
 * Menu Principal de la aplicacion (MVC - View).
 * Solo captura datos y renderiza; toda regla de negocio vive en los Controllers.
 */
public class MenuPrincipalUI {

    private final UsuarioController usuarioController;
    private final UsuarioUI usuarioUI;

    private final LoginUI loginUI;
    private final MonstruoUI monstruoUI;
    private final CombateUI combateUI;
    private final RankingUI rankingUI;

    public MenuPrincipalUI() {
        this.usuarioController = new UsuarioController();
        MonstruoController monstruoController = new MonstruoController(usuarioController);
        CombateController combateController = new CombateController(usuarioController);
        RankingController rankingController = new RankingController(usuarioController);
        this.usuarioUI = new UsuarioUI(usuarioController);

        this.loginUI = new LoginUI(usuarioController);
        this.monstruoUI = new MonstruoUI(monstruoController);
        this.combateUI = new CombateUI(combateController, monstruoController);
        this.rankingUI = new RankingUI(rankingController);
    }

    public void iniciar() {
        int opcion;
        do {
            opcion = usuarioController.estaAutenticado() ? menuSesionActiva() : menuPublico();
            if (usuarioController.isBloqueado() && !usuarioController.estaAutenticado()) {
                System.out.println("\n[!] Sesion bloqueada por 3 intentos fallidos. La aplicacion se cerrara.");
                return;
            }
        } while (opcion != 0);

        System.out.println("\nGracias por jugar en Arena Segura. ¡Hasta la proxima!");
    }

    private int menuPublico() {
        System.out.println("\n========================================================");
        System.out.println("            ARENA SEGURA - SIMULADOR RPG & JDBC         ");
        System.out.println("========================================================");
        System.out.println("1. Iniciar Sesion (Login Seguro)");
        System.out.println("2. Registrar Nuevo Usuario");
        System.out.println("3. Ver Ranking General (Publico)");
        System.out.println("4. Demo de Inyeccion SQL (version insegura vs segura)");
        System.out.println("5. Demo de rendimiento: 1000 inserciones (comparativa JDBC)");
        System.out.println("0. Salir");

        int op = ConsolaUtil.leerEntero("Elige una opcion: ", 0, 5);
        switch (op) {
            case 1 -> loginUI.mostrarLogin();
            case 2 -> loginUI.mostrarRegistro();
            case 3 -> rankingUI.mostrarRankingGeneral();
            case 4 -> { LoginInseguroDemo.main(new String[0]); ConsolaUtil.pausar(); }
            case 5 -> { LoginInseguroDemo.benchmarkInserciones(); ConsolaUtil.pausar(); }
            default -> { }
        }
        return op;
    }

    private int menuSesionActiva() {
        Usuario actual = usuarioController.getUsuarioAutenticado();
        boolean esAdmin = actual.esAdmin();

        System.out.println("\n========================================================");
        System.out.printf("  ARENA SEGURA | SESION: %s [%s]%n", actual.getUsuario(), actual.getRol());
        System.out.println("========================================================");
        System.out.println("1. Combatir en la Arena (Registra victoria solo si ganas)");
        System.out.println("2. Consultar Bestiario (Catalogo de Monstruos)");
        System.out.println("3. Filtrar Monstruos por Vida Minima");
        System.out.println("4. Ver Ranking General de Victorias");
        System.out.println("5. Mi Historial de Victorias");

        if (esAdmin) {
            System.out.println("\n--- PANEL DE ADMINISTRADOR (ADMIN) ---");
            System.out.println("6. [ADMIN] Crear Nuevo Monstruo");
            System.out.println("7. [ADMIN] Actualizar Monstruo");
            System.out.println("8. [ADMIN] Eliminar Monstruo (Borrado Seguro)");
            System.out.println("9. [ADMIN] Gestionar Jugadores (CRUD Jugadores)");
        }

        System.out.println("\n0. Cerrar Sesion");

        int op = ConsolaUtil.leerEntero("Elige una opcion: ", 0, esAdmin ? 9 : 5);

        switch (op) {
            case 1 -> combateUI.iniciarEnfrentamiento();
            case 2 -> { monstruoUI.mostrarCatalogo(); ConsolaUtil.pausar(); }
            case 3 -> monstruoUI.formularioFiltrarPorVida();
            case 4 -> rankingUI.mostrarRankingGeneral();
            case 5 -> rankingUI.mostrarHistorialPersonal();
            case 6 -> monstruoUI.formularioCrear();
            case 7 -> monstruoUI.formularioActualizar();
            case 8 -> monstruoUI.formularioEliminarSeguro();
            case 9 -> usuarioUI.mostrarGestion();
            case 0 -> {
                usuarioController.logout();
                System.out.println("Sesion cerrada con exito.");
            }
            default -> { }
        }
        return op;
    }
}
