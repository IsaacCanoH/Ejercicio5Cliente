package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static com.mx.cbtis.automaticas.EscenarioGestor.texto;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

class CrearProyectoIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E04 | Proyecto válido: crea una tarjeta y persiste los datos")
    void autE04() throws Exception {
        String nombre = app.prefijo + " nuevo";
        app.nombresProyectos.add(nombre);
        pagina.navigate("/inicio");
        boton("Agregar").click();
        rellenarProyecto(nombre);
        pagina.locator("#staticBackdrop").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Guardar").setExact(true)).click();
        assertThat(pagina.locator("#cardContainer .card")
                .filter(new Locator.FilterOptions().setHasText(nombre))).isVisible();
        var guardado = app.buscarProyecto(nombre);
        assertNotNull(guardado);
        assertEquals(nombre, texto(guardado, "nombre"));
        assertEquals("2026-10-11", texto(guardado, "fecha_inicio"));
        assertEquals("2026-10-18", texto(guardado, "fecha_finalizacion"));
        assertEquals("En proceso", texto(guardado, "estado_proyecto"));
    }

    @Test
    @DisplayName("AUT-F04 | Proyecto incompleto: nombre vacío bloquea la creación")
    void autF04() throws Exception {
        pagina.navigate("/inicio");
        boton("Agregar").click();
        rellenarProyecto("");
        pagina.locator("#staticBackdrop").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Guardar").setExact(true)).click();
        assertEquals(true, pagina.locator("#nombre").evaluate("el => el.validity.valueMissing"));
        assertEquals(0, envios("saveProyecto"));
        assertThat(pagina.locator("#staticBackdrop")).isVisible();
    }
}

