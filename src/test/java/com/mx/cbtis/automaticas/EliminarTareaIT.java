package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static com.mx.cbtis.automaticas.EscenarioGestor.entero;
import static com.mx.cbtis.automaticas.EscenarioGestor.texto;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

class EliminarTareaIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E09 | Eliminación válida: tarea ausente del tablero y del backend")
    void autE09() throws Exception {
        abrirTablero();
        eliminarTarea();
        assertThat(pagina.locator("#modalAlerta")).isHidden();
        assertThat(pagina.locator("#tareasPendientes").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(texto(app.tarea, "nombre")).setExact(true))).hasCount(0);
        assertFalse(app.tareasDelProyecto().stream()
                .anyMatch(t -> entero(t, "id_tarea") == entero(app.tarea, "id_tarea")));
    }

    @Test
    @DisplayName("AUT-F09 | Eliminación fallida: HTTP 500 simulado conserva tarea y muestra error")
    void autF09() throws Exception {
        abrirTablero();
        simularError("eliminarTarea", 500);
        eliminarTarea();
        assertThat(pagina.getByRole(AriaRole.ALERT).filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("eliminar.*tarea", Pattern.CASE_INSENSITIVE)))).isVisible();
        assertThat(pagina.locator("#tareasPendientes").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(texto(app.tarea, "nombre")).setExact(true))).isVisible();
        assertEquals(entero(app.tarea, "id_tarea"), entero(app.consultarTarea(), "id_tarea"));
    }
}

