package com.mx.cbtis.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class ProyectosTest {

    private static final String JSON_DATOS = """
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

    private static final String JSON_NULOS = """
            {
                "id_proyecto": null,
                "nombre": null,
                "descripcion": null,
                "fecha_inicio": null,
                "fecha_finalizacion": null,
                "estado_proyecto": null,
                "fondo": null
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializaElConstructorConLosNombresYValoresDelContrato() throws Exception {
        Proyectos dato = new Proyectos(17, "Proyecto de prueba", "Descripcion", "2026-10-25", "2026-10-30", "En proceso", "#FFFFFF");

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void deserializaElJsonConservandoTodosLosCampos() throws Exception {
        Proyectos dato = mapper.readValue(JSON_DATOS, Proyectos.class);

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void permiteRestablecerLosCamposANullAlDeserializar() throws Exception {
        Proyectos dato = new Proyectos(17, "Proyecto de prueba", "Descripcion", "2026-10-25", "2026-10-30", "En proceso", "#FFFFFF");

        mapper.readerForUpdating(dato).readValue(JSON_NULOS);

        assertEquals(mapper.readTree(JSON_NULOS), mapper.valueToTree(dato));
    }
}

