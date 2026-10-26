package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static com.mx.cbtis.automaticas.EscenarioGestor.entero;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

class EliminarProyectoIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E10 | Eliminación válida: proyecto ausente de la lista y del backend")
    void autE10() throws Exception {
        pagina.navigate("/inicio");
        assertThat(tarjeta()).isVisible();
        eliminarProyecto();
        assertThat(pagina.locator("#modalAlerta")).isHidden();
        assertThat(tarjeta()).hasCount(0);
        assertNull(app.buscarProyecto(app.prefijo));
    }

    @Test
    @DisplayName("AUT-F10 | Eliminación fallida: HTTP 500 simulado conserva proyecto y muestra error")
    void autF10() throws Exception {
        pagina.navigate("/inicio");
        assertThat(tarjeta()).isVisible();
        simularError("eliminarProyecto", 500);
        eliminarProyecto();
        assertThat(pagina.getByRole(AriaRole.ALERT).filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("eliminar.*proyecto", Pattern.CASE_INSENSITIVE)))).isVisible();
        assertThat(tarjeta()).isVisible();
        assertEquals(entero(app.proyecto, "id_proyecto"), entero(app.buscarProyecto(app.prefijo), "id_proyecto"));
    }
}

