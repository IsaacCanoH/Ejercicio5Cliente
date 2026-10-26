package com.mx.cbtis.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AsignacionesTest {

    private static final String JSON_DATOS = """
            {
                "id_asignacion": 17,
                "id_usuario": 4,
                "id_tarea": 8,
                "fecha_asignacion": "2026-10-25"
            }
            """;

    private static final String JSON_NULOS = """
            {
                "id_asignacion": null,
                "id_usuario": null,
                "id_tarea": null,
                "fecha_asignacion": null
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializaElConstructorConLosNombresYValoresDelContrato() throws Exception {
        Asignaciones dato = new Asignaciones(17, 4, 8, "2026-10-25");

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void deserializaElJsonConservandoTodosLosCampos() throws Exception {
        Asignaciones dato = mapper.readValue(JSON_DATOS, Asignaciones.class);

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void permiteRestablecerLosCamposANullAlDeserializar() throws Exception {
        Asignaciones dato = new Asignaciones(17, 4, 8, "2026-10-25");

        mapper.readerForUpdating(dato).readValue(JSON_NULOS);

        assertEquals(mapper.readTree(JSON_NULOS), mapper.valueToTree(dato));
    }
}

