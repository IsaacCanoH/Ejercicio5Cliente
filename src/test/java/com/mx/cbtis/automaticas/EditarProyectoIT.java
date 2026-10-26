package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.mx.cbtis.automaticas.EscenarioGestor.entero;
import static com.mx.cbtis.automaticas.EscenarioGestor.texto;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

class EditarProyectoIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E05 | Edición válida: conserva ID y actualiza descripción del proyecto")
    void autE05() throws Exception {
        abrirEdicionProyecto();
        assertThat(pagina.locator("#modalEditar.modal")).isVisible();
        pagina.locator("#editarDescripcion").fill("Edición " + app.prefijo);
        pagina.locator("#modalEditar.modal").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Guardar").setExact(true)).click();
        assertThat(tarjeta()).containsText("Edición " + app.prefijo);
        var guardado = app.buscarProyecto(app.prefijo);
        assertNotNull(guardado);
        assertEquals(entero(app.proyecto, "id_proyecto"), entero(guardado, "id_proyecto"));
        assertEquals("Edición " + app.prefijo, texto(guardado, "descripcion"));
        assertEquals(List.of(), erroresPagina);
    }

    @Test
    @DisplayName("AUT-F05 | Edición fallida: proyecto no encontrado simulado informa el error")
    void autF05() throws Exception {
        simularError("actualizarProyecto", 404);
        abrirEdicionProyecto();
        assertThat(pagina.getByRole(AriaRole.ALERT).filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("proyecto", Pattern.CASE_INSENSITIVE)))).isVisible();
        assertThat(pagina.locator("#modalEditar.modal")).isHidden();
        assertEquals(texto(app.proyecto, "descripcion"), texto(app.buscarProyecto(app.prefijo), "descripcion"));
    }
}

