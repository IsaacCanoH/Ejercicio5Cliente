package com.mx.cbtis.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ComentariosTest {

    private static final String JSON_DATOS = """
            {
                "id_comentario": 17,
                "id_usuario": "Ana",
                "id_tarea": 8,
                "texto_comentario": "Comentario de prueba",
                "fecha_hora": "2026-10-25 12:00:00"
            }
            """;

    private static final String JSON_NULOS = """
            {
                "id_comentario": null,
                "id_usuario": null,
                "id_tarea": null,
                "texto_comentario": null,
                "fecha_hora": null
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializaElConstructorConLosNombresYValoresDelContrato() throws Exception {
        Comentarios dato = new Comentarios(17, "Ana", 8, "Comentario de prueba", "2026-10-25 12:00:00");

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void deserializaElJsonConservandoTodosLosCampos() throws Exception {
        Comentarios dato = mapper.readValue(JSON_DATOS, Comentarios.class);

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void permiteRestablecerLosCamposANullAlDeserializar() throws Exception {
        Comentarios dato = new Comentarios(17, "Ana", 8, "Comentario de prueba", "2026-10-25 12:00:00");

        mapper.readerForUpdating(dato).readValue(JSON_NULOS);

        assertEquals(mapper.readTree(JSON_NULOS), mapper.valueToTree(dato));
    }
}

