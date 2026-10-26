package com.mx.cbtis.automaticas;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

/** Datos exclusivos por caso y limpieza limitada a esos registros. */
final class EscenarioGestor implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    final String prefijo;
    final Set<String> nombresProyectos = new LinkedHashSet<>();
    final Set<String> correosUsuarios = new LinkedHashSet<>();
    final APIRequestContext api;
    ObjectNode usuario;
    JsonNode proyecto;
    JsonNode tarea;
    Map<String, Integer> limpieza = Map.of();

    EscenarioGestor(APIRequestContext api, String caso) {
        this.api = api;
        prefijo = "P2-" + caso + "-" + UUID.randomUUID().toString().substring(0, 8);
        nombresProyectos.add(prefijo);
        String correo = prefijo.toLowerCase(java.util.Locale.ROOT) + "@example.test";
        correosUsuarios.add(correo);
        usuario = JSON.valueToTree(Map.of(
                "nombre", "Usuario " + prefijo, "correo_electronico", correo,
                "contrasena", "AutoP2_123", "rol", "Estudiante", "imagen_usuario", "#483078"));
    }

    void iniciar() {
        post("/addUsuario", usuario);
        JsonNode registrado = listar("/buscarUsuarios").stream()
                .filter(u -> texto(u, "correo_electronico").equals(texto(usuario, "correo_electronico")))
                .findFirst().orElse(null);
        assertNotNull(registrado, "La API debe persistir el usuario de prueba");
        usuario.put("id_usuario", entero(registrado, "id_usuario"));
        post("/addProyecto", Map.of(
                "nombre", prefijo, "descripcion", "Proyecto base " + prefijo,
                "fecha_inicio", "2026-10-11", "fecha_finalizacion", "2026-10-18",
                "estado_proyecto", "En proceso", "fondo", "/Images/blanco.png"));
        proyecto = buscarProyecto(prefijo);
        assertNotNull(proyecto, "La API debe persistir el proyecto de prueba");
        post("/addTareas", Map.of(
                "nombre", "Tarea " + prefijo, "descripcion", "Descripción base",
                "fecha_vencimiento", "2026-10-16", "lista", "pendientes",
                "estado", "primary", "id_proyecto", entero(proyecto, "id_proyecto")));
        List<JsonNode> tareas = tareasDelProyecto();
        assertEquals(1, tareas.size(), "El proyecto nuevo debe contener una tarea");
        tarea = tareas.get(0);
        post("/addComentario", Map.of(
                "id_tarea", entero(tarea, "id_tarea"), "id_usuario", texto(usuario, "nombre"),
                "texto_comentario", "Comentario base " + prefijo, "fecha_hora", "2026-10-11 12:00:00"));
    }

    JsonNode post(String ruta, Object datos) {
        RequestOptions opciones = RequestOptions.create();
        if (datos != null) {
            opciones.setData(datos instanceof JsonNode ? JSON.convertValue(datos, Object.class) : datos);
        }
        return leer(api.post(ruta, opciones));
    }

    JsonNode post(String ruta) {
        return post(ruta, null);
    }

    JsonNode get(String ruta) {
        return leer(api.get(ruta));
    }

    private JsonNode leer(APIResponse respuesta) {
        try {
            assertTrue(respuesta.ok(), () -> "HTTP " + respuesta.status() + " al consultar " + respuesta.url());
            return JSON.readTree(respuesta.text());
        } catch (java.io.IOException error) {
            throw new IllegalStateException("La API no devolvió JSON válido", error);
        } finally {
            respuesta.dispose();
        }
    }

    List<JsonNode> listar(String ruta) {
        return elementos(post(ruta));
    }

    List<JsonNode> tareasDelProyecto() {
        return elementos(post("/getTarea", Map.of("id_proyecto", entero(proyecto, "id_proyecto"))));
    }

    List<JsonNode> comentariosDeLaTarea() {
        return elementos(post("/getComentario", Map.of("id_tarea", entero(tarea, "id_tarea"))));
    }

    JsonNode consultarTarea() {
        return get("/buscarTarea?id_tarea=" + entero(tarea, "id_tarea"));
    }

    JsonNode buscarProyecto(String nombre) {
        return listar("/buscarProyectos").stream()
                .filter(p -> texto(p, "nombre").equals(nombre)).findFirst().orElse(null);
    }

    static List<JsonNode> elementos(JsonNode arreglo) {
        assertTrue(arreglo.isArray(), "Se esperaba un arreglo JSON");
        List<JsonNode> elementos = new ArrayList<>();
        arreglo.forEach(elementos::add);
        return elementos;
    }

    static int entero(JsonNode nodo, String campo) {
        return nodo.path(campo).asInt();
    }

    static String texto(JsonNode nodo, String campo) {
        return nodo.path(campo).asText();
    }

    @Override
    public void close() {
        List<JsonNode> proyectos = listar("/buscarProyectos").stream()
                .filter(p -> nombresProyectos.contains(texto(p, "nombre"))).toList();
        Set<Integer> idsProyectos = new HashSet<>();
        proyectos.forEach(p -> idsProyectos.add(entero(p, "id_proyecto")));
        if (proyecto != null) {
            idsProyectos.add(entero(proyecto, "id_proyecto"));
        }
        List<JsonNode> tareas = listar("/buscarTareas").stream()
                .filter(t -> idsProyectos.contains(entero(t, "id_proyecto"))).toList();
        Set<Integer> idsTareas = new HashSet<>();
        tareas.forEach(t -> idsTareas.add(entero(t, "id_tarea")));
        if (tarea != null) {
            idsTareas.add(entero(tarea, "id_tarea"));
        }
        List<JsonNode> comentarios = listar("/buscarComentarios").stream()
                .filter(c -> idsTareas.contains(entero(c, "id_tarea"))).toList();
        for (JsonNode comentario : comentarios) {
            leer(api.delete("/deleteComentario", RequestOptions.create().setData(JSON.convertValue(comentario, Object.class))));
        }
        for (JsonNode registro : tareas) {
            leer(api.delete("/deleteTareas", RequestOptions.create().setData(JSON.convertValue(registro, Object.class))));
        }
        for (JsonNode registro : proyectos) {
            leer(api.delete("/deleteProyecto", RequestOptions.create().setData(JSON.convertValue(registro, Object.class))));
        }
        List<JsonNode> usuarios = listar("/buscarUsuarios").stream()
                .filter(u -> correosUsuarios.contains(texto(u, "correo_electronico"))).toList();
        for (JsonNode registro : usuarios) {
            leer(api.delete("/deleteUsuario", RequestOptions.create().setData(JSON.convertValue(registro, Object.class))));
        }
        limpieza = Map.of("usuarios", usuarios.size(), "proyectos", proyectos.size(),
                "tareas", tareas.size(), "comentarios", comentarios.size());
    }
}
