package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.mx.cbtis.automaticas.EscenarioGestor.entero;
import static com.mx.cbtis.automaticas.EscenarioGestor.texto;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.options.AriaRole;

class ComentarioIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E08 | Comentario válido: un único envío, respuesta exitosa y persistencia")
    void autE08() throws Exception {
        abrirTablero();
        abrirTarea();
        String comentario = "Comentario automático " + app.prefijo;
        pagina.locator("#comentarioT").fill(comentario);
        var respuesta = pagina.waitForResponse(r -> ruta(r.url()).equals("/saveComentario"),
                () -> pagina.locator("button[onclick=\"guardarComentario()\"]").click());
        assertTrue(respuesta.ok(), "saveComentario respondió HTTP " + respuesta.status());
        assertTrue(respuesta.headers().getOrDefault("content-type", "").contains("application/json"));
        assertThat(pagina.locator("#editarTarea")).isHidden();
        assertThat(boton(texto(app.tarea, "nombre")).first()).isVisible();
        abrirTarea();
        assertThat(pagina.locator("#contenedorComentario")).containsText(comentario);
        var guardados = app.comentariosDeLaTarea().stream()
                .filter(c -> texto(c, "texto_comentario").equals(comentario)).toList();
        assertEquals(1, guardados.size());
        assertEquals(entero(app.tarea, "id_tarea"), entero(guardados.get(0), "id_tarea"));
        assertEquals(texto(app.usuario, "nombre"), texto(guardados.get(0), "id_usuario"));
        assertEquals(1, envios("saveComentario"));
        assertEquals(0, envios("saveTarea"));
    }

    @Test
    @DisplayName("AUT-F08 | Comentario incompleto: texto vacío bloquea el envío")
    void autF08() throws Exception {
        abrirTablero();
        abrirTarea();
        pagina.locator("button[onclick=\"guardarComentario()\"]").click();
        assertThat(pagina.locator("#editarTarea").getByRole(AriaRole.ALERT))
                .containsText(Pattern.compile("comentario", Pattern.CASE_INSENSITIVE));
        assertEquals(0, envios("saveComentario"));
        assertEquals(1, app.comentariosDeLaTarea().size());
    }
}

