package com.mx.cbtis.automaticas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.AriaRole;
import com.mx.cbtis.Ejercicio5ClienteApplication;

@SpringBootTest(classes = Ejercicio5ClienteApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(PruebaNavegador.Resultado.class)
abstract class PruebaNavegador {
    @LocalServerPort
    private int puerto;
    private Playwright playwright;
    private Browser navegador;
    private APIRequestContext api;
    private BrowserContext contexto;
    private Path evidencias;
    private boolean fallo = true;
    protected Page pagina;
    protected EscenarioGestor app;
    protected final List<Map<String, Object>> peticiones = new ArrayList<>();
    private final List<Map<String, Object>> respuestas = new ArrayList<>();
    protected final List<String> erroresPagina = new ArrayList<>();

    @BeforeAll
    void iniciarNavegador() {
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        String browser = opcion("e2e.browser", "P2_BROWSER", "chrome");
        BrowserType.LaunchOptions opciones = new BrowserType.LaunchOptions().setHeadless(true);
        if (!browser.equals("chromium")) {
            opciones.setChannel(browser);
        }
        navegador = playwright.chromium().launch(opciones);
    }

    @BeforeEach
    void prepararCaso(TestInfo info) throws IOException {
        fallo = true;
        peticiones.clear();
        respuestas.clear();
        erroresPagina.clear();
        var identificador = Pattern.compile("AUT-[EF]\\d+").matcher(info.getDisplayName());
        assertTrue(identificador.find(), "Cada prueba debe conservar su identificador AUT");
        String run = opcion("e2e.run", "P2_RUN", "final");
        assertTrue(run.matches("[a-zA-Z0-9_-]+"), "Nombre de ejecución inválido");
        evidencias = Path.of("target", "e2e", run, identificador.group());
        Files.createDirectories(evidencias);
        String frontend = opcion("e2e.frontend", "FRONTEND_URL", "http://localhost:" + puerto);
        String backend = opcion("e2e.backend", "BACKEND_URL", "http://localhost:8081");
        api = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(backend).setTimeout(10000));
        contexto = navegador.newContext(new Browser.NewContextOptions()
                .setBaseURL(frontend).setViewportSize(1440, 1000));
        // Los iconos decorativos de un CDN no deben bloquear las pruebas funcionales.
        contexto.route("https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css",
                ruta -> ruta.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                        .setStatus(200).setContentType("text/css").setBody("/* iconos externos omitidos en E2E */")));
        contexto.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true).setSnapshots(true).setSources(true));
        pagina = contexto.newPage();
        pagina.setDefaultTimeout(4000);
        pagina.setDefaultNavigationTimeout(20000);
        pagina.onPageError(erroresPagina::add);
        pagina.onRequest(solicitud -> {
            if (esPostDelCliente(solicitud.url(), solicitud.method(), frontend)) {
                String cuerpo = solicitud.postData() == null ? "" : solicitud.postData();
                peticiones.add(Map.of("metodo", solicitud.method(), "ruta", ruta(solicitud.url()),
                        "datos", cuerpo.replaceAll("(\"?contrasena\"?\\s*[:=]\\s*)[^&,}]+", "$1[oculta]")));
            }
        });
        pagina.onResponse(respuesta -> {
            if (esPostDelCliente(respuesta.url(), respuesta.request().method(), frontend)) {
                respuestas.add(Map.of("ruta", ruta(respuesta.url()), "estado", respuesta.status()));
            }
        });
        app = new EscenarioGestor(api, identificador.group());
        app.iniciar();
        pagina.addInitScript("localStorage.setItem('usuario', " + json(app.usuario.toString()) + ");");
    }

    @AfterEach
    void limpiarCaso() throws IOException {
        // La limpieza también se intenta si la preparación o el caso fallan.
        try {
            if (pagina != null && !pagina.isClosed()) {
                pagina.screenshot(new Page.ScreenshotOptions().setPath(evidencias.resolve("captura.png")));
            }
            if (contexto != null) {
                if (fallo) {
                    contexto.tracing().stop(new Tracing.StopOptions().setPath(evidencias.resolve("traza.zip")));
                } else {
                    contexto.tracing().stop();
                }
            }
        } finally {
            try {
                try {
                    if (app != null) {
                        app.close();
                    }
                } finally {
                    if (evidencias != null) {
                        EscenarioGestor.JSON.writerWithDefaultPrettyPrinter().writeValue(
                                evidencias.resolve("peticiones-y-respuestas.json").toFile(),
                                Map.of("requests", peticiones, "responses", respuestas, "pageErrors", erroresPagina));
                        EscenarioGestor.JSON.writeValue(evidencias.resolve("limpieza.json").toFile(),
                                app == null ? Map.of() : app.limpieza);
                    }
                }
            } finally {
                try {
                    if (contexto != null) {
                        contexto.close();
                    }
                } finally {
                    if (api != null) {
                        api.dispose();
                    }
                    app = null;
                    pagina = null;
                    contexto = null;
                    api = null;
                }
            }
        }
    }

    @AfterAll
    void cerrarNavegador() {
        if (playwright != null) {
            playwright.close();
        }
    }

    protected Locator boton(String nombre) {
        return pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(nombre).setExact(true));
    }

    protected Locator tarjeta() {
        return pagina.locator("#cardContainer .card").filter(new Locator.FilterOptions().setHas(
                pagina.getByRole(AriaRole.HEADING,
                        new Page.GetByRoleOptions().setName(app.prefijo).setExact(true))));
    }

    protected void abrirTablero() {
        var respuesta = pagina.waitForResponse(r -> ruta(r.url()).equals("/obtenerTareas"),
                new Page.WaitForResponseOptions().setTimeout(20000),
                () -> pagina.navigate("/proyecto?id_proyecto=" + EscenarioGestor.entero(app.proyecto, "id_proyecto")));
        assertTrue(respuesta.ok());
        assertThat(pagina.locator("#tareasPendientes").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(EscenarioGestor.texto(app.tarea, "nombre")).setExact(true)))
                .isVisible();
    }

    protected void abrirTarea() {
        var respuesta = pagina.waitForResponse(r -> ruta(r.url()).equals("/obtenerComentarios"),
                new Page.WaitForResponseOptions().setTimeout(20000),
                () -> boton(EscenarioGestor.texto(app.tarea, "nombre")).first().click());
        assertTrue(respuesta.ok());
        assertThat(pagina.locator("#editarTarea")).isVisible();
    }

    protected void abrirEdicionProyecto() {
        pagina.navigate("/inicio");
        assertThat(tarjeta()).isVisible();
        tarjeta().locator("[data-bs-toggle=\"dropdown\"]").click();
        tarjeta().getByText("Editar", new Locator.GetByTextOptions().setExact(true)).click();
    }

    protected void eliminarTarea() {
        pagina.locator("#tareasPendientes li").filter(new Locator.FilterOptions().setHas(
                boton(EscenarioGestor.texto(app.tarea, "nombre")))).locator("button").last().click();
        aceptarEliminacion();
    }

    protected void eliminarProyecto() {
        tarjeta().locator("[data-bs-toggle=\"dropdown\"]").click();
        tarjeta().getByText("Eliminar", new Locator.GetByTextOptions().setExact(true)).click();
        aceptarEliminacion();
    }

    private void aceptarEliminacion() {
        assertThat(pagina.locator("#modalAlerta")).isVisible();
        pagina.locator("#modalAlerta").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Aceptar").setExact(true)).click();
    }

    protected void rellenarRegistro(JsonNode usuario) {
        pagina.locator("input[name=\"nombre\"]").fill(EscenarioGestor.texto(usuario, "nombre"));
        pagina.locator("input[name=\"correo_electronico\"]").fill(EscenarioGestor.texto(usuario, "correo_electronico"));
        pagina.locator("input[name=\"contrasena\"]").fill(EscenarioGestor.texto(usuario, "contrasena"));
        pagina.locator("select[name=\"rol\"]").selectOption("Estudiante");
    }

    protected void rellenarProyecto(String nombre) {
        pagina.locator("#staticBackdrop #nombre").fill(nombre);
        pagina.locator("#staticBackdrop textarea[name=\"descripcion\"]").fill("Proyecto creado por la prueba automática");
        pagina.locator("#fecha_inicio").fill("2026-10-11");
        pagina.locator("#fecha_finalizacion").fill("2026-10-18");
        pagina.locator("#estado_proyecto").selectOption("En proceso");
    }

    protected void simularError(String endpoint, int estado) {
        pagina.route("**/" + endpoint, ruta -> ruta.fulfill(new com.microsoft.playwright.Route.FulfillOptions()
                .setStatus(estado).setContentType("application/json")
                .setBody("{\"status\":" + estado + ",\"error\":\"Fallo simulado por Playwright\"}")));
    }

    protected long envios(String endpoint) {
        return peticiones.stream().filter(p -> p.get("ruta").equals("/" + endpoint)).count();
    }

    protected static String json(Object dato) {
        try {
            return EscenarioGestor.JSON.writeValueAsString(dato);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    protected static String ruta(String url) {
        return URI.create(url).getPath();
    }

    private static boolean esPostDelCliente(String url, String metodo, String frontend) {
        URI destino = URI.create(url);
        URI origen = URI.create(frontend);
        return metodo.equals("POST") && destino.getScheme().equals(origen.getScheme())
                && destino.getAuthority().equals(origen.getAuthority());
    }

    private static String opcion(String propiedad, String variable, String defecto) {
        return System.getProperty(propiedad, System.getenv().getOrDefault(variable, defecto));
    }

    public static class Resultado implements AfterTestExecutionCallback {
        @Override
        public void afterTestExecution(ExtensionContext contexto) {
            ((PruebaNavegador) contexto.getRequiredTestInstance()).fallo =
                    contexto.getExecutionException().isPresent();
        }
    }
}
