package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static com.mx.cbtis.automaticas.EscenarioGestor.entero;
import static com.mx.cbtis.automaticas.EscenarioGestor.texto;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

class LoginIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E02 | Login válido: accede a inicio y conserva el usuario")
    void autE02() throws Exception {
        pagina.navigate("/login");
        pagina.locator("#exampleInputEmail1").fill(texto(app.usuario, "correo_electronico"));
        pagina.locator("#exampleInputPassword1").fill(texto(app.usuario, "contrasena"));
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Iniciar sesi"))).click();
        assertThat(pagina).hasURL(Pattern.compile(".*/inicio$"));
        var guardado = EscenarioGestor.JSON.readTree((String) pagina.evaluate("localStorage.getItem('usuario')"));
        assertEquals(entero(app.usuario, "id_usuario"), entero(guardado, "id_usuario"));
        assertThat(tarjeta()).isVisible();
    }

    @Test
    @DisplayName("AUT-F02 | Login inválido: rechaza una contraseña incorrecta")
    void autF02() throws Exception {
        pagina.navigate("/login");
        pagina.evaluate("localStorage.removeItem('usuario')");
        pagina.locator("#exampleInputEmail1").fill(texto(app.usuario, "correo_electronico"));
        pagina.locator("#exampleInputPassword1").fill("Incorrecta_123");
        pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(Pattern.compile("Iniciar sesi"))).click();
        assertThat(pagina.locator("#alerta5")).isVisible();
        assertThat(pagina).hasURL(Pattern.compile(".*/login$"));
        assertNull(pagina.evaluate("localStorage.getItem('usuario')"));
    }
}

