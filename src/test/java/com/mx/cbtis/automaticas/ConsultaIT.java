package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.mx.cbtis.automaticas.EscenarioGestor.texto;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

class ConsultaIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E03 | Consulta válida: muestra proyecto, tarea y comentario reales")
    void autE03() throws Exception {
        pagina.navigate("/inicio");
        assertThat(tarjeta()).containsText(texto(app.proyecto, "descripcion"));
        tarjeta().getByRole(AriaRole.LINK,
                new Locator.GetByRoleOptions().setName("Ir al proyecto").setExact(true)).click();
        assertThat(pagina.locator("#tareasPendientes").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(texto(app.tarea, "nombre")).setExact(true))).isVisible();
        abrirTarea();
        assertThat(pagina.locator("#contenedorComentario")).containsText("Comentario base " + app.prefijo);
        assertThat(pagina.locator("#contenedorComentario")).containsText(texto(app.usuario, "nombre"));
        assertEquals(List.of(), erroresPagina);
    }

    @Test
    @DisplayName("AUT-F03 | Consulta fallida: HTTP 500 simulado muestra aviso visible")
    void autF03() throws Exception {
        simularError("consultarProyectos", 500);
        pagina.navigate("/inicio");
        assertThat(pagina.getByRole(AriaRole.ALERT)).containsText("No fue posible cargar los proyectos");
        assertThat(pagina.locator("#cardContainer .card")).hasCount(0);
        assertNotNull(app.buscarProyecto(app.prefijo));
    }
}

