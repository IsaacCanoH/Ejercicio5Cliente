package com.mx.cbtis.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class TareasTest {

    private static final String JSON_DATOS = """
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

    private static final String JSON_NULOS = """
            {
                "id_tarea": null,
                "nombre": null,
                "descripcion": null,
                "fecha_vencimiento": null,
                "lista": null,
                "estado": null,
                "id_proyecto": null
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializaElConstructorConLosNombresYValoresDelContrato() throws Exception {
        Tareas dato = new Tareas(17, "Tarea de prueba", "Descripcion", "2026-10-30", "pendientes", "primary", 8);

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void deserializaElJsonConservandoTodosLosCampos() throws Exception {
        Tareas dato = mapper.readValue(JSON_DATOS, Tareas.class);

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void permiteRestablecerLosCamposANullAlDeserializar() throws Exception {
        Tareas dato = new Tareas(17, "Tarea de prueba", "Descripcion", "2026-10-30", "pendientes", "primary", 8);

        mapper.readerForUpdating(dato).readValue(JSON_NULOS);

        assertEquals(mapper.readTree(JSON_NULOS), mapper.valueToTree(dato));
    }
}

