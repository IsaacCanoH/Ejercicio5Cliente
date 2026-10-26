package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.mx.cbtis.automaticas.EscenarioGestor.texto;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistroIT extends PruebaNavegador {

    @Test
    @DisplayName("AUT-E01 | Registro válido: usuario persistido y redirección a login")
    void autE01() throws Exception {
        var datos = app.usuario.deepCopy();
        datos.put("nombre", "Registro " + app.prefijo);
        datos.put("correo_electronico", "registro-" + app.prefijo.toLowerCase(java.util.Locale.ROOT) + "@example.test");
        app.correosUsuarios.add(texto(datos, "correo_electronico"));
        pagina.navigate("/register");
        rellenarRegistro(datos);
        boton("Registrar").click();
        assertThat(pagina).hasURL(Pattern.compile(".*/login$"));
        var guardados = app.listar("/buscarUsuarios").stream()
                .filter(u -> texto(u, "correo_electronico").equals(texto(datos, "correo_electronico"))).toList();
        assertEquals(1, guardados.size());
        assertEquals(texto(datos, "nombre"), texto(guardados.get(0), "nombre"));
        assertEquals("Estudiante", texto(guardados.get(0), "rol"));
    }

    @Test
    @DisplayName("AUT-F01 | Registro incompleto: nombre vacío bloquea el envío")
    void autF01() throws Exception {
        pagina.navigate("/register");
        var datos = app.usuario.deepCopy();
        datos.put("nombre", "");
        rellenarRegistro(datos);
        boton("Registrar").click();
        assertThat(pagina.locator("form")).hasClass(Pattern.compile(".*was-validated.*"));
        assertEquals(true, pagina.locator("input[name=\"nombre\"]").evaluate("el => el.validity.valueMissing"));
        assertEquals(0, envios("saveUsuario"));
        assertThat(pagina).hasURL(Pattern.compile(".*/register$"));
    }
}

