package com.mx.cbtis.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class UsuarioTest {

    private static final String JSON_DATOS = """
            {
                "id_usuario": 17,
                "nombre": "Ana",
                "correo_electronico": "ana@example.com",
                "contrasena": "clave-prueba",
                "rol": "Estudiante",
                "imagen_usuario": "#FFFFFF"
            }
            """;

    private static final String JSON_NULOS = """
            {
                "id_usuario": null,
                "nombre": null,
                "correo_electronico": null,
                "contrasena": null,
                "rol": null,
                "imagen_usuario": null
            }
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializaElConstructorConLosNombresYValoresDelContrato() throws Exception {
        Usuario dato = new Usuario(17, "Ana", "ana@example.com", "clave-prueba", "Estudiante", "#FFFFFF");

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void deserializaElJsonConservandoTodosLosCampos() throws Exception {
        Usuario dato = mapper.readValue(JSON_DATOS, Usuario.class);

        assertEquals(mapper.readTree(JSON_DATOS), mapper.valueToTree(dato));
    }

    @Test
    void permiteRestablecerLosCamposANullAlDeserializar() throws Exception {
        Usuario dato = new Usuario(17, "Ana", "ana@example.com", "clave-prueba", "Estudiante", "#FFFFFF");

        mapper.readerForUpdating(dato).readValue(JSON_NULOS);

        assertEquals(mapper.readTree(JSON_NULOS), mapper.valueToTree(dato));
    }
}

