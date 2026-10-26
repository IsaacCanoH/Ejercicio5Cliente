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

class CrearTareaIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E06 | Tarea válida: crea una tarea pendiente en el proyecto correcto")
    void autE06() throws Exception {
        String nombre = "Nueva " + app.prefijo;
        abrirTablero();
        pagina.locator("#btnAgregarTarea").click();
        pagina.locator("#inputTarea").fill(nombre);
        pagina.locator("#btnTarea").click();
        assertThat(pagina.locator("#tareasPendientes").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(nombre).setExact(true))).isVisible();
        var creadas = app.tareasDelProyecto().stream().filter(t -> texto(t, "nombre").equals(nombre)).toList();
        assertEquals(1, creadas.size());
        assertEquals("pendientes", texto(creadas.get(0), "lista"));
        assertEquals(entero(app.proyecto, "id_proyecto"), entero(creadas.get(0), "id_proyecto"));
    }

    @Test
    @DisplayName("AUT-F06 | Tarea incompleta: nombre vacío no envía ni crea un registro")
    void autF06() throws Exception {
        abrirTablero();
        pagina.locator("#btnAgregarTarea").click();
        pagina.locator("#btnTarea").click();
        assertThat(pagina.getByRole(AriaRole.ALERT).filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("nombre.*tarea", Pattern.CASE_INSENSITIVE)))).isVisible();
        assertEquals(0, envios("verificarTarea"));
        assertEquals(1, app.tareasDelProyecto().size());
    }
}

