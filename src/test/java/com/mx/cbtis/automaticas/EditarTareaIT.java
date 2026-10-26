package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.mx.cbtis.automaticas.EscenarioGestor.entero;
import static com.mx.cbtis.automaticas.EscenarioGestor.texto;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

class EditarTareaIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E07 | Edición válida: actualiza descripción, fecha y lista de la tarea")
    void autE07() throws Exception {
        abrirTablero();
        abrirTarea();
        pagina.locator("#descripcionT").fill("Actualizada " + app.prefijo);
        pagina.locator("#editarTarea").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("Fechas"))).click();
        pagina.locator("#inputFechas").fill("2026-10-17");
        pagina.locator("#cambiarLista").selectOption("terminado");
        pagina.locator("#editarTarea").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Guardar cambios").setExact(true)).click();
        assertThat(pagina.locator("#trabajoTerminado").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(texto(app.tarea, "nombre")).setExact(true))).isVisible();
        var guardada = app.consultarTarea();
        assertEquals(entero(app.tarea, "id_tarea"), entero(guardada, "id_tarea"));
        assertEquals(entero(app.proyecto, "id_proyecto"), entero(guardada, "id_proyecto"));
        assertEquals("Actualizada " + app.prefijo, texto(guardada, "descripcion"));
        assertEquals("2026-10-17", texto(guardada, "fecha_vencimiento"));
        assertEquals("terminado", texto(guardada, "lista"));
    }

    @Test
    @DisplayName("AUT-F07 | Edición fallida: HTTP 500 simulado informa y conserva la tarea")
    void autF07() throws Exception {
        abrirTablero();
        abrirTarea();
        simularError("saveTarea", 500);
        pagina.locator("#descripcionT").fill("No guardar " + app.prefijo);
        pagina.locator("#editarTarea").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Guardar cambios").setExact(true)).click();
        assertThat(pagina.locator("#editarTarea").getByRole(AriaRole.ALERT))
                .containsText(Pattern.compile("guardar.*tarea", Pattern.CASE_INSENSITIVE));
        assertThat(pagina.locator("#editarTarea")).isVisible();
        assertEquals(texto(app.tarea, "descripcion"), texto(app.consultarTarea(), "descripcion"));
    }
}

