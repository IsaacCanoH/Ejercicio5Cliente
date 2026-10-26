package com.mx.cbtis.controller;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mx.cbtis.modelo.Comentarios;
import com.mx.cbtis.modelo.Proyectos;
import com.mx.cbtis.modelo.Tareas;
import com.mx.cbtis.modelo.Usuario;

class GestorControllerTest {

    private static final String API = "http://localhost:8081";

    private static final String JSON_USUARIO = """
            {
                "id_usuario": 17,
                "nombre": "Ana",
                "correo_electronico": "ana@example.com",
                "contrasena": "clave-prueba",
                "rol": "Estudiante",
                "imagen_usuario": "#FFFFFF"
            }
            """;

    private static final String JSON_PROYECTOS = """
            {
                "id_proyecto": 17,
                "nombre": "Proyecto de prueba",
                "descripcion": "Descripcion",
                "fecha_inicio": "2026-10-25",
                "fecha_finalizacion": "2026-10-30",
                "estado_proyecto": "En proceso",
                "fondo": "#FFFFFF"
            }
            """;

    private static final String JSON_TAREAS = """
            {
                "id_tarea": 17,
                "nombre": "Tarea de prueba",
                "descripcion": "Descripcion",
                "fecha_vencimiento": "2026-10-30",
                "lista": "pendientes",
                "estado": "primary",
                "id_proyecto": 8
            }
            """;

    private static final String JSON_COMENTARIOS = """
            {
                "id_comentario": 17,
                "id_usuario": "Ana",
                "id_tarea": 8,
                "texto_comentario": "Comentario de prueba",
                "fecha_hora": "2026-10-25 12:00:00"
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        // Las vistas se comprueban por nombre y modelo; el HTML real lo validan los IT.
        mvc = MockMvcBuilders.standaloneSetup(new GestorController())
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
    }

    @ParameterizedTest
    @CsvSource({"/, home", "/tutorial, tutorial", "/manual, manual", "/login, login"})
    void devuelveLaVistaDeLaRuta(String ruta, String vista) throws Exception {
        mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(view().name(vista));
    }

    static Stream<Arguments> formularios() {
        return Stream.of(
                Arguments.of("/register", "register", "usuario", Usuario.class),
                Arguments.of("/inicio", "inicio", "proyectos", Proyectos.class),
                Arguments.of("/tarea", "proyecto", "tareas", Tareas.class));
    }

    @ParameterizedTest
    @MethodSource("formularios")
    void preparaElModeloDeLaVista(String ruta, String vista, String atributo, Class<?> tipo) throws Exception {
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class)) {
            mvc.perform(get(ruta))
                    .andExpect(status().isOk())
                    .andExpect(view().name(vista))
                    .andExpect(model().attribute(atributo, instanceOf(tipo)));

            construccion.constructed().forEach(template -> verifyNoInteractions(template));
        }
    }

    @Test
    void preparaElProyectoConTareaComentarioYUsuarios() throws Exception {
        Usuario usuario = mapper.readValue(JSON_USUARIO, Usuario.class);
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class,
                (template, contexto) -> doReturn(new Usuario[] {usuario}).when(template)
                        .postForObject(eq(API + "/buscarUsuarios"), any(HttpHeaders.class), eq(Usuario[].class)))) {

            var resultado = mvc.perform(get("/proyecto"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("proyecto"))
                    .andExpect(model().attribute("tarea", instanceOf(Tareas.class)))
                    .andExpect(model().attribute("comentarios", instanceOf(Comentarios.class)))
                    .andReturn();

            assertEquals(mapper.readTree("[" + JSON_USUARIO + "]"),
                    mapper.valueToTree(resultado.getModelAndView().getModel().get("listaUsuarios")));
            RestTemplate template = unicoTemplate(construccion);
            verify(template).postForObject(eq(API + "/buscarUsuarios"), any(HttpHeaders.class), eq(Usuario[].class));
            verifyNoMoreInteractions(template);
        }
    }

    static Stream<Arguments> listados() {
        return Stream.of(
                Arguments.of("/consultarProyectos", "/buscarProyectos", Proyectos.class, false),
                Arguments.of("/consultarProyectos", "/buscarProyectos", Proyectos.class, true),
                Arguments.of("/consultarUsuarios", "/buscarUsuarios", Usuario.class, false),
                Arguments.of("/consultarUsuarios", "/buscarUsuarios", Usuario.class, true),
                Arguments.of("/consultarTareas", "/buscarTareas", Tareas.class, false),
                Arguments.of("/consultarTareas", "/buscarTareas", Tareas.class, true),
                Arguments.of("/consultarComentarios", "/buscarComentarios", Comentarios.class, false),
                Arguments.of("/consultarComentarios", "/buscarComentarios", Comentarios.class, true));
    }

    @ParameterizedTest
    @MethodSource("listados")
    void devuelveElListadoDeLaApiInclusoSiEstaVacio(
            String ruta, String backend, Class<?> tipo, boolean vacio) throws Exception {
        Object respuesta = arreglo(tipo, vacio);
        String esperado = vacio ? "[]" : "[" + json(tipo) + "]";
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class,
                (template, contexto) -> doReturn(respuesta).when(template)
                        .postForObject(eq(API + backend), any(HttpHeaders.class), eq(respuesta.getClass())))) {

            mvc.perform(post(ruta))
                    .andExpect(status().isOk())
                    .andExpect(content().json(esperado, true));

            RestTemplate template = unicoTemplate(construccion);
            ArgumentCaptor<HttpHeaders> cabeceras = ArgumentCaptor.forClass(HttpHeaders.class);
            verify(template).postForObject(eq(API + backend), cabeceras.capture(), eq(respuesta.getClass()));
            assertEquals(MediaType.APPLICATION_JSON, cabeceras.getValue().getContentType());
            verifyNoMoreInteractions(template);
        }
    }

    static Stream<Arguments> filtros() {
        return Stream.of(
                Arguments.of("/obtenerTareas", "/getTarea", Tareas.class, "id_proyecto", false),
                Arguments.of("/obtenerTareas", "/getTarea", Tareas.class, "id_proyecto", true),
                Arguments.of("/obtenerComentarios", "/getComentario", Comentarios.class, "id_tarea", false),
                Arguments.of("/obtenerComentarios", "/getComentario", Comentarios.class, "id_tarea", true));
    }

    @ParameterizedTest
    @MethodSource("filtros")
    void enviaLaRelacionAlBackendYDevuelveSusResultados(
            String ruta, String backend, Class<?> tipo, String campo, boolean vacio) throws Exception {
        Object respuesta = arreglo(tipo, vacio);
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class,
                (template, contexto) -> doReturn(respuesta).when(template)
                        .postForObject(eq(API + backend), any(Object.class), eq(respuesta.getClass())))) {

            mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"" + campo + "\":8}"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(vacio ? "[]" : "[" + json(tipo) + "]", true));

            RestTemplate template = unicoTemplate(construccion);
            ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
            verify(template).postForObject(eq(API + backend), cuerpo.capture(), eq(respuesta.getClass()));
            assertInstanceOf(tipo, cuerpo.getValue());
            JsonNode recibido = mapper.valueToTree(cuerpo.getValue());
            assertEquals(8, recibido.get(campo).intValue());
            verifyNoMoreInteractions(template);
        }
    }

    static Stream<Arguments> consultasPorId() {
        return Stream.of(
                Arguments.of("/actualizarProyecto", "/buscarProyecto", Proyectos.class, "id_proyecto"),
                Arguments.of("/actualizarTarea", "/buscarTarea", Tareas.class, "id_tarea"));
    }

    @ParameterizedTest
    @MethodSource("consultasPorId")
    void consultaElIdentificadorCorrectoParaEditar(
            String ruta, String backend, Class<?> tipo, String campo) throws Exception {
        Object respuesta = mapper.readValue(json(tipo), tipo);
        String url = API + backend + "?" + campo + "={" + campo + "}";
        Map<String, Integer> parametros = Map.of(campo, 17);
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class,
                (template, contexto) -> doReturn(respuesta).when(template)
                        .getForObject(eq(url), eq(tipo), eq(parametros)))) {

            mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"" + campo + "\":17}"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(json(tipo), true));

            RestTemplate template = unicoTemplate(construccion);
            verify(template).getForObject(eq(url), eq(tipo), eq(parametros));
            verifyNoMoreInteractions(template);
        }
    }

    static Stream<Arguments> altasDeFormulario() {
        return Stream.of(
                Arguments.of("/saveUsuario", "/addUsuario", Usuario.class, "/login"),
                Arguments.of("/saveProyecto", "/addProyecto", Proyectos.class, "/inicio"),
                Arguments.of("/saveTarea", "/addTareas", Tareas.class, "/proyecto?id_proyecto=8"));
    }

    @ParameterizedTest
    @MethodSource("altasDeFormulario")
    void enlazaElFormularioEnviaSusDatosYRedirige(
            String ruta, String backend, Class<?> tipo, String redireccion) throws Exception {
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class,
                (template, contexto) -> doReturn(ResponseEntity.ok(1)).when(template)
                        .postForEntity(eq(API + backend), any(Object.class), eq(Integer.class)))) {

            var solicitud = post(ruta).contentType(MediaType.APPLICATION_FORM_URLENCODED);
            mapper.readTree(json(tipo)).fields()
                    .forEachRemaining(campo -> solicitud.param(campo.getKey(), campo.getValue().asText()));

            mvc.perform(solicitud)
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(redireccion));

            RestTemplate template = unicoTemplate(construccion);
            ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
            verify(template).postForEntity(eq(API + backend), cuerpo.capture(), eq(Integer.class));
            assertInstanceOf(tipo, cuerpo.getValue());
            assertEquals(mapper.readTree(json(tipo)), mapper.valueToTree(cuerpo.getValue()));
            verifyNoMoreInteractions(template);
        }
    }

    static Stream<Arguments> enviosJson() {
        return Stream.of(
                Arguments.of("/verificarLogin", "/getUsuarioLogin", Usuario.class, Usuario.class),
                Arguments.of("/saveComentario", "/addComentario", Comentarios.class, Integer.class),
                Arguments.of("/verificarTarea", "/addTareas", Tareas.class, Integer.class),
                Arguments.of("/cambiosTareas", "/addTareas", Tareas.class, Integer.class));
    }

    @ParameterizedTest
    @MethodSource("enviosJson")
    void transmiteElJsonYConservaElEstadoYLaRespuestaDelBackend(
            String ruta, String backend, Class<?> entrada, Class<?> salida) throws Exception {
        Object respuesta = salida == Integer.class ? 1 : mapper.readValue(json(salida), salida);
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class,
                (template, contexto) -> doReturn(ResponseEntity.accepted().body(respuesta)).when(template)
                        .postForEntity(eq(API + backend), any(Object.class), eq(salida)))) {

            mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json(entrada)))
                    .andExpect(status().isAccepted())
                    .andExpect(content().json(salida == Integer.class ? "1" : json(salida), true));

            RestTemplate template = unicoTemplate(construccion);
            ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
            verify(template).postForEntity(eq(API + backend), cuerpo.capture(), eq(salida));
            assertInstanceOf(entrada, cuerpo.getValue());
            assertEquals(mapper.readTree(json(entrada)), mapper.valueToTree(cuerpo.getValue()));
            verifyNoMoreInteractions(template);
        }
    }

    static Stream<Arguments> eliminaciones() {
        return Stream.of(
                Arguments.of("/eliminarProyecto", "/deleteProyecto", Proyectos.class, "/inicio"),
                Arguments.of("/eliminarTarea", "/deleteTareas", Tareas.class, "/proyecto"));
    }

    @ParameterizedTest
    @MethodSource("eliminaciones")
    void eliminaConDeleteYConservaLosDatosDeLaSolicitud(
            String ruta, String backend, Class<?> tipo, String redireccion) throws Exception {
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class,
                (template, contexto) -> doReturn(ResponseEntity.ok(1)).when(template)
                        .exchange(eq(API + backend), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Integer.class)))) {

            mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json(tipo)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(redireccion));

            RestTemplate template = unicoTemplate(construccion);
            ArgumentCaptor<HttpEntity> solicitud = ArgumentCaptor.forClass(HttpEntity.class);
            verify(template).exchange(eq(API + backend), eq(HttpMethod.DELETE), solicitud.capture(), eq(Integer.class));
            assertInstanceOf(tipo, solicitud.getValue().getBody());
            assertEquals(mapper.readTree(json(tipo)), mapper.valueToTree(solicitud.getValue().getBody()));
            verifyNoMoreInteractions(template);
        }
    }

    @Test
    void propagaElFalloDeConexionSinSimularUnGuardadoExitoso() throws Exception {
        ResourceAccessException error = new ResourceAccessException("API no disponible");
        try (MockedConstruction<RestTemplate> construccion = mockConstruction(RestTemplate.class,
                (template, contexto) -> doThrow(error).when(template)
                        .postForEntity(eq(API + "/addTareas"), any(Object.class), eq(Integer.class)))) {

            ServletException resultado = assertThrows(ServletException.class,
                    () -> mvc.perform(post("/cambiosTareas").contentType(MediaType.APPLICATION_JSON)
                            .content(JSON_TAREAS)));

            assertEquals(error, resultado.getCause());
            RestTemplate template = unicoTemplate(construccion);
            verify(template).postForEntity(eq(API + "/addTareas"), any(Tareas.class), eq(Integer.class));
            verifyNoMoreInteractions(template);
        }
    }

    private RestTemplate unicoTemplate(MockedConstruction<RestTemplate> construccion) {
        assertEquals(1, construccion.constructed().size());
        return construccion.constructed().get(0);
    }

    private Object arreglo(Class<?> tipo, boolean vacio) throws Exception {
        Object resultado = Array.newInstance(tipo, vacio ? 0 : 1);
        if (!vacio) {
            Array.set(resultado, 0, mapper.readValue(json(tipo), tipo));
        }
        return resultado;
    }

    private String json(Class<?> tipo) {
        return Map.of(
                Usuario.class, JSON_USUARIO,
                Proyectos.class, JSON_PROYECTOS,
                Tareas.class, JSON_TAREAS,
                Comentarios.class, JSON_COMENTARIOS).get(tipo);
    }
}

