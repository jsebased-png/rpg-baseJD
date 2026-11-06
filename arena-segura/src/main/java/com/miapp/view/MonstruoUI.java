package com.miapp.view;

import com.miapp.controller.MonstruoController;
import com.miapp.model.Monstruo;

import java.util.List;

/**
 * Vista para la interaccion con el Bestiario y gestion de Monstruos (MVC - View).
 * Renderiza catalogos, formularios de captura y mensajes de resultados del controlador.
 */
public class MonstruoUI {

    private final MonstruoController controller;

    public MonstruoUI(MonstruoController controller) {
        this.controller = controller;
    }

    public void mostrarCatalogo() {
        System.out.println("\n========================================================");
        System.out.println("            CATALOGO DEL BESTIARIO DE MONSTRUOS         ");
        System.out.println("========================================================");

        List<Monstruo> monstruos = controller.listarTodos();
        if (monstruos.isEmpty()) {
            System.out.println("El catalogo esta vacio o no hay conexion con la BD.");
            return;
        }

        imprimirTablaMonstruos(monstruos);
    }

    public void formularioCrear() {
        System.out.println("\n--- [ADMIN] CREAR NUEVO MONSTRUO ---");
        String nombre = ConsolaUtil.leerTexto("Nombre del monstruo: ");
        int vida = ConsolaUtil.leerEntero("Puntos de Vida (HP): ", 1, 9999);
        int ataque = ConsolaUtil.leerEntero("Puntos de Ataque (ATK): ", 0, 999);

        var res = controller.crearMonstruo(nombre, vida, ataque);
        System.out.println((res.exitoso() ? "[+] " : "[-] ") + res.mensaje());
        ConsolaUtil.pausar();
    }

    public void formularioActualizar() {
        System.out.println("\n--- [ADMIN] ACTUALIZAR MONSTRUO ---");
        mostrarCatalogo();
        int id = ConsolaUtil.leerEntero("ID del monstruo a editar (0 para cancelar): ", 0, 999999);
        if (id == 0) return;

        String nombre = ConsolaUtil.leerTexto("Nuevo nombre: ");
        int vida = ConsolaUtil.leerEntero("Nueva vida (HP): ", 1, 9999);
        int ataque = ConsolaUtil.leerEntero("Nuevo ataque (ATK): ", 0, 999);

        var res = controller.actualizarMonstruo(id, nombre, vida, ataque);
        System.out.println((res.exitoso() ? "[+] " : "[-] ") + res.mensaje());
        ConsolaUtil.pausar();
    }

    public void formularioEliminarSeguro() {
        System.out.println("\n--- [ADMIN] ELIMINAR MONSTRUO (CONTROL SEGURO) ---");
        mostrarCatalogo();
        int id = ConsolaUtil.leerEntero("ID del monstruo a eliminar (0 para cancelar): ", 0, 999999);
        if (id == 0) return;

        int confirmar = ConsolaUtil.leerEntero("¿Estas seguro de eliminar el monstruo ID " + id + "? (1. Si, 2. No): ", 1, 2);
        if (confirmar != 1) {
            System.out.println("Operacion cancelada.");
            return;
        }

        var res = controller.eliminarMonstruoSeguro(id);
        System.out.println((res.exitoso() ? "[+] " : "[-] ") + res.mensaje());
        ConsolaUtil.pausar();
    }

    public void formularioFiltrarPorVida() {
        System.out.println("\n--- CONSULTA DE MONSTRUOS POR VIDA MINIMA ---");
        int vidaMinima = ConsolaUtil.leerEntero("Vida minima requerida: ", 1, 9999);
        List<Monstruo> filtrados = controller.listarPorVidaMinima(vidaMinima);

        System.out.printf("%nMonstruos con vida >= %d:%n", vidaMinima);
        if (filtrados.isEmpty()) {
            System.out.println("Ningun monstruo cumple con el criterio.");
        } else {
            imprimirTablaMonstruos(filtrados);
        }
        ConsolaUtil.pausar();
    }

    private void imprimirTablaMonstruos(List<Monstruo> lista) {
        System.out.printf("%-5s | %-25s | %-10s | %-10s%n", "ID", "Nombre", "Vida (HP)", "Ataque");
        System.out.println("------+---------------------------+------------+-----------");
        for (Monstruo m : lista) {
            System.out.printf("%-5d | %-25s | %-10d | %-10d%n",
                    m.getId(), m.getNombre(), m.getVida(), m.getAtaque());
        }
        System.out.println("--------------------------------------------------------");
    }
}

